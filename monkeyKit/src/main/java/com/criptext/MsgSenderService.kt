package com.criptext

import android.app.Service
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.util.Log
import com.criptext.ClientData
import com.criptext.MonkeyKitSocketService
import com.criptext.comunication.AsyncConnSocket
import com.criptext.comunication.MOKMessage
import com.criptext.comunication.MessageTypes
import com.criptext.lib.KotlinWatchdog
import com.criptext.lib.Watchdog
import com.criptext.security.AESUtil
import com.criptext.security.RandomStringBuilder
import com.criptext.socket.SecureSocketService
import com.google.gson.JsonObject
import org.json.JSONObject
import java.util.*

/**
 * Created by gesuwall on 6/10/16.
 */

abstract class MsgSenderService() : Service() , SecureSocketService {
    protected lateinit var clientData: ClientData
    protected lateinit var aesutil: AESUtil
    protected var watchdog: KotlinWatchdog? = null
    protected lateinit var asyncConnSocket: AsyncConnSocket

    val pendingMessages: MutableList<JsonObject>

    init {
        pendingMessages = mutableListOf();
    }

     override fun startWatchdog(){
        if(watchdog == null) {
            watchdog = KotlinWatchdog(this);
            watchdog!!.start();
        }
        Log.d("Watchdog", "Watchdog ready");
    }

    fun resendPendingMessages(){
        val messages = pendingMessages.toList()
        for(msg in messages)
            sendJsonThroughSocket(msg)
    }

    fun getJsonMessageId(json: JsonObject) = json.get("args").asJsonObject.get("id").asString

    fun removePendingMessage(id: String){
        val index = pendingMessages.binarySearch { n ->
            id.compareTo(getJsonMessageId(n))
        }
        if(index > 0) {
            pendingMessages.removeAt(index)
            if(pendingMessages.isEmpty()) {
                watchdog?.cancel()
                watchdog = null
            }
        }
    }

    /**
     * Agrega un mensaje a la base de datos del watchdog. Si el watchdog no esta corriendo, lo
     * inicia.
     * @param json mensaje a guardar
     * @throws JSONException
     */
    private fun addMessageToWatchdog(json: JsonObject) {
        val args = json.getAsJsonObject("args");
        try {
            pendingMessages.add(json);
            startWatchdog()
        }catch (ex: Exception){
            ex.printStackTrace();
        }
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
        }
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

    fun persistFileMessageAndSend(pathToFile: String, sessionIDTo: String, file_type: Int,
                                  pushMessage: String, gsonParamsMessage: JsonObject): MOKMessage{
        //TODO PERSIST FILE & SEND
        return MOKMessage();
    }

    fun persistMessageAndSend(messageText: String, sessionIDTo: String,
                              pushMessage: String, gsonParamsMessage: JsonObject): MOKMessage{
        //TODO PERSIST FILE & SEND
        return sendMessage(messageText, sessionIDTo, pushMessage, gsonParamsMessage, true);
    }

    companion object {
        val transitionMessagesPrefs = "MonkeyKit.transitionMessages";
        val lastSyncPrefs = "MonkeyKit.lastSyncTime";
        val lastSyncKey = "MonkeyKit.lastSyncKey";

    }




}
