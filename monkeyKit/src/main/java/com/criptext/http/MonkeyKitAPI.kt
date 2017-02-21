package com.criptext.http

import com.criptext.MonkeyKitSocketService
import com.google.gson.JsonObject
import okhttp3.FormBody
import okhttp3.Request

/**
 * Created by gesuwall on 2/21/17.
 */

class MonkeyKitAPI {

    companion object {
        fun getConversations(monkeyId: String, qty: Int, timestamp: Long): Request {
            val data = JsonObject()
            data.addProperty("monkeyId", monkeyId)
            data.addProperty("qty", qty)
            data.addProperty("timestamp", timestamp)
            val body = FormBody.Builder().add("data", data.toString()).build()
            return Request.Builder()
                .url(MonkeyKitSocketService.httpsURL +"/user/conversations")
                .post(body)
                .build()
        }
    }
}