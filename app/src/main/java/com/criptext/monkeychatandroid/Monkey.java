package com.criptext.monkeychatandroid;

import android.app.ActivityManager;
import android.content.Context;

import com.criptext.lib.MonkeyKit;

/**
 * Created by Daniel Tigse on 4/19/16.
 */

public class Monkey {

    public static String APP_ID = "idkgwf6ghcmyfvvrxqiwwmi";
    public static String APP_KEY = "9da5bbc32210ed6501de82927056b8d2";

    public static void startMonkeyService(Context c, String name, String sessionid, String CRIPTEXT_APP_ID, String CRIPTEXT_APP_KEY){
        if(!isMyServiceRunning(MyServiceClass.class, c)){
            MonkeyKit.startMonkeyService(c, MyServiceClass.class,
                    name,
                    sessionid,
                    CRIPTEXT_APP_ID, CRIPTEXT_APP_KEY);
        }
    }

    public static boolean isMyServiceRunning(Class<?> serviceClass, Context context) {
        ActivityManager manager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

}
