package com.criptext.lib

import android.os.AsyncTask
import android.util.Log
import com.criptext.MonkeyKitSocketService
import java.lang.ref.WeakReference

/**
 * Created by gesuwall on 6/24/16.
 */

class ServiceTimeoutTask(service: MonkeyKitSocketService): AsyncTask<Int, Int, Int>(){
    private val serviceRef: WeakReference<MonkeyKitSocketService> = WeakReference(service)

    init {
        //Every time we create a new task reset the start time
        // so that we can keep the service alive longer
        startTime = System.currentTimeMillis();
    }
    override fun doInBackground(vararg p0: Int?): Int? {
        Thread.sleep(TIMEOUT)
        return 0
    }

    override fun onPostExecute(result: Int?) {
        val service = serviceRef.get()
        if(service != null && service.delegateHandler.newMessageDelegate == null){
            val currentTime = System.currentTimeMillis();
            if(currentTime - startTime > TIMEOUT) {
                Log.d("TIMEOUT", "kill service")
                service.stopSelf()
            }
        }
    }

    companion object {
        private var startTime = 0L
        private val TIMEOUT = 10000L
    }

}
