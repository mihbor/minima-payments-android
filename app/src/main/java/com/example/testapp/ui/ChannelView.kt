package com.example.testapp.ui

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import com.example.testapp.Channel
import com.example.testapp.MainActivity
import com.example.testapp.blockNumber
import com.example.testapp.logic.eltooScriptCoins
import com.example.testapp.logic.multisigScriptBalances
import com.ionspin.kotlin.bignum.decimal.BigDecimal.Companion.ZERO
import ltd.mbor.minimak.Balance
import ui.TokenIcon

@Composable
fun ChannelView(
  channel: Channel,
  balances: Map<String, Balance>,
  activity: MainActivity?,
  setRequestSentOnChannel: (Channel) -> Unit,
  updateChannel: (Channel) -> Unit
) {
  multisigScriptBalances.firstOrNull{ it.tokenId == channel.tokenId }?.let{
    TokenIcon(it.tokenId, balances)
    Text("${it.tokenName} token funding balance: ${it.confirmed.toPlainString()}")
  }
  if (multisigScriptBalances.any { it.unconfirmed > ZERO || it.confirmed > ZERO }) {
    Text("Channel balance: me ${channel.my.balance.toPlainString()}, counterparty ${channel.their.balance.toPlainString()}")
    ChannelTransfers(channel, activity, setRequestSentOnChannel)
  }
  Settlement(
    channel.copy(status = if (multisigScriptBalances.any { it.unconfirmed > ZERO || it.confirmed > ZERO }) "OPEN" else channel.status),
    blockNumber,
    eltooScriptCoins[channel.eltooAddress] ?: emptyList(),
    updateChannel
  )
}
