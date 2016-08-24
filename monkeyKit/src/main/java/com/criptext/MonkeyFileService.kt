package com.criptext

import android.app.IntentService
import android.content.Intent
import android.os.PowerManager
import android.support.v4.content.LocalBroadcastManager
import android.util.Base64
import android.util.Log
import android.webkit.MimeTypeMap
import com.criptext.comunication.Compressor
import com.criptext.comunication.MOKMessage
import com.criptext.lib.KeyStoreCriptext
import com.criptext.security.AESUtil
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.*
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Service for HTTP transfers of file messages. This service is independent of MonkeyKitSocket service
 *
 * When it uploads a file, if MonkeyKitSocketService is not bound to a client, it updates the
 * database. For this, the developer must implement the onFileUploadFinished method with the necessary
 * logic to update the database
 * Created by GAumala on 8/1/16.
 */

abstract class MonkeyFileService: IntentService(TAG){
    var aesUtil: AESUtil? = null
    private var wakeLock: PowerManager.WakeLock? = null
    override fun onHandleIntent(p0: Intent?) {

        if(wakeLock == null){
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG)
            wakeLock?.acquire();
        }

        val intent = p0 as Intent

        val appId = intent.getStringExtra(APPID_KEY)
        val appKey = intent.getStringExtra(APPKEY_KEY)
        val credential = Credentials.basic(appId, appKey);

        if(intent.getBooleanExtra(ISUPLOAD_KEY, false)){
            val mokMessage = MOKMessage(intent)
            if(aesUtil == null)
                aesUtil = AESUtil(this, mokMessage.sid)

            val ext = FilenameUtils.getExtension(mokMessage.msg)
            val name = FilenameUtils.getBaseName(mokMessage.msg)
            val rawByteData = IOUtils.toByteArray(FileInputStream(mokMessage.msg));
            val processedByteData = processSentBytes(intent.getBooleanExtra(ENCR_KEY, true), rawByteData)
            val data = createUploadDataJsonString(mokMessage, intent.getStringExtra(PUSH_KEY), rawByteData.size)

            val http = authorizedHttpClient(appId, appKey, 300L, 10L)
            val uploadResponse = uploadFile(http, credential, data, processedByteData, ext = ext)
            onFileUploadFinished(mokMessage, uploadResponse == null)
            broadcastResponse(UPLOAD_ACTION, mokMessage, uploadResponse)
        } else {
            val mokDownload = MOKDownload(intent)
            if(aesUtil == null)
                aesUtil = AESUtil(this, mokDownload.sid)
            val http = authorizedHttpClient(appId, appKey, 10L, 300L)
            val downloadBytes = downloadFile(http, credential, MonkeyKitSocketService.httpsURL
                    + "/file/open/"+ FilenameUtils.getBaseName(mokDownload.msg))
            val filepath = processReceivedBytes(downloadBytes, mokDownload)
            onFileDownloadFinished(mokDownload.id, filepath == null)
            broadcastResponse(DOWNLOAD_ACTION, mokDownload.id, filepath != null)
        }

    }

    /**
     * Create a Json object with data to send along with the uploading file.
     * @param mokMessage the file message to upload
     * @param push the push message to send with the file
     * @param fileSize the uncompressed size of the file to upload
     * @return A json encoded string with the JSON object created, ready to be included in a
     * Okhttp multipart form
     */
    private fun createUploadDataJsonString(mokMessage: MOKMessage, push: String, fileSize: Int): String{
        val args = JsonObject();
        var paramsMessage: JsonObject = mokMessage.params ?: JsonObject()
        var propsMessage: JsonObject = mokMessage.props ?: JsonObject()

        val monkeyId = mokMessage.sid

        args.addProperty("sid", monkeyId)
        args.addProperty("rid", mokMessage.rid)


        args.add("params", paramsMessage);
        args.addProperty("id", mokMessage.message_id);
        args.addProperty("push", push);

        propsMessage.addProperty("size", fileSize)
        args.add("props", propsMessage)

        return args.toString()

    }

    private fun processReceivedBytes(downloadBytes: ByteArray?, mokDownload: MOKDownload): String?{
        var resultBytes: ByteArray? = null
        if(downloadBytes != null){
            val props = mokDownload.props
            if(props.get("encr").asString == "1"){//Decrypt
                val claves = KeyStoreCriptext.getString(this, mokDownload.sid);
                val claveArray = claves.split(":")
                resultBytes = aesUtil!!.decryptWithCustomKeyAndIV(downloadBytes, claveArray[0], claveArray[1])
                if(props.get("device").asString == "web"){
                    var utf8str = resultBytes.toString(charset("utf8"))
                    utf8str = utf8str.substring(utf8str.indexOf(",") + 1, utf8str.length);
                    resultBytes = Base64.decode(utf8str.toByteArray(charset("utf8")), 0);
                }
            }

            if (props.get("cmpr").asString == "gzip") {//Decompress
                val compressor = Compressor();
                resultBytes = compressor.gzipDeCompress(resultBytes);
            }

            val filepath = mokDownload.msg
            IOUtils.write(resultBytes, FileOutputStream(File(filepath)))
            return filepath
        }

        return null
    }
    /**
     * Compress bytes and optionally encrypt them
     * @param isEncrypted if true, the bytes will be encrypted with AES
     * @param byteData Array of bytes to compress
     * @return an array of bytes compressed and optionally encrypted
     */
    private fun processSentBytes(isEncrypted: Boolean, byteData: ByteArray): ByteArray{
        //COMPRIMIMOS CON GZIP
        val compressor = Compressor();
        val compressedData = compressor.gzipCompress(byteData);

        //ENCRIPTAMOS
        return if(isEncrypted){
            aesUtil!!.encrypt(compressedData)
        } else compressedData;

    }

    /**
     * Uploads a file to MonkeyKit Server using OkHttp3.
     * @param httpClient Okhttp client with MonkeyKit Server URL, credentials and appropiate timeout
     * for file upload
     * @param credentials the basic auth credentials for MonkeyKit Server. user: APP_ID, pass: APP_KEY
     * @param jsonData A json encoded String with data to send along with the file
     * @param byteData binary data of the file to send, This should be compressed and optionally
     * encrypted
     * @param ext File extension of the binary data to upload
     * @return a Json encoded String with the servers response. If a network error occurred during
     * upload, null is returned
     */
    private fun uploadFile(httpClient: OkHttpClient,credentials: String, jsonData: String,
                   byteData: ByteArray, ext: String): String?{
        val mimeType= MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        val tempFileName = cacheDir.absolutePath + "/sendFile${System.currentTimeMillis()}.$ext"
        val sendFile = File(tempFileName)
        FileUtils.writeByteArrayToFile(sendFile, byteData)
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("data", jsonData)
            .addFormDataPart("file", tempFileName, RequestBody.create(MediaType.parse("$mimeType"), sendFile)).build()

        val request = Request.Builder()
                .url(MonkeyKitSocketService.httpsURL + "/file/new")
                .header("Authorization", credentials)
                .post(body).build()


        try {
            val response = httpClient.newCall(request).execute().body().string();
            if(response != null)
                Log.d("FileService", response)
            return response
        }catch(ex: Exception){
            return null
        } finally{
            FileUtils.deleteQuietly(sendFile)
        }
    }

    private fun downloadFile(httpClient: OkHttpClient,credentials: String, url: String): ByteArray?{
        val request = Request.Builder()
                .url(url)
                .header("Authorization", credentials)
                .build()
        try {
            val response = httpClient.newCall(request).execute().body().bytes()
            return response
        }catch(ex: Exception){
            ex.printStackTrace()
            return null
        } finally{
        }
    }
    /**
     * Send a local broadcast with the response from the server
     * @param action The action that resulted in a server response
     * @param mokMessage the mokMessage used with the action
     * @param response The server response to be sent as a local broadcast
     */
    private fun broadcastResponse(action: String, mokMessage: MOKMessage?, response: String?){
        val intent = Intent(action)
                .putExtra(RESPONSE_KEY, response);
        mokMessage?.toIntent(intent)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private fun broadcastResponse(action: String, msgId: String, downloadComplete: Boolean){
        val intent = Intent(action)
        intent.putExtra(MOKMessage.ID_KEY, msgId)
        intent.putExtra(RESPONSE_KEY, if(downloadComplete) "ok" else null)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
    /**
     * Starts an OkHttp client
     * @param appId Monkey Kit APP ID. Needed for basic auth
     * @param appKey Monkey Kit APP KEY. Needed for basic auth
     * @param writeTimeout a Long with the time to wait to upload data
     * @param readTimeout a Long with the time to wait to download data
     * @param readTimeout a new instance of OkHttpClient for MonkeyKit Server
     */
    fun authorizedHttpClient(appId: String, appKey: String, writeTimeout: Long, readTimeout: Long) = OkHttpClient().newBuilder()
                .authenticator({ route, response ->
                    val credential = Credentials.basic(appId, appKey);
                    response.request().newBuilder()
                            .header("Authorization", credential).build()
                })
                .connectTimeout(10L, TimeUnit.SECONDS)
                .writeTimeout(writeTimeout, TimeUnit.SECONDS)
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .build()

    override fun onDestroy() {
        super.onDestroy()
        wakeLock?.release()
    }

    /**
     * Callback to be executed once a file finishes uploading. This will only be called if the
     * MonkeyKitSocketService is not bound to a client. This means that the upload operation
     * finished in background and the app is no longer active, so this service must update the
     * database with the result of the upload action.
     * @param mokMessage Message uploaded.
     * @param error true if the message could not be uploaded successfully.
     */
    abstract fun onFileUploadFinished(mokMessage: MOKMessage, error: Boolean)

    /**
     * Callback to be executed once a file finishes downloading. This will only be called if the
     * MonkeyKitSocketService is not bound to a client. This means that the download operation
     * finished in background and the app is no longer active, so this service must update the
     * database with the result of the download action.
     * @param messageId Id of the message downloaded.
     * @param error true if the message could not be downloaded successfully.
     */
    abstract fun onFileDownloadFinished(messageId: String, error: Boolean)

    private data class MOKDownload(val id: String, val msg: String,val props: JsonObject, val sid: String){
        constructor(intent: Intent): this(id = intent.getStringExtra(MOKMessage.ID_KEY),
                msg = intent.getStringExtra(MOKMessage.MSG_KEY),
                props = JsonParser().parse(intent.getStringExtra(MOKMessage.PROPS_KEY)).asJsonObject,
                sid = intent.getStringExtra(MOKMessage.SID_KEY))
    }
    companion object {
        val PUSH_KEY = "MonkeyFileService.push"
        val ENCR_KEY = "MonkeyFileService.encr"
        val APPID_KEY = "MonkeyFileService.appId"
        val APPKEY_KEY = "MonkeyFileService.appKey"
        val ISUPLOAD_KEY = "MonkeyFileService.isUploadService"
        val TAG = "MonkeyFileService"

        val UPLOAD_ACTION = "MonkeyFileService.UploadAction"
        val DOWNLOAD_ACTION = "MonkeyFileService.DownloadAction"
        val RESPONSE_KEY = "MonkeyFileService.Response"
        val DOWNLOAD_OK = "MonkeyFileService.DownloadOk"
    }
}