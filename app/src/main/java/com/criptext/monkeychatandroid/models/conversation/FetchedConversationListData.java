package com.criptext.monkeychatandroid.models.conversation;

import android.text.TextUtils;

import com.criptext.comunication.MOKConversation;
import com.criptext.monkeychatandroid.models.DatabaseHandler;
import com.criptext.monkeychatandroid.models.message.MessageItem;
import com.criptext.monkeykitui.conversation.MonkeyConversation;
import com.google.gson.JsonObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Created by gesuwall on 2/16/17.
 */
public class FetchedConversationListData {
    public final ConversationItem[] fetchedConversations;
    public final HashSet<String> usersToFetch;

    public FetchedConversationListData(ConversationItem[] fetchedConversations, HashSet<String> usersToFetch) {
        this.fetchedConversations = fetchedConversations;
        this.usersToFetch = usersToFetch;
    }

    public static FetchedConversationListData fromMOKConversationList(List<MOKConversation> list,
                                                               File downloadDir, String monkeyId) {
        final HashSet<String> usersToFetch = new HashSet<>();
        ArrayList<ConversationItem> monkeyConversations = new ArrayList<>();
        for(MOKConversation mokConversation : list){
            String convName = "Unknown";
            String admins;
            JsonObject convInfo = mokConversation.getInfo();
            if(convInfo!=null && convInfo.has("name"))
                convName = convInfo.get("name").getAsString();
            MessageItem lastItem = null;
            if(mokConversation.getLastMessage() != null)
            lastItem = DatabaseHandler.createMessage(mokConversation.getLastMessage(),
                    downloadDir.getAbsolutePath(), monkeyId);
            ConversationItem conversationItem = new ConversationItem(mokConversation.getConversationId(),
                    convName, mokConversation.getLastModified() * 1000,
                    DatabaseHandler.getSecondaryTextByMessageType(lastItem, mokConversation.isGroup()),
                    mokConversation.getUnread(),
                    mokConversation.isGroup(), mokConversation.getMembers()!=null? TextUtils.join("," ,mokConversation.getMembers()):"",
                    mokConversation.getAvatarURL(),
                    MonkeyConversation.ConversationStatus.receivedMessage.ordinal());
            if(convInfo!=null && convInfo.has("admin")) {
                admins = convInfo.get("admin").getAsString();
                conversationItem.setAdmins(admins);
            }
            if(mokConversation.getUnread()>0) {
                conversationItem.status = MonkeyConversation.ConversationStatus.receivedMessage.ordinal();
            }
            else if(mokConversation.getLastMessage()!=null){
                if(mokConversation.getLastMessage().isMyOwnMessage(monkeyId)){
                    conversationItem.status = MonkeyConversation.ConversationStatus.deliveredMessage.ordinal();
                }
                else{
                    conversationItem.status = MonkeyConversation.ConversationStatus.receivedMessage.ordinal();
                }
            }
            monkeyConversations.add(conversationItem);
        }
        final ConversationItem[] conversationItems = new ConversationItem[monkeyConversations.size()];
        for(int i = 0; i < monkeyConversations.size(); i++){
            if(monkeyConversations.get(i).isGroup()){
                usersToFetch.addAll(Arrays.asList(monkeyConversations.get(i).getGroupMembers().split(",")));
            }
            conversationItems[i] = new ConversationItem(monkeyConversations.get(i));
        }

        return new FetchedConversationListData(conversationItems, usersToFetch);
    }
}
