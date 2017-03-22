package com.criptext.monkeychatandroid.models;


import com.criptext.comunication.MOKNotification;
import com.criptext.monkeychatandroid.models.conversation.ConversationItem;
import com.criptext.monkeykitui.conversation.MonkeyConversation;
import com.google.gson.JsonObject;

import java.util.HashSet;

/**
 * Created by gesuwall on 3/22/17.
 */
public class NotificationMessages {
    final DatabaseHandler db = new DatabaseHandler();

    public ConversationItem updateGroupWithCreateNotification(MOKNotification not){
        String groupId = not.getProps().get("group_id").getAsString();
        String memberIds = not.getProps().get("members").getAsString();
        JsonObject info = not.getProps().getAsJsonObject("info");
        String conv_name = info.get("name").getAsString();
        String avatar_url = info.get("avatar").getAsString();

        ConversationItem conv = db.getConversationById(groupId);

        if(conv == null){
            conv = new ConversationItem(groupId, conv_name, System.currentTimeMillis(),
                    "Write to this group", 0, true, memberIds, avatar_url,
                    MonkeyConversation.ConversationStatus.empty.ordinal());
            conv.save();
        }else{
            conv.setName(conv_name);
            conv.setGroupMembers(memberIds);
            conv.setAvatarFilePath(avatar_url);
        }
        db.syncConversation(conv);
        return conv;
    }

    public void parseGroupNotifications(HashSet<String> missingConversations, MOKNotification not, int type) {
        try {
            switch (type) {
                case com.criptext.comunication.MessageTypes.MOKGroupNewMember: {
                    String id = not.getReceiverId();
                    ConversationItem conv = db.addGroupMember(id,
                            not.getProps().get("new_member").getAsString());
                    if (conv == null)
                        missingConversations.add(id);
                }
                break;
                case com.criptext.comunication.MessageTypes.MOKGroupRemoveMember: {
                    String id = not.getReceiverId();
                    db.removeGroupMember(id, not.getSenderId());
                }
                break;
                case com.criptext.comunication.MessageTypes.MOKGroupCreate: {
                    ConversationItem conv = updateGroupWithCreateNotification(not);
                    //remove created group from missing conversations
                    missingConversations.remove(conv.getConvId());
                }
                break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
