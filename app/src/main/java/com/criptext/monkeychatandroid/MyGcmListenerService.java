package com.criptext.monkeychatandroid;

import com.criptext.gcm.MonkeyGcmListenerService;

import org.jetbrains.annotations.NotNull;

/**
 * Created by gesuwall on 6/24/16.
 */
public class MyGcmListenerService extends MonkeyGcmListenerService {
    @Override
    public void createNotification() {

    }

    @NotNull
    @Override
    public Class<?> getSocketServiceClass() {
        return MyServiceClass.class;
    }
}
