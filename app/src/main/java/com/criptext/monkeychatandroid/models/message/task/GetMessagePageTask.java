package com.criptext.monkeychatandroid.models.message.task;

import android.os.AsyncTask;

import com.criptext.monkeychatandroid.models.DatabaseHandler;
import com.criptext.monkeychatandroid.models.message.MessageItem;

import java.util.List;

/**
 * Created by gesuwall on 9/20/16.
 */
public class GetMessagePageTask extends AsyncTask<Void, Void, List<MessageItem>>{
    private String conversationId;
    private int rowsPerPage;
    private int pageOffset;

    public OnQueryReturnedListener onQueryReturnedListener = null;

    public GetMessagePageTask(String conversationId, int rowsPerPage, int pageOffset){
        this.conversationId = conversationId;
        this.rowsPerPage = rowsPerPage;
        this.pageOffset = pageOffset;
    }
    @Override
    protected List<MessageItem> doInBackground(Void... params) {
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return DatabaseHandler.getMessages(conversationId, rowsPerPage, pageOffset);
    }

    @Override
    protected void onCancelled() {
        onQueryReturnedListener = null;
    }

    @Override
    protected void onPostExecute(List<MessageItem> messageItems) {
        if(onQueryReturnedListener != null)
            onQueryReturnedListener.onQueryReturned(messageItems);
    }

    public interface OnQueryReturnedListener {
        void onQueryReturned(List<MessageItem> messagePage);
    }
}
