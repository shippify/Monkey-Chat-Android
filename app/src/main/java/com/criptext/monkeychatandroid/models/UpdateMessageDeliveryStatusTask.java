package com.criptext.monkeychatandroid.models;

import android.os.AsyncTask;

import com.activeandroid.ActiveAndroid;
import com.criptext.monkeykitui.recycler.MonkeyItem;

/**
 * Created by gesuwall on 1/5/17.
 */
public class UpdateMessageDeliveryStatusTask extends AsyncTask<String, Void, MessageItem> {

    public OnQueryReturnedListener onQueryReturnedListener = null;

    public UpdateMessageDeliveryStatusTask(){
    }

    public UpdateMessageDeliveryStatusTask(OnQueryReturnedListener listener){
        onQueryReturnedListener = listener;
    }

    @Override
    protected MessageItem doInBackground(String... params) {
        for(String s : params){
            MessageItem item = DatabaseHandler.getMessageById(s);
            if(item != null) {
                try {
                    ActiveAndroid.beginTransaction();
                    item.setStatus(MonkeyItem.DeliveryStatus.delivered.ordinal());
                    item.save();
                    ActiveAndroid.setTransactionSuccessful();
                } finally {
                    ActiveAndroid.endTransaction();
                }
                return item;
            }
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
