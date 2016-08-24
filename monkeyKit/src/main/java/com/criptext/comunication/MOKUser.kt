package com.criptext.comunication

import com.google.gson.JsonObject

class MOKUser(var monkeyId: String, var info: JsonObject?) {

    constructor(monkeyId: String): this(monkeyId, null)

    fun getAvatarURL(): String? {
        if(info!!.has("avatar")){
            return info!!.get("avatar").asString
        }
        else{
            return "https://monkey.criptext.com/user/icon/default/"+monkeyId
        }
    }

    companion object {

    }
}