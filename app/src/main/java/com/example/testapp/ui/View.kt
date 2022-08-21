package com.example.testapp.ui

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.testapp.TAG
import com.example.testapp.minima.balances
import java.math.BigDecimal


@Composable
fun View(
  inited: Boolean,
  uid: String,
  setUid: (String) -> Unit,
  address: String,
  amount: BigDecimal,
  tokenId: String,
  setTokenId: (String) -> Unit,
  isReaderMode: Boolean,
  startEmitting: (BigDecimal) -> Unit,
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
        Text("Reader")
        Switch(checked = !isReaderMode, onCheckedChange = { if (isReaderMode) startEmitting(amount) else stopEmitting() })
        Text("Emitter")
      }
      OutlinedTextField(address, {}, enabled = !isReaderMode)
      var expanded by remember { mutableStateOf(false) }
      Log.i(TAG, "tokenid: $tokenId balance: " + balances.find { it.tokenid == tokenId }?.let { (it.token?.name ?: "Minima") + " [${it.sendable.toPlainString()}]" })
      OutlinedTextField(
        value = balances.find { it.tokenid == tokenId }?.let { (it.token?.name ?: "Minima") + " [${it.sendable.toPlainString()}]" } ?: "",
        {},
        modifier = Modifier.clickable(onClick = { expanded = !expanded }),
        readOnly = true
      )
      DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        balances.forEach {
          DropdownMenuItem(enabled = !isReaderMode && it.sendable > BigDecimal.ZERO, onClick = {
            setTokenId(it.tokenid)
          }) {
            Text(it.token?.name ?: "Minima")
            Text(" [${it.sendable.toPlainString()}]")
          }
        }
      }
      OutlinedNumberField(amount, !isReaderMode, startEmitting)
    }
  }
}
