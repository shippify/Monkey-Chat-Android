package com.criptext.comunication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.criptext.MonkeyFileService
import com.criptext.MonkeyKitSocketService
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.lang.ref.WeakReference

/**
 * Receives the broadcast messages from the FileService that runs in a background thread. useful
 * to post updates about file uplaad/download to the main thread.
 * Created by Gabriel on 8/3/16.
 */

class FileBroadcastReceiver(service: MonkeyKitSocketService) : BroadcastReceiver(){
    val serviceRef: WeakReference<MonkeyKitSocketService> = WeakReference(service)

    override fun onReceive(p0: Context?, p1: Intent?) {
        val intent = p1!!
        val response = intent.getStringExtra(MonkeyFileService.RESPONSE_KEY)
         when(intent.action){
             MonkeyFileService.UPLOAD_ACTION -> {
                 val mokMessage = MOKMessage(intent)
                 if (response != null) {
                     val parser = JsonParser()
                     val jsonResp = parser.parse(response).asJsonObject
                     handleSentFile(jsonResp, mokMessage)
                 } else
                     serviceRef.get()?.delegateHandler?.processMessageFromHandler(CBTypes.onFileFailsUpload,
                             arrayOf(mokMessage))
             }
             MonkeyFileService.DOWNLOAD_ACTION -> {
                 val msgId = intent.getStringExtra(MOKMessage.ID_KEY)
                 val timeorder = intent.getLongExtra(MOKMessage.DATESORT_KEY, 0L)
                 val convId = intent.getStringExtra(MOKMessage.CONVERSATION_KEY)
                 serviceRef.get()?.delegateHandler?.processMessageFromHandler(CBTypes.onFileDownloadFinished,
                         arrayOf(msgId, timeorder, convId, response != null))

             }
         }
    }

    fun handleSentFile(json: JsonObject, newMessage: MOKMessage){
            try {
                val response = json.get("data").asJsonObject;
                val props = JsonObject();
                props.addProperty("status", MessageTypes.Status.delivered);
                props.addProperty("old_id", newMessage.message_id);
                serviceRef.get()?.delegateHandler?.processMessageFromHandler(CBTypes.onAcknowledgeReceived
                        , arrayOf(newMessage.rid, newMessage.sid, response.get("messageId").asString
                                    ,newMessage.message_id, false, 2));

            } catch (e: Exception) {
                e.printStackTrace();
            }
        }
    }