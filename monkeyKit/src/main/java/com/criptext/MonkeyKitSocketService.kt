package com.criptext

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.criptext.comunication.*
import com.criptext.database.CriptextDBHandler
import com.criptext.lib.MonkeyKitDelegate
import com.criptext.security.AESUtil
import com.criptext.security.AsyncAESInitializer
import com.criptext.socket.SecureSocketService

/**
 * Created by gesuwall on 5/25/16.
 */

abstract class MonkeyKitSocketService : MsgSenderService() {

    override var portionsMessages: Int = 15
    override var lastTimeSynced: Long = 0L

    private var socketInitialized = false


    val messageHandler: MOKMessageHandler by lazy {
        MOKMessageHandler(this)
    }

    fun downloadFile(fileName: String, props: String, monkeyId: String, runnable: Runnable){
        //TODO DOWNLOAD FILE
        super.fileUploader.downloadFile(fileName, props, monkeyId, runnable)
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
        asyncConnSocket.disconectSocket()
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

    override fun executeInDelegate(method:CBTypes, info:Array<Any>) {
        //super already stores received messages and passes the to delegate
        super.executeInDelegate(method, info)
        when (method) {
            /*
            CBTypes.onConnectOK -> {
                if (info[0] != null && (info[0] as String).compareTo("null") != 0)
                {
                    if (java.lang.Long.parseLong(info[0] as String) >= lastTimeSynced)
                        lastTimeSynced=java.lang.Long.parseLong(info[0] as String)
                }
            }
            onMessageReceived -> {
                //GUARDO EL MENSAJE EN LA BASE DE MONKEY SOLO SI NO HAY DELEGATES
                val message = info[0] as MOKMessage
                val tipo = CriptextDBHandler.getMonkeyActionType(message)
                when (tipo) {
                    MessageTypes.blMessageDefault, MessageTypes.blMessageAudio, MessageTypes.blMessageDocument, MessageTypes.blMessagePhoto, MessageTypes.blMessageShareAFriend, MessageTypes.blMessageScreenCapture -> {
                        storeMessage(message, true, object:Runnable {
                            public override fun run() {
                                for (i in 0..delegates.size() - 1)
                                {
                                    delegates.get(i).onMessageRecieved(message)
                                }
                            }
                        })
                    }
                }
            }
            */
            CBTypes.onAcknowledgeReceived -> {
                Log.d("MonkeyKitSocketService", "ack rec.")
                delegate?.onAcknowledgeRecieved(info[0] as MOKMessage)
            }
            /*
            onSocketConnected -> {
                val hasDelegates = false
                for (i in 0..delegates.size() - 1)
                {
                    delegates.get(i).onSocketConnected()
                    hasDelegates = true
                }
                //MANDO EL GET
                //if(hasDelegates)//Comente esta linea porque
                //Si el service se levanta es bueno que haga un get y obtenga los mensajes
                //que importa si no se actualiza el lastmessage desde el service.
                //Con esto cuando abres el mensaje desde el push siempre muestra los unread messages
                MonkeyKit.instance().sendSync(getLastTimeSynced())
            }
            onSocketDisconnected -> {
                for (i in 0..delegates.size() - 1)
                {
                    delegates.get(i).onSocketDisconnected()
                }
            }
            onNetworkError -> {
                for (i in 0..delegates.size() - 1)
                {
                    delegates.get(i).onNetworkError(info[0] as Exception)
                }
            }
            onDeleteReceived -> {
                for (i in 0..delegates.size() - 1)
                {
                    delegates.get(i).onDeleteRecieved(info[0] as MOKMessage)
                }
            }
            onCreateGroupOK -> {
                for (i in 0..delegates.size() - 1)
                {
                    delegates.get(i).onCreateGroupOK(info[0] as String)
                }
            }
            onCreateGroupError -> {
                for (i in 0..delegates.size() - 1)
                {
                    delegates.get(i).onCreateGroupError(info[0] as String)
                }
            }
            onDeleteGroupOK -> {
                for (i in 0..delegates.size() - 1)
                {
                    delegates.get(i).onDeleteGroupOK(info[0] as String)
                }
            }
            onDeleteGroupError -> {
                for (i in 0..delegates.size() - 1)
                {
                    delegates.get(i).onDeleteGroupError(info[0] as String)
                }
            }
            onContactOpenMyConversation -> {
                for (i in 0..delegates.size() - 1)
                {
                    delegates.get(i).onContactOpenMyConversation(info[0] as String)
                }
            }
            onGetGroupInfoOK -> {
                for (i in 0..delegates.size() - 1)
                {
                    delegates.get(i).onGetGroupInfoOK(info[0] as JsonObject)
                }
            }
            onGetGroupInfoError -> {
                for (i in 0..delegates.size() - 1)
                {
                    delegates.get(i).onGetGroupInfoError(info[0] as String)
                }
            }
            onNotificationReceived -> {
                for (i in 0..delegates.size() - 1)
                {
                    delegates.get(i).onNotificationReceived(info[0] as MOKMessage)
                }
            }
            onMessageBatchReady -> {
                sendGetOK()
                val batch = info[0] as ArrayList<MOKMessage>
                storeMessageBatch(batch, object:Runnable {
                    public override fun run() {
                        for (i in 0..delegates.size() - 1)
                        {
                            delegates.get(i).onMessageBatchReady(batch)
                        }
                    }
                })
            }
            */
        }
    }

    override var delegate: MonkeyKitDelegate? = null
        get() = field
        set(value) {
            field = value
        }

    override fun startSocketConnection(aesUtil: AESUtil?) {
        super.startSocketConnection(aesUtil)
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
        if(pendingMessages.isEmpty()){
            watchdog?.cancel()
            watchdog = null
        }
    }


}
