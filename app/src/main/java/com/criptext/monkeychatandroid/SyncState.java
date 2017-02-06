package com.criptext.monkeychatandroid;

import com.criptext.comunication.MOKMessage;
import com.criptext.http.HttpSync;
import com.criptext.monkeychatandroid.state.ChatState;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Created by gabriel on 2/3/17.
 */

public class SyncState {

    public static int withNewMessages(ChatState state, HttpSync.SyncData data) {
        final String activeConversationId = state.getActiveConversationId();
        boolean activeConversationNeedsUpdate = false;
        final HashMap<String, List<MOKMessage>> newMessagesMap = data.getNewMessages();

        Iterator<String> iterator = data.getConversationsToUpdate().iterator();
        while (iterator.hasNext()) {
            String convId = iterator.next();

            if((activeConversationId != null) && activeConversationId.equals(convId)) {
                activeConversationNeedsUpdate = true;
            } else {
                //clear cached messages of inactive conversations that need update
                state.messagesMap.remove(convId);
            }

        }

        if (activeConversationNeedsUpdate)
            return newMessagesMap.get(activeConversationId).size();
        else
            return 0;


    }
}
