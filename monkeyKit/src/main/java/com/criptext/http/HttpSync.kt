package com.criptext.http

import android.content.Context
import android.util.Base64
import android.util.Log
import com.criptext.ClientData
import com.criptext.MonkeyKitSocketService
import com.criptext.comunication.*
import com.criptext.lib.KeyStoreCriptext
import com.criptext.security.AESUtil
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

    val isFirstTime: Boolean by lazy { !KeyStoreCriptext.hasSyncedBefore(contextRef.get()) }

    private fun executeHttp(http: OkHttpClient, request: Request): Response? =
        try {
            http.newCall(request).execute()
        } catch (ex: IOException) {
            null
        }

    private fun getBatch(since: Long, qty: Int): SyncResponse {
        val parser = JsonParser()

        var remaining = qty
        var _since = since
        var batch = SyncResponse(listOf(), listOf(), listOf())

        while (remaining > 0) {
            val request = Request.Builder()
                    .url(MonkeyKitSocketService.httpsURL + "/user/messages/" +
                            "${clientData.monkeyId}/$_since/$remaining")
                    .build()
            var response: Response? = null
            while (response == null) {
                val http = OpenConversationTask.authorizedHttpClient(clientData, timeout)
                response = executeHttp(http, request)
                timeout += 10
            }

            if (response.isSuccessful) {
                val body = response.body()
                val jsonResponse = parser.parse(body.string()).asJsonObject
                val data = jsonResponse.getAsJsonObject("data")
                batch += processBatch(data)
                remaining = data.get("remaining").asInt
                if (remaining > 0) {
                    val array = data.get("messages").asJsonArray;
                    //All elements in the array of the Sync response should have a datetime field
                    _since = array[array.size() - 1].asJsonObject.get("datetime").asLong
                }
                body.close()
            } else Log.e("HttpSync", "Sync response error. code: ${response.code()} ${response.body().string()}")
        }

        if (isFirstTime)
            KeyStoreCriptext.setFirstSyncSuccess(contextRef.get())

        return batch
    }

    fun execute(since: Long, qty: Int) = SyncData(clientData.monkeyId, getBatch(since, qty))

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
                if(keyFromStore.isNotEmpty()) {
                    keyMap.put(id, keyFromStore)
                    return keyFromStore
                }
            }
        }
        return ""
    }

    private fun processMessage(messages: MutableList<MOKMessage>, currentMessage: JsonObject,
                               currentMessageType: String, props: JsonObject?, params: JsonObject?): Long {
        val messageIsEncrypted = if(props?.has("encr") ?: false) props!!.get("encr").asInt == 1 else false
        val remote = AsyncConnSocket.createMOKMessageFromJSON(currentMessage, params, props, true)
        if (messageIsEncrypted){
            val existingKey = getKeyForConversation(remote.sid)
            val validKey = OpenConversationTask.attemptToDecrypt(remote, clientData, aesUtil, existingKey)
            if(validKey == null){
                //No key could decrypt this message, discard
                return 0L
            } else {
                if(validKey != existingKey)
                    keyMap.put(remote.sid, validKey) //update the keyMap with valid keys
            }
        }
        else if ((props?.has("encoding") ?: false) && (currentMessageType != MessageTypes.MOKFile)) {
            if(props?.get("encoding")?.asString  == "base64")
                remote.msg = String(Base64.decode(remote.msg.toByteArray(), Base64.NO_WRAP))
        }

        messages.add(remote);
        return remote.datetimeorder
    }
    /**
	 * Procesa el JSONArray que llega despues de un GET O SYNC. Decripta los mensajes que necesitan
	 * ser decriptados y arma un ArrayList de MOKMessages para pasarselo al Thread principal
	 * @param protocol GET o SYNC de MessageTypes
	 * @param data JsonObject "args" del GET o SYNC
	 * @param parser
	 */
	private fun processBatch(data: JsonObject): SyncResponse{
        val parser = JsonParser()
        val array = data.get("messages").asJsonArray;
        val messages: MutableList<MOKMessage> = LinkedList()
        val notifications: MutableList<MOKNotification> = LinkedList()
        val deletes: MutableList<MOKDelete> = LinkedList()

       array.forEach { jsonMessage ->
            val currentMessage = jsonMessage.asJsonObject
            val props = getJsonFromMessage(currentMessage, "props", parser)
            val params = getJsonFromMessage(currentMessage, "params", parser)

            val currentMessageType = currentMessage.get("type").asString
            if (currentMessageType == MessageTypes.MOKText || currentMessageType == MessageTypes.MOKFile) {
                processMessage(messages, currentMessage, currentMessageType, props, params)
            }
            else if(currentMessageType == MessageTypes.MOKNotif){
                //params has to be non null, otherwise what's the point of the notification?
                val remote = AsyncConnSocket.createMOKMessageFromJSON(currentMessage,
                        params ?: JsonObject(), props, true);
                notifications.add(MOKNotification(remote))

            }
            else if(currentMessageType == MessageTypes.MOKProtocolDelete.toString()){
                val remote = AsyncConnSocket.createMOKMessageFromJSON(currentMessage, params, props, false);
                deletes.add(MOKDelete(remote))
            }
            else throw IllegalArgumentException("HttpSync response included a JSON item that is neither Message, Notification, nor Delete")
        }

        return SyncResponse(messages, notifications, deletes);
	}

    data class SyncResponse(val messages: List<MOKMessage>, val notifications: List<MOKNotification>,
                            val deletes: List<MOKDelete>) {

        operator fun plus(resp: SyncResponse) = SyncResponse(messages + resp.messages,
                notifications + resp.notifications, deletes + resp.deletes)

        fun isNotEmpty() = messages.isNotEmpty() || notifications.isNotEmpty() ||  deletes.isNotEmpty()

        fun newTimestamp() = messages.lastOrNull()?.datetime?.toLong() ?: notifications.lastOrNull()?.timestamp
                            ?: deletes.lastOrNull()?.timestamp

    }

    class SyncData(val monkeyId: String, response: SyncResponse?) {
        val notifications: MutableList<MOKNotification>
        val deletes: HashMap<String, MutableList<MOKDelete>>
        val newMessages: HashMap<String, MutableList<MOKMessage>>
        val conversationsToUpdate: LinkedHashSet<String>
        val missingConversations: HashSet<String>
        val users: HashSet<String>
        var newTimestamp: Long

        private fun getMessageList(conversationId: String): MutableList<MOKMessage> {
            if (!newMessages.containsKey(conversationId))
                newMessages[conversationId] = LinkedList()

            return newMessages[conversationId]!!
        }

        private fun getDeleteList(conversationId: String): MutableList<MOKDelete> {
            if (!deletes.containsKey(conversationId))
                deletes[conversationId] = LinkedList()

            return deletes[conversationId]!!
        }

        init {
            notifications = if (response != null) ArrayList(response.notifications) else mutableListOf()
            deletes = hashMapOf()
            newMessages = hashMapOf()
            conversationsToUpdate = LinkedHashSet()
            users = HashSet()
            newTimestamp = response?.newTimestamp() ?: 0L
            missingConversations = HashSet()

            if (response != null) {
                response.messages.forEach { message ->
                    val convId = message.getConversationID(monkeyId)
                    val list = getMessageList(convId)
                    list.add(message)
                    conversationsToUpdate.add(convId)
                    users.add(convId)
                }

                response.deletes.forEach { del ->
                    val convId = del.getConversationID(monkeyId)
                    val list = getDeleteList(convId)
                    list.add(del)
                    conversationsToUpdate.add(convId)
                }

            }
        }

        fun isNotEmpty() = notifications.isNotEmpty() || deletes.isNotEmpty() || newMessages.isNotEmpty()

        fun addMessage(message: MOKMessage) = {
            val list = getMessageList(message.getConversationID(monkeyId))
            list.add(message)
        }

        fun addDelete(delete: MOKDelete) = {
            val list = getDeleteList(delete.getConversationID(monkeyId))
            list.add(delete)
        }

        fun addMessages(messages: List<MOKMessage>) {
            messages.forEach { m -> addMessage(m) }
        }

        fun addDeletes(deletes: List<MOKDelete>) {
            deletes.forEach { m -> addDelete(m) }
        }

        fun addNotifications(notificationsList: List<MOKNotification>) {
                notifications.addAll(notificationsList)
        }

        companion object {
            fun newInstance(ctx: Context?, clientData: ClientData): HttpSync? {
                if (ctx != null) {
                    val aesUtil = AESUtil(ctx, clientData.monkeyId)
                    return HttpSync(ctx, clientData, aesUtil)
                }
                return null
            }
        }
    }
}