package com.criptext.monkeychatandroid.models.conversation;

import android.os.AsyncTask;

import com.activeandroid.ActiveAndroid;
import com.criptext.monkeychatandroid.models.DatabaseHandler;
import com.criptext.monkeykitui.conversation.MonkeyConversation;
import com.criptext.monkeykitui.conversation.holder.ConversationTransaction;

/**
 * Created by gesuwall on 3/8/17.
 */
public class SyncMissingConversationTask extends AsyncTask<String, Void, ConversationItem> {
    public OnQueryReturnedListener onQueryReturnedListener = null;
    private ConversationTransaction transaction;

    public SyncMissingConversationTask(ConversationTransaction transaction){
        this.transaction = transaction;
    }

    @Override
    protected ConversationItem doInBackground(String... params) {
        String groupid = params[0];

        final DatabaseHandler db = new DatabaseHandler();
        ConversationItem conv = db.getConversationById(params[0]);
        if (conv == null)
            conv = new ConversationItem(groupid, "Uknown ", System.currentTimeMillis(),
                    "Write to this conversation", 0, false, "", "",
                    MonkeyConversation.ConversationStatus.empty.ordinal());

        ActiveAndroid.beginTransaction();
        try {
            transaction.updateConversation(conv);
            db.syncConversation(conv);
            ActiveAndroid.setTransactionSuccessful();
            return conv;
        } finally {
            ActiveAndroid.endTransaction();
        }
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
