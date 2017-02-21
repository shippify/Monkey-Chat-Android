package com.criptext.lib

import android.util.Base64
import android.util.Log
import com.criptext.comunication.AsyncConnSocket
import com.criptext.comunication.MOKConversation
import com.criptext.comunication.MOKMessage
import com.criptext.comunication.MessageTypes
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.util.*

/**
 * Created by gabriel on 2/3/17.
 */

class MonkeyJson {

    companion object {
    /**
     * An open response returns a string with the last seen value if is a conversation with an user.
     * If it's a group then it returns an object with the user's monkey id as key and last seen string
     * as value. Since we currently only support one last seen value, in groups we take the lowest
     * value of the object. This method will eventually be removed.
     * @param props props object of the open response
     * @return
     */
        public fun getLastSeenFromOpenResponseProps(props: JsonObject): String? {
            if (!props.has("last_seen"))
                return null

            try {
                val lastSeenObj = props.get("last_seen").asJsonObject
                val it : Iterator<Map.Entry<String, JsonElement>> = lastSeenObj.entrySet().iterator()
                var result = Long.MAX_VALUE
                while (it.hasNext())
                    result = Math.min(it.next().value.asLong, result)

                if (result == Long.MAX_VALUE)
                    return null
                else return "" + result;
            } catch (ex: IllegalStateException){
                return props.get("last_seen").asString
            }
        }

        fun parsePendingMsgsFromFile(fileContents: String, separator: String): List<JsonObject> {
            val jsonArray = fileContents.split(separator)
            val parser = JsonParser()
            return jsonArray
                .map { it ->
                    try {
                    parser.parse(it).asJsonObject
                    } catch (ex: Exception) {
                        JsonObject()
                    }
                }.filter { it -> it != null && it.entrySet().size > 0 }
        }

        fun sanitizePendingMsgsForFile(list: List<JsonObject>): List<JsonObject> {
            return list.filter { it -> it.entrySet().size > 0 && it.has("args") }
        }

        /**
         * Parses de arraay from getConversations response.
         * @return an array with 2 lists of MOKConversation. The first list is the parsed conversations
         * and the second list is a subset of the first list, containing conversations whose lastmessage
         * needs to be decrypted.
         */
        fun parseConversationsList(jsonArrayStr: String): Array<ArrayList<MOKConversation>> {
            val parser = JsonParser()
            val array = parser.parse(jsonArrayStr).asJsonArray
            val conversationList = ArrayList<MOKConversation>()
            val conversationsToDecrypt = ArrayList<MOKConversation>()

            for (jsonMessage in array) {
                var currentConv: JsonObject?
                var currentMessage: JsonObject?
                var remote: MOKMessage? = null
                try {
                    currentConv = jsonMessage.asJsonObject
                    currentMessage = currentConv.getAsJsonObject("last_message")!!
                    val paramsStr = currentMessage.get("params")?.asString
                    //init params props
                    val params = when (paramsStr) {
                        null -> null
                        "{}" -> null
                        "null" -> null
                        else -> parser.parse(paramsStr).asJsonObject
                    }
                    val propsStr = currentMessage.get("props")?.asString
                    val props = if (propsStr != null) parser.parse(propsStr).asJsonObject else null
                    val currentMsgType = currentMessage.get("type")?.asString


                    val jsonArray = currentConv.get("members")?.asJsonArray ?: JsonArray()
                    val arrayList = ArrayList<String>()
                    for (member in jsonArray) {
                        arrayList.add(member.asString)
                    }

                    val newConv = MOKConversation(
                        conversationId = currentConv.get("id").asString,
                        info = currentConv.get("info").asJsonObject,
                        members = arrayList.toTypedArray(),
                        lastMessage = remote,
                        lastSeen = currentConv.get("last_seen").asDouble.toLong() * 1000,
                        unread = currentConv.get("unread").asInt,
                        lastModified = currentConv.get("last_modified")?.asDouble?.toLong() ?: 0L)

                    conversationList.add(newConv)

                    if (currentMsgType == MessageTypes.MOKText || currentMsgType == MessageTypes.MOKFile) {
                        remote = AsyncConnSocket.createMOKMessageFromJSON(currentMessage, params, props, true)
                        newConv.lastMessage = remote
                        if (props?.get("encr")?.asInt == 1) {
                            conversationsToDecrypt.add(newConv)
                        } else if (remote.type != MessageTypes.MOKFile
                                && props?.get("encoding")?.asString == "base64") {
                            remote.msg = String(Base64.decode(remote.msg.toByteArray(), Base64.NO_WRAP))
                        }
                    }
                } catch (ex: IllegalArgumentException) {
                    Log.e("MonkeyKit", "Error fetching conversation: " + ex.message)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }

            return arrayOf(conversationList, conversationsToDecrypt)

        }
    }
}
