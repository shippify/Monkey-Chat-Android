package com.criptext.monkeychatandroid;

import com.criptext.monkeychatandroid.models.ConversationItem;
import com.criptext.monkeychatandroid.models.DatabaseHandler;
import com.criptext.monkeykitui.MonkeyConversationsFragment;
import com.criptext.monkeykitui.conversation.MonkeyConversation;
import com.criptext.monkeykitui.conversation.holder.ConversationTransaction;

import java.util.Map;
import java.util.Set;

/**
 * Created by gesuwall on 10/11/16.
 */
public class ConversationManager {
    ConversationItem activeConversationItem = null;
    MonkeyConversationsFragment fragment = null;

    public ConversationManager() {

    }

    public void updateConversationBadge(ConversationItem conversationItem, int unreadMessages) {
        DatabaseHandler.updateConversationNewMessagesCount(conversationItem, unreadMessages);
        if(fragment != null)
            fragment.updateConversation(conversationItem);
    }

    public void updateConversations(Set<Map.Entry<MonkeyConversation, ConversationTransaction>> updateSet) {
        if(fragment != null)
            fragment.updateConversations(updateSet);
    }

    public void updateConversationLastOpen(String conversationId, long lastOpen) {
        if(activeConversationItem != null && activeConversationItem.getConvId().equals((conversationId))) {
            activeConversationItem.setDatetime(lastOpen);
            DatabaseHandler.updateConversation(activeConversationItem);
        } else throw new IllegalStateException("Tried to update the lastOpen of a non-active conversation");
    }
}
