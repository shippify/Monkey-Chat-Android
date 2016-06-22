package com.criptext

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.AsyncTask
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.util.Base64
import android.util.Log
import android.webkit.MimeTypeMap
import com.criptext.comunication.*
import com.criptext.database.CriptextDBHandler
import com.criptext.http.MonkeyHttpClient
import com.criptext.http.OpenConversationTask
import com.criptext.lib.KeyStoreCriptext
import com.criptext.lib.KotlinWatchdog
import com.criptext.lib.MonkeyKitDelegate
import com.criptext.lib.PendingMessageStore
import com.criptext.security.AESUtil
import com.criptext.security.AsyncAESInitializer
import com.criptext.security.RandomStringBuilder
import com.criptext.socket.SecureSocketService
import com.google.gson.JsonObject
import org.apache.commons.io.FilenameUtils
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

    var portionsMessages: Int = 15
    var lastTimeSynced: Long = 0L

    protected lateinit var clientData: ClientData
    protected lateinit var aesutil: AESUtil
    protected var watchdog: KotlinWatchdog? = null
    protected lateinit var asyncConnSocket: AsyncConnSocket
    internal lateinit var fileUploader: FileUploader
    var delegate: MonkeyKitDelegate? = null
    private var socketInitialized = false
    var isSyncService: Boolean = false
    private set
    internal var wakeLock: PowerManager.WakeLock? = null


    val messageHandler: MOKMessageHandler by lazy {
        MOKMessageHandler(this)
    }

    fun downloadFile(fileName: String, props: String, monkeyId: String, runnable: Runnable){
        fileUploader.downloadFile(fileName, props, monkeyId, runnable)
    }

    private fun initializeMonkeyKitService(intent: Intent){
        clientData = ClientData(intent)
        //Log.d("MonkeyKitSocketService", "session: ${clientData.monkeyId}")
        val asyncAES = AsyncAESInitializer(this, clientData.monkeyId)
        asyncAES.execute()

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isSyncService = intent?.getBooleanExtra(MonkeyKitSocketService.SYNC_SERVICE_KEY, false) ?: false
        if(isSyncService){
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MonkeyKitSocketService")
            wakeLock?.acquire();
            initializeMonkeyKitService(intent!!)
            return START_NOT_STICKY
        }
        return super.onStartCommand(intent, flags, startId)
    }
    override fun onBind(intent: Intent?): IBinder? {

        if(delegate == null) {
            initializeMonkeyKitService(intent!!)
            return MonkeyBinder()
        }

        return null
    }

    override fun onUnbind(intent: Intent?): Boolean {
        delegate = null
        return true
    }



    inner class MonkeyBinder : Binder() {

        fun getService(delegate: MonkeyKitDelegate): MonkeyKitSocketService{
            this@MonkeyKitSocketService.delegate = delegate
            return this@MonkeyKitSocketService;
        }

    }
    fun addMessageToDecrypt(encrypted: MOKMessage) {
        //TODO ADD UNDECRYPTED MAYBE?
    }

    fun executeInDelegate(method:CBTypes, info:Array<Any>) {
        //super already stores received messages and passes the to delegate
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


    fun startSocketConnection(aesUtil: AESUtil?) {
        fileUploader = FileUploader(this, aesUtil);
        if(aesUtil != null) {
            this.aesutil = aesUtil
            startSocketConnection()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        watchdog?.cancel()
        //persist pending messages to a file
        if(pendingMessages.isNotEmpty()){
            Log.d("serviceOnDestroy", "save messages")
            val task = PendingMessageStore.AsyncStoreTask(this, pendingMessages.toList())
            task.execute()
        }

        //persist last time synced
        asyncConnSocket.disconectSocket()
        KeyStoreCriptext.setLastSync(this, lastTimeSynced)
    }

    fun startSocketConnection() {
        asyncConnSocket = AsyncConnSocket(clientData, messageHandler, this);
        asyncConnSocket.conectSocket()
        socketInitialized = true
    }

    val serviceClientData: ClientData
        get() = clientData

    fun decryptAES(encryptedText: String) = aesutil.decrypt(encryptedText)

    fun isSocketConnected(): Boolean = socketInitialized && asyncConnSocket.isConnected

    fun notifySyncSuccess() {
        if(pendingMessages.isEmpty()){
            watchdog?.cancel()
            watchdog = null
        }
    }


    val pendingMessages: MutableList<JsonObject>

    init {
        pendingMessages = mutableListOf();
    }

     fun startWatchdog(){
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

    fun sendJsonThroughSocket(json: JsonObject) {
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
        val transitionMessagesPrefs = "MonkeyKit.transitionMessages";
        val lastSyncPrefs = "MonkeyKit.lastSyncTime";
        val lastSyncKey = "MonkeyKit.lastSyncKey";

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



}
