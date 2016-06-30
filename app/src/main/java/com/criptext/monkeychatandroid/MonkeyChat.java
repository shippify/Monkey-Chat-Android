package com.criptext.monkeychatandroid;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;

import io.realm.DynamicRealm;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmMigration;

/**
 * Created by Daniel Tigse on 4/19/16.
 */

public class MonkeyChat extends Application{


    public String MONKEY_REALM = "SampleApp.MonkeyKitRealm";
    private static MonkeyChat singleton;

    public static String MONKEY_ID = "MonkeyChat.MonkeyId";
    public static String IS_REGISTERED = "MonkeyChat.IsRegistered";
    public static String FULLNAME = "MonkeyChat.FullName";

    @Override
    public void onCreate() {
        super.onCreate();
        singleton = this;
    }

    public static MonkeyChat getInstance(){
        return singleton;
    }

    public Realm getNewMonkeyRealm(){
        RealmConfiguration libraryConfig = getMonkeyConfig();
        return Realm.getInstance(libraryConfig);
    }

    public RealmConfiguration getMonkeyConfig(){
        byte[] encryptKey= "Zba1P^06@#$S&9=wB@N6Ly!118&Ofg4*4;}32fF4d#59g#6Vai08D3S7B*3MJP64".getBytes();
        return new RealmConfiguration.Builder(this.getApplicationContext())
                .name(MONKEY_REALM)
                .encryptionKey(encryptKey)
                .schemaVersion(1)
                .migration(migration)
                .build();
    }

    RealmMigration migration = new RealmMigration() {
        @Override
        public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {
            //Example here https://realm.io/docs/java/latest/#migrations
        }
    };

}
