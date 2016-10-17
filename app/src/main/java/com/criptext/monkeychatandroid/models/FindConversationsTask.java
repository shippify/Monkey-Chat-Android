package com.criptext.monkeychatandroid.models;

import android.os.AsyncTask;

import java.util.HashMap;

/**
 * Created by gesuwall on 10/14/16.
 */
public class FindConversationsTask extends AsyncTask<String, Void, HashMap<String, ConversationItem>> {
    public OnQueryReturnedListener onQueryReturnedListener = null;

    public FindConversationsTask(){
    }

    public FindConversationsTask(OnQueryReturnedListener listener){
        onQueryReturnedListener = listener;
    }

    @Override
    protected HashMap<String, ConversationItem> doInBackground(String... params) {
        return DatabaseHandler.getConversationsById(params);
    }

    @Override
    protected void onCancelled() {
        onQueryReturnedListener = null;
    }

    @Override
    protected void onPostExecute(HashMap<String, ConversationItem> result) {
        if(onQueryReturnedListener != null)
            onQueryReturnedListener.onQueryReturned(result);
    }

    public interface OnQueryReturnedListener {
        void onQueryReturned(HashMap<String, ConversationItem> result);
    }
}
