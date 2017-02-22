package com.criptext.lib

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*

/**
 * stores pending messages in memory and in disk.
 * Created by gesuwall on 6/15/16.
 */

class PendingMessageStore(messages: List<JsonObject>) {
    private val pendingMessages: LinkedList<JsonObject>
    /**
     * List of messages that have not been successfully delivered yet
     */
    fun add(ctx: Context, jsonMessage: JsonObject) {
        pendingMessages.add(jsonMessage)
        AsyncStoreTask(ctx, pendingMessages).execute()
    }

    init {
        pendingMessages = LinkedList(messages)
    }

    /**
     * get the id of a message
     */
    private fun getJsonMessageId(json: JsonObject) = json.get("args").asJsonObject.get("id").asString

    /**
     * Uses binary search to remove a message from the pending messages list.
     * @param id id of the message to remove
     * @param successAction action to execute if remove succeeds and clears the list
     */
    fun removePendingMessage(id: String, successAction: () -> Unit){
        val index = pendingMessages.indexOfFirst { n ->
            id.compareTo(getJsonMessageId(n)) == 0
        }

        if(index > -1) {
            pendingMessages.removeAt(index)
        }

        if (pendingMessages.isEmpty())
            successAction.invoke()
    }

    fun toList(): List<JsonObject> {
        return pendingMessages.toList()

    }

    fun forEach(action: (JsonObject) -> Unit) {
        pendingMessages.forEach { it -> action.invoke(it) }
    }

    companion object {
        private val filename = "pending.txt";
        private val separator = "\n{sp]\n";
        fun store(context: Context, messages: List<JsonObject>) {
            val file = context.openFileOutput(filename, Context.MODE_PRIVATE)
            var first = true
            for (msg in messages) {
                val line: String
                if (first) {
                    line = msg.toString()
                    first = false
                } else
                    line = separator + msg.toString()
                try {
                    file.write(line.toByteArray())
                } catch(ex: IOException) {
                    ex.printStackTrace()
                }
            }
            file.close()
        }

        fun retrieve(context: Context?): List<JsonObject> {
            if (context == null) {
                return listOf()
            } else {
                var file: FileInputStream? = null
                try {
                    file = context.openFileInput(filename)
                    val array: ByteArray = ByteArray(file.available())
                    file.read(array)
                    val jsonStr = String(array)
                    return MonkeyJson.parsePendingMsgsFromFile(jsonStr, separator)
                } catch(ex: FileNotFoundException) {
                    return listOf()
                } finally {
                    file?.close()
                }
            }
        }

        fun clean(context: Context){
            context.deleteFile(filename)
        }
    }

    class AsyncStoreTask(var ctx: Context?, var messages: List<JsonObject>): AsyncTask<Void, Void, Int>(){
        override fun doInBackground(vararg p0: Void?): Int {
            store(ctx!!, messages)
            ctx = null
            return 0
        }
    }

    class AsyncCleanTask(var ctx: Context?): AsyncTask<Void, Void, Int>(){
        override fun doInBackground(vararg p0: Void?): Int {
            clean(ctx!!)
            ctx = null
            return 0
        }
    }

}
