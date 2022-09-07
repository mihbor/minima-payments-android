package com.example.testapp.ui

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
import com.example.testapp.scope
import com.example.testapp.send
import com.example.testapp.ui.theme.TestAppTheme
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.ionspin.kotlin.bignum.decimal.BigDecimal.Companion.ZERO
import kotlinx.coroutines.launch
import minima.Balance
import minima.TokenDescriptor

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun View(
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
  stopEmitting: () -> Unit
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
          value = balances[tokenId]?.let { (it.token?.name ?: "Minima") + " [${it.sendable.toPlainString()}]" } ?: "",
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
              setTokenId(it.tokenid)
            }) {
              Text(it.token?.name ?: "Minima")
              Text(" [${it.sendable.toPlainString()}]")
            }
          }
        }
      }
      Row {
        DecimalNumberField(amount, enabled = !isReaderMode, setValue = setAmount)
        if (isReaderMode) {
          val context = LocalContext.current
          Button(enabled = address.isNotBlank() && amount > ZERO && balances[tokenId]?.sendable?.let{ it >= amount } ?: false, onClick = {
            scope.launch {
              val success = send(address, amount, tokenId)
              Toast.makeText(context, "Sending result: $success", Toast.LENGTH_LONG).show()
              if (success) {
                setAmount(ZERO)
              }
            }
          }) {
            Text("Send!")
          }
        }
      }
      Row {
        ChannelListing()
      }
    }
  }
}

private val previewBalances = listOf(
  Balance("0x00", null, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, 1),
  Balance("0x01234567890", TokenDescriptor("test"), BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, 1),
).associateBy { it.tokenid }

@Preview(showBackground = true)
@Composable
fun ViewConsumer() {
  TestAppTheme {
    View(true, "uid123", {}, previewBalances, "", ZERO, "0x00", {}, true, {}, {}, {})
  }
}

@Preview(showBackground = true)
@Composable
fun ViewEmitter() {
  TestAppTheme {
    View(true, "uid456", {}, previewBalances, "address", BigDecimal.ONE, "0x01234567890", {}, false, {}, {}, {})
  }
}