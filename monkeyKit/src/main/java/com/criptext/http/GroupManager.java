package com.criptext.http;

import android.util.Log;

import com.androidquery.callback.AjaxCallback;
import com.criptext.MonkeyKitSocketService;
import com.criptext.comunication.CBTypes;
import com.criptext.security.AESUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by gesuwall on 6/23/16.
 */
public class GroupManager extends AQueryHttp {

    public GroupManager(MonkeyKitSocketService service, AESUtil aesUtil) {
        super(service, aesUtil);
    }

    /**
     * Create a group asynchronously. If there are not errors with the server, the answers arrive via onCreateGroupOK()
     * from MonkeyKitDelegate, otherwise arrive via onCreateGroupError()
     * @param members String with the sessionIDs of the members of the group.
     * @param groupname String with the group name
     * @param group_id String with the group id (optional)
     */
    public void createGroup(String members, String groupname, String group_id){
        try {

            String urlconnect = MonkeyKitSocketService.Companion.getHttpsURL() +"/group/create";
            AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>();

            JSONObject localJSONObjectInfo = new JSONObject();
            localJSONObjectInfo.put("name", groupname);

            JSONObject localJSONObject1 = new JSONObject();
            localJSONObject1.put("members",members);
            localJSONObject1.put("info",localJSONObjectInfo);
            localJSONObject1.put("session_id", monkeyID);
            if(group_id!=null)
                localJSONObject1.put("group_id", group_id);

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("data", localJSONObject1.toString());

            cb.url(urlconnect).type(JSONObject.class).weakHandler(this, "onCreateGroup");
            cb.params(params);

            aq.auth(handle).ajax(cb);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onCreateGroup(String url, final JSONObject jo, com.androidquery.callback.AjaxStatus status) {
        final MonkeyKitSocketService service = serviceRef.get();
        if(jo!=null){
            try {
                JSONObject json = jo.getJSONObject("data");
                service.processMessageFromHandler(CBTypes.onCreateGroupOK, new Object[]{json.getString("group_id")});
            }
            catch(Exception e){
                service.processMessageFromHandler(CBTypes.onCreateGroupError, new Object[]{""});
                e.printStackTrace();
            }
        }
        else
            service.processMessageFromHandler(CBTypes.onCreateGroupError, new Object[]{status.getCode()+" - "+status.getMessage()});
    }

    /************************************************************************/

    /**
     * Delete a group asynchronously int the Monkey server. If there are not errors with the server, the answers arrive via
     * onDeleteGroupOK() from MonkeyKitDelegate, otherwise arrive via onDeleteGroupError()
     * @param groupID ID of the group
     */
    public void deleteGroup(String groupID){
        try {

            String urlconnect = MonkeyKitSocketService.Companion.getHttpsURL() +"/group/delete";
            AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>();

            JSONObject localJSONObject1 = new JSONObject();
            localJSONObject1.put("group_id",groupID);
            localJSONObject1.put("session_id", monkeyID);

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("data", localJSONObject1.toString());

            cb.url(urlconnect).type(JSONObject.class).weakHandler(this, "onDeleteGroup");
            cb.params(params);

            aq.auth(handle).ajax(cb);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onDeleteGroup(String url, final JSONObject jo, com.androidquery.callback.AjaxStatus status) {

        final MonkeyKitSocketService service = serviceRef.get();
        if(jo!=null && service != null){
            try {
                System.out.println("MONKEY - onDeleteGroup: " + jo.toString());
                JSONObject json = jo.getJSONObject("data");
                service.processMessageFromHandler(CBTypes.onDeleteGroupOK, new Object[]{json.getString("group_id")});
            }
            catch(Exception e){
                service.processMessageFromHandler(CBTypes.onDeleteGroupError, new Object[]{""});
                e.printStackTrace();
            }
        }
        else if(service != null)
            service.processMessageFromHandler(CBTypes.onDeleteGroupError, new Object[]{status.getCode()+" - "+status.getMessage()});
    }

    /************************************************************************/

    /**
     * Add a member to a group asynchronously. If there are not errors with the server, the answers arrive via
     * onAddMemberToGroupOK() from MonkeyKitDelegate, otherwise arrive via onAddMemberToGroupError()
     * @param new_member Session ID of the new member
     * @param groupID ID of the group
     */
    public void addMemberToGroup(String new_member, String groupID){
        try {

            String urlconnect = MonkeyKitSocketService.Companion.getHttpsURL() +"/group/addmember";
            AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>();

            JSONObject localJSONObject1 = new JSONObject();
            localJSONObject1.put("session_id",monkeyID);
            localJSONObject1.put("group_id",groupID);
            localJSONObject1.put("new_member",new_member);

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("data", localJSONObject1.toString());

            System.out.println("MONKEY - Sending api - "+localJSONObject1);

            cb.url(urlconnect).type(JSONObject.class).weakHandler(this, "onAddMemberToGroup");
            cb.params(params);

            aq.auth(handle).ajax(cb);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onAddMemberToGroup(String url, final JSONObject jo, com.androidquery.callback.AjaxStatus status) {

        final MonkeyKitSocketService service = serviceRef.get();
        if(jo!=null && service != null){
            try {
                System.out.println("MONKEY - onAddMemberToGroup - " + jo.toString());
                //JSONObject json = jo.getJSONObject("data");
                service.processMessageFromHandler(CBTypes.onAddMemberToGroupOK, new Object[]{});
            }
            catch(Exception e){
                service.processMessageFromHandler(CBTypes.onAddMemberToGroupError, new Object[]{""});
                e.printStackTrace();
            }
        }
        else if(service != null)
            service.processMessageFromHandler(CBTypes.onAddMemberToGroupError, new Object[]{status.getCode()+" - "+status.getMessage()});
    }

    /************************************************************************/

    public void getGroupInfo(String groupID){
        try {
            Log.d("MonkeyKit", "Info for " + groupID + " plz.");
            String urlconnect = MonkeyKitSocketService.Companion.getHttpsURL() +"/group/info";
            AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>();

            JSONObject localJSONObject1 = new JSONObject();
            localJSONObject1.put("group_id",groupID);

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("data", localJSONObject1.toString());

            System.out.println("MONKEY - Sending api - " + localJSONObject1);

            cb.url(urlconnect).type(JSONObject.class).weakHandler(this, "onGetGroupInfo");
            cb.params(params);

            System.out.println("handle:" + handle);
            aq.auth(handle).ajax(cb);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onGetGroupInfo(String url, final JSONObject jo, com.androidquery.callback.AjaxStatus status) {

        final MonkeyKitSocketService service = serviceRef.get();
        if(jo!=null && service != null){
            try {
                JSONObject json = jo.getJSONObject("data");
                System.out.println("MONKEY - onGetGroupInfo - " + json);

                JsonParser jsonParser = new JsonParser();
                JsonObject gsonObject = (JsonObject)jsonParser.parse(json.toString());
                service.processMessageFromHandler(CBTypes.onGetGroupInfoOK, new Object[]{gsonObject});
            }
            catch(Exception e){
                service.processMessageFromHandler(CBTypes.onGetGroupInfoError, new Object[]{""});
                e.printStackTrace();
            }
        }
        else if(service != null)
            service.processMessageFromHandler(CBTypes.onGetGroupInfoError, new Object[]{status.getCode() + " - " + status.getMessage()});
    }

}
