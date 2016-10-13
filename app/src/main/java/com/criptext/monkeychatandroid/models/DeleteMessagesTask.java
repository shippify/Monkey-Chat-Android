package com.criptext.monkeychatandroid.models;

import android.os.AsyncTask;
import android.util.Log;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.criptext.comunication.MOKDelete;
import com.criptext.comunication.MOKMessage;
import com.criptext.monkeykitui.conversation.MonkeyConversation;
import com.criptext.monkeykitui.conversation.holder.ConversationTransaction;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by gesuwall on 10/7/16.
 */
public class DeleteMessagesTask extends AsyncTask<Void, Void, HashMap<MonkeyConversation, ConversationTransaction>> {

    private HashMap<String, List<MOKMessage>> newMessagesMap;
    private HashMap<String, List<MOKDelete>> deletesMap;
    public OnQueryReturnedListener onQueryReturnedListener = null;

    public DeleteMessagesTask(HashMap<String, List<MOKMessage>> newMessagesMap,
                              HashMap<String, List<MOKDelete>> deletesMap) {
        this.newMessagesMap = newMessagesMap;
        this.deletesMap = deletesMap;
    }

    private From newDeleteQuery(List<MOKDelete> deleteList) {
        String queryStr = "";
        String[] array = new String[deleteList.size()];

        int iterations = 0;
        for (MOKDelete delete : deleteList) {
            queryStr += "messageId = ? OR ";
            array[iterations++] = delete.getMessageId();

        }
        queryStr = queryStr.substring(0, queryStr.length() - 4);
        return new Select().from(MessageItem.class).where(queryStr, array);
    }

    private MOKDelete getLastDelete(List<MOKDelete> deletes, String id) {
        for(MOKDelete delete : deletes)
            if(delete.getMessageId().equals(id))
                return delete;
        return null;
    }

    private MessageItem getLastMessage(String conversationId){
        return new Select().from(MessageItem.class)
                    .where("conversationId = ?", conversationId)
                    .orderBy("timestampOrder DESC")
                    .executeSingle();
    }

    private int getNewMessagesChange(int newMessages, String conversationId, List<MOKDelete> deletes) {
        List<MessageItem> newMessagesList = new Select().from(MessageItem.class)
                        .where("conversationId = ?", conversationId)
                        .orderBy("timestampOrder DESC")
                        .limit(newMessages)
                        .execute();
        int change = 0;
        for(MOKDelete delete : deletes) {
            for(MessageItem newMessage: newMessagesList) {
                if (delete.getMessageId().equals(newMessage.getMessageId())) {
                    change++;
                    Log.d("DeleteMessages", "unsend " + newMessage.getMessageText());
                    if(newMessages == change)
                        return change * (-1);
                }
            }

        }

        return change * (-1);
    }

    private ConversationTransaction copyTransaction(final ConversationItem itemToCopy) {
        final String secondaryTextCopy = itemToCopy.getSecondaryText();
        final int statusCopy = itemToCopy.getStatus();
        final long datetimeCopy = itemToCopy.getDatetime();
        final int newMessagesCopy  = itemToCopy.getTotalNewMessages();
        return new ConversationTransaction() {
            @Override
            public void updateConversation(@NotNull MonkeyConversation conversation) {
                ConversationItem convItem = (ConversationItem) conversation;
                convItem.setSecondaryText(secondaryTextCopy);
                convItem.setStatus(statusCopy);
                convItem.setDatetime(datetimeCopy);
                convItem.setTotalNewMessage(newMessagesCopy);
            }
        };
    }

    @Override
    protected HashMap<MonkeyConversation, ConversationTransaction> doInBackground(Void... params) {
        Iterator<Map.Entry<String, List<MOKDelete>>> iterator = deletesMap.entrySet().iterator();
        HashMap<MonkeyConversation, ConversationTransaction> result = new HashMap<>();
        ActiveAndroid.beginTransaction();
        try {
            while (iterator.hasNext()) {
                Map.Entry<String, List<MOKDelete>> entry = iterator.next();
                String conversationId = entry.getKey();
                List<MOKDelete> deleteList = entry.getValue();
                MessageItem lastMessage = null;
                ConversationItem conversation = DatabaseHandler.getConversationById(conversationId);

                int newMessagesChange = 0;

                int existingUnreadMessages = 0;
                if(conversation != null)
                    existingUnreadMessages = conversation.getTotalNewMessages() ;
                Log.d("DeleteMessages", "unread messages before deletes " + existingUnreadMessages);

                if(existingUnreadMessages > 0) {
                    newMessagesChange = getNewMessagesChange(existingUnreadMessages, conversationId,
                            deleteList);
                }

                Log.d("DeleteMessages", "unread to delete " + newMessagesChange);
                //Since newMessages has the latest messages, any deleted message can't be the last message
                boolean lastMessageCouldHaveDeleted = !newMessagesMap.containsKey(conversationId);
                boolean canUpdateConversation = lastMessageCouldHaveDeleted || newMessagesChange < 0;

                if(lastMessageCouldHaveDeleted) {
                    lastMessage = getLastMessage(conversationId);
                }

                List<MessageItem> deletedMessages = newDeleteQuery(deleteList).execute();
                boolean deletedMessageFromDB = false;
                for(MessageItem del : deletedMessages) {
                    del.delete();
                    deletedMessageFromDB = true;
                }

                if(deletedMessageFromDB && canUpdateConversation) {
                    MessageItem newLastMessage = getLastMessage(conversationId);
                    if(newMessagesChange < 0) {
                        ConversationTransaction transaction = DatabaseHandler.
                                newDeletedMsgsTransaction(newLastMessage, newMessagesChange);
                        transaction.updateConversation(conversation);
                        if(conversation != null) {
                            result.put(conversation, copyTransaction(conversation));
                            transaction.updateConversation(conversation);
                            conversation.save();
                        }
                    }
                }
            }
            ActiveAndroid.setTransactionSuccessful();
            return result;
        } finally {
            ActiveAndroid.endTransaction();
        }
    }

    @Override
    protected void onCancelled(){
        onQueryReturnedListener = null;
    }

    @Override
    protected void onPostExecute(HashMap<MonkeyConversation, ConversationTransaction> result) {
        if(onQueryReturnedListener != null) onQueryReturnedListener.onQueryReturned(result);
    }

    public interface OnQueryReturnedListener {
        void onQueryReturned(HashMap<MonkeyConversation, ConversationTransaction> transactions);
    }
}
