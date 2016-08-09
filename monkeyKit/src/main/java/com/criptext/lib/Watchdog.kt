package com.criptext.lib

import android.os.Handler
import android.util.Log
import com.criptext.MonkeyKitSocketService
import java.lang.ref.WeakReference

/**
 * Created by gesuwall on 6/13/16.
 */

class Watchdog(service: MonkeyKitSocketService){
    private val handler: Handler
    private var runnable: Runnable? = null
    private var cancelled: Boolean
    var attempts = 0.0
    get() {
        field += 1
        if(field > 3) field = 0.0
        return field
    }

    val serviceRef: WeakReference<MonkeyKitSocketService>

    init {
        handler = Handler()
        cancelled = false

        serviceRef = WeakReference(service)
    }

    fun start(){

        if(runnable!=null) {
            return
        }
        else
            runnable = Runnable {
                val service = serviceRef.get()
                if(!cancelled) {
                    service?.forceDisconnect()
                    val newTimeout = BASE_TIMEOUT + Math.pow(2.0, attempts).toLong() * 1000L
                    Log.d("Watchdog", "next: $newTimeout")
                    handler.postDelayed(runnable, newTimeout);
                };
            };

        handler.postDelayed(runnable, BASE_TIMEOUT);
    }

    fun cancel(){
        cancelled = true
    }

    companion object {
        private val BASE_TIMEOUT = 4000L;
    }

}
