package com.criptext.monkeychatandroid;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.criptext.monkeychatandroid.models.MessageLoader;
import com.criptext.monkeykitui.conversation.MonkeyConversation;
import com.criptext.monkeykitui.recycler.MonkeyItem;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Created by gesuwall on 8/17/16.
 */
public class ChatDataFragment extends Fragment{
    HashMap<String, List<MonkeyItem>> chatMap;
    Collection<MonkeyConversation> conversationsList;
    MessageLoader messageLoader;
    GroupData groupData;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }
}
