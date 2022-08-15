package com.example.testapp.minima

import android.util.Log
import com.example.testapp.minima.MDS.client
import com.example.testapp.scope
import com.ionspin.kotlin.bignum.serialization.kotlinx.bigdecimal.bigDecimalHumanReadableSerializerModule
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import java.net.URLEncoder
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

private const val TAG = "MDS"

typealias Callback = (suspend (JsonElement) -> Unit)?

val json = Json {
  serializersModule = bigDecimalHumanReadableSerializerModule
}

fun encodeURIComponent(data: String): String = URLEncoder.encode(data, "UTF-8")

/**
 * The MAIN Minima Callback function
 */
var MDS_MAIN_CALLBACK: Callback = null;

/**
 * Main MINIMA Object for all interaction
 */
object MDS {
  
  //RPC Host for Minima
  var mainhost = ""
  
  //The MiniDAPP UID
  var minidappuid: String? = null

  //Is logging RPC enabled
  var logging = false
  
  //When debuggin you can hard set the Host and port
  var DEBUG_HOST: String? = null
  var DEBUG_PORT = -1
  
  //An allowed TEST Minidapp ID for SQL - can be overridden
  var DEBUG_MINIDAPPID = "0x00"

  val client = HttpClient(OkHttp) {
    install(HttpTimeout) {
      requestTimeoutMillis = 30000
      socketTimeoutMillis = 60000
    }
    engine {
//      https {
//        trustManager = object : X509TrustManager {
//          override fun getAcceptedIssuers(): Array<X509Certificate?> = arrayOf()
//          override fun checkClientTrusted(certs: Array<X509Certificate?>?, authType: String?) {}
//          override fun checkServerTrusted(certs: Array<X509Certificate?>?, authType: String?) {}
//        }
//      }
      config {
        val trustManager = object : X509TrustManager {
          override fun getAcceptedIssuers(): Array<X509Certificate?> = arrayOf()
          override fun checkClientTrusted(certs: Array<X509Certificate?>?, authType: String?) {}
          override fun checkServerTrusted(certs: Array<X509Certificate?>?, authType: String?) {}
        }
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(trustManager), null)
        sslSocketFactory(sslContext.socketFactory, trustManager)
      }
    }
  }

  /**
   * Minima Startup - with the callback function used for all Minima messages
   */
  suspend fun init(minidappuid: String, host: String = "localhost", port: Int = 9003, callback: Callback = null) {
    this.minidappuid = minidappuid
    log("Initialising MDS [$minidappuid]")

    val mainport 	= port+1
    
    log("MDS FILEHOST  : https://$host:$port/")
    
    mainhost 	= "https://$host:$mainport/"
    log("MDS MAINHOST : "+ mainhost)
    
    //Store this for poll messages
    MDS_MAIN_CALLBACK = callback
    
    scope.launch {
      PollListener()
    }

    log("MDS init complete")
    MDSPostMessage(json.parseToJsonElement("""{ "event" : "inited" }"""))
  }
  
  /**
   * Log some data with a timestamp in a consistent manner to the console
   */
  fun log(output: String){
    Log.w(TAG, "Minima @ ${Date().toLocaleString()} : $output")
  }
  
  /**
   * Runs a function on the Minima Command Line - same format as MInima
   */
  suspend fun cmd(command: String) =
    httpPostAsync("${mainhost}cmd?uid=$minidappuid", command)

  /**
   * Runs a SQL command on this MiniDAPPs SQL Database
   */
  suspend fun sql(command: String) =
    httpPostAsync("${mainhost}sql?uid=$minidappuid", command)

//  /**
//   * Form GET / POST parameters..
//   */
//  object form{
//
//    //Return the GET parameter by scraping the location..
//    fun getParams(parameterName: String): String?{
//      var result: String? = null
//      val items = window.location.search.substring(1).split("&");
//      for (item in items) {
//        val tmp = item.split("=");
//        //console.log("TMP:"+tmp);
//        if (tmp[0] == parameterName) result = decodeURIComponent(tmp[1])
//      }
//      return result
//    }
//  }
}

/**
 * Post a message to the Minima Event Listeners
 */
suspend fun MDSPostMessage(data: JsonElement){
  MDS_MAIN_CALLBACK?.invoke(data)
}

var PollCounter = 0
var PollSeries  = ""

//@Serializable
//data class Msg(
//  val series: String,
//  val counter: Int,
//  val status: Boolean,
//  val message: dynamic? = null,
//  val response: Msg? = null
//)

suspend fun PollListener(){
  
  //The POLL host
  val pollhost = "${MDS.mainhost}poll?uid=${MDS.minidappuid}"
  val polldata = "series=$PollSeries&counter=$PollCounter"
  
  httpPostAsyncPoll(pollhost, polldata) { msg: JsonElement ->
    //Are we on the right Series
    if (PollSeries != msg.jsonObject["series"]?.jsonPrimitive?.content) {
    
      //Reset to the right series.
      PollSeries = msg.jsonObject["series"]!!.jsonPrimitive.content
      PollCounter = msg.jsonObject["counter"]!!.jsonPrimitive.int
    
    } else {
    
      //Is there a message ?
      val response = msg.jsonObject.get("response")?.jsonObject
      if (msg.jsonObject["status"]!!.jsonPrimitive.boolean == true && response?.get("message") != null) {
      
        //Get the current counter
        PollCounter = (response.get("counter")?.jsonPrimitive?.int?:0) + 1
      
        MDSPostMessage(response.get("message")!!)
      }
    }
  
    //And around we go again
    PollListener()
  }
}

/**
 * Utility function for GET request
 *
 * @param theUrl
 * @param callback
 * @param params
 * @returns
 */
suspend fun httpPostAsync(theUrl: String, params: String): JsonElement? {
  MDS.log("POST_RPC:$theUrl PARAMS:$params")

  val response = client.post(theUrl) {
//    headers {
//      append(HttpHeaders.ContentType, "text/plain; charset=UTF-8")
//    }
    setBody(encodeURIComponent(params))
  }
  return if (response.status.isSuccess()) {
    MDS.log("STATUS: ${response.status}; RESPONSE:${response.bodyAsText()}");

    json.parseToJsonElement(response.bodyAsText())
  } else null
}

/**
 * Utility function for GET request (UNUSED for now..)
 *
 * @param theUrl
 * @param callback
 * @returns
 */
/*function httpGetAsync(theUrl, callback)
{
    var xmlHttp = new XMLHttpRequest();
    xmlHttp.onreadystatechange = function() {
        if (xmlHttp.readyState == 4 && xmlHttp.status == 200){
        	if(MDS.logging){
				console.log("RPC      : "+theUrl);
				console.log("RESPONSE : "+xmlHttp.responseText);
			}

			//Always a JSON ..
        	var rpcjson = JSON.parse(xmlHttp.responseText);
        	
        	//Send it to the callback function..
        	if(callback){
        		callback(rpcjson);
        	}
        }
    }
	xmlHttp.open("GET", theUrl, true); // true for asynchronous
    xmlHttp.send(null);
}*/

suspend fun httpPostAsyncPoll(theUrl: String, params: String, callback: Callback){
  if(MDS.logging){
    MDS.log("POST_POLL_RPC:$theUrl PARAMS:$params")
  }
  
  try {
    val response = client.post(theUrl) {
//    headers {
//      append(HttpHeaders.ContentType, "text/plain; charset=UTF-8")
//    }
      setBody(encodeURIComponent(params))
    }
    if (response.status.isSuccess()) {
      if(MDS.logging){
        MDS.log("STATUS: ${response.status}; RESPONSE:${response.bodyAsText()}")
      }

      callback?.invoke(json.parseToJsonElement(response.bodyAsText()))
    } else {
      MDS.log("STATUS: ${response.status}; RESPONSE:${response.bodyAsText()}")
      MDS.log("Error Polling $theUrl - reconnect in 10s")
      delay(10000)
      PollListener()
    }
  } catch (e: Exception) {
    Log.e(TAG, e.message, e)
    MDS.log("Error Polling - reconnect in 10s")
    delay(10000)
    PollListener()
  }
}