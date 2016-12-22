package com.criptext.monkeychatandroid.models;

import android.os.AsyncTask;
import android.util.Log;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.criptext.monkeykitui.conversation.holder.ConversationTransaction;

import java.util.HashMap;
import java.util.List;

/**
 * Created by gesuwall on 10/6/16.
 */
public class UpdateConversationsTask extends AsyncTask<String, Integer, List<ConversationItem>> {

    public OnQueryReturnedListener onQueryReturnedListener = null;
    private ConversationTransaction transaction;

    public UpdateConversationsTask(ConversationTransaction transaction){
        this.transaction = transaction;
    }

    public UpdateConversationsTask(ConversationTransaction transaction, OnQueryReturnedListener listener){
        this(transaction);
        onQueryReturnedListener = listener;
    }

    @Override
    protected void onCancelled() {
        onQueryReturnedListener = null;
    }

    @Override
    protected List<ConversationItem> doInBackground(String... params) {
        ActiveAndroid.beginTransaction();
        List<ConversationItem> results = null;
        try {

            From from = new Select().from(ConversationItem.class);
            String[] args = new String[params.length];
            StringBuilder queryBuilder = new StringBuilder();
            int index = 0;
            for (String key : params) {
                queryBuilder.append("idConv = ? OR ");
                args[index++] = key;
            }

            String query = queryBuilder.substring(0, queryBuilder.length() - 4);
            from.where(query, (Object [])args);


            List<ConversationItem> listToUpdate = from.execute();
            for(ConversationItem conversation: listToUpdate) {
                transaction.updateConversation(conversation);
                conversation.save();
            }
            results = listToUpdate;
            ActiveAndroid.setTransactionSuccessful();
        } finally {
            ActiveAndroid.endTransaction();
        }
        return results;
    }

    @Override
    protected void onPostExecute(List<ConversationItem> conversationItems) {
        if(onQueryReturnedListener != null)
            onQueryReturnedListener.onQueryReturned(conversationItems);
    }

    public interface OnQueryReturnedListener {
        void onQueryReturned(List<ConversationItem> results);
    }
}
