package com.criptext.monkeychatandroid.models;

import android.os.AsyncTask;

/**
 * Created by gesuwall on 9/20/16.
 */
public class FindConversationTask extends AsyncTask<String, Void, ConversationItem> {
    public OnQueryReturnedListener onQueryReturnedListener = null;

    public FindConversationTask(){
    }

    public FindConversationTask(OnQueryReturnedListener listener){
        onQueryReturnedListener = listener;
    }

    @Override
    protected ConversationItem doInBackground(String... params) {
        for(String s : params){
            ConversationItem item = DatabaseHandler.getConversationById(s);
            if(item != null)
                return item;
        }
        return null;
    }

    @Override
    protected void onPostExecute(ConversationItem conversationItem) {
        if(onQueryReturnedListener != null)
            onQueryReturnedListener.onQueryReturned(conversationItem);
    }

    public interface OnQueryReturnedListener {
        void onQueryReturned(ConversationItem result);
    }
}
