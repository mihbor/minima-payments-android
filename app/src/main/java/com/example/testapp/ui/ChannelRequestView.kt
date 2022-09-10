package com.example.testapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.testapp.ChannelState
import com.example.testapp.acceptRequest
import com.example.testapp.minima.Output
import com.example.testapp.minima.json
import com.example.testapp.scope
import com.example.testapp.sendDataToService
import com.example.testapp.ui.theme.TestAppTheme
import com.ionspin.kotlin.bignum.decimal.BigDecimal.Companion.ZERO
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray

@Composable
fun ChannelRequestView(channel: ChannelState, updateTx: Pair<Int, JsonObject>, settleTx: Pair<Int, JsonObject>, dismiss: () -> Unit) {
  val context = LocalContext.current

  var accepting by remember { mutableStateOf(false) }
  val outputs = settleTx.second["outputs"]?.jsonArray?.map { json.decodeFromJsonElement<Output>(it) }
  val myOutput = outputs?.find { it.miniaddress == channel.myAddress }
  val balanceChange = channel.myBalance - (myOutput?.amount ?: ZERO)

  Column {
    Text("Request received to send ${balanceChange.toPlainString()} Minima over channel ${channel.id}")
    Button(onClick = {
      accepting = false
      dismiss()
    }) {
      Text("Reject")
    }
    if(accepting) {
      Text("Use contactless again to complete transaction")
    } else Button(onClick = {
      scope.launch {
        channel.acceptRequest(updateTx, settleTx).let{ (updateTx, settleTx) -> context.sendDataToService("TXN_UPDATE_ACK;$updateTx;$settleTx")}
        accepting = true
      }
    }) {
      Text("Accept")
    }
  }
}

@Composable
@Preview
fun PreviewChannelRequest() {
  TestAppTheme {
    ChannelRequestView(channel = fakeChannel, updateTx = 1 to JsonObject(emptyMap()), settleTx = 2 to JsonObject(emptyMap())) { }
  }
}