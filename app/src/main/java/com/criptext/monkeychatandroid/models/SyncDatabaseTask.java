package com.criptext.monkeychatandroid.models;

import android.os.AsyncTask;

import com.activeandroid.ActiveAndroid;
import com.criptext.comunication.MOKDelete;
import com.criptext.comunication.MOKMessage;
import com.criptext.comunication.MOKNotification;
import com.criptext.http.HttpSync;
import com.criptext.monkeychatandroid.models.conversation.ConversationItem;
import com.criptext.monkeychatandroid.models.message.MessageItem;
import com.criptext.monkeykitui.recycler.MonkeyItem;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * Created by gesuwall on 10/7/16.
 */
public class SyncDatabaseTask extends AsyncTask<Void, Void, Integer> {

    private HttpSync.SyncData syncData;
    private String monkeyId, pathToMessages;
    private Runnable runnable;
    private DatabaseHandler db = new DatabaseHandler();

    public SyncDatabaseTask(HttpSync.SyncData syncData, Runnable runnable) {
        this.syncData = syncData;
        this.runnable = runnable;
    }

    public SyncDatabaseTask(HttpSync.SyncData syncData, Runnable runnable, String monkeyId,
                            String pathToMessages) {
        this.syncData = syncData;
        this.runnable = runnable;
        this.monkeyId = monkeyId;
        this.pathToMessages = pathToMessages;
    }

    private void addNewMessages(List<MOKMessage> newMessages) {
        Iterator<MOKMessage> iterator = newMessages.iterator();
        while (iterator.hasNext()) {
            MOKMessage newMessage = iterator.next();
            if(!db.existMessage(newMessage.getMessage_id())) {
                if(db.existMessage(newMessage.getOldId())){
                    db.updateMessageStatus(newMessage.getMessage_id(), newMessage.getOldId(), MonkeyItem.DeliveryStatus.delivered);
                }else {
                    MessageItem newItem = db.createMessage(newMessage, pathToMessages, monkeyId);
                    newItem.save();
                }
            } else iterator.remove();
        }
    }

    private void deleteMessages(List<MOKDelete> newDeletes) {
        Iterator<MOKDelete> iterator = newDeletes.iterator();
        while (iterator.hasNext()) {
            MOKDelete newDelete = iterator.next();
            MessageItem deletedItem = db.getMessageById(newDelete.getMessageId());
            if(deletedItem != null)
                deletedItem.delete();
            else iterator.remove();
        }
    }

    private void syncNotifications(HttpSync.SyncData syncData) {
        List<MOKNotification> notifications = syncData.getNotifications();
        Iterator<MOKNotification> notificationIterator = notifications.iterator();
        HashSet<String> missingConversations = syncData.getMissingConversations();
        while (notificationIterator.hasNext()) {
            MOKNotification not = notificationIterator.next();
            if (not.getProps().has("monkey_action")) {
                int type = not.getProps().get("monkey_action").getAsInt();
                new NotificationMessages().parseGroupNotifications(missingConversations, not, type);
            }
        }
    }

    @Override
    protected Integer doInBackground(Void... params) {
        HashMap<String, List<MOKMessage>> newMessagesMap = syncData.getNewMessages();
        HashMap<String, List<MOKDelete>> newDeletesMap = syncData.getDeletes();
        Iterator<String> iterator = syncData.getConversationsToUpdate().iterator();

        ActiveAndroid.beginTransaction();
        try {

            while (iterator.hasNext()) {
                String convId = iterator.next();
                List<MOKMessage> newMessages = newMessagesMap.get(convId);
                List<MOKDelete> deletes = newDeletesMap.get(convId);
                boolean removed = false;

                if (newMessages != null) {
                    addNewMessages(newMessages);
                    if (newMessages.isEmpty()) {
                        newMessagesMap.remove(convId);
                        removed = true;
                    }
                }

                if (deletes != null) {
                    deleteMessages(deletes);
                    if (deletes.isEmpty()) {
                        deletes.remove(convId);
                        removed = true;
                    }
                }

                if(removed && !newMessagesMap.containsKey(convId) && !newDeletesMap.containsKey(convId))
                    iterator.remove();
                else
                    db.syncConversation(convId, syncData);
            }

            syncNotifications(syncData);

            ActiveAndroid.setTransactionSuccessful();
            return 1;
        } finally {
            ActiveAndroid.endTransaction();
        }
    }

    @Override
    protected void onCancelled(){
        runnable = null;
    }

    @Override
    protected void onPostExecute(Integer result) {
        if (runnable != null)
            runnable.run();
    }
}
