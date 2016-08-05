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
import com.criptext.comunication.MonkeyHttpResponse
import com.criptext.comunication.PushMessage
import com.criptext.security.RandomStringBuilder
import com.google.gson.JsonObject
import org.apache.commons.io.FilenameUtils
import java.util.*

abstract class MKDelegateActivity : AppCompatActivity(), MonkeyKitDelegate {

    var service: MonkeyKitSocketService? = null

    abstract val serviceClassName: Class<*>

    val isSocketConnected: Boolean
        get () =  service?.isSocketConnected() ?: false

    private val messagesToForwardToService = ArrayList<DelegateMOKMessage>()
    val pendingFiles = HashMap<String, DelegateMOKMessage>();
    private val pendingDownloads = HashMap<String, DownloadMessage>();

    private val monkeyKitConnection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            Log.d("MKDelegateActivity", "service connected")
            val binder = p1 as MonkeyKitSocketService.MonkeyBinder
            val sService = binder.getService(this@MKDelegateActivity)
            service = sService

            val removeTexts =  { it: DelegateMOKMessage ->
                it.message.type.toInt() == MessageTypes.blMessageDefault }
            val sendTexts = { ss: MonkeyKitSocketService, it: DelegateMOKMessage ->
                val res = ss.sendMessage(it.message, it.push, it.isEncrypted)
            }

            removeAndSend(sService, removeTexts, sendTexts)
            onBoundToService()

        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            service = null
        }
    }

    fun removeAndSend(socketService: MonkeyKitSocketService,
                      removeCondition: (DelegateMOKMessage) -> Boolean?,
                      sendAction: (MonkeyKitSocketService, DelegateMOKMessage) -> Unit){
        var i = messagesToForwardToService.size - 1
            while(i > -1){
                val shouldRemove = removeCondition.invoke(messagesToForwardToService[i]) ?: false
                if(shouldRemove){
                    val msg = messagesToForwardToService.removeAt(i)
                    sendAction.invoke(socketService, msg)
                }
                i -= 1
            }
    }

    override fun onStart() {
        super.onStart()
        MonkeyKitSocketService.bindMonkeyService(this, monkeyKitConnection, serviceClassName)

    }

    override fun onStop() {
        super.onStop()
        service = null
        unbindService(monkeyKitConnection)
    }

    abstract fun onBoundToService()

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

    fun persistFileMessageAndSend(filePath: String, monkeyIDFrom: String, monkeyIDTo: String, fileType: Int,
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

        storeSentMessage(newMessage)
        return newMessage

    }

    fun persistMessageAndSend(text: String, monkeyIDFrom: String, monkeyIDTo: String, params: JsonObject,
                              pushMessage: PushMessage, isEncrypted: Boolean): MOKMessage{
        val newMessage = createMOKMessage(text, monkeyIDFrom, monkeyIDTo,
                MessageTypes.blMessageDefault, params)
        val socketService = service
        if(socketService != null ){
            socketService.sendMessage(newMessage, pushMessage, isEncrypted)
        } else {
            messagesToForwardToService.add(DelegateMOKMessage(newMessage, pushMessage, isEncrypted))
        }
        storeSentMessage(newMessage)
        return newMessage
    }

    abstract fun storeSentMessage(message: MOKMessage)

    override fun onSocketConnected() {
        for(file in pendingFiles.values){
            service!!.sendFileMessage(file.message, file.push, file.isEncrypted)
        }
    }

    override fun onAcknowledgeRecieved(senderId: String?, recipientId: String?, newId: String?,
                                       oldId: String?, read: Boolean?, messageType: Int) {
        pendingFiles.remove(oldId)
    }

    override fun onFileFailsUpload(message: MOKMessage) {
        val sentFile = pendingFiles[message.message_id]
        sentFile?.failed = true
    }

    override fun onFileDownloadFinished(fileMessageId: String?, success: Boolean) {
        pendingDownloads.remove(fileMessageId)
    }

    fun resendFile(fileMessage: MOKMessage, pushMessage: PushMessage, isEncrypted: Boolean) {
        if(!pendingFiles.containsKey(fileMessage.message_id)){
           pendingFiles[fileMessage.message_id] = DelegateMOKMessage(fileMessage, pushMessage, isEncrypted)
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

    fun downloadFile(fileMessageId: String, filepath: String, props: JsonObject, monkeyId: String){
        if(!pendingDownloads.containsKey(filepath)){
            service?.downloadFile(fileMessageId, filepath, props.toString(), monkeyId)

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

    }

    private data class DownloadMessage(val fileMessageId: String, val filepath: String, val props: JsonObject)
    companion object{
        val  CONNECTION_NOT_READY_ERROR = "MonkeyKitSocketService is not ready yet."
    }
}
