package com.criptext.http;

import android.os.Message;
import android.util.Base64;
import android.util.Log;

import com.androidquery.AQuery;
import com.androidquery.auth.BasicHandle;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.criptext.MonkeyKitSocketService;
import com.criptext.comunication.AsyncConnSocket;
import com.criptext.comunication.MOKMessage;
import com.criptext.comunication.MessageTypes;
import com.criptext.comunication.MonkeyHttpResponse;
import com.criptext.comunication.MonkeyJsonResponse;
import com.criptext.security.AESUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
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

    public void getInfoById(String monkeyid, final MonkeyJsonResponse monkeyJsonResponse){

        String endpoint = "/info/" + monkeyid;

        //check if it's a group
        if (monkeyid.contains("G:")) {
            endpoint = "/group"+endpoint;
        }else{
            endpoint = "/user"+endpoint;
        }

        aq.auth(handle).ajax(MonkeyKitSocketService.Companion.getHttpsURL()+endpoint, JSONObject.class, new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject response, AjaxStatus status) {
                if (response != null) {
                    try {
                        monkeyJsonResponse.OnSuccess(response.getJSONObject("data"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                        monkeyJsonResponse.OnError(status);
                    }
                }
                else{
                    monkeyJsonResponse.OnError(status);
                }
            }
        });
    }

    public void getConversations(String monkeyid, final AsyncConnSocket asyncConnSocket, final MonkeyJsonResponse monkeyJsonResponse){

        String urlconnect = MonkeyKitSocketService.Companion.getHttpsURL()+"/user/"+monkeyid+"/conversations";
        aq.auth(handle).ajax(urlconnect, JSONObject.class, new AjaxCallback<JSONObject>(){
            @Override
            public void callback(String url, JSONObject response, AjaxStatus status) {
                if(response!=null)
                    try {
                        JsonParser parser = new JsonParser();
                        JsonObject props = new JsonObject(), params = new JsonObject();
                        JSONArray jsonArrayConversations = response.getJSONObject("data").getJSONArray("conversations");
                        JsonArray array = (JsonArray)parser.parse(jsonArrayConversations.toString());
                        MOKMessage remote;
                        for (int i = 0; i < array.size(); i++) {
                            JsonObject currentConv = null;
                            JsonObject currentMessage = null;
                            try {
                                JsonElement jsonMessage = array.get(i);
                                currentConv = jsonMessage.getAsJsonObject();
                                currentMessage = currentConv.getAsJsonObject("last_message");
                                //init params props
                                if (currentMessage.has("params") && !currentMessage.get("params").isJsonNull() && !parser.parse(currentMessage.get("params").getAsString()).isJsonNull())
                                    if (parser.parse(currentMessage.get("params").getAsString()) instanceof JsonObject)
                                        params = (JsonObject) parser.parse(currentMessage.get("params").getAsString());
                                if (currentMessage.has("props") && !currentMessage.get("props").isJsonNull() && !parser.parse(currentMessage.get("props").getAsString()).isJsonNull())
                                    props = (JsonObject) parser.parse(currentMessage.get("props").getAsString());

                                if (currentMessage.has("type") && (currentMessage.get("type").getAsString().compareTo(MessageTypes.MOKText) == 0
                                        || currentMessage.get("type").getAsString().compareTo(MessageTypes.MOKFile) == 0)) {

                                    remote = asyncConnSocket.createMOKMessageFromJSON(currentMessage, params, props);
                                    if (remote.getProps().get("encr").getAsString().compareTo("1") == 0)
                                        remote = asyncConnSocket.getKeysAndDecryptMOKMessage(remote, false);
                                    else if (remote.getProps().has("encoding") && !remote.getType().equals(MessageTypes.MOKFile)) {
                                        if(remote.getProps().get("encoding").getAsString().equals("base64"))
                                            remote.setMsg(new String(Base64.decode(remote.getMsg().getBytes(), Base64.NO_WRAP)));
                                    }
                                    if (remote != null)
                                        currentMessage.addProperty("msg", remote.getMsg());
                                }
                            }
                            catch( Exception ex){
                                ex.printStackTrace();
                            }
                        }

                        JSONObject resp = new JSONObject();
                        resp.put("conversations", new JSONArray(array.toString()));
                        monkeyJsonResponse.OnSuccess(resp);
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
            , String lastTimeStamp, final AsyncConnSocket asyncConnSocket, final MonkeyJsonResponse monkeyJsonResponse){

        String newlastTimeStamp = lastTimeStamp;
        if(lastTimeStamp==null || lastTimeStamp.length()==0 || lastTimeStamp.equals("0"))
            newlastTimeStamp = "";

        String urlconnect = MonkeyKitSocketService.Companion.getHttpsURL()+"/conversation/messages/"+monkeyid+"/"+conversationId+"/"+numberOfMessages+"/"+newlastTimeStamp;
        aq.auth(handle).ajax(urlconnect, JSONObject.class, new AjaxCallback<JSONObject>(){
            @Override
            public void callback(String url, JSONObject response, AjaxStatus status) {
                if(response!=null){
                    try {
                        JsonParser parser = new JsonParser();
                        JsonObject props = new JsonObject(), params = new JsonObject();
                        JSONArray jsonArrayMessages = response.getJSONObject("data").getJSONArray("messages");
                        JsonArray array = (JsonArray)parser.parse(jsonArrayMessages.toString());
                        MOKMessage remote;
                        for (int i = 0; i < array.size(); i++) {
                            JsonObject currentMessage = null;
                            try {
                                JsonElement jsonMessage = array.get(i);
                                currentMessage = jsonMessage.getAsJsonObject();
                                //init params props
                                if (currentMessage.has("params") && !currentMessage.get("params").isJsonNull() && !parser.parse(currentMessage.get("params").getAsString()).isJsonNull())
                                    if (parser.parse(currentMessage.get("params").getAsString()) instanceof JsonObject)
                                        params = (JsonObject) parser.parse(currentMessage.get("params").getAsString());
                                if (currentMessage.has("props") && !currentMessage.get("props").isJsonNull() && !parser.parse(currentMessage.get("props").getAsString()).isJsonNull())
                                    props = (JsonObject) parser.parse(currentMessage.get("props").getAsString());

                                if (currentMessage.has("type") && (currentMessage.get("type").getAsString().compareTo(MessageTypes.MOKText) == 0
                                        || currentMessage.get("type").getAsString().compareTo(MessageTypes.MOKFile) == 0)) {

                                    remote = asyncConnSocket.createMOKMessageFromJSON(currentMessage, params, props);
                                    if (remote.getProps().get("encr").getAsString().compareTo("1") == 0)
                                        remote = asyncConnSocket.getKeysAndDecryptMOKMessage(remote, false);
                                    else if (remote.getProps().has("encoding") && !remote.getType().equals(MessageTypes.MOKFile)) {
                                        if(remote.getProps().get("encoding").getAsString().equals("base64"))
                                            remote.setMsg(new String(Base64.decode(remote.getMsg().getBytes(), Base64.NO_WRAP)));
                                    }
                                    if (remote != null)
                                        currentMessage.addProperty("msg", remote.getMsg());
                                }
                            }
                            catch( Exception ex){
                                ex.printStackTrace();
                            }
                        }

                        JSONObject resp = new JSONObject();
                        resp.put("messages", new JSONArray(array.toString()));
                        monkeyJsonResponse.OnSuccess(resp);
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
