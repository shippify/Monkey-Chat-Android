package com.criptext.gcm

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.preference.PreferenceManager

/**
 * Created by gesuwall on 5/31/16.
 */

class MyInstanceIDListenerService : Service() {
    override fun onBind(p0: Intent?): IBinder? {
        throw UnsupportedOperationException()
    }

    fun onTokenRefresh() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.edit().putBoolean(MonkeyRegistrationService.SENT_TOKEN_TO_SERVER, false).apply();
    }

}
