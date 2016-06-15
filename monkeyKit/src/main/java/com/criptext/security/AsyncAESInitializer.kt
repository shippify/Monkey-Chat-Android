package com.criptext.security

import android.content.Context
import android.os.AsyncTask
import android.provider.Settings
import com.criptext.MsgSenderService
import com.criptext.lib.PendingMessageStore

import com.criptext.socket.SecureSocketService
import com.google.gson.JsonObject

import java.lang.ref.WeakReference

/**
 * Asynchronously performs operations needed to start a socket service. the onPostExecute method
 * should call startSocketConnection at the end.
 * Created by gesuwall on 6/1/16.
 */
class AsyncAESInitializer(socketService: SecureSocketService, internal var monkeyID: String) : AsyncTask<Void, Void, AsyncAESInitializer.InitializerResult>() {
    internal var socketServiceRef: WeakReference<SecureSocketService>
    private val isSyncService: Boolean

    init {
        socketServiceRef = WeakReference(socketService)
        isSyncService = !(socketService is MsgSenderService)
    }

    override fun doInBackground(vararg voids: Void): InitializerResult? {
            val context = socketServiceRef.get().context
            var pendingMessages: List<JsonObject>? = null
            if(!isSyncService)
                pendingMessages = PendingMessageStore.retrieve(context)
            return InitializerResult(pendingMessages, AESUtil(context, monkeyID))
    }

    override fun onPostExecute(result: InitializerResult?) {
        val service = socketServiceRef.get()
        val senderService = service as? MsgSenderService
        val messages = result!!.pendingMessages
        if(messages != null && messages.isNotEmpty())
            senderService?.addPendingMessages(messages)
        service?.startSocketConnection(result.util)
    }

    data class InitializerResult(val pendingMessages: List<JsonObject>?, val util: AESUtil)
}
