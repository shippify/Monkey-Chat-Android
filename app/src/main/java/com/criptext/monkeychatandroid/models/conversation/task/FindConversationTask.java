package com.criptext.monkeychatandroid.models.conversation.task;

import android.os.AsyncTask;

import com.criptext.monkeychatandroid.models.DatabaseHandler;
import com.criptext.monkeychatandroid.models.conversation.ConversationItem;

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
            ConversationItem item = new DatabaseHandler().getConversationById(s);
            if(item != null)
                return item;
        }
        return null;
    }

    @Override
    protected void onCancelled() {
        onQueryReturnedListener = null;
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
