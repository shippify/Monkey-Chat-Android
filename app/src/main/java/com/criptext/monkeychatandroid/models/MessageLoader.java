package com.criptext.monkeychatandroid.models;

import com.criptext.monkeychatandroid.MainActivity;
import com.criptext.monkeykitui.recycler.MonkeyItem;

import java.lang.ref.WeakReference;
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
    private String mySessionId;
    private String conversationId;
    private String membersIds;
    private MainActivity activity;
    private MessageCounter messageCounter;


    public MessageLoader(String conversationId, String membersIds, String mySessionId, MainActivity activity){
        this.conversationId = conversationId;
        this.membersIds = membersIds;
        this.mySessionId = mySessionId;
        this.activity = activity;
        messageCounter = new MessageCounter();
    }

    public MessageLoader(String conversationId, String membersIds, String mySessionId, MainActivity activity, int pageSize){
        this.conversationId = conversationId;
        this.membersIds = membersIds;
        this.mySessionId = mySessionId;
        this.activity = activity;
        messageCounter = new MessageCounter(pageSize);
    }

    /**
     * Loads a new page of messages from realm and adds them to the adapter.
     * @param realm
     */
    public void loadNewPage(Realm realm){
        if(messageCounter.lastIndex == 0){ // 0 means there are no more messages to load, goodbye.
            return;
        }
        //Make the query
        final RealmResults<MessageModel> realmResults = DatabaseHandler.getMessages(realm, mySessionId, conversationId);
        //Add an async listener to the query
        realmResults.addChangeListener(new NewMessagesListener(conversationId, membersIds, activity, false, messageCounter));
    }

    public void loadFirstPage(Realm realm){
        //Make the query
        final RealmResults<MessageModel> realmResults = DatabaseHandler.getMessages(realm, mySessionId, conversationId);
        //Add an async listener to the query
        realmResults.addChangeListener(new NewMessagesListener(conversationId, membersIds, activity, true, messageCounter));

    }

    public void countNewMessages(String conversationId, int newMessages){
        if(this.conversationId.equals(conversationId))
            messageCounter.lastIndex -= newMessages;
    }

    public void countNewMessage(String conversationId){
        if(this.conversationId.equals(conversationId))
            messageCounter.lastIndex -= 1;
    }

    private static class NewMessagesListener implements RealmChangeListener<RealmResults<MessageModel>> {
        WeakReference<MainActivity> activityRef;
        boolean firstPage;
        MessageCounter messageCounter;
        String conversationId;
        String membersIds;

        private NewMessagesListener(String conversationId, String membersIds, MainActivity activity, boolean firstPage, MessageCounter mCounter){
            activityRef = new WeakReference<MainActivity>(activity);
            this.conversationId = conversationId;
            this.membersIds = membersIds;
            this.firstPage = firstPage;
            messageCounter = mCounter;
        }

        @Override
        public void onChange(RealmResults element) {
            element.removeChangeListeners();
            final int totalMessages = element.size();
            ArrayList<MonkeyItem> messageModels;
            if(totalMessages < messageCounter.pageSize){//Make copies of the Realm instances
                messageModels = MessageItem.insertSortCopy(element);
                updateActivity(messageModels, true);
            } else {
                //Calculate the index for the beginning and ending of the requested page
                int endIndex = Math.min(element.size(), messageCounter.lastIndex);
                int startIndex = Math.max(0, endIndex - messageCounter.pageSize);

                //Make a copy of the page and add it to the adapter.
                messageModels = MessageItem.insertSortCopy(element.subList(startIndex, endIndex));
                updateActivity(messageModels, messageModels.size() < messageCounter.pageSize);
                messageCounter.lastIndex = startIndex;
            }
        }

        private void updateActivity(ArrayList<MonkeyItem> messages, boolean hasReachedEnd){
            final MainActivity act = activityRef.get();
            if(act != null){
                if(firstPage){
                    act.startChatWithMessages(conversationId, membersIds, messages, hasReachedEnd);
                } else {
                    act.addOldMessages(messages, hasReachedEnd);
                    if(messages.size() < messageCounter.pageSize)
                        act.addOldMessagesFromServer(conversationId);
                }
            }
        }

    }

    public int getPageSize(){
        return messageCounter.getPageSize();
    }

    private static class MessageCounter {
        private int pageSize;
        private int lastIndex;

        private static int DEFAULT_PAGE_SIZE = 30;

        private MessageCounter() {
            lastIndex = Integer.MAX_VALUE;
            this.pageSize = DEFAULT_PAGE_SIZE;
        }
        private MessageCounter(int pageSize){
            lastIndex = Integer.MAX_VALUE;
            this.pageSize = pageSize;
        }

        public int getPageSize() {
            return pageSize;
        }
    }


}
