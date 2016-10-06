package com.criptext.monkeychatandroid.models;

import android.os.AsyncTask;
import android.util.Log;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.criptext.monkeykitui.conversation.holder.ConversationTransaction;

import java.util.HashMap;
import java.util.List;

/**
 * Created by gesuwall on 10/6/16.
 */
class UpdateConversationsTask extends AsyncTask<HashMap<String, ConversationTransaction>, Integer, Integer> {

    @Override
    protected Integer doInBackground(HashMap<String, ConversationTransaction>... params) {
        ActiveAndroid.beginTransaction();
        HashMap<String, ConversationTransaction> map = params[0];
        try {

            From from = new Select().from(ConversationItem.class);
            String[] args = new String[map.size()];
            String query = "";
            int index = 0;
            for (String key : map.keySet()) {
                query += "idConv = ? OR ";
                args[index++] = key;
            }

            query = query.substring(0, query.length() - 4);
            from.where(query, args);


            List<ConversationItem> listToUpdate = from.execute();
            for(ConversationItem conversation: listToUpdate) {
                ConversationTransaction transaction = map.get(conversation.getConvId());
                transaction.updateConversation(conversation);
                conversation.save();
            }
            ActiveAndroid.setTransactionSuccessful();
        } finally {
            ActiveAndroid.endTransaction();
        }
        return 1;
    }
}
