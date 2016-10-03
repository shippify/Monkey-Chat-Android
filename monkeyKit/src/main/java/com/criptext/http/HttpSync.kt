package com.criptext.http

import android.content.Context
import android.os.Message
import android.util.Base64
import android.util.Log
import com.criptext.ClientData
import com.criptext.MonkeyKitSocketService
import com.criptext.comunication.AsyncConnSocket
import com.criptext.comunication.MOKMessage
import com.criptext.comunication.MessageTypes
import com.criptext.lib.KeyStoreCriptext
import com.criptext.security.AESUtil
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.*

/**
 * Created by gesuwall on 9/30/16.
 */
class HttpSync(ctx: Context, val clientData: ClientData, val aesUtil: AESUtil) {
    val contextRef: WeakReference<Context>
    val keyMap: HashMap<String, String>
    private val initialTimeout = 20L
    private var timeout: Long = initialTimeout
    set(value) {
      field = if(value > 60) initialTimeout else value
    }
    init {
        contextRef = WeakReference(ctx)
        keyMap = HashMap()
    }

    private fun executeHttp(http: OkHttpClient, request: Request): Response? =
        try {
            http.newCall(request).execute()
        } catch (ex: IOException) {
            Log.d("HttpSync", "timed out: ${ex.message}")
            null
        }

    fun getBatch(since: Long, qty: Int): List<MOKMessage>{
            //TODO TEST THIS!!
            val parser = JsonParser()
            val request = Request.Builder()
                    .url(MonkeyKitSocketService.httpsURL + "/user/messages/" +
                            "${clientData.monkeyId}/$since/$qty")
                    .build()
            var response: Response? = null

            while (response == null){
                val http = OpenConversationTask.authorizedHttpClient(clientData, timeout)
                response = executeHttp(http, request)
                timeout += 10
            }

            if(response.isSuccessful) {
                val body = response.body()
                val jsonResponse = parser.parse(body.string()).asJsonObject
                val data = jsonResponse.getAsJsonObject("data")
                val batch = processBatch(data)
                val remaining = data.get("remaining").asInt
                if(remaining > 0)
                    batch.addAll(getBatch(since, Math.min(remaining, qty)))
                body.close()
                return batch
            } else {
                Log.e("HttpSync", response.body().string())
            }

            return listOf()
    }
    fun getJsonFromMessage(jsonMessage: JsonObject, key: String, parser: JsonParser): JsonObject?{
        if (jsonMessage.has(key)) {
            val paramsStr = jsonMessage.get(key)?.asString
            if(paramsStr != null)
                return parser.parse(paramsStr) as? JsonObject?
        }

        return null
    }

    fun getKeyForConversation(id: String): String{
        if(keyMap.containsKey(id))
            return keyMap[id]!!
        else {
            val ctx = contextRef.get()
            if (ctx != null) {
                val keyFromStore = KeyStoreCriptext.getString(ctx, id)
                keyMap.put(id, keyFromStore)
                return keyFromStore
            }
        }
        return ""
    }
    /**
	 * Procesa el JSONArray que llega despues de un GET O SYNC. Decripta los mensajes que necesitan
	 * ser decriptados y arma un ArrayList de MOKMessages para pasarselo al Thread principal
	 * @param protocol GET o SYNC de MessageTypes
	 * @param data JsonObject "args" del GET o SYNC
	 * @param parser
	 */
	private fun processBatch(data: JsonObject): MutableList<MOKMessage>{
		System.out.println("MOK PROTOCOL SYNC");
        val parser = JsonParser()
        val array = data.get("messages").asJsonArray;
        val batch: MutableList<MOKMessage> = mutableListOf()

       array.forEach { jsonMessage ->
            val currentMessage = jsonMessage.asJsonObject
            val props = getJsonFromMessage(currentMessage, "props", parser)
            val params = getJsonFromMessage(currentMessage, "params", parser)

            val currentMessageType = currentMessage.get("type").asString
            val messageIsEncrypted = if(props?.has("encr") ?: false) props!!.get("encr").asInt == 1 else false
            if (currentMessageType == MessageTypes.MOKText || currentMessageType == MessageTypes.MOKFile) {
                val remote = AsyncConnSocket.createMOKMessageFromJSON(currentMessage, params, props, true)
                if (messageIsEncrypted){
                    val existingKey = getKeyForConversation(remote.sid)
                    val validKey = OpenConversationTask.attemptToDecrypt(remote, clientData, aesUtil, existingKey)
                    if(validKey == null){
                        //No key could decrypt this message, discard
                        return mutableListOf()
                    } else {
                        if(validKey != existingKey)
                            keyMap.put(remote.sid, validKey) //update the keyMap with valid keys
                    }
                }
                else if ((props?.has("encoding") ?: false) && (currentMessageType != MessageTypes.MOKFile)) {
                    if(props?.get("encoding")?.asString  == "base64")
                        remote.msg = String(Base64.decode(remote.msg.toByteArray(), Base64.NO_WRAP))
                }

                batch.add(remote);
            }
            else if(currentMessageType == MessageTypes.MOKNotif){
                //val remote = AsyncConnSocket.createMOKMessageFromJSON(currentMessage, params, props, true);
            }
            else if(currentMessageType == MessageTypes.MOKProtocolDelete.toString()){
                //DELETE
            }
        }

        return batch;
	}

    companion object {
        fun newInstance(ctx: Context?, clientData: ClientData): HttpSync?{
            if(ctx != null) {
                val aesUtil = AESUtil(ctx, clientData.monkeyId)
                return HttpSync(ctx, clientData, aesUtil)
            }
            return null
        }
    }
}