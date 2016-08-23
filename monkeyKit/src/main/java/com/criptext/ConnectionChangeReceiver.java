package com.criptext;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.criptext.comunication.AsyncConnSocket;

/**
 * Created by danieltigse on 8/3/16.
 */

public class ConnectionChangeReceiver extends BroadcastReceiver {

    MonkeyKitSocketService service;

    public ConnectionChangeReceiver(MonkeyKitSocketService service){
        this.service = service;
    }

    /** The absence of a connection type. */
    private static final int NO_CONNECTION_TYPE = -1;

    /** The last processed network type. */
    private static int sLastType = 1;

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        final int currentType = activeNetworkInfo != null
                ? activeNetworkInfo.getType() : NO_CONNECTION_TYPE;

        // Avoid handling multiple broadcasts for the same connection type
        if (sLastType != currentType && service!=null
                && MonkeyKitSocketService.Companion.getStatus().ordinal() >
                MonkeyKitSocketService.ServiceStatus.running.ordinal()) {
            if (activeNetworkInfo != null) {
                if (activeNetworkInfo.isConnectedOrConnecting() && service!=null) {
                    service.startSocketConnection();
                }
            } else {
                //Disconnected
                if(service!=null && service.getDelegate()!=null)
                    service.getDelegate().onSocketDisconnected();
            }

            sLastType = currentType;
        }

    }

}
