package com.example.testapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.testapp.*
import com.example.testapp.ui.theme.TestAppTheme
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import getChannels
import kotlinx.coroutines.launch
import ltd.mbor.minimak.Coin
import ltd.mbor.minimak.MDS
import ltd.mbor.minimak.getCoins
import updateChannelStatus


@Composable
fun ChannelListing(activity: MainActivity?, setRequestSentOnChannel: (ChannelState) -> Unit) {
  val channels = remember { mutableStateListOf<ChannelState>() }
  LaunchedEffect("channels") {
    channels.loadChannels()
  }

  LazyColumn {
    item {
      Row {
        Button(onClick = {
          scope.launch {
            channels.loadChannels()
          }
        }) {
          Text("Refresh")
        }
      }
    }
    item {
      ChannelTable(channels, eltooScriptCoins, activity, setRequestSentOnChannel) { index, channel ->
        channels[index] = channel
      }
    }
  }
}

@Composable
fun ChannelTable(
  channels: List<ChannelState>,
  eltooScriptCoins: Map<String, List<Coin>>,
  activity: MainActivity?,
  setRequestSentOnChannel: (ChannelState) -> Unit,
  updateChannel: (Int, ChannelState) -> Unit
) {

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
          ChannelTransfers(channel, activity, setRequestSentOnChannel)
        }
        Settlement(channel, blockNumber, eltooScriptCoins[channel.eltooAddress] ?: emptyList()) {
          updateChannel(index, it)
        }
      }
    }
  }
}

suspend fun MutableList<ChannelState>.loadChannels() {
  val newChannels = getChannels().map { channel ->
    val eltooCoins = MDS.getCoins(address = channel.eltooAddress)
    eltooScriptCoins.put(channel.eltooAddress, eltooCoins)
    if (channel.status == "OPEN" && eltooCoins.isNotEmpty()) updateChannelStatus(channel, "TRIGGERED")
    else if (channel.status in listOf("TRIGGERED", "UPDATED") && eltooCoins.isEmpty()) updateChannelStatus(channel, "SETTLED")
    else channel
  }
  clear()
  addAll(newChannels)
}

@Composable
@Preview
fun PreviewChannelListing() {
  TestAppTheme {
    ChannelListing(null, {})
  }
}

@Composable
@Preview
fun PreviewChannelTable() {
  TestAppTheme {
    Column {
      ChannelTable(
        listOf(fakeChannel, fakeChannel.copy(status = "TRIGGERED", eltooAddress = "Mx999", sequenceNumber = 3, updateTx = "abc")),
        mapOf("Mx999" to listOf(Coin(address = "", miniAddress = "", amount = BigDecimal.ONE, coinId = "", storeState = true, tokenId = "0x00", created = "100", state = emptyList()))),
        null,
        {}
      ) { _, _ -> }
    }
  }
}