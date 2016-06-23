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
     * Crea un nuevo chat de grupo de forma asincrona. Si no hay errores en el servidor la respuesta
     * llegara en onCreateGroupOK() del MonkeyKitDelegate, De lo contrario se ejecuta onCreateGroupError()
     * @param members String con los sessionID de los miembros del grupo separados por comas.
     * @param groupname String con el nombre del grupo
     */
    public void createGroup(String members, String groupname){
        try {

            String urlconnect = MonkeyKitSocketService.Companion.getHttpsURL() +"/group/create";
            AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>();

            JSONObject localJSONObjectInfo = new JSONObject();
            localJSONObjectInfo.put("name", groupname);

            JSONObject localJSONObject1 = new JSONObject();
            localJSONObject1.put("members",members);
            localJSONObject1.put("info",localJSONObjectInfo);
            localJSONObject1.put("session_id", monkeyID);

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
                service.executeInDelegate(CBTypes.onCreateGroupOK, new Object[]{json.getString("group_id")});
            }
            catch(Exception e){
                service.executeInDelegate(CBTypes.onCreateGroupError, new Object[]{""});
                e.printStackTrace();
            }
        }
        else
            service.executeInDelegate(CBTypes.onCreateGroupError, new Object[]{status.getCode()+" - "+status.getMessage()});
    }

    /************************************************************************/

    /**
     * Elimina un grupo de manera asincrona en el servidor de MonkeyKit. Si no hay errores en el servidor la respuesta
     * llegara en onDeleteGroupOK() del MonkeyKitDelegate, De lo contrario se ejecuta onDeleteGroupError()
     * @param groupID ID del grupo
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
                service.executeInDelegate(CBTypes.onDeleteGroupOK, new Object[]{json.getString("group_id")});
            }
            catch(Exception e){
                service.executeInDelegate(CBTypes.onDeleteGroupError, new Object[]{""});
                e.printStackTrace();
            }
        }
        else if(service != null)
            service.executeInDelegate(CBTypes.onDeleteGroupError, new Object[]{status.getCode()+" - "+status.getMessage()});
    }

    /************************************************************************/

    /**
     * Agrega un miembro a un grupo de manera asincrona. Si no hay errores en el servidor la respuesta
     * llegara en onAddMemberToGroupOK() del MonkeyKitDelegate, De lo contrario se ejecuta onAddMemberToGroupError()
     * @param new_member Session ID del nuevo miembro del grupo
     * @param groupID ID del grupo
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
                service.executeInDelegate(CBTypes.onAddMemberToGroupOK, new Object[]{});
            }
            catch(Exception e){
                service.executeInDelegate(CBTypes.onAddMemberToGroupError, new Object[]{""});
                e.printStackTrace();
            }
        }
        else if(service != null)
            service.executeInDelegate(CBTypes.onAddMemberToGroupError, new Object[]{status.getCode()+" - "+status.getMessage()});
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
                service.executeInDelegate(CBTypes.onGetGroupInfoOK, new Object[]{gsonObject});
            }
            catch(Exception e){
                service.executeInDelegate(CBTypes.onGetGroupInfoError, new Object[]{""});
                e.printStackTrace();
            }
        }
        else if(service != null)
            service.executeInDelegate(CBTypes.onGetGroupInfoError, new Object[]{status.getCode() + " - " + status.getMessage()});
    }

}
