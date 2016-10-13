package com.criptext.monkeychatandroid;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.criptext.ClientData;
import com.criptext.MonkeyKitSocketService;
import com.criptext.comunication.MOKMessage;
import com.criptext.monkeychatandroid.models.DatabaseHandler;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Daniel Tigse on 4/19/16.
 */

public class MyServiceClass extends MonkeyKitSocketService{

    @Override
    public void storeReceivedMessage(final MOKMessage message, final Runnable runnable) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        DatabaseHandler.saveIncomingMessage(DatabaseHandler.createMessage(message, this,
                prefs.getString(MonkeyChat.MONKEY_ID, ""), true), runnable);

    }

    @Override
    public void storeMessageBatch(HashMap<String, List<MOKMessage>> messages, final Runnable runnable) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        DatabaseHandler.saveMessageBatch(messages, this, prefs.getString(MonkeyChat.MONKEY_ID, ""), runnable);
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
    }

    @Override
    public void openDatabase() {
    }

    @NotNull
    @Override
    public Class<?> getUploadServiceClass() {
        return MyFileUploadService.class;
    }
}
