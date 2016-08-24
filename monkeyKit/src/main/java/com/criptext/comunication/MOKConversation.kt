package com.criptext.comunication

import com.google.gson.JsonObject

class MOKConversation(var conversationId: String, var info: JsonObject ?, var members: Array<String> ?, var lastMessage: MOKMessage ?,
                      var lastSeen: Long, var unread: Int, var lastModified: Long) {

    constructor(conversationId: String):this(conversationId, null, null, null, 0, 0, 0)

    fun getAvatarURL(): String? {
        if(info!!.has("avatar")){
            return info!!.get("avatar").asString
        }
        else{
            return "https://monkey.criptext.com/user/icon/default/"+conversationId
        }
    }

    fun isGroup(): Boolean{
        return conversationId.contains("G:")
    }

    companion object {

    }
}