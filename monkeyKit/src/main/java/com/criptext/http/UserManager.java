package com.criptext.http;

import android.util.Base64;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.criptext.MonkeyKitSocketService;
import com.criptext.comunication.AsyncConnSocket;
import com.criptext.comunication.CBTypes;
import com.criptext.comunication.MOKConversation;
import com.criptext.comunication.MOKMessage;
import com.criptext.comunication.MOKUser;
import com.criptext.comunication.MessageTypes;
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
import java.util.List;
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
            final MonkeyKitSocketService service = serviceRef.get();
            String urlconnect = MonkeyKitSocketService.Companion.getHttpsURL()+"/user/update";

            JSONObject localJSONObject1 = new JSONObject();
            localJSONObject1.put("monkeyId",monkeyId);
            localJSONObject1.put("params",info);

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("data", localJSONObject1.toString());

            aq.auth(handle).ajax(urlconnect, params, JSONObject.class, new AjaxCallback<JSONObject>(){
                @Override
                public void callback(String url, JSONObject response, AjaxStatus status) {

                    if(service==null)
                        return;

                    if(response!=null)
                        service.processMessageFromHandler(CBTypes.onUpdateUserData, new Object[]{null});
                    else
                        service.processMessageFromHandler(CBTypes.onUpdateUserData, new Object[]{
                                "Error code:"+status.getCode()+" -  Error msg:"+status.getMessage()});
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public void getUserInfoById(final String monkeyid){

        final MonkeyKitSocketService service = serviceRef.get();
        String endpoint = "/user/info/"+monkeyid;

        aq.auth(handle).ajax(MonkeyKitSocketService.Companion.getHttpsURL()+endpoint, JSONObject.class, new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject response, AjaxStatus status) {

                if(service==null)
                    return;

                if (response != null) {
                    try {
                        JsonParser jsonParser = new JsonParser();
                        JsonObject gsonObject = (JsonObject)jsonParser.parse(response.getJSONObject("data").toString());
                        service.processMessageFromHandler(CBTypes.onGetUserInfo, new Object[]{
                                new MOKUser(monkeyid, gsonObject), null});
                    } catch (Exception e) {
                        e.printStackTrace();
                        service.processMessageFromHandler(CBTypes.onGetUserInfo, new Object[]{
                                null, e});
                    }
                }
                else{
                    service.processMessageFromHandler(CBTypes.onGetUserInfo, new Object[]{
                            "Error code:"+status.getCode()+" -  Error msg:"+status.getMessage()});
                }
            }
        });
    }

    public void getUsersInfo(final String monkeyIds){

        final MonkeyKitSocketService service = serviceRef.get();
        String endpoint = "/users/info";

        try {
            JSONObject localJSONObject1 = new JSONObject();
            localJSONObject1.put("monkey_ids", monkeyIds);

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("data", localJSONObject1.toString());

            aq.auth(handle).ajax(MonkeyKitSocketService.Companion.getHttpsURL() + endpoint, params, JSONObject.class, new AjaxCallback<JSONObject>() {
                @Override
                public void callback(String url, JSONObject response, AjaxStatus status) {

                    if (service == null)
                        return;

                    if (response != null) {
                        try {
                            ArrayList<MOKUser> mokUserArrayList = new ArrayList<MOKUser>();
                            for(int i=0; i<response.getJSONArray("data").length(); i++){
                                JSONObject jsonObject = response.getJSONArray("data").getJSONObject(i);
                                JsonParser jsonParser = new JsonParser();
                                JsonObject gsonObject = (JsonObject) jsonParser.parse(jsonObject.toString());
                                mokUserArrayList.add(new MOKUser(gsonObject.get("monkey_id").getAsString(), gsonObject));
                            }

                            service.processMessageFromHandler(CBTypes.onGetUsersInfo, new Object[]{
                                    mokUserArrayList, null});
                        } catch (Exception e) {
                            e.printStackTrace();
                            service.processMessageFromHandler(CBTypes.onGetUsersInfo, new Object[]{
                                    null, e});
                        }
                    } else {
                        service.processMessageFromHandler(CBTypes.onGetUsersInfo, new Object[]{
                                "Error code:" + status.getCode() + " -  Error msg:" + status.getMessage()});
                    }
                }
            });
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void getConversations(String monkeyid, int qty, long timestamp, final AsyncConnSocket asyncConnSocket){

        final MonkeyKitSocketService service = serviceRef.get();
        String urlconnect = MonkeyKitSocketService.Companion.getHttpsURL()+"/user/conversations";

        try {
            JSONObject localJSONObject1 = new JSONObject();
            localJSONObject1.put("monkeyId", monkeyid);
            localJSONObject1.put("qty", qty);
            localJSONObject1.put("timestamp", timestamp);

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("data", localJSONObject1.toString());

            aq.auth(handle).ajax(urlconnect, params, JSONObject.class, new AjaxCallback<JSONObject>() {
                @Override
                public void callback(String url, JSONObject response, AjaxStatus status) {

                    if (service == null)
                        return;

                    if (response != null)
                        try {
                            List<MOKConversation> conversationList = new ArrayList<MOKConversation>();
                            JsonParser parser = new JsonParser();
                            JsonObject props = new JsonObject(), params = new JsonObject();
                            JSONArray jsonArrayConversations = response.getJSONObject("data").getJSONArray("conversations");
                            JsonArray array = (JsonArray) parser.parse(jsonArrayConversations.toString());
                            for (int i = 0; i < array.size(); i++) {
                                JsonObject currentConv = null;
                                JsonObject currentMessage = null;
                                MOKMessage remote = null;
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
                                        if (remote.getProps().has("encr") && remote.getProps().get("encr").getAsString().compareTo("1") == 0)
                                            remote = asyncConnSocket.getKeysAndDecryptMOKMessage(remote, false);
                                        else if (remote.getProps().has("encoding") && !remote.getType().equals(MessageTypes.MOKFile)) {
                                            if (remote.getProps().get("encoding").getAsString().equals("base64"))
                                                remote.setMsg(new String(Base64.decode(remote.getMsg().getBytes(), Base64.NO_WRAP)));
                                        }
                                    }

                                    conversationList.add(new MOKConversation(currentConv.get("id").getAsString(),
                                            currentConv.get("info").getAsJsonObject(),
                                            currentConv.has("members") ? currentConv.get("members").getAsString().split(",") : new String[0],
                                            remote, currentConv.get("last_seen").getAsLong(), currentConv.get("unread").getAsInt(),
                                            currentConv.has("last_modified") ? currentConv.get("last_modified").getAsLong() : 0));

                                    System.out.println("size:" + conversationList.size());
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }

                            service.processMessageFromHandler(CBTypes.onGetConversations, new Object[]{
                                    conversationList, null});

                        } catch (JSONException e) {
                            e.printStackTrace();
                            service.processMessageFromHandler(CBTypes.onGetConversations, new Object[]{
                                    null, e});
                        }
                    else
                        service.processMessageFromHandler(CBTypes.onGetConversations, new Object[]{
                                "Error code:" + status.getCode() + " -  Error msg:" + status.getMessage()});
                }
            });
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void getConversationMessages(String monkeyid, String conversationId, int numberOfMessages
            , String lastTimeStamp, final AsyncConnSocket asyncConnSocket){

        final MonkeyKitSocketService service = serviceRef.get();
        String newlastTimeStamp = lastTimeStamp;
        if(lastTimeStamp==null || lastTimeStamp.length()==0 || lastTimeStamp.equals("0"))
            newlastTimeStamp = "";

        String urlconnect = MonkeyKitSocketService.Companion.getHttpsURL()+"/conversation/messages/"+monkeyid+"/"+conversationId+"/"+numberOfMessages+"/"+newlastTimeStamp;
        aq.auth(handle).ajax(urlconnect, JSONObject.class, new AjaxCallback<JSONObject>(){
            @Override
            public void callback(String url, JSONObject response, AjaxStatus status) {

                if(service==null)
                    return;

                if(response!=null){
                    try {
                        List<MOKMessage> messageList = new ArrayList<MOKMessage>();
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
                                        messageList.add(remote);
                                }
                            }
                            catch( Exception ex){
                                ex.printStackTrace();
                            }
                        }

                        service.processMessageFromHandler(CBTypes.onGetConversationMessages, new Object[]{
                                messageList, null});
                    } catch (JSONException e) {
                        e.printStackTrace();
                        service.processMessageFromHandler(CBTypes.onGetConversationMessages, new Object[]{
                                null, e});
                    }
                }
                else
                    service.processMessageFromHandler(CBTypes.onGetConversationMessages, new Object[]{
                            "Error code:"+status.getCode()+" -  Error msg:"+status.getMessage()});
            }
        });

    }

    public void deleteConversation(String monkeyid, String conversation_id){

        try{
            String urlconnect = MonkeyKitSocketService.Companion.getHttpsURL()+"/user/delete/conversation";
            final MonkeyKitSocketService service = serviceRef.get();

            JSONObject localJSONObject1 = new JSONObject();
            localJSONObject1.put("conversation_id", conversation_id);
            localJSONObject1.put("monkey_id", monkeyid);

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("data", localJSONObject1.toString());

            aq.auth(handle).ajax(urlconnect, params, JSONObject.class, new AjaxCallback<JSONObject>(){
                @Override
                public void callback(String url, JSONObject response, AjaxStatus status) {

                    if(service==null)
                        return;

                    if(response!=null)
                        try {
                            service.processMessageFromHandler(CBTypes.onDeleteConversation, new Object[]{
                                    response.getJSONObject("data").getString("conversation"), null});
                        } catch (JSONException e) {
                            e.printStackTrace();
                            service.processMessageFromHandler(CBTypes.onDeleteConversation, new Object[]{
                                    null, e});
                        }
                    else{
                        service.processMessageFromHandler(CBTypes.onDeleteConversation, new Object[]{
                                "Error code:"+status.getCode()+" -  Error msg:"+status.getMessage()});
                    }
                }
            });
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

}
