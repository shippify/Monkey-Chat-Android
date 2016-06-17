package com.criptext

import android.app.Service
import android.util.Log
import android.webkit.MimeTypeMap
import com.criptext.comunication.AsyncConnSocket
import com.criptext.comunication.FileUploadService
import com.criptext.comunication.MOKMessage
import com.criptext.comunication.MessageTypes
import com.criptext.lib.KotlinWatchdog
import com.criptext.lib.PendingMessageStore
import com.criptext.security.AESUtil
import com.criptext.security.RandomStringBuilder
import com.criptext.socket.SecureSocketService
import com.google.gson.JsonObject
import org.apache.commons.io.FilenameUtils
import org.json.JSONObject
import java.io.FileInputStream
import java.util.*

/**
 * Created by gesuwall on 6/10/16.
 */

abstract class MsgSenderService() : Service() , SecureSocketService {
    protected lateinit var clientData: ClientData
    protected lateinit var aesutil: AESUtil
    protected var watchdog: KotlinWatchdog? = null
    protected lateinit var asyncConnSocket: AsyncConnSocket
    internal lateinit var fileUploader: FileUploader

    val pendingMessages: MutableList<JsonObject>

    init {
        pendingMessages = mutableListOf();
    }

     override fun startWatchdog(){
        if(watchdog == null) {
            watchdog = KotlinWatchdog(this);
            watchdog!!.start();
        }
    }

    /**
     * Makes a copy of the current state of the pendingMessages list and sends through the socket
     * all the contained messages.
     */
    fun resendPendingMessages(){
        val messages = pendingMessages.toList()
        for(msg in messages)
            sendJsonThroughSocket(msg)
    }

    fun addPendingMessages(messages: List<JsonObject>){
        pendingMessages.addAll(0, messages)
    }

    /**
     * get the id of a message
     */
    fun getJsonMessageId(json: JsonObject) = json.get("args").asJsonObject.get("id").asString

    /**
     * Uses binary search to remove a message from the pending messages list.
     * @param id id of the message to remove
     */
    fun removePendingMessage(id: String){
        val index = pendingMessages.binarySearch { n ->
            id.compareTo(getJsonMessageId(n))
        }

        if(index > -1) {
            pendingMessages.removeAt(index)
            if(pendingMessages.isEmpty()) {
                watchdog?.cancel()
                watchdog = null
            }
        }
    }

    /**
     * Ads a message to the list of pending messages and starts the watchdog.
     * @param json mensaje a guardar
     * @throws JSONException
     */
    private fun addMessageToWatchdog(json: JsonObject) {
        pendingMessages.add(json);
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
                "" + datetime, "" + type, params, null);
        message.datetimeorder = datetimeorder;
        return message;
    }

    private fun createSendProps(old_id: String): JsonObject {
        val props = JsonObject();
        props.addProperty("str", "0");
        props.addProperty("encr", "1");
        props.addProperty("device", "android");
        props.addProperty("old_id", old_id);
        return props;
    }

    private fun createSendJSON(idnegative: String, sessionIDTo: String, elmensaje: String,
                               pushMessage: String, params: JsonObject, props: JsonObject): JsonObject {

        val args= JsonObject();
        val json= JsonObject();
        val pushObject = JsonObject();

        try {
            pushObject.addProperty("key", "text");
            pushObject.addProperty("value", pushMessage.replace("\\\\", "\\"));

            args.addProperty("id", idnegative);
            args.addProperty("sid", clientData.monkeyId);
            args.addProperty("rid", sessionIDTo);
            args.addProperty("msg", aesutil.encrypt(elmensaje));
            args.addProperty("type", MessageTypes.MOKText);
            args.addProperty("push", pushObject.toString());
            if (params != null)
                args.addProperty("params", params.toString());
            if (props != null)
                args.addProperty("props", props.toString());

            json.add("args", args);
            json.addProperty("cmd", MessageTypes.MOKProtocolMessage);
        } catch(ex: Exception){
            ex.printStackTrace();
        }

        return json;
    }

    override fun sendJsonThroughSocket(json: JsonObject) {
        asyncConnSocket.sendMessage(json);
    }

    /**
     * Disconnect the socket immediately. Useful for reconnecting.
     */
    fun forceDisconnect(){
        if(isSocketConnected()){
            asyncConnSocket.sendDisconectFromPull()
            delegate?.onSocketDisconnected()
        } else {
            Log.d("forceDisconnect", "${asyncConnSocket.socketStatus}")
            startSocketConnection();
        }
    }

    override fun startSocketConnection(aesUtil: AESUtil?) {
        fileUploader = FileUploader(this, aesUtil);
    }
    private fun sendMessage(elmensaje: String, sessionIDTo: String, pushMessage: String,
                            params: JsonObject, persist: Boolean): MOKMessage{

        if(elmensaje.length > 0){
            try {

                val newMessage = createMOKMessage(elmensaje, sessionIDTo, MessageTypes.blMessageDefault, params);

                val props = createSendProps(newMessage.message_id);
                newMessage.props = props;


                val json= createSendJSON(newMessage.message_id, sessionIDTo, elmensaje, pushMessage,
                        params, props);


                addMessageToWatchdog(json);
                if(persist) {
                    storeMessage(newMessage, false, Runnable {
                            sendJsonThroughSocket(json);
                        })
                }
                else{
                    sendJsonThroughSocket(json);
                }

                return newMessage;
            }
            catch (e: Exception) {
                e.printStackTrace();
                return MOKMessage() ;
            }
        }

        return MOKMessage();
    }

    /**
     * Envia un mensaje sin guardarlo en la base de datos.
     * @param elmensaje el texto del mensaje
     * @param sessionIDTo session ID del remitente
     * @param pushMessage mensaje a enviar en el push notification
     * @param params JsonObject con el params a enviar en el MOKMessage
     * @return el MOKMessage enviado.
     */
    private fun sendMessage(elmensaje: String, sessionIDTo: String, pushMessage: String, params: JsonObject): MOKMessage {
        return sendMessage(elmensaje, sessionIDTo, pushMessage, params, false);

    }

    fun uploadFile(json: JSONObject, message: MOKMessage){
        //TODO UPLOAD WITH OKHTTP
    }

    private fun createSendProps(old_id: String, filepath: String, fileType: Int, originalSize: Int): JsonObject{
        val props = JsonObject();
        props.addProperty("str", "0");
        props.addProperty("encr", "1");
        props.addProperty("device", "android");
        props.addProperty("old_id", old_id);

        val ext = FilenameUtils.getExtension(filepath)
        props.addProperty("cmpr", "gzip");
        props.addProperty("file_type", fileType);
        props.addProperty("ext", ext);
        props.addProperty("filename", FilenameUtils.getName(filepath));
        props.addProperty("mime_type", MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext));
        props.addProperty("size", originalSize);
        return props;
    }

    fun persistFileMessageAndSend(pathToFile: String, sessionIDTo: String, file_type: Int,
                                  pushMessage: String, gsonParamsMessage: JsonObject): MOKMessage{
        //TODO PERSIST FILE & SEND
        /*
        val newMessage = createMOKMessage(pathToFile, sessionIDTo, file_type, gsonParamsMessage);
        val file = FileInputStream(pathToFile)
        val fileSize = file.available()
        file.close()
        val props = createSendProps(newMessage.message_id, pathToFile, file_type, fileSize)
        newMessage.props = props
        storeMessage(newMessage, false, Runnable {
            FileUploadService.startUpload(this, newMessage.message_id, pathToFile, clientData, sessionIDTo,
                    file_type, props.toString(), gsonParamsMessage.toString(), pushMessage)
        })
        return MOKMessage();
        */
        return fileUploader.persistFileMessageAndSend(pathToFile, sessionIDTo, file_type,
                        gsonParamsMessage, pushMessage)
    }

    fun persistMessageAndSend(messageText: String, sessionIDTo: String,
                              pushMessage: String, gsonParamsMessage: JsonObject): MOKMessage{
        return sendMessage(messageText, sessionIDTo, pushMessage, gsonParamsMessage, true);
    }

    override fun onDestroy() {
        super.onDestroy()
        //cancel watchdog on destroy
        watchdog?.cancel()
        //persist pending messages to a file
        if(pendingMessages.isNotEmpty()){
            Log.d("serviceOnDestroy", "save messages")
            val task = PendingMessageStore.AsyncStoreTask(this, pendingMessages.toList())
            task.execute()
        }

    }

    companion object {
        val transitionMessagesPrefs = "MonkeyKit.transitionMessages";
        val lastSyncPrefs = "MonkeyKit.lastSyncTime";
        val lastSyncKey = "MonkeyKit.lastSyncKey";

    }




}
