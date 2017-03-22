package com.criptext.monkeychatandroid.models.conversation.task;

import android.os.AsyncTask;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.criptext.comunication.MOKUser;
import com.criptext.monkeychatandroid.models.DatabaseHandler;
import com.criptext.monkeychatandroid.models.conversation.ConversationItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by hirobreak on 20/01/17.
 */
public class UpdateConversationsInfoTask extends AsyncTask<ArrayList<MOKUser>, ArrayList<ConversationItem>, ArrayList<ConversationItem>> {
    public OnQueryReturnedListener onQueryReturnedListener = null;

    public UpdateConversationsInfoTask(){

    }

    public UpdateConversationsInfoTask(OnQueryReturnedListener listener){
        onQueryReturnedListener = listener;
    }

    @Override
    protected ArrayList<ConversationItem> doInBackground(ArrayList<MOKUser>... params) {
        ArrayList<ConversationItem> conversationsUpdated = new ArrayList<>();
        ActiveAndroid.beginTransaction();
        try {

            ArrayList<MOKUser> mokUsers = params[0];
            for (MOKUser user : mokUsers) {
                ConversationItem conversation = new DatabaseHandler().getConversationById(user.getMonkeyId());
                if(conversation == null){
                    continue;
                }
                if(conversation.getName().equals("Unknown") && user.getInfo().has("name")){
                    conversation.setName(user.getInfo().get("name").getAsString());
                    conversation.save();
                    conversationsUpdated.add(conversation);
                }
            }

            ActiveAndroid.setTransactionSuccessful();
        } finally {
            ActiveAndroid.endTransaction();
        }
        return conversationsUpdated;
    }

    @Override
    protected void onCancelled() {
        onQueryReturnedListener = null;
    }

    @Override
    protected void onPostExecute(ArrayList<ConversationItem> conversationsUpdated) {
        if(onQueryReturnedListener != null)
            onQueryReturnedListener.onQueryReturned(conversationsUpdated);
    }

    public interface OnQueryReturnedListener {
        void onQueryReturned(ArrayList<ConversationItem> conversationsUpdated);
    }
}
