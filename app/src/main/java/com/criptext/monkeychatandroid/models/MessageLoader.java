package com.criptext.monkeychatandroid.models;

import android.provider.ContactsContract;

import com.criptext.monkeykitui.recycler.MonkeyAdapter;
import com.criptext.monkeykitui.recycler.MonkeyItem;

import java.util.ArrayList;

import io.realm.RealmChangeListener;
import io.realm.RealmResults;

/**
 * Created by gesuwall on 5/14/16.
 */
public class MessageLoader {
    private String senderId;
    private String receiverId;

    public static int DEFAULT_PAGE_SIZE = 30;
    private int pageSize;
    private int lastIndex;



    private MonkeyAdapter adapter;

    public MessageLoader(String senderId, String receiverId){
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.pageSize = DEFAULT_PAGE_SIZE;
        this.lastIndex = Integer.MAX_VALUE;
    }

    public MessageLoader(String senderId, String receiverId, int pageSize){
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.pageSize = pageSize;
        this.lastIndex = Integer.MAX_VALUE;
    }

    public void setAdapter(MonkeyAdapter adapter) {
        this.adapter = adapter;
    }

    public void loadNewPage(){
        if(lastIndex == 0){
            adapter.setHasReachedEnd(true);
            return;
        }

        final RealmResults<MessageModel> realmResults = DatabaseHandler.getMessages(senderId, receiverId);
        realmResults.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                realmResults.removeChangeListener(this);
                final int totalMessages = realmResults.size();
                ArrayList<MonkeyItem> messageModels;
                if(totalMessages < pageSize){
                    messageModels = MessageItem.insertSortCopy(realmResults);
                    adapter.addOldMessages(messageModels, true);
                } else {
                   int endIndex = Math.min(realmResults.size(), lastIndex);
                   int startIndex = Math.max(0, endIndex - pageSize);

                   messageModels = MessageItem.insertSortCopy(realmResults.subList(startIndex, endIndex));
                   adapter.addOldMessages(messageModels, messageModels.size() < pageSize);
                   lastIndex = startIndex;
                }
            }
        });
    }

}
