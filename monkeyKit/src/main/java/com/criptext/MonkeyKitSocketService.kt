package com.criptext

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.criptext.comunication.AsyncConnSocket

/**
 * Created by gesuwall on 5/25/16.
 */

class MonkeyKitSocketService : Service() {
    private lateinit var clientData: ClientData
    lateinit var asyncConnSocket: AsyncConnSocket

    override fun onBind(intent: Intent?): IBinder? {
        clientData = ClientData(intent!!)
        return MonkeyBinder()
    }

    inner class MonkeyBinder : Binder() {
        fun getService() : MonkeyKitSocketService {
            return this@MonkeyKitSocketService;
        }
    }

}
