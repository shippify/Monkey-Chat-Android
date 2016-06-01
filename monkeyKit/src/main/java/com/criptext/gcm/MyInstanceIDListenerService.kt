package com.criptext.gcm

import android.content.Intent
import android.preference.PreferenceManager
import com.google.android.gms.iid.InstanceIDListenerService

/**
 * Created by gesuwall on 5/31/16.
 */

class MyInstanceIDListenerService : InstanceIDListenerService() {

    override fun onTokenRefresh() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.edit().putBoolean(MonkeyRegistrationService.SENT_TOKEN_TO_SERVER, false).apply();
    }

}
