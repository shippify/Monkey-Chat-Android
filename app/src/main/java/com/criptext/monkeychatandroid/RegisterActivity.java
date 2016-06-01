package com.criptext.monkeychatandroid;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.criptext.lib.MonkeyInit;

public class RegisterActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private EditText editTextName;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        editTextName = (EditText)findViewById(R.id.editTextFullname);
        progressBar = (ProgressBar) findViewById(R.id.progressBarCargando);
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    }

    public void onContinue(final View v){
        v.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        MonkeyInit mStart = new MonkeyInit(RegisterActivity.this, null,
                SensitiveData.APP_ID, SensitiveData.APP_KEY,
                editTextName.getText().toString()){
            @Override
            public void onSessionOK(String sessionID){
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(MonkeyChat.IS_REGISTERED,true);
                editor.putString(MonkeyChat.MONKEY_ID,sessionID);
                editor.putString(MonkeyChat.FULLNAME,editTextName.getText().toString());
                editor.commit();
                startActivity(new Intent(RegisterActivity.this,MainActivity.class));
                finish();
            }

            @Override
            public void onSessionError(String exceptionName) {

                v.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);

                final AlertDialog.Builder alert = new AlertDialog.Builder(RegisterActivity.this);
                alert.setMessage(exceptionName);
                alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                alert.show();
            }
        };
        mStart.register();
    }
}
