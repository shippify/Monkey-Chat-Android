package com.criptext.monkeychatandroid.models;

import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.util.Log;

import com.activeandroid.ActiveAndroid;
import com.criptext.monkeykitui.conversation.MonkeyConversation;

import java.util.List;

/**
 * Stores a new conversation in background, querying the database for existing unread messages
 * to set data such as totalNewMessages and lastMessageText. This assumes that the local DB has
 * all the possible unread messages of the conversation. This task should be executed after the
 * onUserInfo callback, which should have been triggered after receiving one or more messages
 * from an unexistant conversation.
 * Created by Gabriel on 10/20/16.
 */
public class StoreNewConversationTask extends AsyncTask<ConversationItem, Void, ConversationItem> {
    public OnQueryReturnedListener onQueryReturnedListener = null;

    public StoreNewConversationTask(){
    }

    public StoreNewConversationTask(OnQueryReturnedListener listener){
        onQueryReturnedListener = listener;
    }

    @Override
    protected ConversationItem doInBackground(ConversationItem... params) {
        ConversationItem conv = params[0];
        ActiveAndroid.beginTransaction();
        try {
            List<MessageItem> messages = DatabaseHandler.getAllMessagesSince(conv.getConvId(), conv.lastOpen);
            conv.setTotalNewMessage(messages.size());
            if (!messages.isEmpty()) {
                MessageItem lastMessage = messages.get(messages.size() - 1);
                conv.setSecondaryText(DatabaseHandler.getSecondaryTextByMessageType(lastMessage, conv.isGroup()));
                conv.setStatus(MonkeyConversation.ConversationStatus.receivedMessage.ordinal());
            }
            conv.save();
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
