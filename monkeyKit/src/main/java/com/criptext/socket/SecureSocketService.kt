package com.criptext.socket

import android.content.Context
import android.util.Base64
import android.util.Log
import com.criptext.ClientData
import com.criptext.comunication.MessageTypes
import com.criptext.http.MonkeyHttpClient
import com.criptext.lib.KeyStoreCriptext
import com.criptext.security.AESUtil
import org.apache.http.HttpResponse
import org.apache.http.client.ClientProtocolException
import org.apache.http.client.methods.HttpGet
import org.apache.http.params.BasicHttpParams
import org.apache.http.params.HttpConnectionParams
import org.apache.http.params.HttpParams
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * Created by gesuwall on 6/1/16.
 */

interface SecureSocketService {
    var portionsMessages: Int
    var lastTimeSynced: Long

    fun startSocketConnection()

    fun startSocketConnection(aesUtil: AESUtil?)

    fun isSocketConnected(): Boolean

    fun sendJsonThroughSocket(json:JSONObject)

    fun startWatchdog()

    fun notifySyncSuccess()

    fun decryptAES(encryptedText: String): String

    fun sendSync(lastTimeSync: Long){
        try {

           val args = JSONObject();
           val json = JSONObject();

            args.put("since", lastTimeSync);

            if(lastTimeSync == 0L) {
                args.put("groups", 1);
            }
            args.put("qty", ""+ portionsMessages);
            json.put("args", args);
            json.put("cmd", MessageTypes.MOKProtocolSync);

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

            val args = JSONObject();
            val json = JSONObject();

            args.put("messages_since", since);
            if(since.equals("0")) {
                args.put("groups", 1);
            }
            args.put("qty", "" + portionsMessages);
            json.put("args", args);
            json.put("cmd", MessageTypes.MOKProtocolGet);

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
            KeyStoreCriptext.putString(context, sessionIdTo, newKeys);
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

    companion object {

        val baseURL = "monkey.criptext.com"
        val httpsURL = "https://" + baseURL

    }

}
