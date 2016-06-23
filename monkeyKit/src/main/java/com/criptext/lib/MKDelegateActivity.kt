package com.criptext.lib

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import com.criptext.ClientData
import com.criptext.MonkeyKitSocketService
import com.criptext.socket.SecureSocketService

abstract class MKDelegateActivity : AppCompatActivity(), MonkeyKitDelegate {

    var service: MonkeyKitSocketService? = null
    private set

    abstract val serviceClassName: Class<*>

    private val monkeyKitConnection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            val binder = p1 as MonkeyKitSocketService.MonkeyBinder
            val sService = binder.getService(this@MKDelegateActivity)
            service = sService

            onBoundToService()

        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            service = null
        }
    }

    override fun onStart() {
        super.onStart()
        MonkeyKitSocketService.bindMonkeyService(this, monkeyKitConnection, serviceClassName)

    }

    override fun onStop() {
        super.onStop()
        unbindService(monkeyKitConnection)
    }

    abstract fun onBoundToService()
}
