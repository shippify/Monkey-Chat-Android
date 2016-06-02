package com.criptext.lib;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import com.criptext.security.AESUtil;
import com.criptext.security.RSAUtil;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
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
 * Created by gesuwall on 2/10/16.
 */
public class MonkeyInit {
    private AsyncTask<String, String, String> async;
    private WeakReference<Context> ctxRef;
    private AESUtil aesUtil;
    final String urlUser, urlPass, fullname, myOldMonkeyId;

    public MonkeyInit(Context context, String sessionId, String user, String pass, String fullname){
        this.myOldMonkeyId = sessionId == null ? "" : sessionId;
        this.urlUser = user;
        this.urlPass = pass;
        this.fullname = fullname;
        ctxRef = new WeakReference<>(context);
        async = new AsyncTask<String, String, String>(){

            @Override
            protected void onPostExecute(String res){
                if(!res.endsWith("Exception"))
                    onSessionOK(res);
                else
                    onSessionError(res);

            }

            @Override
            protected String doInBackground(String... params) {
                try {
                    //Generate keys
                    if(myOldMonkeyId.isEmpty()) {
                        aesUtil = new AESUtil(ctxRef.get(), myOldMonkeyId);
                        return getSessionHTTP(params[0], params[1], params[2]);
                    } else {
                        return userSync(myOldMonkeyId);
                    }

                }catch (Exception ex){
                    ex.printStackTrace();
                    return ex.getClass().getName();
                }
            }
        };
    }

    public void onSessionOK(String session){
    //grab your new session id
    }

    public void onSessionError(String exceptionName){
    //grab your new session id
    }

    /**
     * Debes de llamar a este metodo para que de forma asincrona se registre el usuario con MonkeyKit
     */
    public void register(){
        async.execute(urlUser, urlPass, fullname);
    }

    public void cancel(){
        if(async != null)
            async.cancel(true);
    }

    private String storeKeysIV(String sessionId, String pubKey){
        //Encrypt workers
        RSAUtil rsa = new RSAUtil(Base64.decode(pubKey.getBytes(),0));
        String usk=rsa.encrypt(aesUtil.strKey+":"+aesUtil.strIV);
        //Guardo mis key & Iv
        KeyStoreCriptext.putString(ctxRef.get(), sessionId, aesUtil.strKey+":"+aesUtil.strIV);
        return usk;

    }

    private String getSessionHTTP(String urlUser, String urlPass, String fullname) throws JSONException,
            UnsupportedEncodingException, ClientProtocolException, IOException{
        // Create a new HttpClient and Post Header
        HttpClient httpclient = MonkeyKit.newMonkeyHttpClient();
        HttpPost httppost = MonkeyKit.newMonkeyHttpPost(MonkeyKit.URL + "/user/session", urlUser, urlPass);

        JSONObject localJSONObject1 = new JSONObject();

        JSONObject user_info = new JSONObject();
        user_info.put("name",fullname);

        localJSONObject1.put("username",urlUser);
        localJSONObject1.put("password",urlPass);
        localJSONObject1.put("session_id", myOldMonkeyId);
        localJSONObject1.put("expiring","0");
        localJSONObject1.put("user_info",user_info);

        JSONObject params = new JSONObject();
        params.put("data", localJSONObject1.toString());
        Log.d("getSessionHTTP", "Req: " + params.toString());

        JSONObject finalResult = MonkeyKit.getHttpResponseJson(httpclient, httppost, params.toString());

        Log.d("getSesssionHTTP", finalResult.toString());
        finalResult = finalResult.getJSONObject("data");

        String sessionId = finalResult.getString("sessionId");
        String pubKey = finalResult.getString("publicKey");
        pubKey = pubKey.replace("-----BEGIN PUBLIC KEY-----\n", "").replace("\n-----END PUBLIC KEY-----", "");

        String encriptedKeys = storeKeysIV(sessionId, pubKey);

        //retornar el session solo despues del connect exitoso
        return  connectHTTP(finalResult.getString("sessionId"), encriptedKeys);

    }

    private String userSync(String sessionId) throws Exception{
 // Create a new HttpClient and Post Header
        RSAUtil rsaUtil = new RSAUtil();
        rsaUtil.generateKeys();

        HttpClient httpclient = MonkeyKit.newMonkeyHttpClient();
        HttpPost httppost = MonkeyKit.newMonkeyHttpPost(MonkeyKit.URL+"/user/key/sync", urlUser, urlPass);

        JSONObject localJSONObject1 = new JSONObject();

        localJSONObject1.put("session_id", sessionId);
        localJSONObject1.put("public_key", "-----BEGIN PUBLIC KEY-----\n" + rsaUtil.getPublicKey() + "\n-----END PUBLIC KEY-----");
        //System.out.println("-----BEGIN PUBLIC KEY-----\n" + rsaUtil.pubKeyStr + "\n-----END PUBLIC KEY-----");
        JSONObject params = new JSONObject();
        params.put("data", localJSONObject1.toString());
        Log.d("userSyncMS", "Req: " + params.toString());

        JSONObject finalResult = MonkeyKit.getHttpResponseJson(httpclient, httppost, params.toString());
         Log.d("userSyncMS", finalResult.toString());
        finalResult = finalResult.getJSONObject("data");

        final String keys = finalResult.getString("keys");
        String decriptedKey = rsaUtil.desencrypt(keys);
        KeyStoreCriptext.putString(ctxRef.get() ,sessionId, decriptedKey);

        try {
            aesUtil = new AESUtil(ctxRef.get(), sessionId);
        } catch (Exception ex){
            ex.printStackTrace();
            //Como fallo algo con esas keys las encero y creo unas nuevas
            KeyStoreCriptext.putString(ctxRef.get(), sessionId, "");
            aesUtil = new AESUtil(ctxRef.get(), sessionId);
            return getSessionHTTP(this.urlUser, this.urlPass, this.fullname);
        }

        return sessionId;


    }
    private String connectHTTP(String sessionId, String encriptedKeys) throws JSONException,
            UnsupportedEncodingException, ClientProtocolException, IOException{
 // Create a new HttpClient and Post Header
        HttpClient httpclient = MonkeyKit.newMonkeyHttpClient();
        HttpPost httppost = MonkeyKit.newMonkeyHttpPost(MonkeyKit.URL+"/user/connect", urlUser, urlPass);

        JSONObject localJSONObject1 = new JSONObject();

        localJSONObject1.put("usk", encriptedKeys);
        localJSONObject1.put("session_id", sessionId);

        JSONObject params = new JSONObject();
        params.put("data", localJSONObject1.toString());
        Log.d("connectHTTP", "Req: " + params.toString());

        JSONObject finalResult = MonkeyKit.getHttpResponseJson(httpclient, httppost, params.toString());
         Log.d("connectHTTP", finalResult.toString());
        finalResult = finalResult.getJSONObject("data");

        return finalResult.getString("sessionId");


    }
}
