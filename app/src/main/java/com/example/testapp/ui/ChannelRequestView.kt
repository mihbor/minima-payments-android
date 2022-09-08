package com.example.testapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.testapp.ChannelState
import com.example.testapp.acceptRequest
import com.example.testapp.minima.Output
import com.example.testapp.minima.json
import com.example.testapp.scope
import com.example.testapp.sendDataToService
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray

@Composable
fun ChannelRequestView(channel: ChannelState, updateTx: Pair<Int, JsonObject>, settleTx: Pair<Int, JsonObject>, dismiss: () -> Unit) {
  val context = LocalContext.current

  val outputs = settleTx.second["outputs"]!!.jsonArray.map { json.decodeFromJsonElement<Output>(it) }
  val myOutput = outputs.find { it.miniaddress == channel.myAddress }!!
  val balanceChange = channel.myBalance - myOutput.amount

  Column {
    Text("Request received to send ${balanceChange.toPlainString()} [${myOutput.tokenid}] over channel ${channel.id}")
    Button(onClick = dismiss) {
      Text("Ignore")
    }
    Button(onClick = {
      scope.launch {
        channel.acceptRequest(updateTx, settleTx).let{ (updateTx, settleTx) -> context.sendDataToService("TXN_UPDATE_ACK;$updateTx;$settleTx")}
      }
    }) {
      Text("Accept")
    }
  }
}