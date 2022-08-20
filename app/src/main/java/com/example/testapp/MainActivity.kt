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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.testapp.minima.MDS
import com.example.testapp.minima.getAddress
import com.example.testapp.ui.theme.TestAppTheme
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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

  var address by mutableStateOf("")
  var tokenId by mutableStateOf("0x00")
  var amount by mutableStateOf(ZERO)

  var cardReader: CardReader = CardReader(this)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      TestAppTheme {
        // A surface container using the 'background' color from the theme
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
          View(address, amount, isReaderModeOn, { scope.launch { emitAmount(it) } }, this::enableReaderMode )
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

  private suspend fun startEmitting(amount: BigDecimal) {
    Log.i(TAG, "Disabling reader mode")
    Toast.makeText(this, "Disabling reader mode", Toast.LENGTH_LONG).show()
    val activity: Activity = this
    NfcAdapter.getDefaultAdapter(activity)?.disableReaderMode(activity)
    address = getAddress()
    sendDataToService("$address;0x00;${amount.toPlainString()}")
    isReaderModeOn = false
  }

  private suspend fun emitAmount(amount: BigDecimal) {
    Log.i(TAG, "Update amount")
    this.amount = amount
    if (isReaderModeOn) startEmitting(amount)
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
fun OutlinedNumberField(value: BigDecimal, enabled: Boolean, setValue: (BigDecimal) -> Unit) {
  var text by remember { mutableStateOf(value.toPlainString()) }
  value.takeUnless { it == text.toBigDecimalOrNull() || it == ZERO && text.isEmpty()} ?.let { text = it.toPlainString() }
  OutlinedTextField(
    value = text,
    onValueChange = {
      if (it.isEmpty()) setValue(ZERO)
      else it.toBigDecimalOrNull()?.let{ setValue(it) }
      text = it
    },
    enabled = enabled,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
  )
}

@Composable
fun View(address: String, amount: BigDecimal, isReaderMode: Boolean, startEmitting: (BigDecimal) -> Unit, stopEmitting: () -> Unit) {
  val scope = rememberCoroutineScope()

  var uid by remember { mutableStateOf("") }
  var inited by remember { mutableStateOf(false) }

  Column {
    Row {
      Text("MiniDApp UID:")
    }
    Row {
      OutlinedTextField(value = uid,
        modifier = Modifier
          .width(400.dp)
          .padding(1.dp),
        textStyle = TextStyle(fontSize = (20.sp)),
        onValueChange = {
          Log.i(TAG, "New UID value $it")
          uid = it
        }
      )
    }
    Row {
      Button(onClick = {
        inited = false
        scope.launch {
          MDS.init(uid) {
            if (it.jsonObject["event"]?.jsonPrimitive?.content == "inited") {
              Log.i(TAG, "inited")
              inited = true
            }
          }
        }
      }){
        Text("Update")
      }
    }
    if (inited) InitedView(address, amount, setAmount = startEmitting, isReaderMode, startEmitting, stopEmitting)
  }
}

@Composable
fun InitedView(address: String, amount: BigDecimal, setAmount: (BigDecimal) -> Unit, isReaderMode: Boolean, startEmitting: (BigDecimal) -> Unit, stopEmitting: () -> Unit) {
  Column {
    Row {
      Text("Reader")
      Switch(checked = !isReaderMode, onCheckedChange = { if (isReaderMode) startEmitting(amount) else stopEmitting() })
      Text("Emitter")
    }
    OutlinedTextField(address, {}, enabled = !isReaderMode)
    OutlinedNumberField(amount, !isReaderMode, setAmount)
  }
}

@Composable
fun Messages(messages: List<NdefMessage>) {
  messages.forEach { msg ->
    Text(msg.records.joinToString { it.payload.toString() })
  }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
  TestAppTheme {
    View("test", ONE, false, {}, {})
  }
}

@Preview
@Composable
fun InitedViewConsumer() {
  TestAppTheme {
    InitedView(address = "", amount = ZERO, {}, isReaderMode = true, {}, {})
  }
}

@Preview
@Composable
fun InitedViewProducer() {
  TestAppTheme {
    InitedView(address = "0x1234567890", amount = ONE, {}, isReaderMode = false, {}, {})
  }
}