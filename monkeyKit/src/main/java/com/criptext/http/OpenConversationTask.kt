package com.criptext.http

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import com.criptext.ClientData
import com.criptext.MonkeyKitSocketService

import com.criptext.comunication.CBTypes
import com.criptext.comunication.MOKMessage
import com.criptext.lib.KeyStoreCriptext
import com.criptext.security.AESUtil
import com.criptext.security.DecryptTask
import com.criptext.security.EncryptedMsg
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.*
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

/**
 * Created by gesuwall on 6/6/16.
 */

class OpenConversationTask(service: MonkeyKitSocketService, val undecrypted: MOKMessage?) :
        AsyncTask<String, Void, OpenConversationTask.OpenConvData>(){

   val serviceRef: WeakReference<MonkeyKitSocketService>
    val clientData: ClientData

    init {
        this.serviceRef = WeakReference(service)
        clientData = service.serviceClientData

    }

    fun getAESData(monkeyId: String) :String? {
        val service = serviceRef.get()
        if(service != null)
            return KeyStoreCriptext.getString(service, monkeyId)

        return null
    }

    override fun doInBackground(vararg p0: String?): OpenConvData? {
        val conversationId = p0[0]!!
        val response = sendOpenConversationRequest(p0[0]!!,clientData)

        val aesData = getAESData(clientData.monkeyId)
        if(isCancelled || aesData == null)
            return OpenConvData(null, null, "", conversationId)

        val aesutil = AESUtil(aesData)

        if(response != null) {

            if(undecrypted==null) {
                //there is no pending message to decrypt, we are only interested in the keys

                //This encrypted key has the other user's key and IV separated by ':', but we can't use it
                // yet, it is encypted
                val data = response.getAsJsonObject("data")
                val encryptedKey = data.get("convKey").asString
                return OpenConvData(null, null, aesutil.decrypt(encryptedKey), conversationId)
            }

            //decrypt the pending message
            return attemptToDecryptWithOpenResponse(
                    openConversationResponse = response,
                    aesutil = aesutil,
                    pendingMessage = undecrypted,
                    clientData = clientData)
        } else //server failed to handle new keys, discard the message.
            return OpenConvData(null, messageFailDecrypted = undecrypted,
                validKey = KeyStoreCriptext.getString(serviceRef.get(), conversationId),
                conversationId = conversationId)
    }

    override fun onPostExecute(openconvData: OpenConvData?) {
        val service = serviceRef.get()
        if(service != null && openconvData!= null){
            KeyStoreCriptext.putString(service, openconvData.conversationId, openconvData.validKey)
            if(openconvData.messageOkDecrypted!=null)
                service.processMessageFromHandler(CBTypes.onMessageReceived, Array<Any?>(1, { i -> openconvData.messageOkDecrypted}))
            else if(openconvData.messageFailDecrypted!=null) //TODO Handle onMessageFailDecrypt
                service.processMessageFromHandler(CBTypes.onMessageFailDecrypt, Array<Any?>(1, { i -> openconvData.messageFailDecrypted}))
        }
    }

    data class OpenConvData(val messageOkDecrypted:MOKMessage?, val messageFailDecrypted: MOKMessage?,
                            val validKey: String, val conversationId: String)
    companion object {

        fun authorizedHttpClient(clientData: ClientData, timeout: Long) = OkHttpClient().newBuilder()
                .authenticator({ route, response ->
                    val credential = Credentials.basic(clientData.appId, clientData.appKey);
                    response.request().newBuilder()
                            .header("Authorization", credential).build()
                })
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .build()

        fun authorizedHttpClient(clientData: ClientData) = authorizedHttpClient(clientData, 20)

        fun sendOpenConversationRequest(user_to: String, clientData: ClientData): JsonObject? {

            val http = authorizedHttpClient(clientData)

            val data = JsonObject();
            data.addProperty("user_to", user_to);
            data.addProperty("monkey_id", clientData.monkeyId);

            val body = FormBody.Builder().add("data", data.toString()).build()

            val credential = Credentials.basic(clientData.appId, clientData.appKey);
            val request = Request.Builder()
                    .url(MonkeyKitSocketService.httpsURL + "/user/key/exchange")
                    .header("Authorization", credential)
                    .post(body).build()


            val response = http.newCall(request).execute()
            if (response.isSuccessful) {
                val respBody = response.body().string();
                val parser = JsonParser()
                return parser.parse(respBody).asJsonObject
            } else {
                Log.e("sendOpenConversation", response.body().string())
                return null
            }

        }

        fun getTextEncryptedWithLatestKeys(msgId: String, clientData: ClientData): String? {
            //TODO TEST THIS!!
            val http = authorizedHttpClient(clientData)
            val request = Request.Builder()
                    .url(MonkeyKitSocketService.httpsURL + "/message/$msgId/open/secure")
                    .build()
            val response = http.newCall(request).execute().body().string();
            val parser = JsonParser()
            val jsonResponse = parser.parse(response).asJsonObject
            val data = jsonResponse.getAsJsonObject("data")
            if (data != null)
                return data.get("message").asString

            return null
        }


        fun attemptToDecryptWithNewKeys(message: MOKMessage, clientData: ClientData,
                                    newKey: String): Boolean {
            //1st attempt to decrypt, if it works return
            if (DecryptTask.decryptMessage(EncryptedMsg.fromSecret(message, newKey))) {
                return true
            } else {
                //Decryption didn't work with current key. Ask the server to encrypt again with current keys
                //then do a 2nd attempt to decrypt, if it works add to decrypted list
                message.msg = getTextEncryptedWithLatestKeys(message.message_id, clientData) ?: message.msg
                return DecryptTask.decryptMessage((EncryptedMsg.fromSecret(message, newKey)))
                }
        }

        fun attemptToDecryptWithOpenResponse(openConversationResponse: JsonObject, clientData: ClientData,
                                             aesutil: AESUtil, pendingMessage: MOKMessage)
                : OpenConvData? {

            val data = openConversationResponse.getAsJsonObject("data");
            if (data == null) {
                Log.e("OpenConversationTask", "Can't decrypt. data object not found in ${openConversationResponse.toString()}.")
                return null;
            }
            //This encrypted key has the other user's key and IV separated by ':', but we can't use it
            // yet, it is encypted
            val encryptedKey = data.get("convKey").asString
            //MonkeyID of the other user
            val conversationId = data.get("session_to").asString

            if (conversationId != pendingMessage.sid)
                throw IllegalArgumentException("Can't decrypt the message's sender ID does not match the ID of the " +
                        "key given by the Json object. All messages must have the same sender ID.")

            //the result of this decryption is a string key and IV separated by ':'. Ready to use

            try {
                val conversationKey = aesutil.decrypt(encryptedKey);
                val decrypted = attemptToDecryptWithNewKeys(pendingMessage, clientData, conversationKey)
                if(decrypted)
                    return OpenConvData(pendingMessage, null, conversationKey, conversationId)
                else {
                    Log.e("OpenConversationTask", "can't decrypt ${pendingMessage.message_id}. discarding")
                    return OpenConvData(null, pendingMessage, conversationKey, conversationId)
                }
            }
            catch(e:Exception) {
                Log.e("OpenConversationTask", "can't decrypt ${pendingMessage.message_id}. discarding")
            }

            return null;
        }

        fun attemptToDecrypt(message: MOKMessage, clientData: ClientData, aesutil: AESUtil,
                             existingKey: String): String? {
            //1st attempt to decrypt, if it works return
            if (existingKey.isNotEmpty() && DecryptTask.decryptMessage(EncryptedMsg.fromSecret(message, existingKey))) {
                return existingKey
            } else {
                val response = sendOpenConversationRequest(message.sid, clientData)
                //retry to decrypt the pending message with the key from response
                if (response != null) {
                    val convData = attemptToDecryptWithOpenResponse(
                            openConversationResponse = response, aesutil = aesutil,
                            pendingMessage = message, clientData = clientData)

                    if (convData?.messageOkDecrypted != null)
                        return convData!!.validKey
                }

                return null
            }
        }

        fun attemptToDecryptAndUpdateKeyStore(remote: MOKMessage, ctx: Context,
                                          clientData: ClientData, aesutil: AESUtil): MOKMessage? {
            val existingKey = KeyStoreCriptext.getString(ctx, remote.sid)
            val validKey = OpenConversationTask.Companion.attemptToDecrypt(remote, clientData,
            aesutil, existingKey);
            if(validKey != null && validKey != existingKey)
                KeyStoreCriptext.putString(ctx, remote.sid, validKey);
            else if(validKey == null)
                return null
            return remote
        }
    }

}
