package com.criptext.http;

import android.util.Base64;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.criptext.MonkeyKitSocketService;
import com.criptext.comunication.AsyncConnSocket;
import com.criptext.comunication.CBTypes;
import com.criptext.comunication.MOKMessage;
import com.criptext.comunication.MessageTypes;
import com.criptext.security.AESUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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

    public void updateUserData(String monkeyId, JSONObject info){

        try {
            String urlconnect = MonkeyKitSocketService.Companion.getHttpsURL()+"/user/update";

            JSONObject localJSONObject1 = new JSONObject();
            localJSONObject1.put("monkeyId",monkeyId);
            localJSONObject1.put("params",info);

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("data", localJSONObject1.toString());

            aq.auth(handle).ajax(urlconnect, params, JSONObject.class, new AjaxCallback<JSONObject>(){
                @Override
                public void callback(String url, JSONObject response, AjaxStatus status) {

                    if(serviceRef.get()!=null)
                        return;

                    if(response!=null)
                        serviceRef.get().processMessageFromHandler(CBTypes.onUpdateUserData, new Object[]{null});
                    else
                        serviceRef.get().processMessageFromHandler(CBTypes.onUpdateUserData, new Object[]{
                                "Error code:"+status.getCode()+" -  Error msg:"+status.getMessage()});
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public void getInfoById(String monkeyid){

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

                if(serviceRef.get()!=null)
                    return;

                if (response != null) {
                    try {
                        serviceRef.get().processMessageFromHandler(CBTypes.onGetInfo, new Object[]{
                                response.getJSONObject("data"), null});
                    } catch (JSONException e) {
                        e.printStackTrace();
                        serviceRef.get().processMessageFromHandler(CBTypes.onGetInfo, new Object[]{
                                null, e});
                    }
                }
                else{
                    serviceRef.get().processMessageFromHandler(CBTypes.onGetInfo, new Object[]{
                            "Error code:"+status.getCode()+" -  Error msg:"+status.getMessage()});
                }
            }
        });
    }

    public void getConversations(String monkeyid, final AsyncConnSocket asyncConnSocket){

        String urlconnect = MonkeyKitSocketService.Companion.getHttpsURL()+"/user/"+monkeyid+"/conversations";
        aq.auth(handle).ajax(urlconnect, JSONObject.class, new AjaxCallback<JSONObject>(){
            @Override
            public void callback(String url, JSONObject response, AjaxStatus status) {

                if(serviceRef.get()!=null)
                    return;

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

                        serviceRef.get().processMessageFromHandler(CBTypes.onGetConversations, new Object[]{
                                new JSONArray(array.toString()), null});

                    } catch (JSONException e) {
                        e.printStackTrace();
                        serviceRef.get().processMessageFromHandler(CBTypes.onGetConversations, new Object[]{
                                null, e});
                    }
                else
                    serviceRef.get().processMessageFromHandler(CBTypes.onGetConversations, new Object[]{
                            "Error code:"+status.getCode()+" -  Error msg:"+status.getMessage()});
            }
        });

    }

    public void getConversationMessages(String monkeyid, String conversationId, int numberOfMessages
            , String lastTimeStamp, final AsyncConnSocket asyncConnSocket){

        String newlastTimeStamp = lastTimeStamp;
        if(lastTimeStamp==null || lastTimeStamp.length()==0 || lastTimeStamp.equals("0"))
            newlastTimeStamp = "";

        String urlconnect = MonkeyKitSocketService.Companion.getHttpsURL()+"/conversation/messages/"+monkeyid+"/"+conversationId+"/"+numberOfMessages+"/"+newlastTimeStamp;
        aq.auth(handle).ajax(urlconnect, JSONObject.class, new AjaxCallback<JSONObject>(){
            @Override
            public void callback(String url, JSONObject response, AjaxStatus status) {

                if(serviceRef.get()!=null)
                    return;

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

                        serviceRef.get().processMessageFromHandler(CBTypes.onGetConversationMessages, new Object[]{
                                new JSONArray(array.toString()), null});
                    } catch (JSONException e) {
                        e.printStackTrace();
                        serviceRef.get().processMessageFromHandler(CBTypes.onGetConversationMessages, new Object[]{
                                null, e});
                    }
                }
                else
                    serviceRef.get().processMessageFromHandler(CBTypes.onGetConversationMessages, new Object[]{
                            "Error code:"+status.getCode()+" -  Error msg:"+status.getMessage()});
            }
        });

    }

}
