package com.criptext.database

import android.os.AsyncTask
import android.util.Log
import com.criptext.ClientData
import com.criptext.gcm.MonkeyGcmListenerService
import java.lang.ref.WeakReference

/**
 * Created by gesuwall on 6/22/16.
 */

class ClientDataTask(service: MonkeyGcmListenerService): AsyncTask<Void, Void, ClientData>(){
    val serviceRef: WeakReference<MonkeyGcmListenerService>

    init {
        serviceRef = WeakReference(service)
    }
    override fun doInBackground(vararg p0: Void?): ClientData? {
        Thread.sleep(10000);
        return null
    }

    override fun onPostExecute(result: ClientData?) {
        Log.d("ClientDataTask", "GcmService is alive? ${serviceRef.get() != null}")
    }

}