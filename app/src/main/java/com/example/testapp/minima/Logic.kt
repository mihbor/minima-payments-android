package com.example.testapp.minima

import android.util.Log
import com.example.testapp.TAG
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
