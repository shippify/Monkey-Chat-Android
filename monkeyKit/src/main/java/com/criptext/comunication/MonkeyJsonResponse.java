package com.criptext.comunication;

import com.androidquery.callback.AjaxStatus;

import org.json.JSONObject;

/**
 * Created by daniel on 7/05/16.
 */
public abstract class MonkeyJsonResponse {

    public abstract void OnSuccess(JSONObject jsonObject);
    public abstract void OnError(AjaxStatus status);

}
