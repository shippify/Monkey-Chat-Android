package com.criptext.monkeychatandroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.criptext.monkeychatandroid.models.ConversationItem;
import com.criptext.monkeykitui.conversation.ConversationsList;
import com.criptext.monkeykitui.conversation.MonkeyConversation;
import com.criptext.monkeykitui.recycler.MonkeyItem;
import com.criptext.monkeykitui.util.MonkeyFragmentManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

/**
 * Created by gesuwall on 8/17/16.
 */
public class ChatDataFragment extends Fragment{

    HashMap<String, List<MonkeyItem>> messagesMap;
    ConversationsList conversations;
    /**
     * This class is used to handle group methods.
     */
    GroupData groupData;
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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }
}
