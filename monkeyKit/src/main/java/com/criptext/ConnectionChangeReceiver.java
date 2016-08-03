package com.criptext;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;

/**
 * Created by danieltigse on 8/3/16.
 */

public class ConnectionChangeReceiver extends BroadcastReceiver {

    MonkeyKitSocketService service;

    public ConnectionChangeReceiver(MonkeyKitSocketService service){
        this.service = service;
    }

    @Override
    public void onReceive(Context context, Intent intent )
    {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if(cm!=null){
            try{
                if (cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isAvailable() && cm.getActiveNetworkInfo().isConnected()){
                    if(service!=null)
                        service.smartReconnect();
                }
                else if(cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)!= null && cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isAvailable() && cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnected()){
                    if(service!=null)
                        service.smartReconnect();
                }else{
                    Log.i("MonkeySDK","Waiting for network");
                    if(service!=null && service.getDelegate()!=null)
                        service.getDelegate().onSocketDisconnected();
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

}
