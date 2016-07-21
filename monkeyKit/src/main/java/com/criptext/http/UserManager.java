package com.criptext.http;

import com.androidquery.AQuery;
import com.androidquery.auth.BasicHandle;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.criptext.MonkeyKitSocketService;
import com.criptext.comunication.MonkeyJsonResponse;
import com.criptext.security.AESUtil;

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

    /************************************************************************/

    public void updateUserObject(String monkeyId, JSONObject userInfo, final Runnable runnable){

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
                    runnable.run();
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    /**
     * Get all conversation of a user using the monkey ID.
     * @param monkeyid monkeyid ID of the user.
     * @param monkeyJsonResponse callback to receive the response.
     */

    public void getConversations(String monkeyid, final MonkeyJsonResponse monkeyJsonResponse){

        String urlconnect = MonkeyKitSocketService.Companion.getHttpsURL()+"/user/"+monkeyid+"/conversations";
        AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>();

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

}
