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

import com.criptext.ClientData;
import com.criptext.lib.MonkeyInit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

        JSONObject userInfo = new JSONObject();
        JSONArray ignore_params = new JSONArray();
        try {
            userInfo.put("name", editTextName.getText().toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        MonkeyInit mStart = new MonkeyInit(RegisterActivity.this, null,
                SensitiveData.APP_ID, SensitiveData.APP_KEY, userInfo, ignore_params){
            @Override
            public void onSessionOK(String sessionID){
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(MonkeyChat.IS_REGISTERED,true);
                editor.putString(MonkeyChat.MONKEY_ID,sessionID);
                editor.putString(MonkeyChat.FULLNAME, editTextName.getText().toString());
                editor.apply();
                Intent mainIntent =new Intent(RegisterActivity.this,MainActivity.class);
                ClientData data = new ClientData(editTextName.getText().toString(), SensitiveData.APP_ID, SensitiveData.APP_KEY,
                        sessionID);
                data.fillIntent(mainIntent);
                startActivity(mainIntent);
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
