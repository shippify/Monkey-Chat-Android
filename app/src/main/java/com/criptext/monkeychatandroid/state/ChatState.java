package com.criptext.monkeychatandroid.state;

import com.criptext.monkeychatandroid.models.conversation.ConversationItem;
import com.criptext.monkeychatandroid.models.message.MessageItem;
import com.criptext.monkeykitui.conversation.ConversationsList;
import com.criptext.monkeykitui.conversation.DefaultGroupData;
import com.criptext.monkeykitui.recycler.MessagesList;
import com.criptext.monkeykitui.util.MonkeyFragmentManager;
import com.criptext.monkeykitui.util.Utils;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Stack;

/**
 * Created by gabriel on 2/3/17.
 */

public class ChatState {
    public HashMap<String, MessagesList> messagesMap;
    public ConversationsList conversations;
    /**
     * This class is used to handle group methods.
     */
    public DefaultGroupData groupData;

    /**
     * holds the active conversation. This object should be set when the user expresses an intent to
     * open/close the conversation, it should not be tied to the chat fragment.
     */
    public ConversationItem activeConversationItem;
    /**
     * Monkey ID of the current user. This is stored in Shared Preferences, so we use this
     * property to cache it so that we don't have to read from disk every time we need it.
     */
    public String myMonkeyID;
    /**
     * Name of the current user. This is stored in Shared Preferences, so we use this
     * property to cache it so that we don't have to read from disk every time we need it.
     */
    public String myName;


    /**
     * The connection status to be displayed in the UI
     */
    public Utils.ConnectionStatus connectionStatus;

    /**
     * A model of the fragment manager backstack. Useful to determine which fragment should be currently
     * visible.
     */
    public Stack<MonkeyFragmentManager.FragmentTypes> mkFragmentStack;

    public ChatState(String myMonkeyID, String myName) {
        messagesMap = new HashMap<>();
        conversations = new ConversationsList();
        mkFragmentStack = new Stack<>();
        this.myMonkeyID = myMonkeyID;
        this.myName = myName;
        connectionStatus = Utils.ConnectionStatus.connected;
    }

    /**
     * Adds a new entry to the HashMap with the specified conversation ID and messages. Be careful,
     * this will overwrite any previous entries with the same conversation id.
     * @param newMessages list of messages
     */
    public void addNewMessagesList(String conversationId, List<MessageItem> newMessages) {
        MessagesList newList = new MessagesList(conversationId);
        newList.insertMessages(newMessages, false);
        messagesMap.put(conversationId, newList);
    }

    /**
     * returns messages list for a conversation. creates a new list and adds it to the HashMap if
     * none exist. Will never ever return null.
     * @param conversationId id of the conversation of the requested messages
     * @return MessagesList object with cached messages.
     */
    @NotNull
    public MessagesList getLoadedMessages(String conversationId) {
        MessagesList convMessages = messagesMap.get(conversationId);
        if (convMessages == null) {
            convMessages = new MessagesList(conversationId);
            convMessages.setHasReachedEnd(false);
            messagesMap.put(conversationId, convMessages);
        }
        return convMessages;
    }

    public MessagesList getActiveConversationMessages() {
        if (activeConversationItem != null) {
            return getLoadedMessages(activeConversationItem.getConvId());
        }
        return null;
    }

    public String getActiveConversationId() {
        if (activeConversationItem != null) {
            return activeConversationItem.getConvId();
        }
        return null;
    }

}
