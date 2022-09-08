package com.example.testapp.minima

import android.util.Log
import com.example.testapp.TAG
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import minima.Balance
import minima.Coin
import minima.TokenDescriptor

@Serializable
data class Output(
  val address: String,
  @Contextual
  val amount: BigDecimal,
  val tokenid: String,
  val miniaddress: String = ""
)

@Serializable
data class State(
  val port: Int,
  val data: String
)

typealias Input = Output

suspend fun getBlockNumber(): Int {
  val status = MDS.cmd("status")!!
  return status.jsonObject["response"]!!.jsonObject["chain"]!!.jsonObject["block"]!!.jsonPrimitive.int
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

suspend fun newKey(): String {
  val keys = MDS.cmd("keys action:new")
  return keys!!.jsonObject["response"]!!.jsonObject["publickey"]!!.jsonPrimitive.content
}

suspend fun deployScript(text: String): String {
  val newscript = MDS.cmd("""newscript script:"$text" trackall:true""")
  return newscript!!.jsonObject["response"]!!.jsonObject["address"]!!.jsonPrimitive.content
}

suspend fun getCoins(tokenId: String? = null, address: String? = null, sendable: Boolean = false): List<Coin> {
  val coinSimple = MDS.cmd("coins ${tokenId?.let{"tokenid:$tokenId "} ?:""} ${address?.let{"address:$address "} ?:""}sendable:$sendable")
  return coinSimple?.let { it.jsonObject["response"]}?.let{ json.decodeFromJsonElement<Array<Coin>>(it) }
    ?.sortedBy { it.amount } ?: emptyList()
}

suspend fun signTx(txnId: Int, key: String): JsonObject {
  val txncreator = "txnsign id:$txnId publickey:$key;"
  val result = MDS.cmd(txncreator)!!.jsonObject
  Log.i(TAG, "sign: " + result["status"]?.jsonPrimitive?.boolean)
  return result
}

suspend fun post(txnId: Int): JsonObject? {
  val txncreator = "txnpost id:$txnId auto:true;"
  val result = MDS.cmd(txncreator)!!.jsonObject
  return result["response"]?.jsonObject
}

suspend fun exportTx(txnId: Int): String {
  val txncreator = "txnexport id:$txnId;"
  val result = MDS.cmd(txncreator)!!.jsonObject
  return result["response"]!!.jsonObject["data"]!!.jsonPrimitive.content
}

suspend fun importTx(txnId: Int, data: String): JsonObject? {
  val txncreator = "txncreate id:$txnId;" +
    "txnimport id:$txnId data:$data;"
  val result = MDS.cmd(txncreator)?.jsonArray ?: emptyList()
  val txnimport = result.find{ it.jsonObject["command"]?.jsonPrimitive?.content == "txnimport" }?.jsonObject
  val status = txnimport?.get("status")?.jsonPrimitive?.boolean
  Log.i(TAG, "import: " + status)
  return if (status ?: false) txnimport?.get("response")?.jsonObject?.get("transaction")?.jsonObject?.let(json::decodeFromJsonElement) else null
}
