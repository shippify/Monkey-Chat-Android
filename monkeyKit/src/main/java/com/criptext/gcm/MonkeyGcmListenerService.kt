package com.criptext.gcm

import android.os.Bundle
import android.util.Log
import com.google.android.gms.gcm.GcmListenerService

/**
 * Created by gesuwall on 5/30/16.
 */

class MonkeyGcmListenerService: GcmListenerService() {

    override fun onMessageReceived(from: String?, data: Bundle?) {
        super.onMessageReceived(from, data)
        Log.d("GCMListener", from);

    }

}
