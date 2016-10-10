package com.criptext.monkeychatandroid.models;

import android.os.AsyncTask;

import java.util.List;

/**
 * Created by gesuwall on 9/20/16.
 */
public class GetMessagePageTask extends AsyncTask<Void, Void, List<MessageItem>>{
    private String myMonkeyId;
    private String conversationId;
    private int rowsPerPage;
    private int pageNumber;

    public OnQueryReturnedListener onQueryReturnedListener = null;

    public GetMessagePageTask(String myMonkeyId, String conversationId, int rowsPerPage, int pageNumber){
        this.myMonkeyId = myMonkeyId;
        this.conversationId = conversationId;
        this.rowsPerPage = rowsPerPage;
        this.pageNumber = pageNumber;
    }
    @Override
    protected List<MessageItem> doInBackground(Void... params) {
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return DatabaseHandler.getMessages(myMonkeyId, conversationId, rowsPerPage, pageNumber);
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
