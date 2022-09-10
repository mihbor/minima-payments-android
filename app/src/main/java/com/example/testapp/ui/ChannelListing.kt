package com.example.testapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.testapp.*
import com.example.testapp.minima.getCoins
import com.example.testapp.ui.theme.TestAppTheme
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import getChannels
import kotlinx.coroutines.launch
import minima.Coin


@Composable
fun ChannelListing(activity: MainActivity?) {
  var showChannels by remember { mutableStateOf(false) }
  val channels = remember { mutableStateListOf<ChannelState>() }

  LazyColumn {
    item {
      Row {
        Button(
          onClick = {
            showChannels = !showChannels
            if (showChannels) loadChannels(channels)
          },
          colors = ButtonDefaults.buttonColors(
            backgroundColor = (if (showChannels) MaterialTheme.colors.primary else Color.Unspecified),
          ),
        ) {
          Text("Channel listing")
        }
        if (showChannels) {
          Button(onClick = { loadChannels(channels) }
          ) {
            Text("Refresh")
          }
        }
      }
    }
    if (showChannels) item {
      ChannelTable(channels, eltooScriptCoins, activity) { index, channel ->
        channels[index] = channel
      }
    }
  }
}

@Composable
fun ChannelTable(channels: List<ChannelState>, eltooScriptCoins: Map<String, List<Coin>>, activity: MainActivity?, updateChannel: (Int, ChannelState) -> Unit) {

  Row {
    Text("ID", Modifier.width(30.dp), fontSize = 10.sp)
    Text("Status", Modifier.width(60.dp), fontSize = 10.sp)
    Text("Seq\nnumber", Modifier.width(50.dp), fontSize = 10.sp)
    Text("My\nbalance", Modifier.width(50.dp), fontSize = 10.sp)
    Text("Their\nbalance", Modifier.width(50.dp), fontSize = 10.sp)
    Text("Actions", Modifier.width(250.dp), fontSize = 10.sp)
  }
  channels.forEachIndexed { index, channel ->
    Row {
      Text(channel.id.toString(), Modifier.width(30.dp), fontSize = 10.sp)
      Text(channel.status, Modifier.width(60.dp), fontSize = 10.sp)
      Text(channel.sequenceNumber.toString(), Modifier.width(50.dp), fontSize = 10.sp)
      Text(channel.myBalance.toPlainString(), Modifier.width(50.dp), fontSize = 10.sp)
      Text(channel.counterPartyBalance.toPlainString(), Modifier.width(50.dp), fontSize = 10.sp)
      Column(Modifier.width(250.dp)) {
        if (channel.status == "OPEN") {
          ChannelTransfers(channel, activity)
        }
        Settlement(channel, blockNumber, eltooScriptCoins[channel.eltooAddress] ?: emptyList()) {
          updateChannel(index, it)
        }
      }
    }
  }
}

private fun loadChannels(channels: MutableList<ChannelState>) {
  scope.launch {
    val newChannels = getChannels()
    channels.clear()
    channels.addAll(newChannels)
    newChannels.forEach {
      eltooScriptCoins.put(it.eltooAddress, getCoins(address = it.eltooAddress))
    }
  }
}

@Composable
@Preview
fun PreviewChannelListing() {
  TestAppTheme {
    ChannelListing(null)
  }
}

@Composable
@Preview
fun PreviewChannelTable() {
  TestAppTheme {
    Column {
      ChannelTable(
        listOf(fakeChannel, fakeChannel.copy(status = "TRIGGERED", eltooAddress = "Mx999", sequenceNumber = 3, updateTx = "abc")),
        mapOf("Mx999" to listOf(Coin("", BigDecimal.ONE, coinid = "", storestate = true, tokenid = "0x00", created = "100"))),
        null
      ) { _, _ -> }
    }
  }
}