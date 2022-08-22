package com.example.testapp.minima

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.testapp.TAG
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.serialization.json.*
import minima.Balance
import minima.Coin
import minima.TokenDescriptor
import kotlin.random.Random

var inited by mutableStateOf(false)
val balances = mutableStateListOf<Balance>()

data class Output(val address: String, val amount: BigDecimal, val token: String)

fun newTxId() = Random.nextInt(1_000_000_000)

suspend fun initMDS(uid: String, host: String = "localhost", port: Int = 9004) {
  inited = false
  MDS.init(uid, host, port) { msg ->
    when(msg.jsonObject["event"]?.jsonPrimitive?.content) {
      "inited" -> {
        if (MDS.logging) Log.i(TAG, "Connected to Minima.")
        balances.addAll(getBalances())
        inited = true
      }
      "NEWBALANCE" -> {
        val newBalances = getBalances()
        balances.clear()
        balances.addAll(newBalances)
      }
    }
  }
}

suspend fun newAddress(): String {
  val newaddress = try {
    MDS.cmd("newaddress")
  } catch (e: Exception) {
    Log.e(TAG, e.message, e)
    return ""
  }
  Log.i(TAG, "newaddress $newaddress")
  return newaddress!!.jsonObject["response"]!!.jsonObject["miniaddress"]!!.jsonPrimitive.content
}

suspend fun getAddress(): String {
  val getaddress = try {
    MDS.cmd("getaddress")
  } catch (e: Exception) {
    Log.e(TAG, e.message, e)
    return ""
  }
  Log.i(TAG, "getaddress $getaddress")
  return if (getaddress == null) getAddress()
    else getaddress.jsonObject["response"]!!.jsonObject["miniaddress"]!!.jsonPrimitive.content
}

suspend fun getBalances(address: String? = null): List<Balance> {
  val balance = MDS.cmd("balance ${address?.let{"address:$address "} ?:""}")?.jsonObject ?: return emptyList()
  val balances = balance["response"]?.jsonArray?.map { it.jsonObject } ?: emptyList()
  return balances.map {
    try {
      Balance(
        token = if (try {it["token"]!!.jsonPrimitive.contentOrNull}catch (e: Exception) {null} == "Minima") null else json.decodeFromJsonElement<TokenDescriptor>(it["token"]!!.jsonObject),
        tokenid = it["tokenid"]!!.jsonPrimitive.content,
        confirmed = json.decodeFromJsonElement(it["confirmed"]!!.jsonPrimitive),
        unconfirmed = json.decodeFromJsonElement(it["unconfirmed"]!!.jsonPrimitive),
        sendable = json.decodeFromJsonElement(it["sendable"]!!.jsonPrimitive),
        coins = it["coins"]!!.jsonPrimitive.content.toInt()
      )
    } catch (e: Exception) {
      Log.e(TAG, "Exception mapping balance", e)
      throw e
    }
  }
}

suspend fun getCoins(tokenId: String? = null, address: String? = null, sendable: Boolean): List<Coin> {
  val coinSimple = MDS.cmd("coins ${tokenId?.let{"tokenid:$tokenId "} ?:""} ${address?.let{"address:$address "} ?:""}sendable:$sendable")
  return coinSimple?.let { it.jsonObject["response"]}?.let{ json.decodeFromJsonElement<Array<Coin>>(it) }
    ?.sortedBy { it.amount } ?: emptyList()
}

suspend fun coverShortage(tokenId: String, shortage: BigDecimal, inputs: MutableList<Coin>, outputs: MutableList<Output>) {

  val coins = getCoins(tokenId = tokenId, sendable = true).ofAtLeast(shortage)
  coins.forEach { inputs.add(it) }
  val change = coins.sumOf { it.tokenamount ?: it.amount } - shortage
  if (change > BigDecimal.ZERO) outputs.add(Output(newAddress(), change, tokenId))
}

fun List<Coin>.ofAtLeast(amount: BigDecimal): List<Coin> {
  return firstOrNull { (it.tokenamount ?: it.amount) >= amount }
    ?.let{ listOf(it) }
    ?: (listOf(last()) + take(size-1).ofAtLeast(amount - (last().tokenamount ?: last().amount)))
}

fun <T> Iterable<T>.sumOf(selector: (T) -> BigDecimal) = fold(BigDecimal.ZERO) { acc, item -> acc + selector(item) }

suspend fun send(toAddress: String, amount: BigDecimal, tokenId: String): Boolean {
  val txnId = newTxId()
  val inputs = mutableListOf<Coin>()
  val outputs = mutableListOf<Output>()
  coverShortage(tokenId, amount, inputs, outputs)

  val txncreator = "txncreate id:$txnId;" +
    inputs.map{ "txninput id:$txnId coinid:${it.coinid};"}.joinToString("") +
    "txnoutput id:$txnId amount:${amount.toPlainString()} address:$toAddress tokenid:$tokenId;" +
    outputs.map{ "txnoutput id:$txnId amount:${it.amount.toPlainString()} address:${it.address} tokenid:${it.token};"}.joinToString("") +
    "txnsign id:$txnId publickey:auto;" +
    "txnpost id:$txnId auto:true;" +
    "txndelete id:$txnId;"

  val result = MDS.cmd(txncreator)?.jsonArray
  val txnpost = result?.find{it.jsonObject["command"]?.jsonPrimitive?.content == "txnpost"}
  val status = txnpost?.let{ it.jsonObject["status"]?.jsonPrimitive?.booleanOrNull} ?: false
  Log.i(TAG, "send: $status")
  return status
}
