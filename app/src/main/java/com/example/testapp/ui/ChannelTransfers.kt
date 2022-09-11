package com.example.testapp.ui

import android.util.Log
import androidx.compose.foundation.layout.Column
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
fun ChannelTransfers(channel: ChannelState, activity: MainActivity?, setRequestSentOnChannel: (ChannelState) -> Unit) {
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
  if (channel.counterPartyBalance > ZERO) {
    var amount by remember { mutableStateOf(ZERO) }
    var preparingRequest by remember { mutableStateOf(false) }
    DecimalNumberField(amount,
      Modifier
        .width(100.dp)
        .height(50.dp), min = ZERO, max = channel.counterPartyBalance) { it?.let { amount = it } }
    Button(
      onClick = {
        preparingRequest = true
        scope.launch {
          val (updateTx, settleTx) = requestViaChannel(amount, channel)
          activity?.apply {
            disableReaderMode()
            sendDataToService("TXN_REQUEST;$updateTx;$settleTx")
            Log.i(TAG, "TXN_REQUEST sent, updateTxLength: ${updateTx.length}, settleTxLength: ${settleTx.length}")
            setRequestSentOnChannel(channel)
            preparingRequest = false
          }
        }
      },
      enabled = !preparingRequest
    ) {
      Text(if (preparingRequest) "Preparing request..." else "Request via channel", fontSize = 10.sp)
    }
  }
}

@Composable
@Preview
fun PreviewTransfers() {
  TestAppTheme {
    Column {
      ChannelTransfers(fakeChannel, null, {})
    }
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