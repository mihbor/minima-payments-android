package com.example.testapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.testapp.ChannelState
import com.example.testapp.blockNumber
import com.example.testapp.eltooScriptCoins
import com.example.testapp.minima.getCoins
import com.example.testapp.scope
import getChannels
import kotlinx.coroutines.launch


@Composable
fun ChannelListing() {
  var showChannels by remember { mutableStateOf(false) }
  val channels = remember { mutableStateListOf<ChannelState>() }

  Button(
    onClick = {
      showChannels = !showChannels
      if (showChannels) loadChannels(channels)
    },
    modifier = Modifier.background(if (showChannels) MaterialTheme.colors.primary else Color.Unspecified)
  ) {
    Text("Channel listing")
  }
  if (showChannels) {
    Button(onClick = { loadChannels(channels) }
    ) {
      Text("Refresh")
    }
    Row {
      Text("ID")
      Text("Status")
      Text("Sequence number")
      Text("My balance")
      Text("Their balance")
      Text("Actions")
    }
    channels.forEachIndexed { index, channel ->
      Row {
        Text(channel.id.toString())
        Text(channel.status)
        Text(channel.sequenceNumber.toString())
        Text(channel.myBalance.toPlainString())
        Text(channel.counterPartyBalance.toPlainString())
        if (channel.status == "OPEN") {
          ChannelTransfers(channel)
        }
        Settlement(channel, blockNumber, eltooScriptCoins[channel.eltooAddress] ?: emptyList()) {
          channels[index] = it
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