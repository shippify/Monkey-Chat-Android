package com.criptext.gcm

import android.os.Bundle
import android.util.Log
import com.google.android.gms.gcm.GcmListenerService

/**
 * Service that listens for push messages from GCM. When a new message is received while the app is
 * in background it opens a socket connection to get all pending messages and shows a notification
 * to the user.
 * Created by Gabriel on 5/30/16.
 */

class MonkeyGcmListenerService: GcmListenerService() {

    override fun onMessageReceived(from: String?, data: Bundle?) {
        super.onMessageReceived(from, data)
        Log.d("GCMListener", from);

    }

}
