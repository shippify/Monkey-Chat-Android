package com.criptext.comunication

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.util.Base64
import android.util.Log
import android.webkit.MimeTypeMap
import com.criptext.ClientData
import com.criptext.MonkeyKitSocketService
import com.criptext.lib.KeyStoreCriptext
import com.criptext.http.LoggingInterceptor
import com.criptext.security.AESUtil
import com.criptext.security.RandomStringBuilder
import com.criptext.socket.SecureSocketService
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.*
import okio.Buffer
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Created by gesuwall on 6/15/16.
 */

class FileUploadService: IntentService("FileUploadService") {


    fun getContentType(fileType: Int) = when(fileType){
        MessageTypes.FileTypes.Audio -> AUDIO_TYPE
        MessageTypes.FileTypes.Photo -> IMAGE_TYPE
        else -> ""
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
    fun createMOKMessage(clientData: ClientData, textMessage: String, sessionIDTo: String, type: Int, params: JsonObject): MOKMessage{
        val datetimeorder = System.currentTimeMillis();
        val datetime = datetimeorder/1000;
        val srand = RandomStringBuilder.build(3);
        val idnegative = "-" + datetime;
        val message = MOKMessage(idnegative + srand, clientData.monkeyId, sessionIDTo, textMessage,
               "" + datetime, "" + type, params, null);
        message.datetimeorder = datetimeorder;
        return message;
    }

    private fun createSendJSON(clientData: ClientData, aesutil: AESUtil, idnegative: String,
                               sessionIDTo: String, elmensaje: String, pushMessage: String,
                               params: JsonObject?, props: JsonObject?): JsonObject{

        val args = JsonObject();
        val json = JsonObject();
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

    override fun onHandleIntent(p0: Intent?) {
        val clientData = ClientData(p0!!)

        val aesutil = AESUtil(KeyStoreCriptext.getString(this, clientData.monkeyId))
        val JSON = MediaType.parse("application/json; charset=utf-8")
        val contentType = MediaType.parse(getContentType(p0.getIntExtra(FILETYPE_KEY, 0)))
        val receiverId = p0.getStringExtra(RECEIVER_ID_KEY)
        val messageId = p0.getStringExtra(MESSAGE_ID_KEY)

        val parser = JsonParser()
        val paramsStr = p0.getStringExtra(PARAMS_KEY) ?: ""
        val propsStr = p0.getStringExtra(PARAMS_KEY)
        val params = if(paramsStr.isNotEmpty()) parser.parse(p0.getStringExtra(PARAMS_KEY)).asJsonObject
                        else JsonObject()

        val pushMessage = p0.getStringExtra(PUSHMESSAGE_KEY)

        val filepath = p0.getStringExtra(FILEPATH_KEY)
        val http = OkHttpClient().newBuilder()
                .addInterceptor(LoggingInterceptor())
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.MINUTES)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

        var finalData= IOUtils.toByteArray(FileInputStream(filepath));

        //COMPRIMIMOS CON GZIP
        val compressor = Compressor();
        finalData = compressor.gzipCompress(finalData);
        //ENCRIPTAMOS
        finalData= aesutil.encrypt(finalData);
        val tempFile = File.createTempFile(FilenameUtils.getBaseName(filepath), FilenameUtils.getExtension(filepath), cacheDir);

        val outputStream = FileOutputStream(tempFile)
        outputStream.write(finalData)
        outputStream.flush()
        //outputStream.close()

        val args = JsonObject();
        args.addProperty("sid", clientData.monkeyId);
        args.addProperty("rid", receiverId);
        args.addProperty("id", messageId);
        args.addProperty("push", pushMessage.replace("\\\\","\\"));
        args.addProperty("props", propsStr);
        args.addProperty("params", paramsStr);


        val json = JsonObject()
        json.add("data", args)
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file", FilenameUtils.getName(filepath), RequestBody.create(contentType, tempFile))
                .addFormDataPart("data", "data.json", RequestBody.create(JSON, json.toString()))
                .build()

        val credential = Credentials.basic(clientData.appId, clientData.appKey);
        val request = Request.Builder()
                    .header("Authorization", credential)
                    .url(MonkeyKitSocketService.httpsURL + "/file/new")
                    .post(body).build()

        Log.d("FileUploadService", request.body().toString())
        val response = http.newCall(request).execute();
        if(response.isSuccessful)
            Log.d("FileUploadService","success")
        else
            Log.e("FileUploadService", "error: ${response.body().string()}")

    }

    private fun stringifyRequestBody(request: Request): String {
        try {
            val copy = request.newBuilder().build();
            val buffer = Buffer();
            copy.body().writeTo(buffer);
            return buffer.readUtf8();
        } catch (e: IOException) {
            return "did not work";
        }
    }

    companion object {

        val AUDIO_TYPE = "audio/3gpp"
        val IMAGE_TYPE = "image/png"
        val FILETYPE_KEY = "FileUploadService.fileTypeKey"
        val FILEPATH_KEY = "FileUploadService.FilepathKey"
        val RECEIVER_ID_KEY = "FileUploadService.ReceiverMonkeyId"
        val MESSAGE_ID_KEY = "FileUploadService.MessageId"
        val PARAMS_KEY = "FileUploadService.ParamsKey"
        val PROPS_KEY = "FileUploadService.PropsKey"
        val PUSHMESSAGE_KEY = "FileUploadService.PushMessageKey"

        fun startUpload(ctx: Context, messageId: String, filepath: String, clientData: ClientData,
                        monkeyIDTo: String, fileType: Int, jsonProps: String,
                        jsonParams: String, pushMessage: String){
            val intent = Intent(ctx, FileUploadService::class.java)
            clientData.fillIntent(intent)
            intent.putExtra(FILEPATH_KEY, filepath)
            intent.putExtra(RECEIVER_ID_KEY, monkeyIDTo)
            intent.putExtra(MESSAGE_ID_KEY, messageId)
            intent.putExtra(FILETYPE_KEY, fileType)
            intent.putExtra(PARAMS_KEY, jsonParams)
            intent.putExtra(PROPS_KEY, jsonProps)
            intent.putExtra(PUSHMESSAGE_KEY, pushMessage)
            ctx.startService(intent)
        }
    }

}