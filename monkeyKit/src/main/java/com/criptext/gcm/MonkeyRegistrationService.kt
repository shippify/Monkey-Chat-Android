package com.criptext.gcm

import android.app.IntentService
import android.content.Intent
import android.preference.PreferenceManager
import android.util.Log
import com.criptext.ClientData
import com.criptext.MonkeyKitSocketService
import com.criptext.lib.MonkeyKit
import com.criptext.lib.R
import com.google.android.gms.gcm.GoogleCloudMessaging
import com.google.android.gms.iid.InstanceID

/**
 * Created by gesuwall on 5/30/16.
 */

abstract class MonkeyRegistrationService : IntentService(TAG){

    override fun onHandleIntent(intent: Intent?) {

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        try {
            // [START register_for_gcm]
            // Initially this call goes out to the network to retrieve the token, subsequent calls
            // are local.
            // R.string.gcm_defaultSenderId (the Sender ID) is typically derived from google-services.json.
            // See https://developers.google.com/cloud-messaging/android/start for details on this file.
            // [START get_token]
            val monkeyId = intent!!.getStringExtra(ClientData.MONKEY_ID_KEY)
            val appId = intent.getStringExtra(ClientData.APP_ID_KEY)
            val appKey = intent.getStringExtra(ClientData.APP_KEY_KEY)

            val instanceID = InstanceID.getInstance(this);
            val token = instanceID.getToken(getGcm_defaultSenderId(),
            GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            // [END get_token]
            Log.i(TAG, "GCM Registration Token: " + token);

            MonkeyKit.subscribePushHttp(token, monkeyId, appId, appKey)

            // You should store a boolean that indicates whether the generated token has been
            // sent to your server. If the boolean is false, send the token to your server,
            // otherwise your server should have already received the token.
            sharedPreferences.edit().putBoolean(SENT_TOKEN_TO_SERVER, true).apply();
            // [END register_for_gcm]
        } catch (e: Exception) {
            Log.d(TAG, "Failed to complete token refresh" , e);
            // If an exception happens while fetching the new token or updating our registration data
            // on a third-party server, this ensures that we'll attempt the update at a later time.
            sharedPreferences.edit().putBoolean(SENT_TOKEN_TO_SERVER, false).apply();
        }
    }

    abstract fun getGcm_defaultSenderId(): String

    companion object {
        val SENT_TOKEN_TO_SERVER = "MonkeyRegistrationService.SentTokenToServer"
        val TAG = "MonkeyRegService"
    }

}
