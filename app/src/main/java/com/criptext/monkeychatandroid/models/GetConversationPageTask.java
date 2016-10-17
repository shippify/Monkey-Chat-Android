package com.criptext.monkeychatandroid.models;

import android.os.AsyncTask;

import com.criptext.monkeychatandroid.models.ConversationItem;
import com.criptext.monkeychatandroid.models.DatabaseHandler;

import java.util.List;

/**
 * Created by gesuwall on 10/13/16.
 */
public class GetConversationPageTask extends AsyncTask<Void, Void, List<ConversationItem>> {
    private int rowsPerPage;
    private int pageNumber;

    public OnQueryReturnedListener onQueryReturnedListener = null;

    public GetConversationPageTask(int rowsPerPage, int pageNumber){
        this.rowsPerPage = rowsPerPage;
        this.pageNumber = pageNumber;
    }
    @Override
    protected List<ConversationItem> doInBackground(Void... params) {
        return DatabaseHandler.getConversations(rowsPerPage, pageNumber);
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
