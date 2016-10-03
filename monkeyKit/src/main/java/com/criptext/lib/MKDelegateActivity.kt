package com.criptext.lib

import android.content.ComponentName
import android.content.ServiceConnection
import android.support.v7.app.AppCompatActivity
import android.os.IBinder
import android.util.Log
import android.webkit.MimeTypeMap
import com.criptext.MonkeyKitSocketService
import com.criptext.comunication.MOKMessage
import com.criptext.comunication.MessageTypes
import com.criptext.comunication.PushMessage
import com.criptext.security.RandomStringBuilder
import com.google.gson.JsonObject
import org.apache.commons.io.FilenameUtils
import org.json.JSONObject
import java.security.Timestamp
import java.util.*

abstract class MKDelegateActivity : AppCompatActivity(), MonkeyKitDelegate {

    private var service: MonkeyKitSocketService? = null

    abstract val serviceClassName: Class<*>

    val isSocketConnected: Boolean
        get () =  service?.isSocketConnected() ?: false

    private val messagesToForwardToService = ArrayList<DelegateMOKMessage>()
    val pendingFiles = HashMap<String, DelegateMOKMessage>();
    private val pendingDownloads = HashMap<String, DownloadMessage>();

    /**
     * sets the currently active conversation id. When a non null value is set. it sends an "openConversation"
     * to the server. When a null value is set, a "closeConversation" message is sent to the server
     * with the previous conversation Id. You should call this method when your chat activity/fragment
     * starts and when it stops.
     */
    var activeConversation: String? = null
        set(value) {
            if(isSocketConnected) {
                if (value != null)
                    service!!.openConversation(value)
                else if(field != null)
                    service!!.closeConversation(field!!)
            }
            field = value
        }


    private val monkeyKitConnection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            val binder = p1 as MonkeyKitSocketService.MonkeyBinder
            val sService = binder.getService(this@MKDelegateActivity)
            service = sService

            forwardTextMsgsToService(sService)

        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            service = null
        }
    }

    /**
     * Forward to service all the messages that the delegate generated while the service was unavailable.
     * @param socketService reference to the socketService that just bound with this delegate
     */
    fun forwardTextMsgsToService(socketService: MonkeyKitSocketService){
        var i = messagesToForwardToService.size - 1
        while(i > -1){
            val msg = messagesToForwardToService.removeAt(i)
            socketService.sendMessage(msg.message, msg.push, msg.isEncrypted)
            i -= 1
        }
    }

    override fun onStart() {
        super.onStart()
        MonkeyKitSocketService.bindMonkeyService(this, monkeyKitConnection, serviceClassName)

    }

    override fun onStop() {
        super.onStop()
        setOnline(false)
        service = null
        unbindService(monkeyKitConnection)
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

        newMessage.props = propsMessage;

        pendingFiles[newMessage.message_id] = DelegateMOKMessage(newMessage, pushMessage, isEncrypted)

        if(isSocketConnected){
            service!!.sendFileMessage(newMessage, pushMessage, isEncrypted)
        }

        return newMessage

    }

    /**
     * sends a file to a list of users.
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

    abstract fun storeSendingMessage(message: MOKMessage)

    override fun onSocketConnected() {
        for(file in pendingFiles.values){
            service!!.sendFileMessage(file.message, file.push, file.isEncrypted)
        }
        activeConversation = activeConversation //send open conversation
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
     * Asynchronously downloads a file from the MonkeyKit server. Once the the download is finished
     * the onFileDownloadFinished will be executed.
     * @param fileMessageId the ID of the file message to download
     * @param filepath the absolute path where the downloaded file should be stored.
     * @param props the props JsonObject of the file message to download
     * @param senderId the monkey ID of the user who sent this file message
     * @param conversationId an identifier of the conversation to which the download message belongs to
     * This will be used in the onDownloadFinished callback so that you can easily search for your
     * message and update it
     */
    fun downloadFile(fileMessageId: String, filepath: String, props: JsonObject, senderId: String,
                     sortdate: Long, conversationId: String){
        if(!pendingDownloads.containsKey(filepath)){
            service?.downloadFile(fileMessageId, filepath, props.toString(), senderId,
                    sortdate, conversationId)

            pendingDownloads.put(filepath, DownloadMessage(fileMessageId, filepath, props))
        }
    }

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

        activeConversation = null //send close conversation

    }

    /**
     * Create a new chat group.
     * @param members A string with the Monkey ID of the participants, separated by commas.
     * @param groupName A string with the name of the group.
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
     * request all the messages sent to the current user synce the last time it was synced. When
     * the messages are successfully received. the onMessageBatchReady() callback is executed
     *
     * The sync operation is done automatically every time the socket connects to the network. in
     * most cases there is no need for you to call this method manually.
     */
    /*fun sendSync(){
        val socketService = service
        socketService?.sendSync(socketService.lastTimeSynced)
    }*/

    /**
     * Set your status to online or offline depending of the boolean received.
     * @param online boolean if you are online or not
     */
    fun setOnline(online: Boolean){
        val socketService = service
        socketService?.setOnline(online)
    }

    /**
     * Get info of a conversation. The result will arrive via this two delegates: onGetGroupInfo or onGetUserInfo.
     * @param conversationId conversation id.
     */
    fun getConversationInfo(conversationId: String){
        val socketService = service
        if(conversationId.contains("G:"))
            socketService?.getGroupInfoById(conversationId)
        else
            socketService?.getUserInfoById(conversationId)
    }

    /**
     * Send a notification.
     * @param sessionIDTo session ID of the receiver
     * @param paramsObject JsonObject with the parameters
     * @param pushMessage message for push notification
     */
    fun sendNotification(monkeyIDTo: String, paramsObject: JSONObject, pushMessage: String){
        val socketService = service
        socketService?.sendNotification(monkeyIDTo, paramsObject, pushMessage)
    }

    /**
     * Send a temporal notification.
     * @param monkeyIDTo session ID of the receiver
     * @param paramsObject JsonObject with the parameters
     */
    fun sendTemporalNotification(monkeyIDTo: String, paramsObject: JSONObject) {
        val socketService = service
        socketService?.sendTemporalNotification(monkeyIDTo, paramsObject)
    }

    /**
     * Get all conversation of a user using the monkey ID.
     */
    fun getConversationsFromServer(quantity: Int, fromTimestamp: Long){
        val socketService = service
        Log.d("SocketService","status: ${MonkeyKitSocketService.status}")
        socketService?.getAllConversations(quantity, fromTimestamp)
    }

    /**
     * Get all messages of a conversation.
     * @param monkeyid monkeyid ID of the user.
     * @param numberOfMessages number of messages to load.
     * @param lastTimeStamp last timestamp of the message loaded.
     */
    fun getConversationMessages(conversationId: String, numberOfMessages: Int, lastTimeStamp: String){
        val socketService = service
        socketService?.getConversationMessages(conversationId, numberOfMessages, lastTimeStamp)
    }

    /**
     * Remove a group member asynchronously int the Monkey server. This method help you to delete yourself
     * of the group. Response is delivered via monkeyJsonResponse.
     * @param group_id ID of the group
     * @param monkey_id ID of member to delete
     */
    fun removeGroupMember(group_id: String, monkey_id: String){
        val socketService = service
        socketService?.removeGroupMember(group_id, monkey_id)
    }

    /**
     * Add a member to a group asynchronously.
     * @param new_member Session ID of the new member
     * @param group_id ID of the group
     */
    fun addGroupMember(new_member: String, group_id: String){
        val socketService = service
        socketService?.addGroupMember(new_member, group_id)
    }

    fun getUsersInfo(userIds: String){
        val socketService = service
        socketService?.getUsersInfo(userIds)
    }
    /**
     * Delete a conversation.
     * @param monkeyid monkeyid ID of the user.
     */
    fun deleteConversation(conversationId: String){
        val socketService = service
        socketService?.deleteConversation(conversationId)
    }

    private data class DownloadMessage(val fileMessageId: String, val filepath: String,
                                       val props: JsonObject)
    companion object{
        val  CONNECTION_NOT_READY_ERROR = "MonkeyKitSocketService is not ready yet."
    }
}
