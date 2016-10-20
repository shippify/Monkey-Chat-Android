package com.criptext.monkeychatandroid.models;

import android.os.AsyncTask;

import java.util.HashMap;
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

    public void getConversationPage(final GetConversationPageTask.OnQueryReturnedListener listener,
                                    int rowsPerPage, int pageNumber) {
        final GetConversationPageTask newTask = new GetConversationPageTask(rowsPerPage, pageNumber);
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
}
