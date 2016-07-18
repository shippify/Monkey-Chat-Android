package com.criptext.gcm

import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.criptext.MonkeyKitSocketService
import com.google.android.gms.gcm.GcmListenerService
import java.util.*

/**
 * Service that listens for push messages from GCM. When a new message is received while the app is
 * in background it opens a socket connection to get all pending messages and shows a notification
 * to the user.
 * Created by Gabriel on 5/30/16.
 */

abstract class MonkeyGcmListenerService: GcmListenerService() {

    override fun onMessageReceived(from: String?, data: Bundle?) {
        super.onMessageReceived(from, data)
        //Log.d("GCMListener", data!!.toString() + " should notify ${MonkeyKitSocketService.status}");
        if(MonkeyKitSocketService.status < MonkeyKitSocketService.ServiceStatus.bound){
            val args = getNotificationArgs(data!!.getString("loc-args"));
            val key = data.getString("loc-key");
            val message = data.getString("message");
            if (key != null) {
                createLocalizedNotification(key, args!!)
            } else if (message != null) {
                createSimpleNotification(message)
            } else
                Log.e("MonkeyGcmListenerServic", "could not create notification, 'message' and 'loc-key' are null")
        }

        if(MonkeyKitSocketService.status == MonkeyKitSocketService.ServiceStatus.dead){
            val intent = Intent(this, socketServiceClass)
            startService(intent)
        }
    }

    /**
     * Convierte el string loc-args en un array con los argumentos para una notificacion
     * @param loc_args argumentos loc-args separados por comas en un string
     * @return un array con todos los argumentos.
     */
    private fun getNotificationArgs(loc_args: String?): Array<String>?{
        if(loc_args != null){
            val newArgs = StringTokenizer(loc_args, ",");
            val size = newArgs.countTokens();
            return newArgs.toList().toTypedArray() as Array<String>;
        }
        return null

    }

    abstract val socketServiceClass : Class<*>

    abstract fun createSimpleNotification(message: String)

    abstract fun createLocalizedNotification(key: String, args: Array<String>)



}
