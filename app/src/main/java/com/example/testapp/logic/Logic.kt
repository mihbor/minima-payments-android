package com.example.testapp

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.testapp.logic.*
import com.ionspin.kotlin.bignum.decimal.BigDecimal.Companion.ZERO
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ltd.mbor.minimak.*
import java.security.SecureRandom
import java.text.SimpleDateFormat

val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
var inited by mutableStateOf(false)
val balances = mutableStateMapOf<String, Balance>()
var blockNumber by mutableStateOf(0)

fun newTxId() = SecureRandom().nextInt(1_000_000_000)

suspend fun initMDS(uid: String, host: String, port: Int) {
  inited = false
  MDS.init(uid, host, port) { msg ->
    when(msg.jsonObject["event"]?.jsonPrimitive?.content) {
      "inited" -> {
        if (MDS.logging) Log.i(TAG, "Connected to Minima.")
        balances.putAll(MDS.getBalances().associateBy { it.tokenId })
        blockNumber = MDS.getBlockNumber()
        createDB()
        channels.addAll(getChannels(status = "OPEN"))
        channels.forEach { channel ->
          subscribe(channelKey(channel.my.keys, channel.tokenId), from = channel.updatedAt).onEach { msg ->
            log("tx msg: $msg")
            val splits = msg.split(";")
            if (splits[0].startsWith("TXN_UPDATE")) {
              channels.first { it.id == channel.id }.update(splits[0].endsWith("_ACK"), updateTx = splits[1], settleTx = splits[2])
            }
          }.onCompletion {
            log("completed")
          }.launchIn(scope)
        }
        inited = true
      }
      "NEWBALANCE" -> {
        val newBalances = MDS.getBalances().associateBy { it.tokenId }
        balances.clear()
        balances.putAll(newBalances)
      }
      "NEWBLOCK" -> {
        blockNumber = msg.jsonObject["data"]!!.jsonObject["txpow"]!!.jsonObject["header"]!!.jsonObject["block"]!!.jsonPrimitive.content.toInt()
        if (multisigScriptAddress.isNotEmpty()) {
          val newBalances = MDS.getBalances(multisigScriptAddress, confirmations = 0)
          if (newBalances.any { it.confirmed > ZERO } && multisigScriptBalances.none { it.confirmed > ZERO }) {
            setChannelOpen(multisigScriptAddress)
          }
          multisigScriptBalances.clear()
          multisigScriptBalances.addAll(newBalances)
        }
        if (eltooScriptAddress.isNotEmpty()) {
          eltooScriptCoins.put(eltooScriptAddress, MDS.getCoins(address = eltooScriptAddress))
        }
      }
    }
  }
}

suspend fun channelUpdateAck(updateTxText: String, settleTxText: String) {

  MDS.importTx(newTxId(), updateTxText).also { updateTx ->
    val settleTx = MDS.importTx(newTxId(), settleTxText)
    val channel = getChannel(updateTx["outputs"]!!.jsonArray.map { json.decodeFromJsonElement<Output>(it) }.first().address)!!
    val sequenceNumber = settleTx["state"]!!.jsonArray.map { json.decodeFromJsonElement<State>(it) }.find { it.port == 99 }?.data?.toInt()!!

    val outputs = settleTx["outputs"]!!.jsonArray.map { json.decodeFromJsonElement<Coin>(it) }
    val channelBalance = outputs.find { it.miniAddress == channel.my.address }!!.amount to outputs.find { it.miniAddress == channel.their.address }!!.amount
    updateChannel(channel, channelBalance, sequenceNumber, updateTxText, settleTxText)
  }
}
