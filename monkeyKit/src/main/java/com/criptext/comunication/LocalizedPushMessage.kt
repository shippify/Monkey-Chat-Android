package com.criptext.comunication

import android.media.Ringtone
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

/**
 * Created by gesuwall on 6/22/16.
 */

class LocalizedPushMessage(locKey: String, locArgs: Array<String>, ringtone: String): PushMessage("", ringtone){
    init {
        insertData(locKey, locArgs, ringtone)
    }

    override fun insertData(textMessage: String, ringtone: String) {
    }

    fun insertData(locKey: String, locArgs: Array<String>, ringtone: String){
        val iosElement = JsonObject()

        val alertElement = JsonObject()
        val array = JsonArray()
        alertElement.addProperty(loc_key,locKey)
        for(arg in locArgs){
            array.add(JsonPrimitive(arg))
        }
        alertElement.add(loc_args, array)

        iosElement.add(alert, alertElement)
        iosElement.addProperty(sound, ringtone)


        json.add(iosData, iosElement)
        json.addProperty(andData, alertElement.toString())
    }

    companion object {
        val loc_key = "loc-key"
        val loc_args = "loc-args"
    }
}