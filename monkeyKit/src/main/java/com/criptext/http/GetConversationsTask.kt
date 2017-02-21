package com.criptext.http

import android.os.AsyncTask
import com.criptext.ClientData
import com.criptext.MonkeyKitSocketService
import com.criptext.comunication.CBTypes
import com.criptext.comunication.MOKConversation
import com.criptext.lib.MonkeyJson
import com.criptext.security.AESUtil
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import com.google.gson.JsonParser
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Response
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

/**
 * Created by gesuwall on 2/21/17.
 */

class GetConversationsTask(val serviceRef: WeakReference<MonkeyKitSocketService>,
                           val clientData: ClientData, val aesUtil: AESUtil, val monkeyId: String,
                   val qty: Int, val timestamp: Long): AsyncTask<Void, Void, Result<List<MOKConversation>, Exception>>() {

    private fun decryptMessages(lists: Array<List<MOKConversation>>) {
        val undecrypted = lists[1]
        val ctx = serviceRef.get()
        if (ctx != null)
            undecrypted.filter { it -> it.lastMessage != null }
                .forEach { it.lastMessage = OpenConversationTask.attemptToDecryptAndUpdateKeyStore(
                            it.lastMessage!!, ctx, clientData, aesUtil)
                }
    }

    override fun doInBackground(vararg p0: Void?): Result<List<MOKConversation>, Exception> {
        val request = MonkeyKitAPI.getConversations(monkeyId, qty, timestamp)
        val client = OpenConversationTask.authorizedHttpClient(clientData)
        return Result.of { client.newCall(request).execute() }
            .map { res ->
                val lists = MonkeyJson.parseConversationsList(res.body().string())
                decryptMessages(lists) //mutate the objects in result list
                lists[0]
            }
    }

    override fun onPostExecute(result: Result<List<MOKConversation>, Exception>?) {
        val service = serviceRef.get()
        if (service != null)
            when (result) {
                is Result.Success -> service.processMessageFromHandler(CBTypes.onGetConversations,
                        arrayOf(result.value, null))
                is Result.Failure -> service.processMessageFromHandler(CBTypes.onGetConversations,
                        arrayOf(null, result.error))
            }
    }

}