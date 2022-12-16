package com.example.testapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.example.testapp.Channel
import com.example.testapp.MainActivity
import com.example.testapp.logic.acceptRequest
import com.example.testapp.scope
import com.example.testapp.sendDataToService
import com.example.testapp.ui.theme.TestAppTheme
import com.ionspin.kotlin.bignum.decimal.BigDecimal.Companion.ZERO
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import ltd.mbor.minimak.Coin
import ltd.mbor.minimak.json

@Composable
fun ChannelRequestReceived(channel: Channel, updateTx: Pair<Int, JsonObject>, settleTx: Pair<Int, JsonObject>, activity: MainActivity?, dismiss: () -> Unit) {

  var accepting by remember { mutableStateOf(false) }
  var preparingResponse by remember { mutableStateOf(false) }
  val outputs = settleTx.second["outputs"]?.jsonArray?.map { json.decodeFromJsonElement<Coin>(it) }
  val myOutput = outputs?.find { it.miniAddress == channel.my.address }
  val balanceChange = channel.my.balance - (myOutput?.amount ?: ZERO)

  Column {
    Text("Request received to send ${balanceChange.toPlainString()} Minima over channel ${channel.id}")
    Button(onClick = {
      accepting = false
      dismiss()
    }) {
      Text(if (accepting) "Finish" else "Reject")
    }
    if(accepting) {
      Text("Use contactless again to complete transaction")
    } else Button(
      onClick = {
        preparingResponse = true
        scope.launch {
          channel.acceptRequest(updateTx, settleTx).let { (updateTx, settleTx) ->
            activity?.apply {
              disableReaderMode()
              sendDataToService("TXN_UPDATE_ACK;$updateTx;$settleTx")
            }
          }
          accepting = true
          preparingResponse = false
        }
      },
      enabled = !preparingResponse
    ) {
      Text(if (preparingResponse) "Reparing response..." else "Accept")
    }
  }
}

@Composable
@Preview
fun PreviewChannelRequest() {
  TestAppTheme {
    ChannelRequestReceived(channel = fakeChannel, updateTx = 1 to JsonObject(emptyMap()), settleTx = 2 to JsonObject(emptyMap()), null) { }
  }
}