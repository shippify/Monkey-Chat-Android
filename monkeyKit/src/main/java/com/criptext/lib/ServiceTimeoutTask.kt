package com.criptext.lib

import android.os.AsyncTask
import com.criptext.MonkeyKitSocketService
import java.lang.ref.WeakReference

/**
 * Created by gesuwall on 6/24/16.
 */

class ServiceTimeoutTask(service: MonkeyKitSocketService): AsyncTask<Int, Int, Int>(){
    private val serviceRef: WeakReference<MonkeyKitSocketService>
    init {
        serviceRef = WeakReference(service)
    }
    override fun doInBackground(vararg p0: Int?): Int? {
        Thread.sleep(7000)
        return 0
    }

    override fun onPostExecute(result: Int?) {
        val service = serviceRef.get()
        if(service != null && service.delegate == null)
            service.stopSelf()
    }

}
