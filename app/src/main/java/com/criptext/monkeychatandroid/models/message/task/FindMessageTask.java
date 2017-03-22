package com.criptext.monkeychatandroid.models.message.task;

import android.os.AsyncTask;

import com.criptext.monkeychatandroid.models.DatabaseHandler;
import com.criptext.monkeychatandroid.models.message.MessageItem;

/**
 * Created by gesuwall on 9/20/16.
 */
public class FindMessageTask extends AsyncTask<String, Void, MessageItem> {

    public OnQueryReturnedListener onQueryReturnedListener = null;

    public FindMessageTask(){
    }

    public FindMessageTask(OnQueryReturnedListener listener){
        onQueryReturnedListener = listener;
    }

    @Override
    protected MessageItem doInBackground(String... params) {
        for(String s : params){
            MessageItem item = new DatabaseHandler().getMessageById(s);
            if(item != null)
                return item;
        }
        return null;
    }

    @Override
    protected void onPostExecute(MessageItem messageItem) {
        if(onQueryReturnedListener != null)
            onQueryReturnedListener.onQueryReturned(messageItem);
    }

    public interface OnQueryReturnedListener {
        void onQueryReturned(MessageItem result);
    }
}
