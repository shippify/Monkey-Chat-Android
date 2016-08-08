package com.criptext.monkeychatandroid;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.criptext.ClientData;
import com.criptext.MonkeyKitSocketService;
import com.criptext.comunication.MOKMessage;
import com.criptext.monkeychatandroid.models.DatabaseHandler;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import io.realm.Realm;

/**
 * Created by Daniel Tigse on 4/19/16.
 */

public class MyServiceClass extends MonkeyKitSocketService{
    private Realm realm;

    @Override
    public void storeReceivedMessage(MOKMessage message, final Runnable runnable) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        openDatabase();
        DatabaseHandler.saveIncomingMessage(realm, DatabaseHandler.createMessage(message, this, prefs.getString("sessionid", ""), true),
            new Realm.Transaction.OnSuccess() {
                @Override
                public void onSuccess() {
                    runnable.run();
                }
            }, new Realm.Transaction.OnError() {
                @Override
                public void onError(Throwable error) {
                    if(error instanceof IllegalArgumentException){
                        Log.e("StoreReceivedMessage", error.getMessage());
                    }
                    error.printStackTrace();
                }
            });
    }

    @Override
    public void storeMessageBatch(ArrayList<MOKMessage> messages, final Runnable runnable) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        openDatabase();
        DatabaseHandler.saveMessageBatch(realm, messages, this, prefs.getString("sessionid", ""),
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

    @NotNull
    @Override
    public ClientData loadClientData() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String fullname = prefs.getString(MonkeyChat.FULLNAME, null);
        String monkeyID = prefs.getString(MonkeyChat.MONKEY_ID, null);
        return new ClientData(fullname, SensitiveData.APP_ID, SensitiveData.APP_KEY, monkeyID);
    }

    @Override
    public void closeDatabase() {
        if(realm != null)
            realm.close();
        realm = null;
    }

    @Override
    public void openDatabase() {
        if(realm == null){
            realm = MonkeyChat.getInstance().getNewMonkeyRealm();
        }

    }

    @NotNull
    @Override
    public Class<?> getUploadServiceClass() {
        return MyFileUploadService.class;
    }
}
