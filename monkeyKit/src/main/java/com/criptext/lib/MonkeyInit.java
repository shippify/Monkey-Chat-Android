package com.criptext.lib;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import com.criptext.MonkeyKitSocketService;
import com.criptext.http.MonkeyHttpClient;
import com.criptext.security.AESUtil;
import com.criptext.security.RSAUtil;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;

/**
 *
 * MonkeyInit handles all the HTTP requests needed to register with MonkeyKit. Constructor must receive
 * APP ID, APP KEY and the user's fullname and a context reference. If user already has a Monkey ID (due
 * to the fact that he/she has already registered in the past. Your app is responsible for figuring
 * this out), that id should also be passed to the constructor.
 * After calling start() 2 things can happen depending on whether the user already has a Monkey ID or
 * not:
 * If the user doesn't have a MonkeyID (This is the first time he/she registers with your app)
 * then a new Monkey ID is generated with /user/Session/ and then AES keys are generated and sent to
 * MonkeyKit server. After the server confirms that the keys were successfully delivered, the onSessionOK
 * callback is executed.
 * If they user did have a MonkeyID (It's a returning user, or he/she is registering in a new device)
 * then that MonkeyID is sent to the MonkeyKit server. The server's response contains the AES keys that
 * user had previously been using which is essential to decrypt older messages. after storing the AES
 * key the onSessionOK callback is executed.
 * In the onSessionOK callback your app should persist the user's MonkeyID. This finishes preparatios
 * for messaging.
 * Created by Gabriel on 2/10/16.
 */
public class MonkeyInit {

    private AsyncTask<Object, String, ServerResponse> async;
    private WeakReference<Context> ctxRef;
    private AESUtil aesUtil;
    private JSONObject userInfo;
    private JSONArray ignore_params;
    final String urlUser, urlPass, myOldMonkeyId;
    private SharedPreferences prefs;

    public MonkeyInit(Context context, String monkeyId, String user, String pass, JSONObject userInfo, JSONArray ignore_params){

        this.myOldMonkeyId = monkeyId == null ? null : monkeyId;
        this.urlUser = user;
        this.urlPass = pass;
        this.userInfo = userInfo;
        this.ignore_params = ignore_params;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        ctxRef = new WeakReference<>(context);
        async = new AsyncTask<Object, String, ServerResponse>(){

            @Override
            protected void onPostExecute(ServerResponse res){
                if(!res.isError())
                    onSessionOK(res.monkeyId, res.domain, res.port);
                else
                    onSessionError("Network Error");
            }

            @Override
            protected ServerResponse doInBackground(Object... params) {
                try {
                    //Generate keys
                    String session;

                        aesUtil = new AESUtil(ctxRef.get(), "temporal"); // deprecate

                        return initUser((String)params[0], (String)params[1],
                                (JSONObject)params[2], (JSONArray) params[3]);

                }catch(IllegalArgumentException ex){
                } catch(Exception ex){
                    ex.printStackTrace();
                }

                ServerResponse resp =new ServerResponse(null, null, -1);
                resp.error = true;
                return resp;
            }
        };
    }

    public void onSessionOK(String monkeyId, String domain, int port){
    //grab your new session id
    }

    public void onSessionError(String exceptionName){
    //grab your new session id
    }

    /**
     * Debes de llamar a este metodo para que de forma asincrona se registre el usuario con MonkeyKit
     */
    public void register(){
        async.execute(urlUser, urlPass, userInfo, ignore_params);
    }

    public void cancel(){
        if(async != null)
            async.cancel(true);
    }

    private ServerResponse initUser(String urlUser, String urlPass, JSONObject userInfo, JSONArray ignore_params) throws JSONException,
            UnsupportedEncodingException, ClientProtocolException, IOException{
        // Create a new HttpClient and Post Header
        HttpClient httpclient = MonkeyHttpClient.newClient();
        HttpPost httppost = MonkeyHttpClient.newPost(MonkeyKitSocketService.Companion.getHttpsURL() +
                "/user", urlUser, urlPass);

        JSONObject localJSONObject1 = new JSONObject();

        if(myOldMonkeyId!=null){
            localJSONObject1.put("monkey_id", myOldMonkeyId);
        }

        localJSONObject1.put("expiring","0");
        localJSONObject1.put("user_info",userInfo);

        JSONObject params = new JSONObject();
        params.put("data", localJSONObject1.toString());
        Log.d("getSessionHTTP", "Req: " + params.toString());

        JSONObject finalResult = MonkeyHttpClient.getResponse(httpclient, httppost, params.toString());
        Log.d("getUserSesssionHTTP", finalResult.toString());

        finalResult = finalResult.getJSONObject("data");

        prefs.edit().putString("sdomain", finalResult.getString("sdomain")).apply();
        prefs.edit().putString("sport", finalResult.getString("sport")).apply();
        final long lastSync = finalResult.isNull("last_time_synced") ? 0 : finalResult.getLong("last_time_synced");
        KeyStoreCriptext.setLastSync(ctxRef.get() , lastSync);

        String monkeyId = finalResult.getString("monkeyId");
        final String domain = finalResult.getString("sdomain");
        final int port = finalResult.getInt("sport");
        return new ServerResponse(monkeyId, domain, port);
    }

    public class ServerResponse {
        boolean error;
        final String monkeyId;
        final int port;
        final String domain;

        public ServerResponse(String monkeyId, String domain, int port){
            this.monkeyId = monkeyId;
            this.port = port;
            this.domain = domain;
        }

        public boolean isError(){ return error; }

    }
}
