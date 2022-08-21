package com.example.testapp

import android.app.Activity
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.testapp.minima.getAddress
import com.example.testapp.minima.initMDS
import com.example.testapp.minima.inited
import com.example.testapp.ui.View
import com.example.testapp.ui.theme.TestAppTheme
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.BigDecimal.ONE
import java.math.BigDecimal.ZERO


// Recommend NfcAdapter flags for reading from other Android devices. Indicates that this
// activity is interested in NFC-A devices (including other Android devices), and that the
// system should not check for the presence of NDEF-formatted data (e.g. Android Beam).
val READER_FLAGS = NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK

const val TAG = "MainActivity"

val scope = MainScope()

class MainActivity : ComponentActivity(),
  CardReader.DataCallback {
//  val nfcMessages = mutableStateListOf<NdefMessage>()
  var isReaderModeOn by mutableStateOf(true)

  var uid by mutableStateOf("")
  var address by mutableStateOf("")
  var tokenId by mutableStateOf("0x00")
  var amount by mutableStateOf(ZERO)

  var cardReader: CardReader = CardReader(this)

  fun init(uid: String, host: String = "localhost", port: Int = 9004) {
    this.uid = uid
    scope.launch {
      initMDS(uid, host, port)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    intent?.data?.let{ uri ->
      val action = uri.path
      uri.getQueryParameter("uid")?.let { init(it, uri.host ?: "localhost", uri.port) }
      if (action == "/emit") {
        uri.getQueryParameter("address")?.let { address = it }
        uri.getQueryParameter("token")?.let { tokenId = it }
        uri.getQueryParameter("amount")?.toBigDecimalOrNull()?.let{ amount = it }
        scope.launch { startEmitting() }
      }
      Log.i(TAG, "action: $action")
    }
    setContent {
      TestAppTheme {
        // A surface container using the 'background' color from the theme
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
          View(inited, uid, ::init, address, amount, tokenId, { tokenId = it }, isReaderModeOn, { scope.launch { emitAmount(it) } }, this::enableReaderMode )
        }
      }
    }

    val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
    if (nfcAdapter == null) {
      Toast.makeText(this, "NFC is not available", Toast.LENGTH_LONG).show()
      System.err.println("NFC is not available")
//      finish()
      return
    }

//    if (nfcAdapter.isNdefPushEnabled()) {
//      nfcAdapter.setNdefPushMessage(createNdefMessage(), this)
//    } else {
//      Toast.makeText(this, "NDEF push not enabled", Toast.LENGTH_LONG).show()
//
//    }
  }

//  override fun onPause() {
//    super.onPause()
//    scope.launch {
//      startEmitting()
//    }
//  }
//
//  override fun onResume() {
//    super.onResume()
//    enableReaderMode()
//  }

  fun sendDataToService(data: String) {
    val intent = Intent(applicationContext, CardService::class.java)
    intent.putExtra("data", data)
    applicationContext.startService(intent)
  }
//
//  override fun onNewIntent(intent: Intent) {
//    super.onNewIntent(intent)
//
//    if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
//      Toast.makeText(this, "NDEF Discovered!", Toast.LENGTH_LONG).show()
//      System.err.println("NDEF Discovered!")
//      intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)?.also { rawMessages ->
//        val messages: List<NdefMessage> = rawMessages.map { it as NdefMessage }
//        nfcMessages += messages
//      }
//    }
//  }

  private fun enableReaderMode() {
    Log.i(TAG, "Enabling reader mode")
    Toast.makeText(this, "Enabling reader mode", Toast.LENGTH_LONG).show()
    address = ""
    amount = ZERO

    val activity: Activity = this
    val nfc = NfcAdapter.getDefaultAdapter(activity)
    nfc?.enableReaderMode(
      activity,
      cardReader,
      READER_FLAGS,
      null
    )
    isReaderModeOn = true
  }

  private fun startEmitting() {
    Log.i(TAG, "Disabling reader mode")
    Toast.makeText(this, "Disabling reader mode", Toast.LENGTH_LONG).show()
    isReaderModeOn = false
    val activity: Activity = this
    NfcAdapter.getDefaultAdapter(activity)?.disableReaderMode(activity)
    sendDataToService("$address;0x00;${amount.toPlainString()}")
  }

  private suspend fun emitAmount(amount: BigDecimal) {
    Log.i(TAG, "Update amount")
    this.amount = amount
    if (isReaderModeOn) {
      address = getAddress()
      startEmitting()
    }
    else sendDataToService("$address;0x00;${amount.toPlainString()}")
  }

  override fun onDataReceived(data: String) {
    Log.i(TAG, "data received $data")
    val splits = data.split(";")
    this.address = splits[0]
    if (splits.size > 1) this.tokenId = splits[1]
    if (splits.size > 2) this.amount = splits[2].toBigDecimal()
  }
}

//fun createTextRecord(payload: String, locale: Locale, encodeInUtf8: Boolean): NdefRecord {
//  val langBytes = locale.language.toByteArray(Charset.forName("US-ASCII"))
//  val utfEncoding = if (encodeInUtf8) Charset.forName("UTF-8") else Charset.forName("UTF-16")
//  val textBytes = payload.toByteArray(utfEncoding)
//  val utfBit: Int = if (encodeInUtf8) 0 else 1 shl 7
//  val status = (utfBit + langBytes.size).toChar()
//  val data = ByteArray(1 + langBytes.size + textBytes.size)
//  data[0] = status.toByte()
//  System.arraycopy(langBytes, 0, data, 1, langBytes.size)
//  System.arraycopy(textBytes, 0, data, 1 + langBytes.size, textBytes.size)
//  return NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, ByteArray(0), data)
//}

//fun createNdefMessage(): NdefMessage {
//  val text = "Beam me up, Android!"
//  return NdefMessage(createTextRecord(text, Locale.UK, true))
//}

@Composable
fun Messages(messages: List<NdefMessage>) {
  messages.forEach { msg ->
    Text(msg.records.joinToString { it.payload.toString() })
  }
}

@Preview(showBackground = true)
@Composable
fun ViewConsumer() {
  TestAppTheme {
    View(true, "uid", {}, "", ZERO, "0x00", {}, true, {}, {})
  }
}

@Preview(showBackground = true)
@Composable
fun ViewEmitter() {
  TestAppTheme {
    View(true, "uid", {}, "address", ONE, "0x00", {}, false, {}, {})
  }
}