package com.criptext

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.criptext.comunication.AsyncConnSocket
import com.criptext.comunication.CBTypes
import com.criptext.comunication.MOKMessage
import com.criptext.comunication.MOKMessageHandler
import com.criptext.lib.Watchdog
import com.criptext.security.AESUtil
import com.criptext.security.AsyncAESInitializer
import com.criptext.socket.SecureSocketService
import org.json.JSONObject

/**
 * Created by gesuwall on 5/25/16.
 */

class MonkeyKitSocketService : Service(), SecureSocketService {


    override var portionsMessages: Int = 15
    override var lastTimeSynced: Long = 0L

    private lateinit var clientData: ClientData
    private lateinit var aesutil: AESUtil
    private var watchdog: Watchdog? = null
    private lateinit var asyncConnSocket: AsyncConnSocket

    val messageHandler: MOKMessageHandler by lazy {
        MOKMessageHandler(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        clientData = ClientData(intent!!)
        val asyncAES = AsyncAESInitializer(this, clientData.monkeyId)
        return MonkeyBinder()
    }

    /**
     * sends a message to a bound client, so that the client can process it and update its UI.
     * @param type the type of the message
     * @param obj An object that may be a MOKMessage or a list of messages depending on the type
     */
    fun executeInDelegate(type: CBTypes, obj: Any){

    }

    /**
     * Adds a message to a list of messages that haven't been decrypted yet because the necessary
     * keys are missing. Once the key is received, all of those messages are decrypted, stored in
     * the database and any bound client are notified afterwards.
     * @param encrypted message that has not been decrypted yet.
     */
    fun addMessageToDecrypt(encrypted: MOKMessage){

    }

    /**
     * Notifies the MonkeyKit server that the current user has opened an UI with conversation with
     * another user or a group. The server will notify the other party and will return any necessary
     * keys for decrypting messages sent by the other party.
     *
     * This method can also be used to retrieve any missing AES keys.
     */
    fun sendOpenConversation(conversationID: String){

    }

    /**
     * Removes a message encoded in a string from a list of messages that have not been delivered yet
     * so that MonkeyKit won't try to resend it anymore.
     */
    fun removePendingMessage(stringMsg: String){

    }

    inner class MonkeyBinder : Binder() {
        fun getService() : MonkeyKitSocketService {
            return this@MonkeyKitSocketService;
        }
    }

    override fun startSocketConnection(aesUtil: AESUtil?) {
        if(aesUtil != null) {
            this.aesutil = aesUtil
            startSocketConnection()
        }
    }

    override fun startSocketConnection() {
        asyncConnSocket = AsyncConnSocket(clientData, messageHandler, this);
    }

    override val context: Context
    get() = this@MonkeyKitSocketService

    override val appContext: Context
        get() = applicationContext

    override val serviceClientData: ClientData
        get() = clientData

    override fun decryptAES(encryptedText: String) = aesutil.decrypt(encryptedText)

    override fun startWatchdog(){
        if(watchdog == null) {
                watchdog = Watchdog();
            }
            watchdog!!.synced = false;
            Log.d("Watchdog", "Watchdog ready sending Sync");
            watchdog!!.start();
    }

    override fun isSocketConnected() = asyncConnSocket.isConnected

    override fun sendJsonThroughSocket(json: JSONObject) {
        asyncConnSocket.sendMessage(json)
    }

    override fun notifySyncSuccess() {
        watchdog?.synced = true
    }



}
