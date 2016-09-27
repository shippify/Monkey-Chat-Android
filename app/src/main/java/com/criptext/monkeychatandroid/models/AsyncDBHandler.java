package com.criptext.monkeychatandroid.models;

import android.os.AsyncTask;

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

    public void getMessageById(final FindMessageTask.OnQueryReturnedListener listener, String... params){
        final FindMessageTask newTask = new FindMessageTask();
        pendingTasks.add(newTask);
        FindMessageTask.OnQueryReturnedListener trueListener = new FindMessageTask.OnQueryReturnedListener() {
            @Override
            public void onQueryReturned(MessageItem result) {
                pendingTasks.remove(newTask);
                if(listener != null){
                    listener.onQueryReturned(result);
                }
            }
        };
        newTask.onQueryReturnedListener = trueListener;
        newTask.execute(params);
    }

    public void getMessagePage(final GetMessagePageTask.OnQueryReturnedListener listener,
                               String myMonkeyId, String conversationId, int rowsPerPage, int pageNumber){
        final GetMessagePageTask newTask = new GetMessagePageTask(myMonkeyId, conversationId,
                rowsPerPage, pageNumber);
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
        newTask.execute();

    }
}