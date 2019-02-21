package com.criptext

import android.app.Service
import android.content.*
import android.os.AsyncTask
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.util.Base64
import android.util.Log
import com.criptext.comunication.*
import com.criptext.database.CriptextDBHandler
import com.criptext.http.*
import com.criptext.lib.*
import com.criptext.lib.delegates.*
import com.criptext.security.AESUtil
import com.criptext.security.AsyncAESInitializer
import com.criptext.security.RandomStringBuilder
import com.google.gson.JsonObject
import org.apache.http.client.ClientProtocolException
import org.apache.http.client.methods.HttpGet
import org.apache.http.params.BasicHttpParams
import org.apache.http.params.HttpConnectionParams
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.*

/**
 * Created by gesuwall on 5/25/16.
 */

abstract class MonkeyKitSocketService : Service() {

    /**
     * How many messages should have every batch from sync
     */
    var portionsMessages: Int = 15
    /**
     * A timestamp to send as argument for sync. Server will return messages sent after that timestamp
     * It should only be updated when a message is received.
     */
    var lastTimeSynced: Long = 0L
    set (value){
        if(status != ServiceStatus.dead && value > field)
            //only update last sync if service is not dead.
            //setting a lower value is not allowed. It should always increase
            field = value
    }
    /**
     * object that holds data about the app and the logged in user.
     */
    protected lateinit var clientData: ClientData
    /**
     * object used to encrypt, decrypt and generate AES Keys.
     */
    protected lateinit var aesutil: AESUtil
    /**
     * Object that periodically restarts the socket connection if messages were not successfuly delivered
     */
    protected var watchdog: Watchdog? = null
    /**
     * Object that manages the socket connection and runs it in a background thread.
     */
    private lateinit var asyncConnSocket: AsyncConnSocket
    /**
     * Object that manage user methods over http
     */
    internal lateinit var userManager: UserManager
    /**
     * Object that manage group methods over http
     */
    internal lateinit var groupManager : GroupManager
    /**
     * Delegate object that will execute callbacks
     */
    private val delegateHandler = DelegateHandler()
    /**
     * true if the service was started manually only for sync
     */
    var startedManually: Boolean = false
    private set
    /**
     * Keeps the CPU on even if the screen is turned off while  a lock is held
     */
    internal var wakeLock: PowerManager.WakeLock? = null

    /**
     * Persists sent messages until they are acknowledged by server
     */
    var pendingMessageStore : PendingMessageStore? = null
    /**
     * List of actions to execute after socket is connected and sync is complete.
     */
    internal val pendingActions: LinkedList<Runnable> = LinkedList();

    var broadcastReceiver: BroadcastReceiver? = null

    lateinit var messageHandler: MOKMessageHandler
    private set

    internal var receiver : ConnectionChangeReceiver? = null

    private val messagesReceivedDuringSync: LinkedList<MOKMessage> = LinkedList();
    private val notificationsReceivedDuringSync: LinkedList<MOKNotification> = LinkedList();
    private val deletesReceivedDuringSync: LinkedList<MOKDelete> = LinkedList();

    val hasDelegate: Boolean
        get() = delegateHandler.hasDelegate

    /**
     * Starts MonkeyFileService to download a file. once the download is finished. the
     * onFileDownloadFinished callback is executed.
     * @param fileMessageId the ID of the message to download
     * @param fileName The file to download's name
     * @param props A JSON encoded string with the messages props
     * @param monkeyId the monkey ID of the user that originally sent this file
     * @param sortdate the timestamp used for sorting the messages.
     * @param conversationId a unique identifier of the conversation to while the downloaded message
     * belongs to.
     *
     */
    fun downloadFile(fileMessageId: String, fileName: String, props: String, monkeyId: String,
                     sortdate: Long, conversationId: String){
        if(!serviceAPIisReady){
            pendingActions.add(Runnable {
                downloadFile(fileMessageId, fileName, props, monkeyId, sortdate, conversationId)
            })
        } else {
            val intent = Intent(this, uploadServiceClass)
            intent.putExtra(MOKMessage.MSG_KEY, fileName)
            intent.putExtra(MOKMessage.PROPS_KEY, props)
            intent.putExtra(MOKMessage.SID_KEY, monkeyId)
            intent.putExtra(MOKMessage.ID_KEY, fileMessageId)
            intent.putExtra(MOKMessage.DATESORT_KEY, sortdate)
            intent.putExtra(MOKMessage.CONVERSATION_KEY, conversationId)
            intent.putExtra(MonkeyFileService.ISUPLOAD_KEY, false)
            clientData.fillIntent(intent)

            startService(intent)
        }
    }

    private fun playPendingActions(){
        val totalActions = pendingActions.size
        for (i in 1..totalActions){
            val action = pendingActions.removeAt(0)
            action.run()
        }
    }

    fun initPendingMessageStore(list: List<JsonObject>) {
        pendingMessageStore = PendingMessageStore(list)
    }


    private fun addDataToSyncResponse(response: HttpSync.SyncData){
        response.addMessages(messagesReceivedDuringSync)
        response.addNotifications(notificationsReceivedDuringSync)
        messagesReceivedDuringSync.clear()
    }

    fun processMessageFromHandler(method: CBTypes, info: Array<Any?>) {
        try {
            when (method) {
                CBTypes.onSocketConnected -> {
                    playPendingActions()
                    resendPendingMessages()
                    sendSync()
                    delegateHandler.processMessageFromHandler(method, info)
                }
                CBTypes.onSocketDisconnected -> {
                    //If socket disconnected and this handler is still alive we should reconnect
                    //immediately.
                    startSocketConnection()
                    delegateHandler.processMessageFromHandler(method, info)
                }
                CBTypes.onMessageReceived -> {
                    val message = info[0] as MOKMessage
                    if (status == ServiceStatus.syncing)
                        messagesReceivedDuringSync.add(message)
                    else {
                        val tipo = CriptextDBHandler.getMonkeyActionType(message);
                        if (tipo == MessageTypes.blMessageAudio ||
                                tipo == MessageTypes.blMessagePhoto ||
                                tipo == MessageTypes.blMessageDocument ||
                                tipo == MessageTypes.blMessageScreenCapture ||
                                tipo == MessageTypes.blMessageShareAFriend ||
                                tipo == MessageTypes.blMessageDefault)
                            storeReceivedMessage(message, Runnable {
                                //Message received and stored, update lastTimeSynced with with the timestamp
                                //that the server gave the message
                                lastTimeSynced = message.datetime.toLong()
                                delegateHandler.processMessageFromHandler(method, info)
                                if (startedManually && !delegateHandler.hasDelegate)  //if service started manually, stop it manually with a timeout task
                                    ServiceTimeoutTask(this).execute()
                            })
                    }
                }
                CBTypes.onSyncComplete -> {
                    val batch = info[0] as HttpSync.SyncData;
                    status = MonkeyKitSocketService.ServiceStatus.running
                    //add messages that were received while syncing
                    addDataToSyncResponse(batch)

                    syncDatabase(batch, Runnable {
                        //At this point initialization is complete. We are ready to receive and send messages
                        //since status could have changed from initializing to bound, or running, let's play pending actions.
                        //this is needed for uploading photos.
                        lastTimeSynced = batch.newTimestamp
                        KeyStoreCriptext.setLastSync(this, lastTimeSynced)
                        playPendingActions()
                        delegateHandler.processMessageFromHandler(method, info)
                        if (startedManually && !delegateHandler.hasDelegate)  //if service started manually, stop it manually with a timeout task
                            ServiceTimeoutTask(this).execute()
                    })
                }
                CBTypes.onDeleteReceived -> {
                    lastTimeSynced = info[3].toString().toLong()
                    pendingMessageStore?.removePendingMessage(info[0] as String, { watchdog?.cancel() })
                    delegateHandler.processMessageFromHandler(method, info)
                }
                CBTypes.onGetConversations -> {
                    delegateHandler.processMessageFromHandler(method, info)
                    if (status == MonkeyKitSocketService.ServiceStatus.initializing) {
                        //this is the first time service starts, so after adding all conversations, connect the socket
                        startSocketConnection()
                    }
                }
                CBTypes.onNotificationReceived -> {
                    val messageId = info[0] as String
                    val senderId = info[1] as String
                    val receipientId = info[2] as String
                    val params = info[3] as JsonObject
                    val datetime = info[4] as String

                    if (status == MonkeyKitSocketService.ServiceStatus.syncing)
                        notificationsReceivedDuringSync.add(MOKNotification(messageId, senderId,
                                receipientId, params, JsonObject(), datetime.toLong()))
                    else delegateHandler.processMessageFromHandler(method, info)
                }
                CBTypes.onAcknowledgeReceived -> {
                    pendingMessageStore?.removePendingMessage(info[3] as String, { watchdog?.cancel() })
                    delegateHandler.processMessageFromHandler(method, info)
                }
                else -> delegateHandler.processMessageFromHandler(method, info)
            }
        } catch (e: UninitializedPropertyAccessException) {
            pendingActions.add(Runnable { processMessageFromHandler(method, info)})
        }

    }

    private fun initializeMonkeyKitService(){
        messageHandler = MOKMessageHandler(this)
        status = ServiceStatus.initializing
        val asyncAES = AsyncAESInitializer(this)
        asyncAES.initialize()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.getBooleanExtra("start_from_push", false) ?: false) {
            startedManually = true
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MonkeyKitSocketService")
            wakeLock?.acquire()
            initializeMonkeyKitService()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        initializeMonkeyKitService()
        return MonkeyBinder()
    }

    fun initialize(aesUtil: AESUtil, cdata: ClientData, lastSync: Long) {
        lastTimeSynced = lastSync
        clientData = cdata
        userManager = UserManager(this, aesUtil)
        groupManager = GroupManager(this, aesUtil)
        broadcastReceiver = FileBroadcastReceiver(this)
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver!!,
                IntentFilter(MonkeyFileService.UPLOAD_ACTION))
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver!!,
                IntentFilter(MonkeyFileService.DOWNLOAD_ACTION))
        this.aesutil = aesUtil
    }

    fun startFirstSocketConnection(aesUtil: AESUtil, cdata: ClientData, lastSync: Long) {
        initialize(aesUtil, cdata, lastSync)
        //since this is the first time we are connecting, let's get all conversations before syncing
        if(clientData.monkeyId != null && !clientData.monkeyId.equals("")) {
            userManager.getConversations(clientData.monkeyId, 30, 0)
        }
    }
    /**
     * This method gets called by the Async Intializer on its PostExecute method.
     */
    fun startSocketConnection(aesUtil: AESUtil, cdata: ClientData, lastSync: Long) {
        initialize(aesUtil, cdata, lastSync)
        startSocketConnection()
        startConnectivityBroadcastReceiver()
    }

    fun startSocketConnection() {
        println("START SOCKET CONNECTION")
        if(status == ServiceStatus.dead)
            return //status should be equal to initializing, if dead dont do anything
        try {
            asyncConnSocket = AsyncConnSocket(clientData, messageHandler, this);
            asyncConnSocket.conectSocket()
        } catch (ex: UninitializedPropertyAccessException) {
            //Sometimes service calls this function without previously initializing
            Log.e("MonkeyKit", "service not initialized! ${ex.message}" )
            initializeMonkeyKitService()
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        delegateHandler.clear()
        if(startedManually) { //if service started manually, stop it manually with a timeout task
            ServiceTimeoutTask(this).execute()
        }
        return false
    }

    fun persistUnsentMessages() {
        try {
            if(pendingMessageStore == null){
                val task = PendingMessageStore.AsyncCleanTask(this)
                task.execute()
            }
            
            val sanitizedPendingMessages = MonkeyJson.sanitizePendingMsgsForFile(pendingMessageStore?.toList()!!)
            if(sanitizedPendingMessages.isNotEmpty()){
                val task = PendingMessageStore.AsyncStoreTask(this, sanitizedPendingMessages)
                task.execute()
            }else{
                val task = PendingMessageStore.AsyncCleanTask(this)
                task.execute()
            }
        } catch (e: UninitializedPropertyAccessException) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        status = ServiceStatus.dead
        messageHandler.clearServiceReference()

        if(isAsyncSocketConnected())
            asyncConnSocket.disconectSocket()

        //let the CPU go to sleep by releasing the wake lock
        releaseWakeLock()

        //If for some reason, client didn't unbind,
        delegateHandler.clear()
        watchdog?.cancel()
        //persist pending messages to a file
//        persistUnsentMessages()

        //unregister file broadcast receivers
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver!!)
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver!!)
        //persist last time synced
        KeyStoreCriptext.setLastSync(this, lastTimeSynced)
        //unregister connectivity change receiver
        if(receiver != null)
            unregisterReceiver(receiver)
    }

    private fun startConnectivityBroadcastReceiver(){
        if(receiver==null) {
            receiver = ConnectionChangeReceiver(this)
            val conn_changereceived = IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
            registerReceiver(receiver, conn_changereceived)
        }
    }

    fun removeDelegates() {
        delegateHandler.clear()
    }

    inner class MonkeyBinder : Binder() {

        val service: MonkeyKitSocketService
            get() = this@MonkeyKitSocketService
        /**
         * Sets delegates to the service. The delegate must hold a reference to this service before
         * calling this method. Additionally if the service was already running but it is not
         * connected, retry connection.
         */
        fun setDelegate(delegate: Any){
            if (delegate is AcknowledgeDelegate)
                this@MonkeyKitSocketService.delegateHandler.setDelegate(delegate)
            if (delegate is ConnectionDelegate)
                this@MonkeyKitSocketService.delegateHandler.setDelegate(delegate)
            if (delegate is ConversationDelegate)
                this@MonkeyKitSocketService.delegateHandler.setDelegate(delegate)
            if (delegate is ConversationOpenDelegate)
                this@MonkeyKitSocketService.delegateHandler.setDelegate(delegate)
            if (delegate is FileDelegate)
                this@MonkeyKitSocketService.delegateHandler.setDelegate(delegate)
            if (delegate is GroupDelegate)
                this@MonkeyKitSocketService.delegateHandler.setDelegate(delegate)
            if (delegate is NewMessageDelegate)
                this@MonkeyKitSocketService.delegateHandler.setDelegate(delegate)
            if (delegate is NewNotificationDelegate)
                this@MonkeyKitSocketService.delegateHandler.setDelegate(delegate)
            if (delegate is SyncDelegate)
                this@MonkeyKitSocketService.delegateHandler.setDelegate(delegate)
            if (delegate is MonkeyKitDelegate)
                this@MonkeyKitSocketService.delegateHandler.setDelegate(delegate)

            if (status == ServiceStatus.running && !isSocketConnected())
                startSocketConnection()
        }

    }

    private fun releaseWakeLock(){
        wakeLock?.release()
        wakeLock = null
    }


    val serviceClientData: ClientData
        get() = clientData

    private fun decryptAES(encryptedText: String) = aesutil.decrypt(encryptedText)

    fun isSocketConnected() = status > ServiceStatus.initializing && isAsyncSocketConnected()

    private fun isAsyncSocketConnected(): Boolean
     {
         try {
             return asyncConnSocket.isConnected
         } catch (ex: UninitializedPropertyAccessException){
             return false
         }
     }

    /**
     * Creates a new watchdog only if watchdog variable is null.
     */
     private fun startWatchdog(){
        if(watchdog == null) {
            watchdog = Watchdog(this);
            watchdog!!.start();
        }
    }

    /**
     * Makes a copy of the current state of the pendingMessages list and sends through the socket
     * all the contained messages.
     */
    internal fun resendPendingMessages(){
        pendingMessageStore?.forEach { msg -> sendJsonThroughSocket(msg) }
    }

    /**
     * Ads a message to the list of pending messages and starts the watchdog.
     * @param json mensaje a guardar
     * @throws JSONException
     */
    private fun addMessageToWatchdog(json: JsonObject) {
        pendingMessageStore?.add(this, json)
        startWatchdog()
    }

    /**
     * Crea un nuevo MOKMessage con Id unico y con un timestamp actual. Al crear un nuevo MOKMessage
     * para ser enviado siempre debe de usarse este metodo en lugar del constructor por defecto que
     * tiene MOKMessage ya que inicializa varios atributos de la manera correcta para ser enviado.
     * @param textMessage texto a enviar en el mensaje
     * @param sessionIDTo session ID del destinatario
     * @param type tipo del mensaje. Debe de ser uno de los valores de MessageTypes.FileTypes
     * @param params JsonObject con parametros adicionales a enviar.
     * @return Una nueva instancia de MOK Message lista para ser enviada por el socket.
     */
    fun createMOKMessage(textMessage: String, sessionIDTo: String, type: Int, params: JsonObject): MOKMessage {
        val datetimeorder = System.currentTimeMillis();
        val datetime = datetimeorder/1000L;
        val srand = RandomStringBuilder.build(3);
        val idnegative = "-" + datetime;
        val message = MOKMessage(idnegative + srand, clientData.monkeyId, sessionIDTo, textMessage,
                "" + datetime, "" + type, params, JsonObject());
        message.datetimeorder = datetimeorder;
        return message;
    }

    private fun createSendProps(old_id: String, encrypted: Boolean): JsonObject {
        val props = JsonObject();
        props.addProperty("str", "0");
        props.addProperty("encr", if (encrypted) "1" else "0")
        props.addProperty("device", "android");
        props.addProperty("old_id", old_id);
        return props;
    }

    private fun createSendJSON(idnegative: String, sessionIDTo: String, elmensaje: String,
                               pushMessage: PushMessage, params: JsonObject, props: JsonObject, encrypted: Boolean): JsonObject {

        val args= JsonObject();
        val json= JsonObject();

        try {

            args.addProperty("id", idnegative);
            args.addProperty("rid", sessionIDTo);
            args.addProperty("msg", if (encrypted) aesutil.encrypt(elmensaje) else Base64.encodeToString(elmensaje.toByteArray(), Base64.NO_WRAP));
            args.addProperty("type", MessageTypes.MOKText);
            args.addProperty("push", pushMessage.toString());
            args.addProperty("params", params.toString());
            if (!encrypted)
                props.addProperty("encoding", "base64")
            args.addProperty("props", props.toString());
            json.add("args", args);
            json.addProperty("cmd", MessageTypes.MOKProtocolMessage);
        } catch(ex: Exception){
            ex.printStackTrace();
        }

        return json;
    }

    private fun sendJsonThroughSocket(json: JsonObject) {
        asyncConnSocket.sendMessage(json);
    }

    private fun sendSync(since: Long, qty: Int) {
        HttpSyncTask(this, since, qty).execute()
    }

    fun sendSync() {
        status = ServiceStatus.syncing
        sendSync(lastTimeSynced, 100)
    }

    /**
     * Send a notification.
     * @param sessionIDTo session ID of the receiver
     * @param paramsObject JsonObject with the parameters
     * @param pushMessage message for push notification
     */
    fun sendNotification(sessionIDTo: String, paramsObject: JSONObject, pushMessage: String) {

        if(status == ServiceStatus.initializing) {
            pendingActions.add(Runnable {
                sendNotification(sessionIDTo, paramsObject, pushMessage)
            })
        } else try {
            val args = JsonObject()
            val json = JsonObject()

            val idNegative = "-" + System.currentTimeMillis() / 1000
            args.addProperty("id", idNegative)
            args.addProperty("sid", clientData.monkeyId)
            args.addProperty("rid", sessionIDTo)
            args.addProperty("params", paramsObject.toString())
            args.addProperty("type", MessageTypes.MOKNotif)
            args.addProperty("msg", "")
            args.addProperty("push", pushMessage.replace("\\\\", "\\"))

            val props = JsonObject()
            props.addProperty("old_id", idNegative)
            args.addProperty("props", props.toString())

            json.add("args", args)
            json.addProperty("cmd", MessageTypes.MOKProtocolMessage)

            if(isSocketConnected()){
                sendJsonThroughSocket(json)
            }
            else {
                Thread.dumpStack()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    /**
     * Send a temporal notification.
     * @param sessionIDTo session ID of the receiver
     * @param paramsObject JsonObject with the parameters
     */
    fun sendTemporalNotification(sessionIDTo: String, paramsObject: JSONObject) {

        if (status == ServiceStatus.initializing) {
            pendingActions.add(Runnable {
                sendTemporalNotification(sessionIDTo, paramsObject)
            })
        } else try {

            val args = JsonObject()
            val json = JsonObject()

            args.addProperty("rid", sessionIDTo)
            args.addProperty("params", paramsObject.toString())
            args.addProperty("type", MessageTypes.MOKTempNote)
            args.addProperty("msg", "")

            json.add("args", args)
            json.addProperty("cmd", MessageTypes.MOKProtocolMessage)

            if(isSocketConnected()){
                sendJsonThroughSocket(json)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    /**
     * Get info of a user.
     * @param monkeyid monkeyid ID of the user or group.
     */
    fun getUserInfoById(monkeyId: String){
        if(!serviceAPIisReady) {
            pendingActions.add(Runnable {
                getUserInfoById(monkeyId)
            })
        } else
            userManager.getUserInfoById(monkeyId)
    }

    /**
     * Get users info
     * @param monkeyIds string separate by coma with the monkey ids
     */
    fun getUsersInfo(monkeyIds: String){
        if(!serviceAPIisReady) {
            pendingActions.add(Runnable {
                getUsersInfo(monkeyIds)
            })
        } else
            userManager.getUsersInfo(monkeyIds)
    }

    /**
     * Get info of a group.
     * @param monkeyid monkeyid ID of the user or group.
     */
    fun getGroupInfoById(monkeyId: String){
        if(!serviceAPIisReady) {
            pendingActions.add(Runnable {
                getGroupInfoById(monkeyId)
            })
        } else
            groupManager.getGroupInfoById(monkeyId)
    }

    /**
     * Update the metada of a user
     * @param monkeyid monkeyid ID of the user.
     * @param userInfo JSONObject that contains user data.
     */
    fun updateUserData(monkeyId: String, userInfo: JSONObject) {
        if(!serviceAPIisReady) {
            pendingActions.add(Runnable {
                updateUserData(monkeyId, userInfo)
            })
        } else
            userManager.updateUserData(monkeyId, userInfo)
    }

    /**
     * Update the metada of a group
     * @param monkeyid monkeyid ID of the user.
     * @param groupInfo JSONObject that contains group data.
     */
    fun updateGroupData(monkeyId: String, groupInfo: JSONObject) {
        if(status < ServiceStatus.initializing) {
            pendingActions.add(Runnable {
                updateGroupData(monkeyId, groupInfo)
            })
        } else
            groupManager.updateGroupData(monkeyId, groupInfo)
    }

    /**
     * Get all conversation of a user using the monkey ID.
     */
    fun getAllConversations(quantity: Int, fromTimestamp: Long){
        if(status != ServiceStatus.initializing)
           userManager.getConversations(clientData.monkeyId, quantity, fromTimestamp)
    }

    /**
     * Get all messages of a conversation.
     * @param conversationId monkeyid ID of the user.
     * @param numberOfMessages number of messages to load.
     * @param lastTimeStamp last timestamp of the message loaded.
     */
    fun getConversationMessages(conversationId: String, numberOfMessages: Int, lastTimeStamp: String){
        if(!serviceAPIisReady) {
            pendingActions.add(Runnable {
                getConversationMessages(conversationId, numberOfMessages, lastTimeStamp)
            })
        } else
            userManager.getConversationMessages(clientData.monkeyId, conversationId,
                    numberOfMessages, lastTimeStamp, asyncConnSocket)
    }

    /**
     * Delete a conversation.
     * @param monkeyid monkeyid ID of the user.
     */
    fun deleteConversation(conversationId: String){
        if(!serviceAPIisReady) {
            pendingActions.add(Runnable {
                deleteConversation(conversationId)
            })
        } else
            userManager.deleteConversation(clientData.monkeyId, conversationId)
    }

    /**
     * Create a group asynchronously and receive response via delegate
     * @param members String with the sessionIDs of the members of the group.
     * @param group_name String with the group name
     * @param group_id String with the group id (optional)
     */
    fun createGroup(members: String, group_name: String, group_id: String?){
        if(!serviceAPIisReady) {
            pendingActions.add(Runnable {
                createGroup(members, group_name, group_id)
            })
        } else
            groupManager.createGroup(members, group_name, group_id)
    }

    /**
     * Remove a group member asynchronously int the Monkey server. This method help you to delete yourself
     * of the group. Response is delivered via monkeyJsonResponse.
     * @param group_id ID of the group
     * @param monkey_id ID of member to delete
     */
    fun removeGroupMember(group_id: String, monkey_id: String){
        if(status == ServiceStatus.initializing) {
            pendingActions.add(Runnable {
                removeGroupMember(group_id, monkey_id)
            })
        } else
            groupManager.removeGroupMember(group_id, monkey_id)
    }

    /**
     * Add a member to a group asynchronously.
     * @param new_member Session ID of the new member
     * @param group_id ID of the group
     */
    fun addGroupMember(new_member: String, group_id: String){
        if(status == ServiceStatus.initializing) {
            pendingActions.add(Runnable {
                addGroupMember(new_member, group_id)
            })
        } else
            groupManager.addGroupMember(new_member, group_id)
    }

    /**
     * Disconnect the socket immediately. Useful for reconnecting.
     */
    fun forceDisconnect(){
        if(isSocketConnected()){
            asyncConnSocket.sendDisconectFromPull()
            delegateHandler.connectionDelegate?.onSocketDisconnected()
        } else {
            Log.d("forceDisconnect", "${asyncConnSocket.socketStatus}")
            startSocketConnection();
        }
    }

    fun sendMessage(newMessage: MOKMessage, pushMessage: PushMessage, encrypted: Boolean): MOKMessage{

            try {

                val props = createSendProps(newMessage.message_id, encrypted);
                newMessage.props = props;


                val json= createSendJSON(newMessage.message_id, newMessage.rid, newMessage.msg, pushMessage,
                        newMessage.params ?: JsonObject(), props, encrypted);


                if(!serviceAPIisReady) {
                    pendingMessageStore?.add(this, json)
                } else {
                    addMessageToWatchdog(json)
                    sendJsonThroughSocket(json)
                }

            }
            catch (e: Exception) {
                e.printStackTrace();
                return newMessage;
            }

        return newMessage;
    }

    fun sendFileMessage(newMessage: MOKMessage, pushMessage: PushMessage, encrypted: Boolean){
        if(!serviceAPIisReady) {
            pendingActions.add(Runnable {
                sendFileMessage(newMessage, pushMessage, encrypted)
            })
        } else {
            val intent = Intent(this, uploadServiceClass)
            newMessage.toIntent(intent)
            intent.putExtra(MonkeyFileService.ISUPLOAD_KEY, true)
            intent.putExtra(MonkeyFileService.PUSH_KEY, pushMessage.toString())
            intent.putExtra(MonkeyFileService.ENCR_KEY, encrypted)
            clientData.fillIntent(intent)

            startService(intent)
        }
    }

    /**
     * Notifies the MonkeyKit server that the current user has opened an UI with conversation with
     * another user or a group.
     */
    fun openConversation(conversationID: String){

        if(!serviceAPIisReady) {
            pendingActions.add(Runnable {
                openConversation(conversationID)
            })
        } else try {
            val args = JsonObject()
            val json = JsonObject()

            args.addProperty("rid", conversationID)

            json.add("args", args)
            json.addProperty("cmd", MessageTypes.MOKProtocolOpen)

            if(isSocketConnected()){
                sendJsonThroughSocket(json)
            }
            else {
                Thread.dumpStack()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Notify to the server that you have closed the conversation. This method should be called
     * each time user close the conversation or stop been visible.
     * @param conversationId conversation ID.
     */
    fun closeConversation(conversationId: String) {

        if(!serviceAPIisReady) {
            pendingActions.add(Runnable {
                closeConversation(conversationId)
            })
        } else try {
            val args = JsonObject()
            val json = JsonObject()

            args.addProperty("rid", conversationId)
            json.add("args", args)
            json.addProperty("cmd", MessageTypes.MOKProtocolClose)

            if (isSocketConnected()) {
                sendJsonThroughSocket(json)
            } else {
                Thread.dumpStack()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    /**
     * Notify to the server that you are online or offline. This method should be called
     * at onstart or onDestroy of application.
     * @param online .
     */
    fun setOnline(online: Boolean) {

        if(!serviceAPIisReady) {
            pendingActions.add(Runnable {
                setOnline(online)
            })
        } else try {
            val args = JsonObject()
            val json = JsonObject()

            args.addProperty("online", if (online) "1" else "0")
            json.add("args", args)
            json.addProperty("cmd", MessageTypes.MOKProtocolSet)

            if (isSocketConnected()) {
                sendJsonThroughSocket(json)
            } /*else{
                Thread.dumpStack()
            }*/

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    /**
     * Unsend a message from server.
     * @param monkeyId ID of the user or group
     * @param messageid ID of the message that you want to unsend
     */
    fun unsendMessage(senderId: String, recipientId: String, messageId: String) {
        if(senderId != serviceClientData.monkeyId){
            return;
        }

        try {
            val args = JsonObject()
            val json = JsonObject()

            args.addProperty("id", messageId)
            args.addProperty("rid", recipientId)
            json.add("args", args)
            json.addProperty("cmd", MessageTypes.MOKProtocolDelete)

            if(!serviceAPIisReady) {
                pendingMessageStore?.add(this, json)
            } else {
                addMessageToWatchdog(json);
                sendJsonThroughSocket(json);
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun requestKeysForMessage(encryptedMessage: MOKMessage){
        val task = OpenConversationTask(this, encryptedMessage)
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, encryptedMessage.sid) //LAME
    }

    /**
     * Manda un requerimiento HTTP a Monkey para obtener las llaves mas recientes de un usuario que
     * tiene el server. Esta funcion debe de ser llamada en background de lo contrario lanza una excepcion.
     * @param sessionIdTo El session id del usuario cuyas llaves se desean obtener
     * @return Un String con las llaves del usuario. Antes de retornar el resultado, las llaves se
     * guardan en el KeyStoreCriptext.
     */
    fun requestKeyBySession(sessionIdTo: String): String?{
        // Create a new HttpClient and Post Header
        val httpclient = MonkeyHttpClient.newClient()

        try {

            val httppost = MonkeyHttpClient.newPost(httpsURL + "/user/key/exchange",
                    serviceClientData.appId, serviceClientData.appKey)
            val localJSONObject1 = JSONObject()
            val params = JSONObject()
            localJSONObject1.put("user_to",sessionIdTo);
            localJSONObject1.put("session_id",serviceClientData.monkeyId);
            params.put("data", localJSONObject1.toString());
            Log.d("OpenConversation", "Req: " + params.toString());

            val finalResult = MonkeyHttpClient.getResponse(httpclient, httppost, params.toString());
            Log.d("OpenConversation", finalResult.toString());
            val newKeys = decryptAES(finalResult.getJSONObject("data").getString("convKey"));
            KeyStoreCriptext.putStringBlocking(this, sessionIdTo, newKeys);
            return newKeys;

        } catch (ex: JSONException) {
            ex.printStackTrace();
        } catch (e: ClientProtocolException) {
            e.printStackTrace();
        } catch (e: IOException) {
            e.printStackTrace();
            // TODO Auto-generated catch block
        } catch (e: Exception){
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Manda un requerimiento HTTP a Monkey para obtener el texto de un mensaje encriptado con
     * las ultimas llaves del remitente que tiene el server. Esta funcion debe de ser llamada en
     * background de lo contrario lanza una excepcion
     * @param messageId Id del mensaje cuyo texto se quiere obtener
     * @return Un String con el texto encriptado con las llaves mas recientes.
     */
    fun requestTextWithLatestKeys(messageId: String): String?{
        val httpParams = BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 20000);
        HttpConnectionParams.setSoTimeout(httpParams, 25000);
        // Create a new HttpClient and Post Header
        val httpclient = MonkeyHttpClient.newClient();
        val httppost = HttpGet("$httpsURL/message/$messageId/open/secure");

        try {

            val base64EncodedCredentials = "Basic " + Base64.encodeToString(
                    (serviceClientData.appKey).toByteArray(),
                    Base64.NO_WRAP);


            httppost.setHeader("Authorization", base64EncodedCredentials);

            Log.d("OpenSecure", "Req: " + messageId);
            //sets a request header so the page receving the request
            //will know what to do with it
            // Execute HTTP Post Request
            val response = httpclient.execute(httppost);
            val reader = BufferedReader(InputStreamReader(response.entity.content, "UTF-8"));
            val json = reader.readLine();
            val tokener = JSONTokener(json);
            val finalResult = JSONObject(tokener);

            Log.d("OpenSecure", finalResult.toString());
            val newEncryptedMessage = finalResult.getJSONObject("data").getString("message");
            Log.d("OpenSecure", newEncryptedMessage);
            return newEncryptedMessage;

        } catch (ex: JSONException) {
            ex.printStackTrace();
        } catch (ex: ClientProtocolException) {
            ex.printStackTrace();
        } catch (ex: IOException) {
            ex.printStackTrace();
        }

        return null;
    }

    fun setAsUnauthorized(){
        status = ServiceStatus.unauthorized
    }
    abstract val uploadServiceClass: Class<*>

    /**
     * This method is invoked every time a message is received in real time. The implementation of
     * this method should store in the local database a message received through MonkeyKit. it
     * should be asynchronous to avoid blocking the main thread.
     * @param message the message to store.
     * @param runnable once the insertion is complete, execute this runnable to forward the message to
     * the delegates.
     */
    abstract fun storeReceivedMessage(message: MOKMessage, runnable: Runnable)

    /**
     * Once the socket connects, it needs to sync the local database with the server. The implementation
     * of this method should update the database to contain all the changes that occured while the user
     * was offline.
     * @param syncData an object with all the messages that should have been received during the down
     * time, this includes new messages, new notifications, and requested deletions.
     * @param runnable once the update is complete, execute this runnable to forward the update to
     * the delegates
     */
    abstract fun syncDatabase(syncData: HttpSync.SyncData, runnable: Runnable);

    /**
     * Loads all credentials needed to initialize the service. This method will be called in
     * a background thread, so it's ok to do blocking operations.
     * @return a ClientData object with API key, id, and user monkey id.
     */
    abstract fun loadClientData(): ClientData

    companion object {
        val transitionMessagesPrefs = "MonkeyKit.transitionMessages";
        val lastSyncPrefs = "MonkeyKit.lastSyncTime";
        val lastSyncKey = "MonkeyKit.lastSyncKey";

//        val httpsURL = "https://secure.criptext.com"
        val httpsURL = "https://api.talktolk.com"
//        val SYNC_SERVICE_KEY = "SecureSocketService.SyncService"
        var status = ServiceStatus.dead

        private val serviceAPIisReady: Boolean
            get() = status == ServiceStatus.running

        fun bindMonkeyService(context:Context, connection: ServiceConnection, service:Class<*>) {
            val intent = Intent(context, service)
            context.bindService(intent, connection, Context.BIND_ADJUST_WITH_ACTIVITY)
        }
    }

    enum class ServiceStatus {
        unauthorized, dead, initializing, syncing, running
    }

}
