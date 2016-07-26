package com.criptext.http;

import com.androidquery.AQuery;
import com.androidquery.auth.BasicHandle;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.criptext.MonkeyKitSocketService;
import com.criptext.comunication.AsyncConnSocket;
import com.criptext.comunication.MonkeyHttpResponse;
import com.criptext.comunication.MonkeyJsonResponse;
import com.criptext.security.AESUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by danieltigse on 7/20/16.
 */
public class UserManager extends AQueryHttp {

    public UserManager(MonkeyKitSocketService service, AESUtil aesUtil) {
        super(service, aesUtil);
    }

    public void updateUserObject(String monkeyId, JSONObject userInfo, final MonkeyHttpResponse monkeyHttpResponse){

        try {
            String urlconnect = MonkeyKitSocketService.Companion.getHttpsURL()+"/info/update";

            JSONObject localJSONObject1 = new JSONObject();
            localJSONObject1.put("monkeyId",monkeyId);
            localJSONObject1.put("params",userInfo);

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("data", localJSONObject1.toString());

            aq.auth(handle).ajax(urlconnect, params, JSONObject.class, new AjaxCallback<JSONObject>(){
                @Override
                public void callback(String url, JSONObject response, AjaxStatus status) {
                    if(response!=null)
                        monkeyHttpResponse.OnSuccess();
                    else
                        monkeyHttpResponse.OnError();
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public void getConversations(String monkeyid, final MonkeyJsonResponse monkeyJsonResponse){

        String urlconnect = MonkeyKitSocketService.Companion.getHttpsURL()+"/user/"+monkeyid+"/conversations";
        aq.auth(handle).ajax(urlconnect, JSONObject.class, new AjaxCallback<JSONObject>(){
            @Override
            public void callback(String url, JSONObject response, AjaxStatus status) {
                if(response!=null)
                    try {
                        monkeyJsonResponse.OnSuccess(response.getJSONObject("data"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                        monkeyJsonResponse.OnError(status);
                    }
                else
                    monkeyJsonResponse.OnError(status);
            }
        });

    }

    public void getConversationMessages(String monkeyid, String conversationId, int numberOfMessages
            , String lastMessageId, AsyncConnSocket asyncConnSocket, final MonkeyJsonResponse monkeyJsonResponse){

        String urlconnect = MonkeyKitSocketService.Companion.getHttpsURL()+"/conversation/messages/"+monkeyid+"/"+conversationId+"/"+numberOfMessages+"/"+lastMessageId;
        aq.auth(handle).ajax(urlconnect, JSONObject.class, new AjaxCallback<JSONObject>(){
            @Override
            public void callback(String url, JSONObject response, AjaxStatus status) {
                if(response!=null){
                    try {
                        JSONArray jsonArrayMessages = response.getJSONObject("data").getJSONArray("messages");

                    } catch (JSONException e) {
                        e.printStackTrace();
                        monkeyJsonResponse.OnError(status);
                    }
                }
                else
                    monkeyJsonResponse.OnError(status);
            }
        });

    }

}
