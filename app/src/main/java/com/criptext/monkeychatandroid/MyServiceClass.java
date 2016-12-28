package com.criptext.monkeychatandroid;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.criptext.ClientData;
import com.criptext.MonkeyKitSocketService;
import com.criptext.comunication.MOKMessage;
import com.criptext.http.HttpSync;
import com.criptext.monkeychatandroid.models.DatabaseHandler;
import com.criptext.monkeychatandroid.models.SyncDatabaseTask;

import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Created by Daniel Tigse on 4/19/16.
 */

public class MyServiceClass extends MonkeyKitSocketService{
    private File _downloadDir;

    private File getDownloadDir() {
        if(_downloadDir == null)
            _downloadDir = MonkeyChat.getDownloadDir(this);
        return _downloadDir;
    }
    @Override
    public void storeReceivedMessage(final MOKMessage message, final Runnable runnable) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        DatabaseHandler.saveIncomingMessage(DatabaseHandler.createMessage(message, getDownloadDir().getAbsolutePath(),
                prefs.getString(MonkeyChat.MONKEY_ID, "")), runnable);

    }

    @NotNull
    @Override
    public ClientData loadClientData() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String fullname = prefs.getString(MonkeyChat.FULLNAME, null);
        String monkeyID = prefs.getString(MonkeyChat.MONKEY_ID, null);
        String sdomain = prefs.getString(MonkeyChat.SOCKET_DOMAIN, null);
        int sport = prefs.getInt(MonkeyChat.SOCKET_PORT, 1139);
        return new ClientData(fullname, SensitiveData.APP_ID, SensitiveData.APP_KEY, monkeyID, sdomain, sport);
    }

    @NotNull
    @Override
    public Class<?> getUploadServiceClass() {
        return MyFileUploadService.class;
    }

    @Override
    public void syncDatabase(@NotNull HttpSync.SyncData syncData, @NotNull Runnable runnable) {
        SyncDatabaseTask syncTask = new SyncDatabaseTask(syncData, runnable, clientData.getMonkeyId(),
                getDownloadDir().getAbsolutePath());
        syncTask.execute();

    }
}
