package com.criptext.monkeychatandroid.state;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.criptext.monkeychatandroid.MonkeyChat;
import com.criptext.monkeychatandroid.models.message.MessageItem;

import com.criptext.monkeykitui.recycler.MessagesList;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by gesuwall on 8/17/16.
 */
public class ChatDataFragment extends Fragment{

    public ChatState chatState;

    public static ChatDataFragment newInstance(Context context) {
        ChatDataFragment f = new ChatDataFragment();
        //First, initialize the constants from SharedPreferences.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String myMonkeyID = prefs.getString(MonkeyChat.MONKEY_ID, null);
        String myName = prefs.getString(MonkeyChat.FULLNAME, null);
        f.chatState = new ChatState(myMonkeyID, myName);

        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }
}
