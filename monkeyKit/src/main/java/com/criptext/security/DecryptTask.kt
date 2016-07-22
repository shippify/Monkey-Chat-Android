package com.criptext.security

import android.os.AsyncTask
import android.util.Base64
import android.util.Log
import com.criptext.MonkeyKitSocketService
import com.criptext.comunication.CBTypes
import com.criptext.comunication.MOKMessage
import com.criptext.comunication.MessageTypes
import java.lang.ref.WeakReference

/**
 * Created by gesuwall on 6/6/16.
 */

class DecryptTask(service: MonkeyKitSocketService): AsyncTask<EncryptedMsg, MOKMessage, Int>(){
    val serviceRef: WeakReference<MonkeyKitSocketService>
    init {
        serviceRef = WeakReference(service)
    }
    override fun doInBackground(vararg p0: EncryptedMsg?): Int? {
        for(encrypted in p0){
            if(decryptMessage(encrypted!!))
                publishProgress(encrypted.message)
        }

        return 0;
    }

    override fun onProgressUpdate(vararg values: MOKMessage?) {
        val service = serviceRef.get()
        service?.processMessageFromHandler(CBTypes.onMessageReceived, Array(0, { i -> values[i] as Any}));
    }

    companion object {
        fun decryptMessage(encrypted: EncryptedMsg): Boolean{
            try {
                val message = encrypted.message
                if (message.props.get("encr").asString.compareTo("1") == 0 && message.type != MessageTypes.MOKFile) {
                    message.msg = AESUtil.decryptWithCustomKeyAndIV(message.msg,
                            encrypted.key, encrypted.iv);
                    message.encr = "0" //This "encr" property doesn't seem to be of much use
                }
                else if (message.props.has("encoding") && message.type != MessageTypes.MOKFile) {
                    if (message.props.get("encoding").asString == "base64")
                        message.msg = String(Base64.decode(message.msg.toByteArray(), Base64.NO_WRAP))
                }
                return true
            } catch (ex: Exception){
                ex.printStackTrace()
                return false
            }
        }
    }

}
