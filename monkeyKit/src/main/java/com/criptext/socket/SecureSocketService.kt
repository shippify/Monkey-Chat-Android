package com.criptext.socket

import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.AsyncTask
import android.util.Base64
import android.util.Log
import com.criptext.ClientData
import com.criptext.MonkeyKitSocketService
import com.criptext.comunication.CBTypes
import com.criptext.comunication.MOKMessage
import com.criptext.comunication.MessageTypes
import com.criptext.database.CriptextDBHandler
import com.criptext.http.MonkeyHttpClient
import com.criptext.http.OpenConversationTask
import com.criptext.lib.KeyStoreCriptext
import com.criptext.lib.MonkeyKitDelegate
import com.criptext.security.AESUtil
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
 * Created by gesuwall on 6/1/16.
 */

interface SecureSocketService {

    /*
    var portionsMessages: Int
    var lastTimeSynced: Long
    var delegate: MonkeyKitDelegate?

    fun startSocketConnection()

    fun startSocketConnection(aesUtil: AESUtil?)

    fun isSocketConnected(): Boolean

    fun sendJsonThroughSocket(json: JsonObject)

    fun startWatchdog()

    fun notifySyncSuccess(){
        val syncService = this as? MonkeyKitSocketService
        syncService.wakeLock?.release()
        syncService.stopSelf()
    }

    fun decryptAES(encryptedText: String): String

    /**
     * sends a message to a bound client, so that the client can process it and update its UI.
     * @param type the type of the message
     * @param obj An object that may be a MOKMessage or a list of messages depending on the type
     */
    fun executeInDelegate(type: CBTypes, info: Array<Any>){
        when(type){
            CBTypes.onSocketConnected -> {
                delegate?.onSocketConnected()
                sendSync(lastTimeSynced)
            }
            CBTypes.onMessageReceived -> {
                val message = info[0] as MOKMessage
                val tipo = CriptextDBHandler.getMonkeyActionType(message);
                if(tipo == MessageTypes.blMessageAudio ||
                    tipo == MessageTypes.blMessagePhoto ||
                    tipo == MessageTypes.blMessageDocument ||
                    tipo == MessageTypes.blMessageScreenCapture ||
                    tipo == MessageTypes.blMessageShareAFriend ||
                    tipo == MessageTypes.blMessageDefault)
                    storeMessage(message, true, Runnable {
                        delegate?.onMessageRecieved(message)
                    })
            }

            CBTypes.onMessageBatchReady -> {
                val batch = info[0] as ArrayList<MOKMessage>;
                storeMessageBatch(batch, Runnable {
                            delegate?.onMessageBatchReady(batch);
                });
            }
        }
    }

    /**
     * Adds a message to a list of messages that haven't been decrypted yet because the necessary
     * keys are missing. Once the key is received, all of those messages are decrypted, stored in
     * the database and any bound client are notified afterwards.
     * @param encrypted message that has not been decrypted yet.
     */
    fun addMessageToDecrypt(encrypted: MOKMessage)

    /**
     * Notifies the MonkeyKit server that the current user has opened an UI with conversation with
     * another user or a group. The server will notify the other party and will return any necessary
     * keys for decrypting messages sent by the other party.
     *
     * This method can also be used to retrieve any missing AES keys.
     */
    fun sendOpenConversation(conversationID: String){

    }

    fun requestKeysForMessage(encryptedMessage: MOKMessage){

        fun filter(list: MutableList<MOKMessage>, predicate: (MOKMessage) -> Boolean): List<MOKMessage>{
            val filtered = mutableListOf<MOKMessage>()
            var cont = 0
            for(item in list){
                if(predicate.invoke(item)){
                    filtered.add(list.removeAt(cont))
                }
                cont++
            }

            return filtered.toList()
        }


            //val idPredicate: (MOKMessage) -> Boolean =  { it -> it.sid == first.sid }
            //val sameConversationMsgs = filter(pendingMessages, idPredicate)
            val task = OpenConversationTask(this, encryptedMessage)
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, encryptedMessage.sid) //LAME
    }

    fun sendSync(lastTimeSync: Long){
        try {

           val args = JsonObject();
           val json = JsonObject();

            args.addProperty("since", lastTimeSync);

            if(lastTimeSync == 0L) {
                args.addProperty("groups", 1);
            }
            args.addProperty("qty", ""+ portionsMessages);
            json.add("args", args);
            json.addProperty("cmd", MessageTypes.MOKProtocolSync);

            if(isSocketConnected()){
                System.out.println("MONKEY - Enviando Sync:"+json.toString());
                sendJsonThroughSocket(json)
            }
            else
                System.out.println("MONKEY - no pudo enviar Sync - socket desconectado");



        } catch (e: Exception) {
            e.printStackTrace();
        }

        this.lastTimeSynced = lastTimeSync

    }

    fun sendGet(since: String){

        try {

            val args = JsonObject();
            val json = JsonObject();

            args.addProperty("messages_since", since);
            if(since.equals("0")) {
                args.addProperty("groups", 1);
            }
            args.addProperty("qty", "" + portionsMessages);
            json.add("args", args);
            json.addProperty("cmd", MessageTypes.MOKProtocolGet);

            if(isSocketConnected())
                sendJsonThroughSocket(json)

            startWatchdog()

        } catch (e: Exception) {
            e.printStackTrace();
        }


        lastTimeSynced = since.toLong();
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
            KeyStoreCriptext.putStringBlocking(context, sessionIdTo, newKeys);
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
    public fun requestTextWithLatestKeys(messageId: String): String?{
        val httpParams = BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 20000);
        HttpConnectionParams.setSoTimeout(httpParams, 25000);
        // Create a new HttpClient and Post Header
        val httpclient = MonkeyHttpClient.newClient();
        val httppost = HttpGet("$httpsURL/message/$messageId/open/secure");

        try {

            val base64EncodedCredentials = "Basic " + Base64.encodeToString(
                    (serviceClientData.password).toByteArray(),
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

    val context: Context

    val appContext: Context

    val serviceClientData: ClientData

    /**
     * Guarda un mensaje de MonkeyKit en la base de datos. La implementacion de este metodo deberia de
     * ser asincrona para mejorar el rendimiento del servicio. MonkeyKit llamara a este metodo cada
     * vez que reciba un mensaje para guardarlo.
     * @param message
     * @param incoming
     * @param runnable Este runnable debe ejecutarse despues de guardar el mensaje
     */
    abstract fun storeMessage(message: MOKMessage, incoming: Boolean, runnable: Runnable)

    /**
     * Guarda un grupo de mensajes de MonkeyKit que se recibieron despues de un sync en la base de datos.
     * Es sumamente importante implementar esto de forma asincrona porque potencialmente, podrian
     * llegar cientos de mensajes, haciendo la operacion sumamente costosa.
     * @param messages
     * @param runnable Este runnable debe ejecutarse despues de guardar el batch de mensajes
     */
    abstract fun storeMessageBatch(messages: ArrayList<MOKMessage>, runnable: Runnable);


    companion object {

        val baseURL = "monkey.criptext.com"
        val httpsURL = "http://" + baseURL
        val SYNC_SERVICE_KEY = "SecureSocketService.SyncService"

        fun bindMonkeyService(context:Context, connection: ServiceConnection, service:Class<*>, clientData: ClientData) {
            val intent = Intent(context, service)
            clientData.fillIntent(intent)
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        fun startSyncService(context: Context, service:Class<*>, clientData: ClientData){
            val intent = Intent(context, service)
            clientData.fillIntent(intent)
            context.startService(intent)
        }

    }
*/
}
