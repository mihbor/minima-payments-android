package com.example.testapp.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.testapp.*
import com.example.testapp.ui.theme.TestAppTheme
import com.ionspin.kotlin.bignum.decimal.BigDecimal.Companion.ONE
import com.ionspin.kotlin.bignum.decimal.BigDecimal.Companion.ZERO
import kotlinx.coroutines.launch

@Composable
fun ChannelTransfers(channel: ChannelState, activity: MainActivity?) {
//  if (channel.myBalance > ZERO) Row {
//    var amount by remember { mutableStateOf(ZERO) }
//    DecimalNumberField(amount, Modifier.width(60.dp).height(50.dp), min = ZERO, max = channel.myBalance) { it?.let { amount = it } }
//    Button(
//      onClick = {
//        scope.launch {
//          sendViaChannel(amount, channel)
//        }
//      }
//    ) {
//      Text("Send via channel", fontSize = 10.sp)
//    }
//  }
  if (channel.counterPartyBalance > ZERO) Row {
    var amount by remember { mutableStateOf(ZERO) }
    DecimalNumberField(amount, Modifier.width(60.dp).height(50.dp), min = ZERO, max = channel.counterPartyBalance) { it?.let { amount = it } }
    Button(
      onClick = {
        scope.launch {
          val (updateTx, settleTx) = requestViaChannel(amount, channel)
          activity?.apply {
            disableReaderMode()
            sendDataToService("TXN_REQUEST;$updateTx;$settleTx")
          }
        }
      }
    ) {
      Text("Request via channel", fontSize = 10.sp)
    }
  }
}

fun channelKey(vararg keys: String) = keys.joinToString(";")

@Composable
@Preview
fun PreviewTransfers() {
  TestAppTheme {
    ChannelTransfers(
      fakeChannel, null
    )
  }
}

val fakeChannel = ChannelState(
  id = 1,
  sequenceNumber = 0,
  status = "OPEN",
  myBalance = ONE,
  counterPartyBalance = ONE,
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