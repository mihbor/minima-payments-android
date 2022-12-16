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
import com.example.testapp.logic.request
import com.example.testapp.ui.theme.TestAppTheme
import com.ionspin.kotlin.bignum.decimal.BigDecimal.Companion.ONE
import com.ionspin.kotlin.bignum.decimal.BigDecimal.Companion.ZERO
import kotlinx.coroutines.launch

@Composable
fun ChannelTransfers(channel: Channel, activity: MainActivity?, setRequestSentOnChannel: (Channel) -> Unit) {
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
  if (channel.their.balance > ZERO) {
    var amount by remember { mutableStateOf(ZERO) }
    var preparingRequest by remember { mutableStateOf(false) }
    DecimalNumberField(amount,
      Modifier
        .width(100.dp)
        .height(50.dp), min = ZERO, max = channel.their.balance) { it?.let { amount = it } }
    Button(
      onClick = {
        preparingRequest = true
        scope.launch {
          val (updateTx, settleTx) = channel.request(amount)
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

val fakeChannel = Channel(
  id = 1,
  sequenceNumber = 0,
  status = "OPEN",
  tokenId = "0x00",
  my = Channel.Side(
    balance = ONE,
    address = "Mx0123456789",
    keys = Channel.Keys(
      trigger = "0x123",
      update = "0x123",
      settle = "0x123",
    )
  ),
  their = Channel.Side(
    balance = ONE,
    address = "Mx1234567890",
    keys = Channel.Keys(
      trigger = "0x123",
      update = "0x123",
      settle = "0x123",
    )
  ),
  triggerTx = "",
  updateTx = "",
  settlementTx = "",
  timeLock = 10,
  eltooAddress = "Mx123",
  updatedAt = 123
)