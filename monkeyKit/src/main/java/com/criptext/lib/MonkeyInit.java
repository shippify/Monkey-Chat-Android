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

        this.myOldMonkeyId = monkeyId == null ? "" : monkeyId;
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
                    if(myOldMonkeyId.isEmpty()) {
                        aesUtil = new AESUtil(ctxRef.get(), myOldMonkeyId);
                        return getSessionHTTP((String)params[0], (String)params[1],
                                (JSONObject)params[2], (JSONArray) params[3]);
                    } else {
                        return userSync(myOldMonkeyId);
                    }
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
        System.out.println("registrateMonkey: "+urlUser+" - "+urlPass+" - "+userInfo+" - "+ignore_params);
        async.execute(urlUser, urlPass, userInfo, ignore_params);
    }

    public void cancel(){
        if(async != null)
            async.cancel(true);
    }

    private String storeKeysIV(String monkeyId, String pubKey){
        //Encrypt workers
        RSAUtil rsa = new RSAUtil(Base64.decode(pubKey.getBytes(),0));
        String usk=rsa.encrypt(aesUtil.strKey+":"+aesUtil.strIV);
        //Guardo mis key & Iv
        KeyStoreCriptext.putString(ctxRef.get(), monkeyId, aesUtil.strKey+":"+aesUtil.strIV);
        return usk;

    }

    private ServerResponse getSessionHTTP(String urlUser, String urlPass, JSONObject userInfo, JSONArray ignore_params) throws JSONException,
            UnsupportedEncodingException, ClientProtocolException, IOException{
        // Create a new HttpClient and Post Header
        HttpClient httpclient = MonkeyHttpClient.newClient();
        HttpPost httppost = MonkeyHttpClient.newPost(MonkeyKitSocketService.Companion.getHttpsURL() +
                "/user/session", urlUser, urlPass);

        JSONObject localJSONObject1 = new JSONObject();

        localJSONObject1.put("username",urlUser);
        localJSONObject1.put("password",urlPass);
        localJSONObject1.put("monkey_id", myOldMonkeyId);
        localJSONObject1.put("expiring","0");
        localJSONObject1.put("user_info",userInfo);

        JSONObject params = new JSONObject();
        params.put("data", localJSONObject1.toString());
        Log.d("getSessionHTTP", "Req: " + params.toString());

        JSONObject finalResult = MonkeyHttpClient.getResponse(httpclient, httppost, params.toString());

        Log.d("getSesssionHTTP", finalResult.toString());
        finalResult = finalResult.getJSONObject("data");

        String monkeyId = finalResult.getString("monkeyId");
        String pubKey = finalResult.getString("publicKey");
        pubKey = pubKey.replace("-----BEGIN PUBLIC KEY-----\n", "").replace("\n-----END PUBLIC KEY-----", "");

        String encriptedKeys = storeKeysIV(monkeyId, pubKey);

        //retornar el session solo despues del connect exitoso
        return connectHTTP(finalResult.getString("monkeyId"), encriptedKeys, ignore_params);

    }

    private ServerResponse userSync(final String monkeyId) throws Exception{

        // Create a new HttpClient and Post Header
        RSAUtil rsaUtil = new RSAUtil();
        rsaUtil.generateKeys();

        HttpClient httpclient = MonkeyHttpClient.newClient();
        HttpPost httppost = MonkeyHttpClient.newPost(MonkeyKitSocketService.Companion.getHttpsURL() +
                "/user/key/sync", urlUser, urlPass);

        JSONObject localJSONObject1 = new JSONObject();

        localJSONObject1.put("monkey_id", monkeyId);
        localJSONObject1.put("public_key", "-----BEGIN PUBLIC KEY-----\n" + rsaUtil.getPublicKey() + "\n-----END PUBLIC KEY-----");
        //System.out.println("-----BEGIN PUBLIC KEY-----\n" + rsaUtil.pubKeyStr + "\n-----END PUBLIC KEY-----");
        JSONObject params = new JSONObject();
        params.put("data", localJSONObject1.toString());
        Log.d("userSyncMS", "Req: " + params.toString());

        JSONObject finalResult = MonkeyHttpClient.getResponse(httpclient, httppost, params.toString());
         Log.d("userSyncMS", finalResult.toString());
        finalResult = finalResult.getJSONObject("data");

        final String keys = finalResult.getString("keys");
        final long lastSync = finalResult.getLong("last_time_synced");
        String decriptedKey = rsaUtil.desencrypt(keys);
        KeyStoreCriptext.putString(ctxRef.get() ,monkeyId, decriptedKey);
        KeyStoreCriptext.setLastSync(ctxRef.get() , lastSync);

        try {
            aesUtil = new AESUtil(ctxRef.get(), monkeyId);
        } catch (Exception ex){
            ex.printStackTrace();
            //Como fallo algo con esas keys las encero y creo unas nuevas
            KeyStoreCriptext.putString(ctxRef.get(), monkeyId, "");
            aesUtil = new AESUtil(ctxRef.get(), monkeyId);
            return getSessionHTTP(this.urlUser, this.urlPass, this.userInfo, this.ignore_params);
        }


        final String domain = finalResult.getString("sdomain");
        final int port = finalResult.getInt("sport");
        return new ServerResponse(monkeyId, domain, port);
    }

    private ServerResponse connectHTTP(String monkeyId, String encriptedKeys, JSONArray ignore_params) throws JSONException,
            IOException{

        // Create a new HttpClient and Post Header
        HttpClient httpclient = MonkeyHttpClient.newClient();
        HttpPost httppost = MonkeyHttpClient.newPost(MonkeyKitSocketService.Companion.getHttpsURL() +
                "/user/connect", urlUser, urlPass);

        JSONObject localJSONObject1 = new JSONObject();

        localJSONObject1.put("usk", encriptedKeys);
        localJSONObject1.put("monkey_id", monkeyId);
        localJSONObject1.put("ignore_params", ignore_params);

        JSONObject params = new JSONObject();
        params.put("data", localJSONObject1.toString());
        Log.d("connectHTTP", "Req: " + params.toString());

        JSONObject finalResult = MonkeyHttpClient.getResponse(httpclient, httppost, params.toString());
         Log.d("connectHTTP", finalResult.toString());
        finalResult = finalResult.getJSONObject("data");

        prefs.edit().putString("sdomain", finalResult.getString("sdomain")).apply();
        prefs.edit().putString("sport", finalResult.getString("sport")).apply();

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
