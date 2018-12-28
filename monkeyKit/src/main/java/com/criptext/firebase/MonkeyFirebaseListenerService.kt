package com.criptext.firebase

import android.app.Activity
import android.content.Intent
import android.preference.PreferenceManager
import android.util.Log
import com.criptext.ClientData
import com.criptext.MonkeyKitSocketService
import com.criptext.http.MonkeyHttpClient
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


open abstract class MonkeyFirebaseListenerService: FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val from = message!!.from
        val data = message.data

        if(MonkeyKitSocketService.status < MonkeyKitSocketService.ServiceStatus.running){
            val args = getNotificationArgs(data.get("loc-args"));
            val key = data.get("loc-key");
            val message = data.get("message");
            if (key != null && message != null) {
                createLocalizedNotification(key, args!!)
            } else {
                Log.e("MonkeyFirebaseListener", "could not create notification, 'message' and 'loc-key' are null")
            }
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
    protected fun getNotificationArgs(loc_args: String?): Array<String>{
        if(loc_args != null)
            return FirebaseNotificationParser().localizedArgumentsToArray(loc_args)

        return arrayOf()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        //If there is already a valid token in server, then there's need register again

        try {
            val monkeyId = sharedPreferences.getString(ClientData.MONKEY_ID_KEY, "")
            val appId = sharedPreferences.getString(ClientData.APP_ID_KEY, "")
            val appKey = sharedPreferences.getString(ClientData.APP_KEY_KEY, "")

            if( monkeyId == "" || appId == "" || appKey == "" ){
                System.out.print("REGISTRATION TOKEN WRONG!! MONKEID oR APPID OR APPKEY WRONG")
                return
            }

            // [END get_token]
            Log.i(TAG, "FIREBASE Registration Token: " + token);

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

    abstract val socketServiceClass : Class<*>

    abstract fun createSimpleNotification(message: String)

    abstract fun createLocalizedNotification(key: String, args: Array<String>)

}
