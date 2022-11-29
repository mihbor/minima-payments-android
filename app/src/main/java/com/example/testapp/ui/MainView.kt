package com.example.testapp.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.testapp.*
import com.example.testapp.ui.theme.TestAppTheme
import com.google.zxing.BarcodeFormat
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.ionspin.kotlin.bignum.decimal.BigDecimal.Companion.ZERO
import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import ltd.mbor.minimak.Balance

fun encodeAsBitmap(str: String): Bitmap {
  val writer = QRCodeWriter()
  val bitMatrix: BitMatrix = writer.encode(str, BarcodeFormat.QR_CODE, 800, 800)
  val w: Int = bitMatrix.getWidth()
  val h: Int = bitMatrix.getHeight()
  val pixels = IntArray(w * h)
  for (y in 0 until h) {
    for (x in 0 until w) {
      pixels[y * w + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
    }
  }
  val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
  bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
  return bitmap
}

@Composable
fun MainView(
  inited: Boolean,
  uid: String,
  setUid: (String) -> Unit,
  balances: Map<String, Balance>,
  address: String,
  setAddress: (String) -> Unit,
  amount: BigDecimal,
  setAmount: (BigDecimal?) -> Unit,
  tokenId: String,
  setTokenId: (String) -> Unit,
  isSending: Boolean,
  startEmitting: () -> Unit,
  stopEmitting: () -> Unit,
  setRequestSentOnChannel: (ChannelState) -> Unit,
  activity: MainActivity?
) {
  var uidInput by remember { mutableStateOf(uid) }
  var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
  if (address.isNotBlank()) {
    bitmap = encodeAsBitmap("$address;${amount.toPlainString()};$tokenId").asImageBitmap()
  }
  val context = LocalContext.current

  Column {
    Row {
      Text("MiniDApp UID:")
    }
    Row {
      OutlinedTextField(value = uidInput,
        modifier = Modifier.fillMaxWidth(),
//        textStyle = TextStyle(fontSize = (16.sp)),
        onValueChange = { uidInput = it }
      )
    }
    Row {
      Button(onClick = {
        setUid(uidInput)
      }){
        Text("Update")
      }
    }
    if (inited) {
      Row {
        Text("Scan")
        Switch(checked = !isSending, onCheckedChange = { if (isSending) startEmitting() else stopEmitting() })
        Text("Emit")
      }
      OutlinedTextField(address, setAddress, enabled = true, modifier = Modifier.fillMaxWidth())
      TokenSelect(true, balances, tokenId, setTokenId)
      Row {
        Log.i(TAG, "amount in MainView: $amount")
        DecimalNumberField(amount, enabled = true, setValue = setAmount)
        if (isSending) {
          var sending by remember { mutableStateOf(false) }
          Button(
            enabled = !sending && address.isNotBlank() && amount > ZERO && balances[tokenId]?.sendable?.let{ it >= amount } ?: false,
            onClick = {
              sending = true
              scope.launch {
                val success = send(address, amount, tokenId)
                sending = false
                Toast.makeText(context, "Sending result: $success", Toast.LENGTH_LONG).show()
                if (success) {
                  setAmount(ZERO)
                }
              }
            }
          ) {
            Text("Send!")
          }
        }
      }
      Row{
      if (isSending){
        val scanLauncher = rememberLauncherForActivityResult(
          contract = ScanContract(),
          onResult = { result ->
            Log.i(TAG, "scanned code: ${result.contents}")
            result.contents.split(";").apply {
              setAddress(getOrNull(0) ?: "")
              setTokenId(getOrNull(1) ?: "")
              setAmount(getOrNull(2)?.toBigDecimal())
            }
          }
        )
        Button(onClick = {
          scanLauncher.launch(ScanOptions().apply {
            setOrientationLocked(false)
            setPrompt("")
            setBeepEnabled(false)
          })
        }) {
          Text(text = "Scan QR")
        }
      } else {
          bitmap?.let{ Image(bitmap = it, contentDescription = "Scan this QR code") }
        }
      }
      Row {
        ChannelListing(activity, setRequestSentOnChannel)
      }
    }
  }
}

private val previewBalances = listOf(
  Balance("0x00", JsonPrimitive(null), BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, "1"),
  Balance("0x01234567890", JsonPrimitive("test"), BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, "1"),
).associateBy { it.tokenId }

@Preview(showBackground = true)
@Composable
fun ViewConsumer() {
  TestAppTheme {
    MainView(true, "uid123", {}, previewBalances, "", {}, ZERO, {}, "0x00", {}, true, {}, {}, {}, null)
  }
}

@Preview(showBackground = true)
@Composable
fun ViewEmitter() {
  TestAppTheme {
    MainView(true, "uid456", {}, previewBalances, "address", {}, BigDecimal.ONE, {}, "0x01234567890", {}, false, {}, {}, {}, null)
  }
}