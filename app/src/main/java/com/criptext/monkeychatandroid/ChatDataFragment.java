package com.criptext.monkeychatandroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.criptext.monkeychatandroid.models.conversation.ConversationItem;
import com.criptext.monkeychatandroid.models.message.MessageItem;
import com.criptext.monkeykitui.conversation.ConversationsList;

import com.criptext.monkeykitui.recycler.MessagesList;
import com.criptext.monkeykitui.util.MonkeyFragmentManager;

import org.jetbrains.annotations.NotNull;

import com.criptext.monkeykitui.conversation.DefaultGroupData;

import java.util.HashMap;
import java.util.List;
import java.util.Stack;

/**
 * Created by gesuwall on 8/17/16.
 */
public class ChatDataFragment extends Fragment{

    HashMap<String, MessagesList> messagesMap;
    ConversationsList conversations;
    /**
     * This class is used to handle group methods.
     */
    DefaultGroupData groupData;

    /**
     * holds the active conversation. This object should be set when the user expresses an intent to
     * open/close the conversation, it should not be tied to the chat fragment.
     */
    ConversationItem activeConversationItem;
    /**
     * Monkey ID of the current user. This is stored in Shared Preferences, so we use this
     * property to cache it so that we don't have to read from disk every time we need it.
     */
    String myMonkeyID;
    /**
     * Name of the current user. This is stored in Shared Preferences, so we use this
     * property to cache it so that we don't have to read from disk every time we need it.
     */
    String myName;
    Stack<MonkeyFragmentManager.FragmentTypes> mkFragmentStack;

    static ChatDataFragment newInstance(Context context) {
        ChatDataFragment f = new ChatDataFragment();
        f.messagesMap = new HashMap<>();
        f.conversations = new ConversationsList();
        f.mkFragmentStack = new Stack<>();
        //First, initialize the constants from SharedPreferences.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        f.myMonkeyID = prefs.getString(MonkeyChat.MONKEY_ID, null);
        f.myName = prefs.getString(MonkeyChat.FULLNAME, null);

        return f;
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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }
}
