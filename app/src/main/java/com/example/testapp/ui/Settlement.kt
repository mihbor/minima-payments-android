package com.example.testapp.ui

import android.util.Log
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.example.testapp.*
import com.example.testapp.ui.theme.TestAppTheme
import kotlinx.coroutines.launch
import minima.Coin

@Composable
fun Settlement(channel: ChannelState, blockNumber: Int, eltooScriptCoins: List<Coin>, updateChannel: (ChannelState) -> Unit) {

  var settlementTriggering by remember { mutableStateOf(false) }
  var updatePosting by remember { mutableStateOf(false) }
  var settlementCompleting by remember { mutableStateOf(false) }
  Log.i(TAG, "Channel status: " + channel.status)

  if (channel.status == "OPEN") {
    Button(
      onClick = {
        settlementTriggering = true
        scope.launch {
          updateChannel(channel.triggerSettlement())
          settlementTriggering = false
        }
      },
      enabled = !settlementTriggering
    ) {
      Text("Trigger settlement!")
    }
  }
  if (eltooScriptCoins.isNotEmpty()) {
    eltooScriptCoins.forEach {
//      Br()
      Text("[${it.tokenid}] token eltoo coin: ${it.tokenamount?.toPlainString() ?: it.amount.toPlainString()} timelock ${
        (it.created.toInt() + channel.timeLock - blockNumber).takeIf { it > 0 }?.let { "ends in $it blocks" } ?: "ended"}"
      )
    }
    if (channel.status == "TRIGGERED" && channel.sequenceNumber > 0) {
//      Br()
      if (channel.updateTx.isNotEmpty()) Button(
        onClick = {
          updatePosting = true
          scope.launch {
            updateChannel(channel.postUpdate())
            updatePosting = false
          }
        },
        enabled = !updatePosting
      ) {
        Text("Post latest update")
      }
    }
    if (channel.status in listOf("TRIGGERED", "UPDATED")) {
      Button(
        enabled = !settlementCompleting && !updatePosting && eltooScriptCoins.none { it.created.toInt() + channel.timeLock > blockNumber },
        onClick = {
          settlementCompleting = true
          scope.launch {
            updateChannel(channel.completeSettlement())
            settlementCompleting = false
          }
        }
      ) {
        Text("Complete settlement!")
      }
    }
  }
}

@Composable
@Preview
fun previewSettlement() {

  TestAppTheme {
    Settlement(channel = fakeChannel, blockNumber = 5, eltooScriptCoins = emptyList(), updateChannel = {})
  }
}