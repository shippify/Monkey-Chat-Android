package com.criptext.monkeychatandroid.models;

import android.content.Context;
import android.util.Log;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.query.Delete;
import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.criptext.comunication.MOKMessage;
import com.criptext.comunication.MessageTypes;
import com.criptext.monkeykitui.conversation.MonkeyConversation;
import com.criptext.monkeykitui.conversation.holder.ConversationTransaction;
import com.criptext.monkeykitui.recycler.MonkeyItem;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

    public static void saveMessageBatch(HashMap<String, List<MOKMessage>> conversations, Context context,
                                        String userSession, Runnable runnable) {

        ActiveAndroid.beginTransaction();
        try {
            Set<Map.Entry<String, List<MOKMessage>>> set = conversations.entrySet();
            Iterator<Map.Entry<String, List<MOKMessage>>> setIterator = set.iterator();
            while(setIterator.hasNext()) { //iterate every conversation

                Map.Entry<String, List<MOKMessage>> entry = setIterator.next();
                List<MOKMessage> messages = entry.getValue();
                Iterator<MOKMessage> iterator = messages.iterator();

                while (iterator.hasNext()) { //iterate every message
                    MOKMessage message = iterator.next();
                    //Sometimes the acknowledge get lost for network reasons. So thanks to your own messages arrive in
                    //the sync response you can verify that the message is already sent using the old_id param.
                    boolean existOldMessage = false;
                    if(message.getProps()!=null && message.getProps().has("old_id")) {
                        MessageItem oldMessage = getMessageById(message.getProps().get("old_id").getAsString());
                        if(oldMessage!=null){
                            //TODO NOTIFY SOMEHOW A ONACKNOWLEDGE RECEIVED
                            existOldMessage = true;
                        }
                    }
                    //We verify if message doesn't exist. If the message exists we remove it from the original
                    //list to avoid sending to the UI.
                    if(!existMessage(message.getMessage_id()) && !existOldMessage){
                        MessageItem messageItem = null;
                        messageItem.save();
                    } else {
                        iterator.remove();
                        if(messages.isEmpty()) {
                            setIterator.remove();
                        }
                    }
                }
            }

            ActiveAndroid.setTransactionSuccessful();
        }
        finally {
            ActiveAndroid.endTransaction();
            runnable.run();
        }

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

        if(isIncoming && (type==MonkeyItem.MonkeyItemType.audio || type== MonkeyItem.MonkeyItemType.photo))
            item.setStatus(MonkeyItem.DeliveryStatus.sending.ordinal());

        item.setOldMessageId(message.getOldId());

        switch (type){
            case audio:
                if(message.getParams().has("length"))
                    item.setAudioDuration(message.getParams().get("length").getAsLong());
                if(!item.getMessageText().contains("/"))
                    item.setMessageContent(pathToMessagesDir + "/" + message.getMsg());
                break;
            case photo:
                if(!item.getMessageText().contains("/"))
                    item.setMessageContent(pathToMessagesDir + "/" + message.getMsg());
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

    public static void deleteAll(){
        new Delete().from(MessageItem.class).execute();
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

    public static void updateConversations(HashMap<String, ConversationTransaction> map) {
        new UpdateConversationsTask().execute(map);
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

    public static MessageItem unsendMessage(String messageId, String conversationId){
        deleteMessage(messageId);
        return new Select().from(MessageItem.class).where("conversationId = ?", conversationId).orderBy("timestamp DESC").executeSingle();
    }

    public static MessageItem lastConversationMessage(String conversationId){
        return new Select().from(MessageItem.class).where("conversationId = ?", conversationId).orderBy("timestamp DESC").executeSingle();
    }

    public static void deleteMessage(String messageId){
        new Delete().from(MessageItem.class).where("messageId = ?", messageId).execute();
    }

    /****************************
     ******* CONVERSATIONS ******
     ****************************/

    public static List<ConversationItem> getConversations(int rowsPerPage, int pageNumber){

        return new Select()
                .from(ConversationItem.class)
                .orderBy("datetime DESC")
                .limit(rowsPerPage)
                .offset(pageNumber * rowsPerPage)
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


    public static void syncConversation(String id){
        ConversationItem conversation = getConversationById(id);
        if(conversation != null) {
            List<MessageItem> unreadMessages = new Select().from(MessageItem.class)
                    .where("conversationId = ?", id)
                    .where("isIncoming = ?", true)
                    .where("timestampOrder > ?", conversation.lastOpen).execute();
            int unreadMessageCount = unreadMessages.size();
            conversation.setTotalNewMessage(unreadMessageCount);
            MessageItem lastMessage;
            if(unreadMessageCount > 0) {
                lastMessage = unreadMessages.get(unreadMessageCount - 1);
            } else {
                lastMessage = DatabaseHandler.getLastMessage(id);
            }

            int newStatus = MonkeyConversation.ConversationStatus.empty.ordinal();
            if(lastMessage == null) {
                String secondaryText = "Write to this conversation";
                if(conversation.isGroup())
                    secondaryText = "Write to this group";
                conversation.setSecondaryText(secondaryText);
            } else {
                conversation.setSecondaryText(MessageItem.getSecondaryTextByMessageType(lastMessage));
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
    }

    public static void updateConversationNewMessagesCount(ConversationItem conversationItem, int newMessages){
        conversationItem.setTotalNewMessage(newMessages);
        new SaveModelTask().execute(conversationItem);
    }

    public static void deleteConversation(String conversationId){
        new Delete().from(ConversationItem.class).where("idConv = ?", conversationId).execute();
    }

    public static String getSecondaryTextByMessageType(MonkeyItem monkeyItem){
        if(monkeyItem == null)
            return "Write to Contact";
        switch (MonkeyItem.MonkeyItemType.values()[monkeyItem.getMessageType()]) {
            case audio:
                return "Audio";
            case photo:
                return "Photo";
            default:
                return monkeyItem.getMessageText();
        }
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
