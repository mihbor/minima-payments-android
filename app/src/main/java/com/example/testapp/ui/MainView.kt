package com.example.testapp.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.testapp.*
import com.example.testapp.ui.theme.TestAppTheme
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.ionspin.kotlin.bignum.decimal.BigDecimal.Companion.ZERO
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import ltd.mbor.minimak.Balance

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MainView(
  inited: Boolean,
  uid: String,
  setUid: (String) -> Unit,
  balances: Map<String, Balance>,
  address: String,
  amount: BigDecimal,
  tokenId: String,
  setTokenId: (String) -> Unit,
  isReaderMode: Boolean,
  setAmount: (BigDecimal?) -> Unit,
  startEmitting: () -> Unit,
  stopEmitting: () -> Unit,
  setRequestSentOnChannel: (ChannelState) -> Unit,
  activity: MainActivity?
) {

  var uidInput by remember { mutableStateOf(uid) }
  Column {
    Row {
      Text("MiniDApp UID:")
    }
    Row {
      OutlinedTextField(value = uidInput,
        modifier = Modifier
          .width(400.dp)
          .padding(1.dp),
        textStyle = TextStyle(fontSize = (20.sp)),
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
        Switch(checked = !isReaderMode, onCheckedChange = { if (isReaderMode) startEmitting() else stopEmitting() })
        Text("Emit")
      }
      OutlinedTextField(address, {}, enabled = !isReaderMode, modifier = Modifier.fillMaxWidth())
      var expanded by remember { mutableStateOf(false) }
      ExposedDropdownMenuBox(expanded, { expanded = !expanded }) {
        OutlinedTextField(
          value = balances[tokenId]?.let { (it.tokenName ?: "Minima") + " [${it.sendable.toPlainString()}]" } ?: "",
          {},
          modifier = Modifier.fillMaxWidth(),
          readOnly = true,
          enabled = !isReaderMode,
          trailingIcon = {
            ExposedDropdownMenuDefaults.TrailingIcon(
              expanded = expanded
            )
          }
        )
        ExposedDropdownMenu(expanded, { expanded = false }) {
          balances.values.forEach {
            DropdownMenuItem(enabled = !isReaderMode && it.sendable > ZERO, onClick = {
              setTokenId(it.tokenId)
            }) {
              Text(it.tokenName ?: "Minima")
              Text(" [${it.sendable.toPlainString()}]")
            }
          }
        }
      }
      Row {
        Log.i(TAG, "amount in MainView: $amount")
        DecimalNumberField(amount, enabled = !isReaderMode, setValue = setAmount)
        if (isReaderMode) {
          val context = LocalContext.current
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
    MainView(true, "uid123", {}, previewBalances, "", ZERO, "0x00", {}, true, {}, {}, {}, {}, null)
  }
}

@Preview(showBackground = true)
@Composable
fun ViewEmitter() {
  TestAppTheme {
    MainView(true, "uid456", {}, previewBalances, "address", BigDecimal.ONE, "0x01234567890", {}, false, {}, {}, {}, {}, null)
  }
}