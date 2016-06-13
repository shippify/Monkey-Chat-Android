package com.criptext

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.criptext.comunication.*
import com.criptext.lib.MonkeyKitDelegate
import com.criptext.lib.Watchdog
import com.criptext.security.AESUtil
import com.criptext.security.AsyncAESInitializer
import com.criptext.socket.SecureSocketService
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.util.*

/**
 * Created by gesuwall on 5/25/16.
 */

abstract class MonkeyKitSocketService : MsgSenderService(), SecureSocketService {

    override var portionsMessages: Int = 15
    override var lastTimeSynced: Long = 0L

    private var socketInitialized = false


    val messageHandler: MOKMessageHandler by lazy {
        MOKMessageHandler(this)
    }


    fun downloadFile(fileName: String, props: String, monkeyId: String, runnable: Runnable){
        //TODO DOWNLOAD FILE
    }


    override fun onBind(intent: Intent?): IBinder? {
        if(delegate == null) {
            clientData = ClientData(intent!!)
            val asyncAES = AsyncAESInitializer(this, clientData.monkeyId)
            asyncAES.execute()
            return MonkeyBinder()
        }

        return null
    }

    override fun onUnbind(intent: Intent?): Boolean {
        asyncConnSocket.sendLogout()
        return super.onUnbind(intent)
    }

    inner class MonkeyBinder : Binder() {

        fun getService(delegate: MonkeyKitDelegate): MonkeyKitSocketService{
            this@MonkeyKitSocketService.delegate = delegate
            return this@MonkeyKitSocketService;
        }

    }
    override fun addMessageToDecrypt(encrypted: MOKMessage) {
        //TODO ADD UNDECRYPTED MAYBE?
    }

    override var delegate: MonkeyKitDelegate? = null
        get() = field
        set(value) {
            field = value
        }

    override fun startSocketConnection(aesUtil: AESUtil?) {
        if(aesUtil != null) {
            this.aesutil = aesUtil
            startSocketConnection()
        }
    }

    override fun startSocketConnection() {
        asyncConnSocket = AsyncConnSocket(clientData, messageHandler, this);
        asyncConnSocket.conectSocket()
        socketInitialized = true
    }

    override val context: Context
    get() = this@MonkeyKitSocketService

    override val appContext: Context
        get() = applicationContext

    override val serviceClientData: ClientData
        get() = clientData

    override fun decryptAES(encryptedText: String) = aesutil.decrypt(encryptedText)

    override fun isSocketConnected(): Boolean = socketInitialized && asyncConnSocket.isConnected

    override fun notifySyncSuccess() {
        watchdog?.synced = true
    }


}
