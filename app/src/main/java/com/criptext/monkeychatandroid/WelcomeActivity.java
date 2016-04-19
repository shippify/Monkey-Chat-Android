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
            Monkey.startMonkeyService(WelcomeActivity.this, prefs.getString("fullname",""),
                    prefs.getString("sessionid",""),Monkey.APP_ID, Monkey.APP_KEY);
            startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
            finish();
        }
        else{
            startActivity(new Intent(WelcomeActivity.this, RegisterActivity.class));
        }
    }
}
