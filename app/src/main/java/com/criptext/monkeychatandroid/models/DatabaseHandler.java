package com.criptext.monkeychatandroid.models;

import android.util.Log;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.query.Delete;
import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.criptext.comunication.MOKMessage;
import com.criptext.comunication.MOKNotification;
import com.criptext.comunication.MessageTypes;
import com.criptext.http.HttpSync;
import com.criptext.monkeychatandroid.MonkeyChat;
import com.criptext.monkeychatandroid.models.conversation.ConversationItem;
import com.criptext.monkeychatandroid.models.message.MessageItem;
import com.criptext.monkeykitui.conversation.MonkeyConversation;
import com.criptext.monkeykitui.conversation.holder.ConversationTransaction;
import com.criptext.monkeykitui.recycler.EndItem;
import com.criptext.monkeykitui.recycler.MonkeyItem;
import com.criptext.monkeykitui.util.Utils;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Created by daniel on 4/26/16.
 */
public class DatabaseHandler {

    /**********************
    ******* MESSAGES ******
    ***********************/

    public static void saveIncomingMessage(MessageItem messageItem, Runnable runnable) {

        if (!existMessage(messageItem.getMessageId())){ //NO DUPLICATED
            //TODO use a callback
            new SaveModelTask().execute(messageItem);
            runnable.run();
        }
    }

    public static void storeNewMessage(MessageItem messageItem) {
        //Message doesn't exists in DB so we just use save function
        new SaveModelTask().execute(messageItem);
    }

    public static void saveMessages(List<MessageItem> messageItems){
        ActiveAndroid.beginTransaction();
        try {
            for(MessageItem messageItem: messageItems){
                messageItem.save();
            }
            ActiveAndroid.setTransactionSuccessful();
        }
        finally {
            ActiveAndroid.endTransaction();
        }
    }

    public static MessageItem getMessageById(String id) {
        if(id == null){
            return null;
        }
        return new Select().from(MessageItem.class).where("messageId = ?", id).executeSingle();
    }

    public static boolean existMessage(String id) {
        return getMessageById(id) != null;
    }

    public static MessageItem createMessage(MOKMessage message, String pathToMessagesDir, String myMonkeyId){

        MonkeyItem.MonkeyItemType type = MonkeyItem.MonkeyItemType.text;
        if (message.isMediaType()) {
            switch (message.getFileType()){
                case MessageTypes.FileTypes.Audio:
                    type = MonkeyItem.MonkeyItemType.audio;
                    break;
                case MessageTypes.FileTypes.Photo:
                    type = MonkeyItem.MonkeyItemType.photo;
                    break;
            }
        }

        boolean isIncoming = !message.isMyOwnMessage(myMonkeyId);
        MessageItem item = new MessageItem(message.getSenderId(), message.getConversationID(myMonkeyId),
                message.getMessage_id(), message.getMsg(), Long.parseLong(message.getDatetime()),
                message.getDatetimeorder(), isIncoming, type);

        item.setParams(message.getParams()!=null? message.getParams().toString() : "");
        item.setProps(message.getProps()!=null? message.getProps().toString() : "");
        if(!item.getMessageId().contains("-"))
            item.setStatus(MonkeyItem.DeliveryStatus.delivered.ordinal());

        if(type==MonkeyItem.MonkeyItemType.audio || type== MonkeyItem.MonkeyItemType.photo)
            item.setStatus(MonkeyItem.DeliveryStatus.sending.ordinal());

        item.setOldMessageId(message.getOldId());

        switch (type){
            case audio:
                //TODO make sure get params is never null, this right here is a quick fix
                if(message.getParams() != null && message.getParams().has("length"))
                    item.setAudioDuration(message.getParams().get("length").getAsLong());
                if(!item.getMessageText().contains("/"))
                    item.setMessageContent(pathToMessagesDir + "/" + MonkeyChat.VOICENOTES_DIR + "/" + message.getMsg());
                item.setFileSize(message.getFileSize());
                break;
            case photo:
                if(!item.getMessageText().contains("/"))
                    item.setMessageContent(pathToMessagesDir + "/" + MonkeyChat.PHOTOS_DIR + "/" + message.getMsg());
                item.setFileSize(message.getFileSize());
                break;
        }

        return item;
    }

    public static List<MessageItem> getMessages(String conversationId, int rowsPerPage, int pageOffset){
            return new Select()
                    .from(MessageItem.class)
                    .where("conversationId = ?", conversationId)
                    .limit(rowsPerPage)
                    .offset(pageOffset)
                    .orderBy("timestampOrder DESC")
                    .execute();
    }

    public static MessageItem getLastMessage(String conversationId) {
        return new Select()
                .from(MessageItem.class)
                .where("conversationId = ?", conversationId)
                .orderBy("timestampOrder DESC")
                .executeSingle();
    }

    public static void deleteAll(@NotNull String conversationId){
        new Delete().from(MessageItem.class).where("conversationId = ?", conversationId).execute();
    }

    public static void updateMessageStatus(String messageId, String OldMessageId, MonkeyItem.DeliveryStatus outgoingMessageStatus){
        MessageItem result = getMessageById(OldMessageId != null ? OldMessageId : messageId);
        if (result != null) {
            result.setStatus(outgoingMessageStatus.ordinal());
            if(OldMessageId != null){
                result.setOldMessageId(OldMessageId);
                result.setMessageId(messageId);
            }
            new SaveModelTask().execute(result);
        }
    }

    public static void markMessagesAsError(final ArrayList<MOKMessage> errorMessages) {

        ActiveAndroid.beginTransaction();
        try {
            From from = new Select().from(MessageItem.class);
            for(int i = errorMessages.size() - 1; i > -1; i--){
                MOKMessage message = errorMessages.get(i);
                from.or("messageId = ?", message.getMessage_id());
            }
            List<MessageItem> listToUpdate = from.execute();
            for(MessageItem messageItem : listToUpdate){
                messageItem.setStatus(MonkeyItem.DeliveryStatus.error.ordinal());
            }
            ActiveAndroid.setTransactionSuccessful();
        }
        finally {
            ActiveAndroid.endTransaction();
        }
    }

    public static void deleteMessage(MessageItem message){
        new DeleteModelTask().execute(message);
    }

    /****************************
     ******* CONVERSATIONS ******
     ****************************/

    public static List<ConversationItem> getConversations(int conversationsToLoad, int loadedConversations){

        return new Select()
                .from(ConversationItem.class)
                .orderBy("datetime DESC")
                .limit(conversationsToLoad)
                .offset(loadedConversations)
                .execute();
    }

    public static void saveConversations(ConversationItem[] conversations){
        new SaveModelTask().execute(conversations);
    }

    public static ConversationItem getConversationById(String id) {
        return new Select().from(ConversationItem.class).where("idConv = ?", id).executeSingle();
    }

    public static HashMap<String, ConversationItem> getConversationsById(String... ids){
        HashMap<String, ConversationItem> result = new HashMap<>();
        for(String id : ids) {
            ConversationItem conversation = getConversationById(id);
            if(conversation != null)
                result.put(id, conversation);
        }
        return result;
    }

    public static void updateConversationWithSentMessage(ConversationItem conversation,
                final String secondaryText, final MonkeyConversation.ConversationStatus status,
                                    final int unread){
        conversation.setSecondaryText(secondaryText!=null?secondaryText:conversation.getSecondaryText());
        conversation.setStatus(status.ordinal());
        conversation.setTotalNewMessage(unread == 0 ? 0 : conversation.getTotalNewMessages()+unread);

        if(status == MonkeyConversation.ConversationStatus.empty)
            conversation.setTotalNewMessage(0);
        new SaveModelTask().execute(conversation);
    }

    public static void updateConversation(ConversationItem conversation){
        new SaveModelTask().execute(conversation);
    }

    public static List<MessageItem> getAllMessagesSince(String conversationId, long since) {
        Log.d("allMessagesSince", "" + since);
        return new Select().from(MessageItem.class)
                    .where("conversationId = ?", conversationId)
                    .where("isIncoming = ?", true)
                    .where("timestampOrder > ?", since).execute();
    }

    public static void syncConversation(ConversationItem conversation) {
        /*TODO don't calcuate the totalNewMessages value counting the unread messages in the local DB */
            List<MessageItem> unreadMessages = getAllMessagesSince(conversation.getConvId(), conversation.lastOpen);
            int unreadMessageCount = unreadMessages.size();
            conversation.setTotalNewMessage(unreadMessageCount);
            MessageItem lastMessage = DatabaseHandler.getLastMessage(conversation.getConvId());

            int newStatus = MonkeyConversation.ConversationStatus.empty.ordinal();
            if(lastMessage == null) {
                String secondaryText = "Write to this conversation";
                if(conversation.isGroup())
                    secondaryText = "Write to this group";
                conversation.setSecondaryText(secondaryText);
            } else {
                conversation.setSecondaryText(DatabaseHandler.getSecondaryTextByMessageType(lastMessage, conversation.isGroup()));
                conversation.setDatetime(lastMessage.getMessageTimestampOrder());

                if (lastMessage.isIncomingMessage())
                    newStatus = MonkeyConversation.ConversationStatus.receivedMessage.ordinal();
                else if (lastMessage.getMessageTimestampOrder() <= conversation.lastRead)
                    newStatus = MonkeyConversation.ConversationStatus.sentMessageRead.ordinal();
                else
                    newStatus = MonkeyConversation.ConversationStatus.deliveredMessage.ordinal();
            }
            conversation.setStatus(newStatus);
            conversation.save();
    }

    public static void syncConversation(String id, HttpSync.SyncData syncData){
        ConversationItem conversation = getConversationById(id);
        if(conversation != null) {
            syncConversation(conversation);
        }else if(!id.contains("G:")){
            MessageItem lastMessage = getLastMessage(id);
            if(lastMessage != null){
                conversation = new ConversationItem(id,
                        "Unknown", lastMessage.timestampOrder, "Write to this contact",
                        1, false, "", "", MonkeyConversation.ConversationStatus.empty.ordinal());
                conversation.save();
                syncConversation(conversation);
            }
            syncData.getUsers().add(id);
        }
    }

    public static void updateConversationNewMessagesCount(ConversationItem conversationItem, int newMessages){
        conversationItem.setTotalNewMessage(newMessages);
        new SaveModelTask().execute(conversationItem);
    }

    public static void deleteConversation(ConversationItem item){
        new DeleteModelTask().execute(item);
    }

    public static String getSecondaryTextByMessageType(MonkeyItem monkeyItem, boolean isGroup){
        if(monkeyItem == null || monkeyItem instanceof EndItem)
            return isGroup ? "Write to group" : "Write to contact";
        switch (MonkeyItem.MonkeyItemType.values()[monkeyItem.getMessageType()]) {
            case audio:
                return "Audio " + Utils.Companion.getAudioTimeFormattedText(monkeyItem.getAudioDuration());
            case photo:
                return "Photo";
            default:
                return monkeyItem.getMessageText();
        }
    }

    public static ConversationItem addGroupMember(String id, String newMember) {
        ConversationItem conv = DatabaseHandler.getConversationById(id);
        if (conv != null) {
            conv.addMember(newMember);
            conv.save();
            return conv;
        }
        return null;
    }

    public static ConversationItem removeGroupMember(String id, String removedMember) {
        ConversationItem conv = DatabaseHandler.getConversationById(id);
        if (conv != null) {
            conv.removeMember(removedMember);
            conv.save();
            return conv;
        }
        return null;
    }

     public static ConversationItem updateGroupWithCreateNotification(MOKNotification not){
        String groupId = not.getReceiverId();
        String memberIds = not.getProps().get("members").getAsString();
        JsonObject info = not.getProps().getAsJsonObject("info");
        String conv_name = info.get("name").getAsString();
        String avatar_url = info.get("avatar").getAsString();

        ConversationItem conv = getConversationById(groupId);

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
        syncConversation(conv);

        return conv;
    }

    public static ConversationTransaction newCopyTransaction(final ConversationItem itemToCopy) {
        return new ConversationTransaction() {
            @Override
            public void updateConversation(@NotNull MonkeyConversation conversation) {
                ConversationItem newConversationItem = (ConversationItem)conversation;
                newConversationItem.setDatetime(itemToCopy.getDatetime());
                newConversationItem.setSecondaryText(itemToCopy.getSecondaryText());
                newConversationItem.setTotalNewMessage(itemToCopy.getTotalNewMessages());
                newConversationItem.setStatus(itemToCopy.getStatus());

            }
        };
    }

}
