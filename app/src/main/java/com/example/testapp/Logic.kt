package com.example.testapp

import android.util.Log
import androidx.compose.runtime.*
import com.example.testapp.minima.*
import com.example.testapp.minima.State
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import getChannel
import kotlinx.serialization.json.*
import minima.Balance
import minima.Coin
import updateChannel
import updateChannelStatus
import java.security.SecureRandom

var inited by mutableStateOf(false)
val balances = mutableStateListOf<Balance>()
var blockNumber by mutableStateOf(0)

val eltooScriptCoins = mutableStateMapOf<String, List<Coin>>()

fun newTxId() = SecureRandom().nextInt(1_000_000_000)

suspend fun initMDS(uid: String, host: String, port: Int) {
  inited = false
  MDS.init(uid, host, port) { msg ->
    when(msg.jsonObject["event"]?.jsonPrimitive?.content) {
      "inited" -> {
        if (MDS.logging) Log.i(TAG, "Connected to Minima.")
        balances.addAll(getBalances())
        blockNumber = getBlockNumber()
        inited = true
      }
      "NEWBALANCE" -> {
        val newBalances = getBalances()
        balances.clear()
        balances.addAll(newBalances)
      }
      "NEWBLOCK" -> {
        blockNumber = msg.jsonObject["data"]!!.jsonObject["txpow"]!!.jsonObject["header"]!!.jsonObject["block"]!!.jsonPrimitive.content.toInt()
      }
    }
  }
}

suspend fun withChange(tokenId: String, amount: BigDecimal): Pair<List<Coin>, List<Output>> {
  val inputs = mutableListOf<Coin>()
  val outputs = mutableListOf<Output>()
  val coins = getCoins(tokenId = tokenId, sendable = true).ofAtLeast(amount)
  coins.forEach { inputs.add(it) }
  val change = coins.sumOf { it.tokenamount ?: it.amount } - amount
  if (change > BigDecimal.ZERO) outputs.add(Output(newAddress(), change, tokenId))
  return inputs to outputs
}

fun List<Coin>.ofAtLeast(amount: BigDecimal): List<Coin> {
  return firstOrNull { (it.tokenamount ?: it.amount) >= amount }
    ?.let{ listOf(it) }
    ?: (listOf(last()) + take(size-1).ofAtLeast(amount - (last().tokenamount ?: last().amount)))
}

fun <T> Iterable<T>.sumOf(selector: (T) -> BigDecimal) = fold(BigDecimal.ZERO) { acc, item -> acc + selector(item) }

suspend fun send(toAddress: String, amount: BigDecimal, tokenId: String): Boolean {
  val txnId = newTxId()
  val (inputs, outputs) = withChange(tokenId, amount)
  val txncreator = "txncreate id:$txnId;" +
    inputs.map{ "txninput id:$txnId coinid:${it.coinid};"}.joinToString("") +
    "txnoutput id:$txnId amount:${amount.toPlainString()} address:$toAddress tokenid:$tokenId;" +
    outputs.map{ "txnoutput id:$txnId amount:${it.amount.toPlainString()} address:${it.address} tokenid:${it.tokenid};"}.joinToString("") +
    "txnsign id:$txnId publickey:auto;" +
    "txnpost id:$txnId auto:true;" +
    "txndelete id:$txnId;"

  val result = MDS.cmd(txncreator)
  val txnpost = (result as? JsonArray)?.find{it.jsonObject["command"]?.jsonPrimitive?.content == "txnpost"}
    ?: (result as? JsonArray)?.last()
    ?: result?.jsonObject
  val status = txnpost?.let{ it.jsonObject["status"]?.jsonPrimitive?.booleanOrNull} ?: false
  val message = txnpost?.let{ it.jsonObject["message"]?.jsonPrimitive?.content}
  Log.i(TAG, "send status: $status message: $message")
  return status
}

suspend fun importAndPost(tx: String): JsonElement? {
  val txId = newTxId()
  importTx(txId, tx)
  return post(txId)
}

suspend fun ChannelState.triggerSettlement(): ChannelState {
  val response = importAndPost(triggerTx)
  return if (response == null) this
  else updateChannelStatus(this, "TRIGGERED")
}

suspend fun ChannelState.postUpdate(): ChannelState {
  val response = importAndPost(updateTx)
  return if (response == null) this
  else updateChannelStatus(this, "UPDATED")
}

suspend fun ChannelState.completeSettlement(): ChannelState {
  val response = importAndPost(settlementTx)
  return if (response == null) this
  else updateChannelStatus(this, "SETTLED")
}

suspend fun requestViaChannel(amount: BigDecimal, channel: ChannelState) = sendViaChannel(-amount, channel)

suspend fun sendViaChannel(amount: BigDecimal, channel: ChannelState): Pair<String, String> {
  val currentSettlementTx = importTx(newTxId(), channel.settlementTx)!!
  val input = json.decodeFromJsonElement<Input>(currentSettlementTx["inputs"]!!.jsonArray.first())
  val state = currentSettlementTx["state"]!!.jsonArray.find{ it.jsonObject["port"]!!.jsonPrimitive.int == 99 }!!.jsonObject["data"]!!.jsonPrimitive.content
  val updateTxnId = newTxId()
  val updatetxncreator = "txncreate id:$updateTxnId;" +
    "txninput id:$updateTxnId address:${input.address} amount:${input.amount} tokenid:${input.tokenid} floating:true;" +
    "txnstate id:$updateTxnId port:99 value:${state.toInt() + 1};" +
    "txnoutput id:$updateTxnId amount:${input.amount} address:${input.address};" +
    "txnsign id:$updateTxnId publickey:${channel.myUpdateKey};" +
    "txnexport id:$updateTxnId;"
  val updateTxn = MDS.cmd(updatetxncreator)?.jsonArray?.last()!!.jsonObject["response"]!!.jsonObject["data"]!!.jsonPrimitive.content
  val settleTxnId = newTxId()
  val settletxncreator = "txncreate id:$settleTxnId;" +
    "txninput id:$settleTxnId address:${input.address} amount:${input.amount} tokenid:${input.tokenid} floating:true;" +
    "txnstate id:$settleTxnId port:99 value:${state.toInt() + 1};" +
    (if(channel.myBalance - amount > BigDecimal.ZERO)
      "txnoutput id:$settleTxnId amount:${(channel.myBalance - amount).toPlainString()} address:${channel.myAddress};"
    else "") +
    (if(channel.counterPartyBalance + amount > BigDecimal.ZERO)
      "txnoutput id:$settleTxnId amount:${(channel.counterPartyBalance + amount).toPlainString()} address:${channel.counterPartyAddress};"
    else "") +
    "txnsign id:$settleTxnId publickey:${channel.mySettleKey};" +
    "txnexport id:$settleTxnId;"
  val settleTxn = MDS.cmd(settletxncreator)?.jsonArray?.last()!!.jsonObject["response"]!!.jsonObject["data"]!!.jsonPrimitive.content

  return updateTxn to settleTxn
//  publish(
//    channelKey(channel.counterPartyTriggerKey, channel.counterPartyUpdateKey, channel.counterPartySettleKey),
//    listOf(if(amount > BigDecimal.ZERO) "TXN_UPDATE" else "TXN_REQUEST", updateTxn.response.data, settleTxn.response.data).joinToString(";")
//  )
}

suspend fun signAndExportTx(id: Int, key: String): String {
  signTx(id, key)
  return exportTx(id)
}

suspend fun ChannelState.acceptRequest(updateTx: Pair<Int, JsonObject>, settleTx: Pair<Int, JsonObject>): Pair<String, String> {
  val sequenceNumber = settleTx.second["state"]!!.jsonArray.map { json.decodeFromJsonElement<State>(it) }.find { it.port == 99 }?.data?.toInt()

  val outputs = settleTx.second["outputs"]!!.jsonArray.map { json.decodeFromJsonElement<Output>(it) }
  val channelBalance = outputs.find { it.miniaddress == myAddress }!!.amount to outputs.find { it.miniaddress == counterPartyAddress }!!.amount

  val signedUpdateTx = signAndExportTx(updateTx.first, myUpdateKey)
  val signedSettleTx = signAndExportTx(settleTx.first, mySettleKey)

  updateChannel(this, channelBalance, sequenceNumber!!, signedUpdateTx, signedSettleTx)

  return signedUpdateTx to signedSettleTx
}

suspend fun channelUpdateAck(updateTxText: String, settleTxText: String) {

  importTx(newTxId(), updateTxText)!!.also { updateTx ->
    val settleTx = importTx(newTxId(), settleTxText)!!
    val channel = getChannel(updateTx["outputs"]!!.jsonArray.map { json.decodeFromJsonElement<Output>(it) }.first().address)!!
    val sequenceNumber = settleTx["state"]!!.jsonArray.map { json.decodeFromJsonElement<State>(it) }.find { it.port == 99 }?.data?.toInt()!!

    val outputs = settleTx["outputs"]!!.jsonArray.map { json.decodeFromJsonElement<Output>(it) }
    val channelBalance = outputs.find { it.miniaddress == channel.myAddress }!!.amount to outputs.find { it.miniaddress == channel.counterPartyAddress }!!.amount
    updateChannel(channel, channelBalance, sequenceNumber, updateTxText, settleTxText)
  }
}
