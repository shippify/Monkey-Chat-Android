package com.criptext.monkeychatandroid;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.criptext.MonkeyKitSocketService;
import com.criptext.comunication.MOKMessage;
import com.criptext.lib.MonkeyKit;
import com.criptext.monkeychatandroid.models.DatabaseHandler;

import java.util.ArrayList;

import io.realm.Realm;

/**
 * Created by Daniel Tigse on 4/19/16.
 */

public class MyServiceClass extends MonkeyKitSocketService{

    @Override
    public void storeMessage(MOKMessage message, boolean incoming, final Runnable runnable) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        DatabaseHandler.saveMessage(DatabaseHandler.createMessage(message, getContext(), prefs.getString("sessionid", ""), incoming),
            new Realm.Transaction.OnSuccess() {
                @Override
                public void onSuccess() {
                    runnable.run();
                }
            }, new Realm.Transaction.OnError() {
                @Override
                public void onError(Throwable error) {
                    error.printStackTrace();
                }
        });

    }

    @Override
    public void storeMessageBatch(ArrayList<MOKMessage> messages, final Runnable runnable) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        DatabaseHandler.saveMessageBatch(messages, getContext(), prefs.getString("sessionid", ""),
                new Realm.Transaction.OnSuccess() {
                    @Override
                    public void onSuccess() {
                        runnable.run();
                    }
                }, new Realm.Transaction.OnError() {
                    @Override
                    public void onError(Throwable error) {
                        error.printStackTrace();
                    }
        });

    }
}
