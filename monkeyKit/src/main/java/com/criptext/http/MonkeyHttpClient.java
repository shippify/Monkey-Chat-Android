package com.criptext.http;

import android.util.Base64;
import android.util.Log;

import com.criptext.MonkeyKitSocketService;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by gesuwall on 6/2/16.
 */
public class MonkeyHttpClient {
    /**
     * Crea un HTTP Client con timeout
     * @return
     */
    public static HttpClient newClient(){
        HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 20000);
        HttpConnectionParams.setSoTimeout(httpParams, 25000);
        return new DefaultHttpClient(httpParams);
    }

    public static HttpPost newPost(String url, String user, String pass){
        HttpPost httppost = new HttpPost(url);
        String base64EncodedCredentials = "Basic " + Base64.encodeToString(
             (user + ":" + pass).getBytes(), Base64.NO_WRAP);
        httppost.setHeader("Authorization", base64EncodedCredentials);
        //sets a request header so the page receving the request
        //will know what to do with it
        httppost.setHeader("Accept", "application/json");
        httppost.setHeader("Content-type", "application/json");
        return httppost;
    }

    public static JSONObject getResponse(HttpClient httpclient, HttpPost httppost, String params) throws IOException,
            JSONException {
        // Execute HTTP Post Request
        StringEntity se = new StringEntity(params);
        // Add your data
        httppost.setEntity(se);
        HttpResponse response = httpclient.execute(httppost);
        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
        String json = reader.readLine();
        Log.d("Response","json " + json);
        if(json.equals("Unauthorized")){
            throw new IllegalArgumentException("Server response: Unauthorized\n Please check your APP_ID and APP_KEY");
        }
        JSONTokener tokener = new JSONTokener(json);
        return new JSONObject(tokener);
    }

    public static void subscribePushHttp(String token, String sessionId, String urlUser, String urlPass, boolean override) throws JSONException,
        IOException {
        // Create a new HttpClient and Post Header
        HttpClient httpclient = newClient();

            HttpPost httppost = newPost(MonkeyKitSocketService.Companion.getHttpsURL() + "/push/subscribe", urlUser, urlPass);

            JSONObject params = new JSONObject();
            JSONObject localJSONObject1 = new JSONObject();
            localJSONObject1.put("token",token);
            localJSONObject1.put("device","android");
            localJSONObject1.put("mode","1");
            localJSONObject1.put("userid", sessionId);
            localJSONObject1.put("override", override?"1":"0");

            params.put("data", localJSONObject1.toString());
            Log.d("subscribePushHttp", "Req: " + params.toString());

            JSONObject finalResult = getResponse(httpclient, httppost, params.toString());
            Log.d("subscribePushHttp", finalResult.toString());

    }

}
