package com.criptext.monkeychatandroid;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.criptext.ClientData;
import com.criptext.lib.MonkeyInit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RegisterActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private EditText editTextName, editTextId;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        final Button submitBtn = (Button)findViewById(R.id.submitBtn);

        editTextName = (EditText)findViewById(R.id.editTextFullname);
        editTextId = (EditText)findViewById(R.id.editTextMonkeyId);
        editTextId.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_DONE)
                    submitBtn.performClick();
                return false;
            }
        });


        progressBar = (ProgressBar) findViewById(R.id.progressBarCargando);
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    }

    public void onContinue(final View v){
        v.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        //userInfo stores any additional data about the user that we want to send to MonkeyKit.
        //In this sample app we are only interested in the user's name
        JSONObject userInfo = new JSONObject();
        JSONArray ignore_params = new JSONArray();
        try {
            userInfo.put("name", editTextName.getText().toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String existingMonkeyId = editTextId.getText().toString();
        if(existingMonkeyId.length() == 0)
            existingMonkeyId = null;

        MonkeyInit mStart = new MonkeyInit(RegisterActivity.this, existingMonkeyId,
                SensitiveData.APP_ID, SensitiveData.APP_KEY, userInfo, ignore_params){
            @Override
            public void onSessionOK(String sessionID, String sdomain, int sport){
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(MonkeyChat.IS_REGISTERED,true);
                editor.putString(MonkeyChat.MONKEY_ID,sessionID);
                editor.putString(MonkeyChat.FULLNAME, editTextName.getText().toString());
                editor.putString(MonkeyChat.FULLNAME, editTextName.getText().toString());
                editor.putString(MonkeyChat.SOCKET_DOMAIN, sdomain);
                editor.putInt(MonkeyChat.SOCKET_PORT, sport);
                editor.apply();
                Intent mainIntent =new Intent(RegisterActivity.this,MainActivity.class);
                ClientData data = new ClientData(editTextName.getText().toString(), SensitiveData.APP_ID, SensitiveData.APP_KEY,
                        sessionID, sdomain, sport);
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
