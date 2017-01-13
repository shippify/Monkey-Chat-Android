package com.criptext.http;

import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.criptext.MonkeyKitSocketService;
import com.criptext.comunication.CBTypes;
import com.criptext.comunication.MOKConversation;
import com.criptext.security.AESUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by gesuwall on 6/23/16.
 */
public class GroupManager extends AQueryHttp {

    public GroupManager(MonkeyKitSocketService service, AESUtil aesUtil) {
        super(service, aesUtil);
    }

    public void getGroupInfoById(final String monkeyid){

        String endpoint = "/group/info/"+monkeyid;

        aq.auth(handle).ajax(MonkeyKitSocketService.Companion.getHttpsURL()+endpoint, JSONObject.class, new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject response, AjaxStatus status) {

                MonkeyKitSocketService service = serviceRef.get();
                if(service==null)
                    return;

                if (response != null) {
                    try {
                        JSONObject data = response.getJSONObject("data");
                        JsonParser jsonParser = new JsonParser();
                        JsonObject gsonObject = (JsonObject)jsonParser.parse(data.getString("group_info"));
                        JSONArray jsonArray = data.getJSONArray("members");
                        ArrayList<String> arrayList = new ArrayList<String>();
                        for(int i = 0; i<jsonArray.length(); i++){
                            arrayList.add(jsonArray.getString(i));
                        }
                        MOKConversation mokConversation = new MOKConversation(data.getString("group_id"),
                                gsonObject, arrayList.toArray(new String[arrayList.size()]), null, 0, 0, 0 );
                        service.processMessageFromHandler(CBTypes.onGetGroupInfo, new Object[]{
                                mokConversation, null});
                    } catch (JSONException e) {
                        e.printStackTrace();
                        service.processMessageFromHandler(CBTypes.onGetGroupInfo, new Object[]{
                                new MOKConversation(monkeyid), e});
                    }
                }
                else{
                    service.processMessageFromHandler(CBTypes.onGetGroupInfo, new Object[]{new MOKConversation(monkeyid),
                        new Exception("Error code:"+status.getCode()+" -  Error msg:"+status.getMessage())});
                }
            }
        });
    }

    public void updateGroupData(final String monkeyId, JSONObject info){

        try {
            String urlconnect = MonkeyKitSocketService.Companion.getHttpsURL()+"/group/update";

            JSONObject localJSONObject1 = new JSONObject();
            localJSONObject1.put("monkeyId",monkeyId);
            localJSONObject1.put("params",info);

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("data", localJSONObject1.toString());

            aq.auth(handle).ajax(urlconnect, params, JSONObject.class, new AjaxCallback<JSONObject>(){
                @Override
                public void callback(String url, JSONObject response, AjaxStatus status) {

                    MonkeyKitSocketService service = serviceRef.get();
                    if(service==null)
                        return;

                    if(response!=null)
                        service.processMessageFromHandler(CBTypes.onUpdateGroupData, new Object[]{monkeyId, null});
                    else
                        service.processMessageFromHandler(CBTypes.onUpdateGroupData, new Object[]{monkeyId,
                                new Exception("Error code:"+status.getCode()+" -  Error msg:"+status.getMessage())});
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public void createGroup(final String members, final String group_name, final String group_id){

        try {
            String urlConnect = MonkeyKitSocketService.Companion.getHttpsURL() +"/group/create";

            JSONObject localJSONObjectInfo = new JSONObject();
            localJSONObjectInfo.put("name", group_name);

            JSONObject localJSONObject1 = new JSONObject();
            if(members.isEmpty())
                localJSONObject1.put("members", monkeyID);
            else
                localJSONObject1.put("members", monkeyID + ',' + members);
            localJSONObject1.put("info",localJSONObjectInfo);
            localJSONObject1.put("session_id", monkeyID);
            if(group_id!=null)
                localJSONObject1.put("group_id", group_id);

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("data", localJSONObject1.toString());

            aq.auth(handle).ajax(urlConnect, params, JSONObject.class, new AjaxCallback<JSONObject>(){
                @Override
                public void callback(String url, JSONObject response, AjaxStatus status) {

                    MonkeyKitSocketService service = serviceRef.get();
                    if(service==null)
                        return;

                    if (response != null) {
                        try {
                            service.processMessageFromHandler(CBTypes.onCreateGroup, new Object[]{
                                    members, group_name, response.getJSONObject("data").getString("group_id"), null});
                        } catch (JSONException e) {
                            e.printStackTrace();
                            service.processMessageFromHandler(CBTypes.onCreateGroup, new Object[]{
                                    members, group_name, group_id!=null?group_id:null, e});
                        }
                    }
                    else{
                        service.processMessageFromHandler(CBTypes.onCreateGroup, new Object[]{
                                members, group_name, group_id!=null?group_id:null, new Exception("Error code:"+status.getCode()+
                                " -  Error msg:"+status.getMessage())});
                    }
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void removeGroupMember(final String group_id, final String monkey_id){

        try {
            String urlConnect = MonkeyKitSocketService.Companion.getHttpsURL() +"/group/delete";

            JSONObject localJSONObject1 = new JSONObject();
            localJSONObject1.put("group_id", group_id);
            localJSONObject1.put("session_id", monkey_id);

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("data", localJSONObject1.toString());

            aq.auth(handle).ajax(urlConnect, params, JSONObject.class, new AjaxCallback<JSONObject>(){
                @Override
                public void callback(String url, JSONObject response, AjaxStatus status) {

                    MonkeyKitSocketService service = serviceRef.get();
                    if(service==null)
                        return;

                    if (response != null) {
                        try {
                            service.processMessageFromHandler(CBTypes.onRemoveGroupMember, new Object[]{
                                    group_id, monkey_id, response.getJSONObject("data").getString("members"), null});
                        } catch (JSONException e) {
                            e.printStackTrace();
                            service.processMessageFromHandler(CBTypes.onRemoveGroupMember, new Object[]{
                                    group_id, monkey_id, null, e});
                        }
                    }
                    else{
                        service.processMessageFromHandler(CBTypes.onRemoveGroupMember, new Object[]{
                                group_id, monkey_id, null, new Exception("Error code:"+status.getCode()+" -  Error msg:"+status.getMessage())});
                    }
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void addGroupMember(final String new_member, final String group_id){
        try {

            String urlConnect = MonkeyKitSocketService.Companion.getHttpsURL() +"/group/addmember";

            JSONObject localJSONObject1 = new JSONObject();
            localJSONObject1.put("session_id", monkeyID);
            localJSONObject1.put("group_id", group_id);
            localJSONObject1.put("new_member",new_member);

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("data", localJSONObject1.toString());

            System.out.println("MONKEY - Sending api - "+localJSONObject1);

            aq.auth(handle).ajax(urlConnect, params, JSONObject.class, new AjaxCallback<JSONObject>(){
                @Override
                public void callback(String url, JSONObject response, AjaxStatus status) {

                    MonkeyKitSocketService service = serviceRef.get();
                    if(service==null)
                        return;

                    if (response != null) {
                        try {
                            service.processMessageFromHandler(CBTypes.onAddGroupMember, new Object[]{
                                    group_id, new_member, response.getJSONObject("data").getString("members"), null});
                        } catch (JSONException e) {
                            e.printStackTrace();
                            service.processMessageFromHandler(CBTypes.onAddGroupMember, new Object[]{
                                    group_id, new_member, null, e});
                        }
                    }
                    else{
                        service.processMessageFromHandler(CBTypes.onAddGroupMember, new Object[]{
                                group_id, new_member, null, new Exception("Error code:"+status.getCode()+" -  Error msg:"+status.getMessage())});
                    }
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
