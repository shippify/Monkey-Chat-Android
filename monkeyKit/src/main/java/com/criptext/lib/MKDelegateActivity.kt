package com.criptext.lib

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.support.v7.app.AppCompatActivity
import android.os.IBinder
import android.util.Log
import android.webkit.MimeTypeMap
import com.criptext.MonkeyKitSocketService
import com.criptext.comunication.MOKConversation
import com.criptext.comunication.MOKMessage
import com.criptext.comunication.MessageTypes
import com.criptext.comunication.PushMessage
import com.criptext.security.RandomStringBuilder
import com.google.gson.JsonObject
import org.apache.commons.io.FilenameUtils
import org.json.JSONObject
import java.io.File
import java.util.*

abstract class MKDelegateActivity : AppCompatActivity(), MonkeyKitDelegate {

    private var service: MonkeyKitSocketService? = null

    abstract val serviceClassName: Class<*>

    val isSocketConnected: Boolean
        get () =  service?.isSocketConnected() ?:
                (MonkeyKitSocketService.status == MonkeyKitSocketService.ServiceStatus.running)

    val isSyncing: Boolean
        get() = MonkeyKitSocketService.status == MonkeyKitSocketService.ServiceStatus.syncing

    private val messagesToForwardToService = ArrayList<DelegateMOKMessage>()
    val pendingFiles = HashMap<String, DelegateMOKMessage>();
    private val pendingDownloads = HashMap<String, DownloadMessage>();


    private var lastMoreMessagesRequest: MoreDataRequest? = null

    private var lastMoreConversationsRequest: MoreDataRequest? = null

    protected var keepServiceAlive = false

    /**
     * Callback to execute when the service is bound to this activity. You may only need this
     * if you need to consume data from MonkeyKit API in an activity that starts after your main
     * chat activity. After this callback executes you can safely start calling API methods.
     */
    var onMonkeyKitServiceReadyListener: OnMonkeyKitServiceReadyListener? = null

    val hasMessagesToForwardToService: Boolean
        get() = messagesToForwardToService.isNotEmpty()
    /**
     * sets the currently active conversation id. When a non null value is set. it sends an "openConversation"
     * to the server. When a null value is set, a "closeConversation" message is sent to the server
     * with the previous conversation Id. You should call this method when your chat activity/fragment
     * starts and when it stops.
     */
    var openConversation: String? = null
        set(value) {
            val s = service
            if (s != null && isSocketConnected) {
                if (value != null)
                    s.openConversation(value)
                else if (field != null)
                    s.closeConversation(field!!)
            }
            //service is only null during rotation, let's ignore that scenario for now, besides
            // in the onSyncComplete callback this function is executed again, so we are safe.
            field = value
        }

    fun newServiceConnection(): CancellableServiceConnection {
        return object : CancellableServiceConnection() {
            override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
                if (!cancelled && p1 != null) { //Unit tests will have null p1, let's ignore that.
                    val binder = p1 as MonkeyKitSocketService.MonkeyBinder
                    onMonkeyKitServiceReadyListener?.onMonkeyKitServiceReady()
                    connectWithNewService(binder)
                }
            }

            override fun onServiceDisconnected(p0: ComponentName?) {
                service = null
            }
        }
    }

    lateinit var monkeyKitConnection: CancellableServiceConnection


    /**
     * Forward to service all the messages that the delegate generated while the service was unavailable.
     * @param socketService reference to the socketService that just bound with this delegate
     */
   fun connectWithNewService(binder: MonkeyKitSocketService.MonkeyBinder){
        val socketService = binder.service
        service = socketService

        binder.setDelegate(this@MKDelegateActivity)
        var i = messagesToForwardToService.size - 1
        while(i > -1){
            val msg = messagesToForwardToService.removeAt(i)
            socketService.sendMessage(msg.message, msg.push, msg.isEncrypted)
            i -= 1
        }

        for(file in pendingFiles.values){
            service!!.sendFileMessage(file.message, file.push, file.isEncrypted)
        }
    }

    override fun onStart() {
        super.onStart()
        startService(Intent(this, serviceClassName))
        monkeyKitConnection = newServiceConnection()
        MonkeyKitSocketService.bindMonkeyService(this, monkeyKitConnection, serviceClassName)

    }

    override fun onStop() {
        super.onStop()
        Log.d("MKDELEGATEACTIVITY","onStop");
        /* service should always be stopped except under 2 conditions:
         * - The device changing configurations (ex. rotating)
         * - The  developer wants to keep it alive (probably because it's going to start a new activity)
         */
        val serviceMustBeStopped = !this.isChangingConfigurations && !keepServiceAlive
        setOnline(!serviceMustBeStopped)
        service = null
        monkeyKitConnection.cancelled = true
        unbindService(monkeyKitConnection)
        if (serviceMustBeStopped)
            stopMonkeyKitService()
        keepServiceAlive = false
    }

    fun stopMonkeyKitService() {
        Log.d("MKDELEGATEACTIVITY","stopMonkeyKitService");
        stopService(Intent(this, serviceClassName))
    }
    /**
     * Creates a new MOK message with a unique local ID and a timestamp of the current system time.
     * This method should be always used to create a new MOKMessage object to send instead of the
     * MOKMessage constructor.
     * @param textMessage text to send in the message
     * @param sessionIDfrom monkeyID of the user that sends the message.
     * @param sessionIDTo monkeyID of the user or group that will receive the message.
     * @param type the message type. It must be a value from MessageTypes.FileTypes
     * @param params JsonObject with additional parameters to send with the message.
     * @return A new instance of MOKMessage ready to be sent through a socket.
     */

    fun createMOKMessage(textMessage: String, sessionIDfrom: String, sessionIDTo: String,
        type: Int, params: JsonObject): MOKMessage{
        val datetimeorder = System.currentTimeMillis();
        val datetime = datetimeorder/1000;
        val srand = RandomStringBuilder.build(3);
        val idnegative = "-" + datetime;
        val message = MOKMessage(idnegative + srand, sessionIDfrom,sessionIDTo, textMessage,
               "" + datetime, "" + type, params, JsonObject());
        message.datetimeorder = datetimeorder;
        return message;
    }

    /**
     * create a JsonObject with the necessary props to send a message.
     * @return new JsonObject with the necessary props to send a message.

     */
    private fun createSendProps(old_id: String, encrypted: Boolean): JsonObject{
        val props = JsonObject();
        props.addProperty("str", "0");
        props.addProperty("encr", if(encrypted)"1" else "0");
        props.addProperty("device", "android");
        props.addProperty("old_id", old_id);
        return props;
    }

    /**
     * sends a file to a conversation and stores the message to the database.
     *
     * The upload file operation is asynchronous. When it is complete it triggers a callback
     * depending on the result. if it was successful it executes onAcknowledgeReceived(), otherwise
     * it executes onFileFailsUpload().
     *
     * @param filePath The path of the file to send.
     * @param monkeyIDFrom Monkey ID of the user that sends the file.
     * @param monkeyIDTo ID of the user or group that will receive the file.
     * @param fileType An integer with the type of the file. It should be a MessageTypes constant
     * @param params JsonObject with additional information to send with the file.
     * @param pushMessage message to display in the push notification of the receiving user(s).
     * @param isEncrypted true if the file should be encrypted before sending.
     * @return The sent MOKMessage with a temporal negative ID. You should replace this ID when the
     * server acknowledges the message.
     */
    fun persistFileMessageAndSend(filePath: String, monkeyIDFrom: String, monkeyIDTo: String, fileType: Int,
                                  params: JsonObject, pushMessage: PushMessage, isEncrypted: Boolean): MOKMessage{

        val newMessage = sendFileMessage(filePath, monkeyIDFrom, monkeyIDTo, fileType, params, pushMessage, isEncrypted)
        storeSendingMessage(newMessage)
        return newMessage

    }

    /**
     * sends a file to a list of users and stores the message to the database.
     *
     * The upload file operation is asynchronous. When it is complete it triggers a callback
     * depending on the result. if it was successful it executes onAcknowledgeReceived(), otherwise
     * it executes onFileFailsUpload().
     *
     * @param filePath The path of the file to send.
     * @param monkeyIDFrom Monkey ID of the user that sends the file.
     * @param monkeyIDTo ID of the user or group that will receive the file.
     * @param fileType An integer with the type of the file. It should be a MessageTypes constant
     * @param params JsonObject with additional information to send with the file.
     * @param pushMessage message to display in the push notification of the receiving user(s).
     * @param isEncrypted true if the file should be encrypted before sending.
     * @return A list of the sent MOKMessages with a temporal negative ID's. You should replace
     * these ID's when the server acknowledges each message.
     */
    fun persistFileMessageAndBroadcast(filePath: String, monkeyIDFrom: String, monkeyIDTo:
        List<String>, fileType: Int, params: JsonObject, pushMessage: PushMessage,
                                       isEncrypted: Boolean) =
    monkeyIDTo.map { id -> persistFileMessageAndSend(filePath, monkeyIDFrom, id, fileType, params,
            pushMessage, isEncrypted) }
    /**
     * sends a file to a conversation.
     *
     * The upload file operation is asynchronous. When it is complete it triggers a callback
     * depending on the result. if it was successful it executes onAcknowledgeReceived(), otherwise
     * it executes onFileFailsUpload().
     *
     * @param filePath The path of the file to send.
     * @param monkeyIDFrom Monkey ID of the user that sends the file.
     * @param monkeyIDTo ID of the user or group that will receive the file.
     * @param fileType An integer with the type of the file. It should be a MessageTypes constant
     * @param params JsonObject with additional information to send with the file.
     * @param pushMessage message to display in the push notification of the receiving user(s).
     * @param isEncrypted true if the file should be encrypted before sending.
     * @return The sent MOKMessage with a temporal negative ID. You should replace this ID when the
     * server acknowledges the message.
     */
    fun sendFileMessage(filePath: String, monkeyIDFrom: String, monkeyIDTo: String, fileType: Int,
                                  params: JsonObject, pushMessage: PushMessage, isEncrypted: Boolean): MOKMessage{
        val newMessage = createMOKMessage(filePath, monkeyIDFrom, monkeyIDTo, fileType, params)
        val propsMessage = createSendProps(newMessage.message_id, isEncrypted);
        propsMessage.addProperty("cmpr", "gzip");
        propsMessage.addProperty("file_type", fileType);
        propsMessage.addProperty("ext", FilenameUtils.getExtension(filePath));
        propsMessage.addProperty("filename", FilenameUtils.getName(filePath));
        propsMessage.addProperty("mime_type",
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(FilenameUtils.getExtension(filePath)));
        propsMessage.addProperty("size", File(filePath).length());

        newMessage.props = propsMessage;

        pendingFiles[newMessage.message_id] = DelegateMOKMessage(newMessage, pushMessage, isEncrypted)

        if(service != null && isSocketConnected){
            service!!.sendFileMessage(newMessage, pushMessage, isEncrypted)
        }

        return newMessage

    }

    /**
     * sends a file to a list of users.
     *
     * The upload file operation is asynchronous. When it is complete it triggers a callback
     * depending on the result. if it was successful it executes onAcknowledgeReceived(), otherwise
     * it executes onFileFailsUpload().
     *
     * @param filePath The path of the file to send.
     * @param monkeyIDFrom Monkey ID of the user that sends the file.
     * @param monkeyIDTo A list of Monkey IDs of the users that will receive the file.
     * @param fileType An integer with the type of the file. It should be a MessageTypes constant
     * @param params JsonObject with additional information to send with the file.
     * @param pushMessage message to display in the push notification of the receiving user(s).
     * @param isEncrypted true if the file should be encrypted before sending.
     * @return a List of the sent MOKMessage's with temporal negative ID's. You should replace these
     * ID's when the server acknowledges each message.
     *
     */
    fun broadcastFileMessage(filePath: String, monkeyIDFrom: String, monkeyIDTo: List<String>, fileType: Int,
                                  params: JsonObject, pushMessage: PushMessage, isEncrypted: Boolean) =
        monkeyIDTo.map { id -> sendFileMessage(filePath, monkeyIDFrom, id, fileType, params,
                pushMessage, isEncrypted) }

    /**
     * sends a text message to a conversation and stores it to the database using the
     * storeSendingMessage method.
     *
     * This method is asynchronous. Once the server receives the message, the onAcknowledgeReceived()
     * callback is executed with the new global id for the message.
     * @param text The text to send
     * @param monkeyIDFrom Monkey ID of the user that sends the message.
     * @param monkeyIDTo Monkey ID of the user or conversation that will receive the message.
     * @param params JsonObject with additional information to send in the message.
     * @param pushMessage message to display in the push notification of the receiving user(s).
     * @param isEncrypted true if the message should be encrypted before sending.
     * @return a MOKMessage of the sent message with a temporal negative ID. You should replace this
     * ID when the server acknowledges the message.
     *
     */
    fun persistMessageAndSend(text: String, monkeyIDFrom: String, monkeyIDTo: String, params: JsonObject,
                              pushMessage: PushMessage, isEncrypted: Boolean): MOKMessage{
        val newMessage = sendMessage(text, monkeyIDFrom, monkeyIDTo, params, pushMessage, isEncrypted)
        storeSendingMessage(newMessage)
        return newMessage
    }

    /**
     * sends a text message to a list of users and stores it to the database using the
     * storeSendingMessage method.
     *
     * This method is asynchronous. Once the server receives the message, the onAcknowledgeReceived()
     * callback is executed with the new global id for the message.
     * @param text The text to send
     * @param monkeyIDFrom Monkey ID of the user that sends the message.
     * @param monkeyIDTo Monkey ID of the user or conversation that will receive the message.
     * @param params JsonObject with additional information to send in the message.
     * @param pushMessage message to display in the push notification of the receiving user(s).
     * @param isEncrypted true if the message should be encrypted before sending.
     * @return a List of the sent MOKMessage's with temporal negative ID's. You should replace these
     * ID's when the server acknowledges each message.
     *
     */
    fun persistMessageAndBroadcast(text: String, monkeyIDFrom: String, monkeyIDTo: List<String>, params: JsonObject,
                              pushMessage: PushMessage, isEncrypted: Boolean) =
        monkeyIDTo.map { id -> persistMessageAndSend(text, monkeyIDFrom, id, params, pushMessage,
                isEncrypted)
        }
    /**
     * sends a text message to a list of users.
     *
     * This method is asynchronous. Once the server receives the message, the onAcknowledgeReceived()
     * callback is executed with the new global id for the message.
     * @param text The text to send
     * @param monkeyIDFrom Monkey ID of the user that sends the message.
     * @param monkeyIDTo A list of Monkey IDs of the users that will receive the message.
     * @param params JsonObject with additional information to send in the message.
     * @param pushMessage message to display in the push notification of the receiving user(s).
     * @param isEncrypted true if the message should be encrypted before sending.
     * @return a List of the sent MOKMessage's with temporal negative ID's. You should replace these
     * ID's when the server acknowledges each message.
     *
     */
    fun broadcastMessage(text: String, monkeyIDFrom: String, monkeyIDTo: List<String>, params: JsonObject,
                              pushMessage: PushMessage, isEncrypted: Boolean) =
        monkeyIDTo.map { id -> sendMessage(text, monkeyIDFrom, id, params, pushMessage, isEncrypted) }


    /**
     * sends a text message to a conversation.
     *
     * This method is asynchronous. Once the server receives the message, the onAcknowledgeReceived()
     * callback is executed with the new global id for the message.
     * @param text The text to send
     * @param monkeyIDFrom Monkey ID of the user that sends the message.
     * @param monkeyIDTo Monkey ID of the user or conversation that will receive the message.
     * @param params JsonObject with additional information to send in the message.
     * @param pushMessage message to display in the push notification of the receiving user(s).
     * @param isEncrypted true if the message should be encrypted before sending.
     * @return a MOKMessage of the sent message with a temporal negative ID. You should replace this
     * ID when the server acknowledges the message.
     *
     */
    fun sendMessage(text: String, monkeyIDFrom: String, monkeyIDTo: String, params: JsonObject,
                              pushMessage: PushMessage, isEncrypted: Boolean): MOKMessage{
        val newMessage = createMOKMessage(text, monkeyIDFrom, monkeyIDTo,
                MessageTypes.blMessageDefault, params)
        val socketService = service
        if(socketService != null ){
            socketService.sendMessage(newMessage, pushMessage, isEncrypted)
        } else {
            messagesToForwardToService.add(DelegateMOKMessage(newMessage, pushMessage, isEncrypted))
        }

        return newMessage
    }

    /**
     * This method is invoked to store a message into the local database before sending it to the
     * server. The implementation should be asynchronous to avoid blocking the main thread.
     * @param message the message to store.
     */
    abstract fun storeSendingMessage(message: MOKMessage)

    override fun onSocketConnected() {
        for(file in pendingFiles.values){
            service!!.sendFileMessage(file.message, file.push, file.isEncrypted)
        }
        openConversation = openConversation //send open conversation
        setOnline(true)
    }

    override fun onAcknowledgeRecieved(senderId: String, recipientId: String, newId: String,
                                       oldId: String, read: Boolean, messageType: Int) {
        pendingFiles.remove(oldId)
    }

    override fun onFileFailsUpload(message: MOKMessage) {
        val sentFile = pendingFiles[message.message_id]
        sentFile?.failed = true
    }

    override fun onFileDownloadFinished(fileMessageId: String, fileMessageTimestamp: Long,
                                        conversationId: String, success: Boolean) {
        pendingDownloads.remove(fileMessageId)
    }

    /**
     * Tries to send again a file message. If the message is already in queue for sending, it won't
     * do anything.
     *
     * The upload file operation is asynchronous. When it is complete it triggers a callback
     * depending on the result. if it was successful it executes onAcknowledgeReceived(), otherwise
     * it executes onFileFailsUpload().
     * @param fileMessage the fileMessage to send. It is assumed that you got this object from either
     * sendFileMessage() or persistFileMessageAndSend()
     * @param pushMessage an object describing the push notification that the receiving user will get
     * @param isEncrypted true if the file should be encrypted before sending.
     */
    fun resendFile(fileMessage: MOKMessage, pushMessage: PushMessage, isEncrypted: Boolean) {
        if(!pendingFiles.containsKey(fileMessage.message_id)){
           pendingFiles[fileMessage.message_id] = DelegateMOKMessage(fileMessage, pushMessage, isEncrypted)
            if(fileMessage.props==null) {
                val propsMessage = createSendProps(fileMessage.message_id, isEncrypted)
                propsMessage.addProperty("cmpr", "gzip")
                propsMessage.addProperty("file_type", fileMessage.fileType);
                propsMessage.addProperty("ext", FilenameUtils.getExtension(fileMessage.msg))
                propsMessage.addProperty("filename", FilenameUtils.getName(fileMessage.msg))
                propsMessage.addProperty("mime_type",
                        MimeTypeMap.getSingleton().getMimeTypeFromExtension(FilenameUtils.getExtension(fileMessage.msg)))
                fileMessage.props = propsMessage
            }
            if(isSocketConnected){
                service!!.sendFileMessage(fileMessage, pushMessage, isEncrypted)
           }
        }
    }

    /**
     * Tries to send again a file message.
     *
     * The upload file operation is asynchronous. When it is complete it triggers a callback
     * depending on the result. if it was successful it executes onAcknowledgeReceived(), otherwise
     * it executes onFileFailsUpload().
     * @param fileMessageId the id of the message to send. It is assumed that you got this object
     * from either sendFileMessage() or persistFileMessageAndSend()
     * @return true if the message is now in queue for sending. if the message could not be added to
     * the queue, then most likely a reference to the message with the provided id does not exists
     * within this instance of MKDelegateActivity. You could try using the overloaded version of
     * resendFile() that takes a MOKMessage object as argument.
     */
    fun resendFile(fileMessageId: String): Boolean{
        val fileMOKMessage = pendingFiles[fileMessageId];
        if(fileMOKMessage != null) {
            if (!fileMOKMessage.failed) {
                Log.e("FileManager", "File $fileMessageId is already sending!");
                return true;
            } else {
                fileMOKMessage.failed = false;
            }

            service?.sendFileMessage(fileMOKMessage.message,
                    fileMOKMessage.push,fileMOKMessage.isEncrypted) ?:
                    Log.e("MonkeyKit", "can't resend file. $CONNECTION_NOT_READY_ERROR")
            return true;
        } else{
            Log.e("MonkeyKit", "FileManager tried to resend a file that has not been sent yet!");
            return false
        }
    }

    /**
     * Downloads a file received in a message. This method is asynchronous, when the response is
     * received, the onFileDownloadFinished() callback is executed with the status of the download.
     * @param fileMessageId the ID of the file message to download
     * @param filepath the absolute path where the downloaded file should be stored.
     * @param props the props JsonObject of the file message to download
     * @param senderId the monkey ID of the user who sent this file message
     * @param sortdate the timestampOrder of the received file.
     * @param conversationId an identifier of the conversation to which the download message belongs to
     * This will be used in the onDownloadFinished callback so that you can easily search for your
     * message and update it
     */
    fun downloadFile(fileMessageId: String, filepath: String, props: JsonObject, senderId: String,
                     sortdate: Long, conversationId: String){
        if(!pendingDownloads.containsKey(fileMessageId)){
            service?.downloadFile(fileMessageId, filepath, props.toString(), senderId,
                    sortdate, conversationId)

            pendingDownloads.put(fileMessageId, DownloadMessage(fileMessageId, filepath, props))
        }
    }

    /**
     * Callback executed when the activity is about to be destroyed with messages that still
     * have not been received by the server. You should mark this messages as error since
     * the service will not keep trying to resend them while the user is away from the app to
     * avoid draining battery.
     * @param errorMessages list of messages that have not been yet delivered.
     */
    abstract fun onDestroyWithPendingMessages(errorMessages: ArrayList<MOKMessage>)

    override fun onDestroy() {
        super.onDestroy()
        val errorMessages = ArrayList<MOKMessage>()
        for(message in messagesToForwardToService)
            errorMessages.add(message.message)
        for(message in pendingFiles.values)
            errorMessages.add(message.message)

        if(errorMessages.isNotEmpty())
            onDestroyWithPendingMessages(errorMessages)

        openConversation = null //send close conversation

    }

    /**
     * Create a new chat group. This method is asynchronous, when the response is received, the
     * onCreateGroup() callback is executed with the new group's info.
     * @param members A string with the Monkey ID of the participants, separated by commas.
     * @param groupName A string with the name of the group.
     * @param groupId Optionally, a string with an id for the group. If value is null, server
     * will randomly choose one for you.
     */
    fun createGroup(members: String, groupName: String, groupId: String?){
        val socketService = service
        if(socketService != null)
            socketService.createGroup(members, groupName, groupId)
        else
            throw IllegalStateException("Socket Service is null.\nMaybe it is not bound yet." +
            "You can wait for the onBoundToService() callback to call this method.")
    }

    /**
     * request all the messages sent to the current user starting from a specific timestamp. When
     * the messages are successfully received. the onMessageBatchReady() callback is executed
     *
     * The sync operation is done automatically every time the socket connects to the network. in
     * most cases there is no need for you to call this method manually.
     * @param from a timestamp to be used as reference. Messages received must not be older than
     * this timestamp
     */
    /*fun sendSync(from: Long){
        this.service?.sendSync(from)
    }*/


    /**
     * Manually request to sync with server. The sync operation is done automatically every time the
     * socket connects to the network. in most cases there is no need for you to call this method
     * manually.
     *
     * However, there is one case in which you will want to use this. If your app supports
     * multi-session and in the onAcknowledgeReceived() callback you receive a message with unknown
     * id, it means that it was sent from a different device. You can sync with server to get that
     * message.
     *
     * Naturally this method is asynchronous, and when the response is received the onSyncComplete
     * callback is executed with your new data.
     */
    fun sendSync(){
        val socketService = service
        socketService?.sendSync()
    }

    /**
     * Set your status to online or offline depending of the boolean received.
     * @param online boolean if you are online or not
     */
    fun setOnline(online: Boolean){
        val socketService = service
        socketService?.setOnline(online)
    }

    /**
     * Request necessary info to render a conversation. This method is asynchronous, when the
     * response is received, either the onGetConversationInfo() or onGetUserInfo() callback is
     * executed with the requested info depending on whether the conversation was a group or not.
     * @param conversationId id of the requested conversation
     */
    fun getConversationInfo(conversationId: String){
        val socketService = service
        if(conversationId.contains("G:"))
            socketService?.getGroupInfoById(conversationId)
        else
            socketService?.getUserInfoById(conversationId)
    }

    /**
     * Send a notification to a conversation.
     * @param sessionIDTo session ID of the receiver
     * @param paramsObject JsonObject with the parameters
     * @param pushMessage message for push notification
     */
    fun sendNotification(monkeyIDTo: String, paramsObject: JSONObject, pushMessage: String){
        val socketService = service
        socketService?.sendNotification(monkeyIDTo, paramsObject, pushMessage)
    }

    /**
     * Send a temporal notification to a conversation.
     * @param monkeyIDTo session ID of the receiver
     * @param paramsObject JsonObject with the parameters
     */
    fun sendTemporalNotification(monkeyIDTo: String, paramsObject: JSONObject) {
        val socketService = service
        socketService?.sendTemporalNotification(monkeyIDTo, paramsObject)
    }

    /**
     * Get all conversation of the current user using his/her monkey ID. This method is
     * asynchronous, when the response is received, the onGetConversations() callback is executed
     * with a list of the conversations you requested.
     * @param quantity the maximum number of conversations to retrieve, it must be between 1 and 100
     * @param fromTimestamp retrieve conversations no older than this timestamp
     */
    fun getConversationsFromServer(quantity: Int, fromTimestamp: Long){
        val socketService = service
        //don't accept repeated requests
        if(lastMoreConversationsRequest == null || lastMoreConversationsRequest!!.expired ||
                lastMoreConversationsRequest!!.acknowledged && fromTimestamp != lastMoreConversationsRequest!!.timestamp) {
            Log.d("HttpSync", "getConversationsFromServer $quantity $fromTimestamp")
            lastMoreConversationsRequest = MoreDataRequest(fromTimestamp)
            socketService?.getAllConversations(quantity, fromTimestamp)
        }
    }

    /**
     * requests all messages of a conversation from the server. This method is asynchronous, when
     * the response is received the onGetConversationMessages() callback with the list of messages.
     * @param monkeyid  ID of the conversation whose messages you request.
     * @param numberOfMessages maximum number of messages to request.
     * @param lastTimeStamp last timestamp of the last oldest message you have from this conversation.
     * If you don't have any use 0. This is useful to avoid getting duplicated messages.
     */
    fun getConversationMessages(conversationId: String, numberOfMessages: Int, lastTimeStamp: Long){
        val socketService = service
        if(lastMoreMessagesRequest == null || lastMoreMessagesRequest!!.expired ||
                lastMoreMessagesRequest!!.acknowledged && lastTimeStamp != lastMoreMessagesRequest!!.timestamp) {
            lastMoreMessagesRequest = MoreDataRequest(lastTimeStamp)
            socketService?.getConversationMessages(conversationId, numberOfMessages, "" + lastTimeStamp)
        }
    }

    override fun onGetConversationMessages(conversationId: String, messages: ArrayList<MOKMessage>, e: Exception?) {
        if(e != null) //Something went wrong, invalidate last request
            lastMoreMessagesRequest = null
        else //request successful. don't accept anymore requests like this
            lastMoreMessagesRequest?.acknowledged = true
    }

    /**
     * Remove a member from a group. This method help you to delete yourself of the group. This
     * method is asynchronous, when the response is received the onRemoveGroupMember() callback with
     * the updated group members list is executed.
     * @param group_id ID of the group
     * @param monkey_id ID of member to delete
     */
    fun removeGroupMember(group_id: String, monkey_id: String){
        val socketService = service
        socketService?.removeGroupMember(group_id, monkey_id)
    }

    /**
     * Add a member to a group. This method is asynchronous, when the response is received the
     * onAddGroupMember() callback with the updated group members list is executed.
     * @param new_member Monkey ID of the new member
     * @param group_id ID of the group
     */
    fun addGroupMember(new_member: String, group_id: String){
        val socketService = service
        socketService?.addGroupMember(new_member, group_id)
    }

  /**
     * Update the metada of a user
     * @param monkeyid monkeyid ID of the user.
     * @param userInfo JSONObject that contains user data.
     */
    fun updateUserData(monkeyId: String, userInfo: JSONObject) {
        service?.updateUserData(monkeyId, userInfo)
    }

    /**
     * Update the metada of a group
     * @param monkeyid monkeyid ID of the user.
     * @param groupInfo JSONObject that contains group data.
     */
    fun updateGroupData(monkeyId: String, groupInfo: JSONObject) {
        service?.updateGroupData(monkeyId, groupInfo)
    }



    /**
     * Requests from server necessary info about a list of users. This info is often used to render
     * the members of a group. This method is asynchronous, when the response is received the
     * onGetUsersInfo() callback with the requested data is executed.
     * @param userIds string with monkeyId's of the requested users separated by commas.
     */
    fun getUsersInfo(userIds: String){
        val socketService = service
        socketService?.getUsersInfo(userIds)
    }
    /**
     * Delete a conversation in the server. This method is asynchronous, when the response is
     * received the onConversationDeleted() callback with the deleted conversation is executed.
     * @param conversationId id of the conversation to delete.
     */
    fun deleteConversation(conversationId: String){
        val socketService = service
        socketService?.deleteConversation(conversationId)
    }

    fun unsendMessage(senderId: String, recipientId: String, messageId: String){
        val socketService = service
        socketService?.unsendMessage(senderId, recipientId, messageId)
    }

    override fun onGetConversations(conversations: ArrayList<MOKConversation>, e: Exception?) {
        if(e != null) //Something went wrong, invalidate last request
            lastMoreConversationsRequest = null
        else //request successful. don't accept anymore requests like this
            lastMoreConversationsRequest?.acknowledged = true
    }

    private data class DownloadMessage(val fileMessageId: String, val filepath: String,
                                       val props: JsonObject)

    class MoreDataRequest(val key: Long) {
        var acknowledged = false
        val timestamp: Long
        init {
            timestamp = System.currentTimeMillis()
        }

        val expired: Boolean
        get() = !acknowledged && (System.currentTimeMillis() - timestamp > dataRequestTimeout)
    }

    companion object{
        val  CONNECTION_NOT_READY_ERROR = "MonkeyKitSocketService is not ready yet."
        private val dataRequestTimeout = 15000L
    }
}
