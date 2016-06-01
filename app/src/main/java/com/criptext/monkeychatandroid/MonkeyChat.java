package com.criptext.monkeychatandroid;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;

import com.criptext.lib.MonkeyKit;

import io.realm.DynamicRealm;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmMigration;
import io.realm.RealmSchema;

/**
 * Created by Daniel Tigse on 4/19/16.
 */

public class MonkeyChat extends Application{


    public String MONKEY_REALM = "SampleApp.MonkeyKitRealm";
    private Realm monkeyRealm;
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

    public Realm getMonkeyKitRealm(){
        if(monkeyRealm == null)
            monkeyRealm = getNewMonkeyRealm();

        return monkeyRealm;
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

    public static String milliSecondsToTimer(long milliseconds){

        String finalTimerString = "";
        String secondsString = "";

        // Convert total duration into time
        int hours = (int)( milliseconds / (1000*60*60));
        int minutes = (int)(milliseconds % (1000*60*60)) / (1000*60);
        int seconds = (int) ((milliseconds % (1000*60*60)) % (1000*60) / 1000);
        // Add hours if there
        if(hours > 0){
            finalTimerString = hours + ":";
        }
        // Prepending 0 to seconds if it is one digit
        if(seconds < 10){
            secondsString = "0" + seconds;
        }else{
            secondsString = "" + seconds;}

        finalTimerString = finalTimerString + minutes + ":" + secondsString;
        // return timer string
        return finalTimerString;
    }

}
