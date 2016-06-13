package com.criptext.http

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import com.criptext.ClientData
import com.criptext.comunication.CBTypes
import com.criptext.comunication.MOKMessage
import com.criptext.lib.KeyStoreCriptext
import com.criptext.security.AESUtil
import com.criptext.security.DecryptTask
import com.criptext.security.EncryptedMsg
import com.criptext.socket.SecureSocketService
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.*
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by gesuwall on 6/6/16.
 */

class OpenConversationTask(service: SecureSocketService, val undecrypted: MOKMessage) : AsyncTask<String, Void, MOKMessage>(){
   val serviceRef: WeakReference<SecureSocketService>
    val clientData: ClientData
    lateinit var newConvKey: String
    lateinit var conversationId: String

    init {
        this.serviceRef = WeakReference(service)
        clientData = service.serviceClientData

    }

    fun getAESData(monkeyId: String) :String? {
        val service = serviceRef.get()
        if(service != null)
            return KeyStoreCriptext.getString(service.context, monkeyId)

        return null
    }

    override fun doInBackground(vararg p0: String?): MOKMessage? {
        conversationId = p0[0]!!
        val response = sendOpenConversationRequest(p0[0]!!,clientData)

        val aesData = getAESData(clientData.monkeyId)
        if(isCancelled || aesData == null)
            return null;

        val aesutil = AESUtil(aesData)

        val data = response.getAsJsonObject("data")
        newConvKey = data.get("convKey").asString

        return attemptToDecryptPendingMessage(
                openConversationResponse = response,
                aesutil = aesutil,
                pendingMessage = undecrypted,
                clientData = clientData)
    }

    override fun onPostExecute(message: MOKMessage?) {
        val service = serviceRef.get()
        if(service != null && message!= null){
            KeyStoreCriptext.putString(service.context, conversationId, KeyStoreCriptext.encryptString(newConvKey))
            service.executeInDelegate(CBTypes.onMessageReceived, Array<Any>(1, { i -> message}))
        }



    }

    companion object {

        fun authorizedHttpClient(clientData: ClientData) = OkHttpClient().newBuilder()
                .authenticator(Authenticator { route, response ->
                    val credential = Credentials.basic(clientData.appId, clientData.appKey);
                    response.request().newBuilder()
                            .header("Authorization", credential).build()
                })
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()

        fun sendOpenConversationRequest(user_to: String, clientData: ClientData): JsonObject {

            val http = authorizedHttpClient(clientData)

            val JSON = MediaType.parse("application/json; charset=utf-8");

            val data = JsonObject();
            data.addProperty("user_to", user_to);
            data.addProperty("session_id", clientData.monkeyId);

            val json = JsonObject();
            json.add("data", data)
            val body = RequestBody.create(JSON, json.toString());

            val request = Request.Builder()
                    .url(SecureSocketService.httpsURL + "/user/key/exchange")
                    .post(body).build()


            val response = http.newCall(request).execute().body().string();
            val parser = JsonParser()
            return parser.parse(response).asJsonObject

        }

        fun getTextEncryptedWithLatestKeys(msg: MOKMessage, clientData: ClientData): String? {
            val http = authorizedHttpClient(clientData)
            val request = Request.Builder()
                    .url(SecureSocketService.httpsURL + "/message/${msg.message_id}/open/secure")
                    .build()
            val response = http.newCall(request).execute().body().string();
            val parser = JsonParser()
            val jsonResponse = parser.parse(response).asJsonObject
            val data = jsonResponse.getAsJsonObject("data")
            if (data != null)
                return data.get("message").asString

            return null
        }

        fun attemptToDecryptPendingMessage(openConversationResponse: JsonObject, clientData: ClientData,
                                            aesutil: AESUtil, pendingMessage: MOKMessage)
                : MOKMessage? {

            val data = openConversationResponse.getAsJsonObject("data");
            if (data == null) {
                Log.e("OpenConversationTask", "Can't decrypt. data object not found in ${openConversationResponse.toString()}.")
                return null;
            }
            val encryptedKey = data.get("convKey").asString
            val conversationId = data.get("session_to").asString

            if (conversationId != pendingMessage.sid)
                throw IllegalArgumentException("Can't decrypt the message's sender ID does not match the ID of the " +
                        "key given by the Json object. All messages must have the same sender ID.")

            val conversationKey = aesutil.decrypt(encryptedKey);

            val decryptedList = mutableListOf<MOKMessage>()
            if (DecryptTask.decryptMessage(EncryptedMsg.fromSecret(pendingMessage,
                    conversationKey))) {
                decryptedList.add(pendingMessage)
            } else {
                pendingMessage.msg = getTextEncryptedWithLatestKeys(pendingMessage, clientData)
                if (DecryptTask.decryptMessage((EncryptedMsg.fromSecret(pendingMessage, conversationKey))))
                    decryptedList.add(pendingMessage)
                else
                    Log.e("OpenConversationTask", "can't decrypt ${pendingMessage.message_id}. discarding")
            }
            return pendingMessage


        }

    }

}
