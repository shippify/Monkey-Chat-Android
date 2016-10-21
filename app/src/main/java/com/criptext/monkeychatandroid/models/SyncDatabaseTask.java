package com.criptext.monkeychatandroid.models;

import android.os.AsyncTask;

import com.activeandroid.ActiveAndroid;
import com.criptext.comunication.MOKDelete;
import com.criptext.comunication.MOKMessage;
import com.criptext.http.HttpSync;
import com.criptext.monkeykitui.recycler.MonkeyItem;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Created by gesuwall on 10/7/16.
 */
public class SyncDatabaseTask extends AsyncTask<Void, Void, Integer> {

    private HttpSync.SyncData syncData;
    private String monkeyId, pathToMessages;
    private Runnable runnable;

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
            if(!DatabaseHandler.existMessage(newMessage.getMessage_id())) {
                if(DatabaseHandler.existMessage(newMessage.getOldId())){
                    DatabaseHandler.updateMessageStatus(newMessage.getMessage_id(), newMessage.getOldId(), MonkeyItem.DeliveryStatus.delivered);
                }else {
                    MessageItem newItem = DatabaseHandler.createMessage(newMessage, pathToMessages, monkeyId);
                    newItem.save();
                }
            } else iterator.remove();
        }
    }

    private void deleteMessages(List<MOKDelete> newDeletes) {
        Iterator<MOKDelete> iterator = newDeletes.iterator();
        while (iterator.hasNext()) {
            MOKDelete newDelete = iterator.next();
            MessageItem deletedItem = DatabaseHandler.getMessageById(newDelete.getMessageId());
            if(deletedItem != null)
                deletedItem.delete();
            else iterator.remove();
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
                    DatabaseHandler.syncConversation(convId);
            }

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
        runnable.run();
    }
}
