package com.criptext.monkeychatandroid;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class WelcomeActivity extends AppCompatActivity {

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        if(prefs.getBoolean("isRegistered",false)){
            MonkeyChat.startMonkeyService(WelcomeActivity.this, prefs.getString("fullname",""),
                    prefs.getString("sessionid",""), MonkeyChat.APP_ID, MonkeyChat.APP_KEY);
            startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
        }
        else{
            startActivity(new Intent(WelcomeActivity.this, RegisterActivity.class));
        }
        finish();
    }
}
