package com.criptext;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.criptext.comunication.AsyncConnSocket;
import com.criptext.comunication.CBTypes;

/**
 * Created by danieltigse on 8/3/16.
 */

public class ConnectionChangeReceiver extends BroadcastReceiver {

    MonkeyKitSocketService service;

    /** The absence of a connection type. */
    private static final int NO_CONNECTION_TYPE = -1;

    /** The last processed network type. */
    private static int sLastType = 1;

    public ConnectionChangeReceiver(MonkeyKitSocketService service){
        this.service = service;
        sLastType = getCurrentConnectivityType(service);
    }

    public int getCurrentConnectivityType(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null ? activeNetworkInfo.getType() : NO_CONNECTION_TYPE;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        final int currentType = getCurrentConnectivityType(context);
        // Avoid handling multiple broadcasts for the same connection type
        if (sLastType != currentType) {
            ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

            if (currentType != NO_CONNECTION_TYPE && activeNetworkInfo.isConnectedOrConnecting()
                    && service!=null) {
                service.startSocketConnection();
            } else {
                //Disconnected
                if(service!=null)
                    service.processMessageFromHandler(CBTypes.onSocketDisconnected, new Object[]{""});
            }
            sLastType = currentType;
        }

    }

}
