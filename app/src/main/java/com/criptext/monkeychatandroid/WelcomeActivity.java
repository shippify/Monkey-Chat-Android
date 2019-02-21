package com.criptext.monkeychatandroid;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

public class WelcomeActivity extends AppCompatActivity {

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        if(prefs.getBoolean(MonkeyChat.IS_REGISTERED,false)){
            Intent mainIntent =new Intent(this,MainActivity.class);
            startActivity(mainIntent);
        }
        else{
            startActivity(new Intent(WelcomeActivity.this, RegisterActivity.class));
        }
        finish();
    }
}
