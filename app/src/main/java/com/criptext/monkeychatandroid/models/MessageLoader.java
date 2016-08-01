package com.criptext.monkeychatandroid.models;

import android.provider.ContactsContract;
import android.util.Log;

import com.criptext.monkeykitui.recycler.MonkeyAdapter;
import com.criptext.monkeykitui.recycler.MonkeyItem;

import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;

/**
 * Loads messages from the RealmDatabase using Pagination. Has a record of the loaded pages, so that
 * it loads the "requested page" when the user scrolls up to a certain position
 * Created by Gabriel on 5/14/16.
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

    /**
     * Loads a new page of messages from realm and adds them to the adapter.
     * @param realm
     */
    public void loadNewPage(Realm realm){
        if(lastIndex == 0){ // 0 means there are no more messages to load, goodbye.
            adapter.setHasReachedEnd(true);
            return;
        }


        //Make the query
        final RealmResults<MessageModel> realmResults = DatabaseHandler.getMessages(realm, senderId, receiverId);
        //Add an async listener to the query
        realmResults.addChangeListener(new RealmChangeListener<RealmResults<MessageModel>>() {
            @Override
            public void onChange(RealmResults<MessageModel> element) {
                realmResults.removeChangeListener(this);
                final int totalMessages = realmResults.size();
                ArrayList<MonkeyItem> messageModels;
                if(totalMessages < pageSize){//Make copies of the Realm instances
                    messageModels = MessageItem.insertSortCopy(realmResults);
                    adapter.addOldMessages(messageModels, true);
                } else {
                    //Calculate the index for the beginning and ending of the requested page
                    int endIndex = Math.min(realmResults.size(), lastIndex);
                    int startIndex = Math.max(0, endIndex - pageSize);

                    //Make a copy of the page and add it to the adapter.
                    messageModels = MessageItem.insertSortCopy(realmResults.subList(startIndex, endIndex));
                    adapter.addOldMessages(messageModels, messageModels.size() < pageSize);
                    lastIndex = startIndex;
                }
            }
        });
    }

}
