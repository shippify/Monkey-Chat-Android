package com.criptext.comunication

import com.google.gson.JsonObject

/**
 * Object that represents a message that will be received as a notification on any device. A
 * ringtone can be specified only for iOS devices.
 * Created by gesuwall on 6/22/16.
 */

open class PushMessage(textMessage: String, ringtone: String){
    protected val json : JsonObject
    init {
        json = JsonObject()
        insertData(textMessage, ringtone)
    }

    open fun insertData(textMessage: String, ringtone: String) {
        val iosElement = JsonObject()
        iosElement.addProperty(alert, textMessage)
        iosElement.addProperty(sound, ringtone)

        val androidElement = JsonObject()
        androidElement.addProperty(alert, textMessage)

        json.addProperty(text, textMessage)
        json.add(iosData, iosElement)
        json.add(andData, androidElement)
    }

    override fun toString(): String {
        return json.toString().replace("\\\\","\\")
    }
    constructor(textMessage: String): this(textMessage, default)

    companion object {
        val text = "text"
        val iosData = "iosData"
        val alert = "alert"
        val sound = "sound"
        val andData = "andData"
        val default = "default"
    }
}
