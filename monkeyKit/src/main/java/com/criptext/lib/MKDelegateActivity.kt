package com.criptext.lib

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import com.criptext.ClientData
import com.criptext.MonkeyKitSocketService
import com.criptext.comunication.LocalizedPushMessage
import com.criptext.comunication.MOKMessage
import com.criptext.comunication.PushMessage
import com.criptext.socket.SecureSocketService
import com.google.gson.JsonObject
import java.util.*
import javax.crypto.EncryptedPrivateKeyInfo

abstract class MKDelegateActivity : AppCompatActivity(), MonkeyKitDelegate {

    var service: MonkeyKitSocketService? = null

    abstract val serviceClassName: Class<*>

    private val monkeyKitConnection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            Log.d("MKDelegateActivity", "service connected")
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

    fun persistFileMessageAndSend(filePath: String, monkeyIDTo: String, fileType: Int,
                                  params: JsonObject, pushMessage: PushMessage, isEncrypted: Boolean): MOKMessage{
        val socketService = service
        if(socketService != null){
            val pushStr = pushMessage.toString();
            return socketService.fileUploader.persistFileMessageAndSend(filePath, monkeyIDTo, fileType,
                    params, pushStr, isEncrypted)
        } else
            throw IllegalStateException("MonkeyKitSocketService is not ready yet.")
    }

    fun resendFile(fileMessageId: String){
        val socketService = service
        if(socketService != null){
            socketService.fileUploader.resendFile(fileMessageId)
        } else
            throw IllegalStateException("MonkeyKitSocketService is not ready yet.")
    }
}
