package com.criptext.monkeychatandroid;

import android.app.Application;

import io.realm.DynamicRealm;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmMigration;

/**
 * Application class. This class is only used to store a few global constants. It also holds the
 * configuration for the default realm.
 * Created by Daniel Tigse on 4/19/16.
 */

public class MonkeyChat extends Application{


    public String MONKEY_REALM = "SampleApp.MonkeyKitRealm";
    private static MonkeyChat singleton;

    /**
     * Constant used to store in Shared Preferences the current user's monkey ID. You need this
     * to start the MonkeyKitSocketService.
     */
    public static String MONKEY_ID = "MonkeyChat.MonkeyId";
    /**
     * Constant used to store in Shared Preferences a boolean that indicates whether the current
     * user has registered with GCM and has successfully subscribed this device to the MonkeyKit Server
     * in order to receive push notifications.
     */
    public static String IS_REGISTERED = "MonkeyChat.IsRegistered";
    /**
     * Constant used to store in Shared Preferences the name of the current user. this is also needed
     * to start the MonkeyKitSocketService.
     */
    public static String FULLNAME = "MonkeyChat.FullName";

    @Override
    public void onCreate() {
        super.onCreate();
        singleton = this;
    }

    public static MonkeyChat getInstance(){
        return singleton;
    }

    /**
     * Opens a new Realm instance with the configuration especified in getMonkeyConfig();
     * @return A new realm instance. You must close this when you are done with it.
     */
    public Realm getNewMonkeyRealm(){
        RealmConfiguration libraryConfig = getMonkeyConfig();
        return Realm.getInstance(libraryConfig);
    }

    /**
     * Create a realm configuration with an encrypted database. the key is stored in the SensitiveData
     * class. It should be hidden as well as your MonkeyKit API key to avoid security risks.
     * @return RealmConfiguration File;
     */
    public RealmConfiguration getMonkeyConfig(){
        byte[] encryptKey= SensitiveData.REALM_KEY.getBytes();
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
