package com.example.testapp.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.example.testapp.ChannelState
import com.example.testapp.requestViaChannel
import com.example.testapp.scope
import com.example.testapp.sendViaChannel
import com.example.testapp.ui.theme.TestAppTheme
import com.ionspin.kotlin.bignum.decimal.BigDecimal.Companion.ONE
import com.ionspin.kotlin.bignum.decimal.BigDecimal.Companion.ZERO
import kotlinx.coroutines.launch

@Composable
fun ChannelTransfers(channel: ChannelState) {
  if (channel.myBalance > ZERO) Row {
//    Br()
    var amount by remember { mutableStateOf(ZERO) }
    DecimalNumberField(amount, min = ZERO, max = channel.myBalance) { it?.let { amount = it } }
    Button(
      onClick = {
        scope.launch {
          sendViaChannel(amount, channel)
        }
      }
    ) {
      Text("Send via channel")
    }
  }
  if (channel.counterPartyBalance > ZERO) {
//    Br()
    var amount by remember { mutableStateOf(ZERO) }
    DecimalNumberField(amount, min = ZERO, max = channel.counterPartyBalance) { it?.let { amount = it } }
    Button(
      onClick = {
        scope.launch {
          requestViaChannel(amount, channel)
        }
      }
    ) {
      Text("Request via channel")
    }
  }
}

fun channelKey(vararg keys: String) = keys.joinToString(";")

@Composable
@Preview
fun previewTransfers() {
  TestAppTheme {
    ChannelTransfers(
      fakeChannel
    )
  }
}

val fakeChannel = ChannelState(
  id = 1,
  sequenceNumber = 0,
  status = "OPEN",
  myBalance = ONE,
  counterPartyBalance = ZERO,
  myAddress = "Mx0123456789",
  counterPartyAddress = "Mx1234567890",
  myTriggerKey = "0x123",
  myUpdateKey = "0x123",
  mySettleKey = "0x123",
  counterPartyTriggerKey = "0x123",
  counterPartyUpdateKey = "0x123",
  counterPartySettleKey = "0x123",
  triggerTx = "",
  updateTx = "",
  settlementTx = "",
  timeLock = 10,
  eltooAddress = "Mx123",
  updatedAt = 123
)