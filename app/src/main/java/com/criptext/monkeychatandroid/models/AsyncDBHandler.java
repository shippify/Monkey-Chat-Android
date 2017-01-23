package com.criptext.monkeychatandroid.models;

import android.os.AsyncTask;

import com.activeandroid.Model;
import com.criptext.comunication.MOKUser;
import com.criptext.monkeychatandroid.models.conversation.ConversationItem;
import com.criptext.monkeychatandroid.models.conversation.task.FindConversationTask;
import com.criptext.monkeychatandroid.models.conversation.task.FindConversationsTask;
import com.criptext.monkeychatandroid.models.conversation.task.GetConversationPageTask;
import com.criptext.monkeychatandroid.models.conversation.task.StoreNewConversationTask;
import com.criptext.monkeychatandroid.models.conversation.task.UpdateConversationsInfoTask;
import com.criptext.monkeychatandroid.models.conversation.task.UpdateConversationsTask;
import com.criptext.monkeychatandroid.models.message.MessageItem;
import com.criptext.monkeychatandroid.models.message.task.FindMessageTask;
import com.criptext.monkeychatandroid.models.message.task.GetMessagePageTask;
import com.criptext.monkeychatandroid.models.message.task.UpdateMessageDeliveryStatusTask;
import com.criptext.monkeykitui.conversation.holder.ConversationTransaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by gesuwall on 9/20/16.
 */
public class AsyncDBHandler {
    LinkedList<AsyncTask> pendingTasks;

    public AsyncDBHandler(){
        pendingTasks = new LinkedList<>();
    }

    public void cancelAll(){
        for(AsyncTask task: pendingTasks) {
           task.cancel(true);
        }
    }
    public void storeNewConversation(final StoreNewConversationTask.OnQueryReturnedListener listener,
                                     ConversationItem params){
        final StoreNewConversationTask newTask = new StoreNewConversationTask();
        pendingTasks.add(newTask);
        StoreNewConversationTask.OnQueryReturnedListener trueListener = new StoreNewConversationTask.OnQueryReturnedListener() {
            @Override
            public void onQueryReturned(ConversationItem result) {
                pendingTasks.remove(newTask);
                if(listener != null){
                    listener.onQueryReturned(result);
                }
            }
        };
        newTask.onQueryReturnedListener = trueListener;
        newTask.execute(params);
    }

    public void storeConversationPage(final SaveModelTask.OnQueryReturnedListener listener, Model[] models) {
        final SaveModelTask newTask = new SaveModelTask();
        pendingTasks.add(newTask);
        SaveModelTask.OnQueryReturnedListener trueListener = new SaveModelTask.OnQueryReturnedListener() {
            @Override
            public void onQueryReturned(Model[] storedModels) {
                pendingTasks.remove(newTask);
                if(listener != null)
                    listener.onQueryReturned(storedModels);
            }
        };
        newTask.onQueryReturnedListener = trueListener;
        newTask.execute(models);
    }
    public void getConversationById(final FindConversationTask.OnQueryReturnedListener listener, String... params){
        final FindConversationTask newTask = new FindConversationTask();
        pendingTasks.add(newTask);
        FindConversationTask.OnQueryReturnedListener trueListener = new FindConversationTask.OnQueryReturnedListener() {
            @Override
            public void onQueryReturned(ConversationItem result) {
                pendingTasks.remove(newTask);
                if(listener != null){
                    listener.onQueryReturned(result);
                }
            }
        };
        newTask.onQueryReturnedListener = trueListener;
        newTask.execute(params);
    }

    public void getConversationsById(final FindConversationsTask.OnQueryReturnedListener listener, String... params) {
        final FindConversationsTask newTask = new FindConversationsTask();
        pendingTasks.add(newTask);
        FindConversationsTask.OnQueryReturnedListener trueListener = new FindConversationsTask.OnQueryReturnedListener() {
            @Override
            public void onQueryReturned(HashMap<String, ConversationItem> result) {
                pendingTasks.remove(newTask);
                if (listener != null) {
                    listener.onQueryReturned(result);
                }
            }
        };
        newTask.onQueryReturnedListener = trueListener;
        newTask.execute(params);
    }

    public void getMessageById(final FindMessageTask.OnQueryReturnedListener listener, String... params) {
        final FindMessageTask newTask = new FindMessageTask();
        pendingTasks.add(newTask);
        FindMessageTask.OnQueryReturnedListener trueListener = new FindMessageTask.OnQueryReturnedListener() {
            @Override
            public void onQueryReturned(MessageItem result) {
                pendingTasks.remove(newTask);
                if (listener != null) {
                    listener.onQueryReturned(result);
                }
            }
        };
        newTask.onQueryReturnedListener = trueListener;
        newTask.execute(params);
    }

    public void updateMessageDeliveryStatus(final UpdateMessageDeliveryStatusTask.OnQueryReturnedListener listener, String... params) {
        final UpdateMessageDeliveryStatusTask newTask = new UpdateMessageDeliveryStatusTask();
        pendingTasks.add(newTask);
        UpdateMessageDeliveryStatusTask.OnQueryReturnedListener trueListener =
                    new UpdateMessageDeliveryStatusTask.OnQueryReturnedListener() {
            @Override
            public void onQueryReturned(MessageItem result) {
                pendingTasks.remove(newTask);
                if (listener != null) {
                    listener.onQueryReturned(result);
                }
            }
        };
        newTask.onQueryReturnedListener = trueListener;
        newTask.execute(params);
    }

    public void getConversationPage(final GetConversationPageTask.OnQueryReturnedListener listener,
                                    int conversationsToLoad, int loadedConversations) {
        final GetConversationPageTask newTask = new GetConversationPageTask(conversationsToLoad, loadedConversations);
        pendingTasks.add(newTask);
        GetConversationPageTask.OnQueryReturnedListener trueListener = new GetConversationPageTask.OnQueryReturnedListener() {
            @Override
            public void onQueryReturned(List<ConversationItem> conversations) {
                pendingTasks.remove(newTask);
                if (listener != null) {
                    listener.onQueryReturned(conversations);
                }
            }
        };
        newTask.onQueryReturnedListener = trueListener;
        newTask.execute();
    }

    public void updateMissingConversationsTask(final UpdateConversationsTask.OnQueryReturnedListener listener,
                                              String[] ids, ConversationTransaction transaction) {
        final UpdateConversationsTask newTask = new UpdateConversationsTask(transaction);
        pendingTasks.add(newTask);
        UpdateConversationsTask.OnQueryReturnedListener trueListener = new UpdateConversationsTask.OnQueryReturnedListener() {
            @Override
            public void onQueryReturned(List<ConversationItem> results) {
                pendingTasks.remove(newTask);
                if(listener != null)
                    listener.onQueryReturned(results);
            }
        };
        newTask.onQueryReturnedListener = trueListener;
        newTask.execute(ids);
    }

    public void getMessagePage(final GetMessagePageTask.OnQueryReturnedListener listener,
                               String conversationId, int rowsPerPage, int pageOffset){
        final GetMessagePageTask newTask = new GetMessagePageTask(conversationId,
                rowsPerPage, pageOffset);
        pendingTasks.add(newTask);
        GetMessagePageTask.OnQueryReturnedListener trueListener = new GetMessagePageTask.OnQueryReturnedListener() {
            @Override
            public void onQueryReturned(List<MessageItem> messagePage) {
                pendingTasks.remove(newTask);
                if(listener != null)
                    listener.onQueryReturned(messagePage);
            }
        };

        newTask.onQueryReturnedListener = trueListener;
        newTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }

    public void updateConversationsInfo(final UpdateConversationsInfoTask.OnQueryReturnedListener listener,
                                        ArrayList<MOKUser>... mokUsers) {
        final UpdateConversationsInfoTask newTask = new UpdateConversationsInfoTask();
        pendingTasks.add(newTask);
        UpdateConversationsInfoTask.OnQueryReturnedListener trueListener = new UpdateConversationsInfoTask.OnQueryReturnedListener() {
            @Override
            public void onQueryReturned(ArrayList<ConversationItem> conversationsUpdated) {
                pendingTasks.remove(newTask);
                if(listener != null)
                    listener.onQueryReturned(conversationsUpdated);
            }
        };
        newTask.onQueryReturnedListener = trueListener;
        newTask.execute(mokUsers);
    }
}
