package com.criptext.lib

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException

/**
 * stores pending messages in memory and in disk.
 * Created by gesuwall on 6/15/16.
 */

class PendingMessageStore {

    companion object {
        private val filename = "pending.txt";
        private val separator = "\nsp\n";
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
                    val jsonArray = jsonStr.split(separator)
                    val parser = JsonParser()
                    return jsonArray.map { it ->
                        parser.parse(it).asJsonObject
                    }
                } catch(ex: FileNotFoundException) {
                    return listOf()
                } finally {
                    file?.close()
                }
            }
        }
    }

    class AsyncStoreTask(var ctx: Context?, var messages: List<JsonObject>): AsyncTask<Void, Void, Int>(){
        override fun doInBackground(vararg p0: Void?): Int {
            store(ctx!!, messages)
            ctx = null
            return 0
        }

    }
}
