package com.criptext.http

import android.os.AsyncTask
import com.criptext.ClientData
import com.criptext.MonkeyKitSocketService
import com.criptext.comunication.CBTypes
import java.lang.ref.WeakReference
import java.net.SocketTimeoutException

/**
 * Created by gesuwall on 10/13/16.
 */

class HttpSyncTask(service: MonkeyKitSocketService, val since: Long, val qty: Int): AsyncTask<Void, Void, HttpSync.SyncData>() {
    val serviceRef: WeakReference<MonkeyKitSocketService>
    val clientData: ClientData

    init {
        serviceRef = WeakReference(service)
        clientData = service.serviceClientData
    }

    fun execHttpSync(): HttpSync.SyncData? {
        try {
            val httpSync = HttpSync.SyncData.newInstance(serviceRef.get(), clientData)
            return httpSync?.execute(since, qty)
        } catch (ex: SocketTimeoutException) {
            return execHttpSync()
        }

    }

    override fun doInBackground(vararg p0: Void?): HttpSync.SyncData? {
        return execHttpSync()
    }

    override fun onPostExecute(result: HttpSync.SyncData?) {
        val service = serviceRef.get()
        if(result != null) //result is only null if context died long ago
            service?.processMessageFromHandler(CBTypes.onSyncComplete, arrayOf(result))
    }

}