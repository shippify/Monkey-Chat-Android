package com.criptext.monkeychatandroid.models;

import android.os.AsyncTask;

import java.util.List;

/**
 * Created by gesuwall on 10/13/16.
 */
public class GetConversationPageTask extends AsyncTask<Void, Void, List<ConversationItem>> {
    private int conversationsToLoad;
    private int loadedConversations;

    public OnQueryReturnedListener onQueryReturnedListener = null;

    public GetConversationPageTask(int conversationsToLoad, int loadedConversations){
        this.conversationsToLoad = conversationsToLoad;
        this.loadedConversations = loadedConversations;
    }
    @Override
    protected List<ConversationItem> doInBackground(Void... params) {
        return DatabaseHandler.getConversations(conversationsToLoad, loadedConversations);
    }

    @Override
    protected void onCancelled() {
        onQueryReturnedListener = null;
    }

    @Override
    protected void onPostExecute(List<ConversationItem> conversationItems) {
        if(onQueryReturnedListener != null)
            onQueryReturnedListener.onQueryReturned(conversationItems);
    }

    public interface OnQueryReturnedListener {
        void onQueryReturned(List<ConversationItem> conversationPage);
    }

}
