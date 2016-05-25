package com.criptext.lib;

import android.os.Handler;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by gesuwall on 10/5/15.
 */
public class Watchdog {
    private static int TIMEOUT = 8000;
    private final Handler handler;
    private Runnable runnable;
    private boolean working;
    public boolean didResponseGet = true;

    public Watchdog() {
        this.handler = new Handler();
        working = false;

    }

    private JSONArray getPendingMessages(){
        JSONArray array = new JSONArray();
        String[] strings = MonkeyKit.instance().getPendingMessages();
        for(String str : strings){
            try {
                array.put(new JSONObject(str));
            } catch (JSONException ex){
                ex.printStackTrace();
            }
        }
        return array;
    }

    public void start(){

        if(runnable!=null) {
            Log.i("Watchdog", "Watchdog ya ha sido llamado primero lo cancelo");
            handler.removeCallbacks(runnable);
        }
        else
            Log.i("Watchdog", "Watchdog start");

        runnable = new Runnable() {
            @Override
            public void run() {
                final JSONArray array = getPendingMessages();
                Log.i("Watchdog", "There are " + array.length() + " messages to send and didResponseGet " + didResponseGet);
                if (array.length() > 0 || !didResponseGet) {
                    MonkeyKit.instance().sendDisconectOnPull();
                    new Handler().postDelayed(new Runnable() {
                                          @Override
                                          public void run() {
                            MonkeyKit.instance().reconnectSocket(new Runnable() {
                                                               @Override
                                                               public void run() {
                                    final int len = array.length();
                                    for (int i = 0; i < len; i++) {
                                        try {
                                            MonkeyKit.instance().sendJSONviaSocket((JSONObject) array.get(i));
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                            }
                        });
                    }
                }, 2000);
                }
                working = false;
            }
        };

        handler.postDelayed(runnable,TIMEOUT);
        working = true;
    }

    public boolean isWorking(){
       return working;
    }


}
