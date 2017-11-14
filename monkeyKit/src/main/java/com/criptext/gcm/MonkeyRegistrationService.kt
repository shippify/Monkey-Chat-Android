package com.criptext.gcm

import android.app.Activity
import android.app.IntentService
import android.content.Intent
import android.preference.PreferenceManager
import android.util.Log
import com.criptext.ClientData
import com.criptext.http.MonkeyHttpClient
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.gcm.GoogleCloudMessaging
import com.google.android.gms.iid.InstanceID

/**
 * This class registers app with GCM on a worker thread. After receiving a new token it sends the
 * token to the MonkeyKit Server.
 * Created by Gabriel on 5/30/16.
 */

abstract class MonkeyRegistrationService : IntentService(TAG){

    override fun onHandleIntent(intent: Intent?) {

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if(sharedPreferences.getBoolean(SENT_TOKEN_TO_SERVER, false))
            return //If there is already a valid token in server, then there's need register again

        try {
            // [START register_for_gcm]
            // Initially this call goes out to the network to retrieve the token, subsequent calls
            // are local.
            // See https://developers.google.com/cloud-messaging/android/start for details on this file.
            // [START get_token]
            val monkeyId = intent!!.getStringExtra(ClientData.MONKEY_ID_KEY) ?:
                        ClientData.throwMissingMonkeyIdException() as String
            val appId = intent.getStringExtra(ClientData.APP_ID_KEY) ?:
                    ClientData.throwMissingAppIdException() as String
            val appKey = intent.getStringExtra(ClientData.APP_KEY_KEY) ?:
                    ClientData.throwMissingAppKeyException() as String

            val instanceID = InstanceID.getInstance(this);
            val token = instanceID.getToken(getGcm_defaultSenderId(),
            GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            // [END get_token]
            Log.i(TAG, "GCM Registration Token: " + token);

            MonkeyHttpClient.subscribePushHttp(token, monkeyId, appId, appKey, true)

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

    /**
     * returns the "project-id" value of the google-services.json that you get from the google
     * developers website. This method is needed because MonkeyKit doesn't use the google services
     * gradle plugin
     */
    abstract fun getGcm_defaultSenderId(): String

    companion object {
        val SENT_TOKEN_TO_SERVER = "MonkeyRegistrationService.SentTokenToServer"
        val TAG = "MonkeyRegService"
        val PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

        /**
         * Check the device to make sure it has the Google Play Services APK. If
         * it doesn't, display a dialog that allows users to download the APK from
         * the Google Play Store or enable it in the device's system settings.
         */
        fun checkPlayServices(activity: Activity) : Boolean{
            val apiAvailability = GoogleApiAvailability.getInstance();
            val resultCode = apiAvailability.isGooglePlayServicesAvailable(activity);
            if (resultCode != ConnectionResult.SUCCESS) {
                if (apiAvailability.isUserResolvableError(resultCode)) {
                    apiAvailability.getErrorDialog(activity, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                            .show();
                } else {
                    Log.i(TAG, "This device is not supported.");
                }
                return false;
            }
            return true;
        }
    }

}
