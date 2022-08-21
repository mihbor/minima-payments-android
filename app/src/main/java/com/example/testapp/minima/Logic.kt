package com.example.testapp.minima

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.testapp.TAG
import kotlinx.serialization.json.*
import minima.Balance
import minima.TokenDescriptor

var inited by mutableStateOf(false)
val balances = mutableStateListOf<Balance>()

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