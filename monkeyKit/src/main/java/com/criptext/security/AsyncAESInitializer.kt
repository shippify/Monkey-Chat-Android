package com.criptext.security

import android.content.Context
import android.os.AsyncTask
import android.provider.Settings

import com.criptext.socket.SecureSocketService

import java.lang.ref.WeakReference

/**
 * Created by gesuwall on 6/1/16.
 */
class AsyncAESInitializer(socketService: SecureSocketService, internal var monkeyID: String) : AsyncTask<Void, Void, AESUtil>() {
    internal var socketServiceRef: WeakReference<SecureSocketService>

    init {
        socketServiceRef = WeakReference(socketService)
    }

    override fun doInBackground(vararg voids: Void): AESUtil? {
            return AESUtil(socketServiceRef.get().context, monkeyID)
    }

    override fun onPostExecute(aesUtil: AESUtil?) {
        val service = socketServiceRef.get()
        service?.startSocketConnection(aesUtil)
    }
}
