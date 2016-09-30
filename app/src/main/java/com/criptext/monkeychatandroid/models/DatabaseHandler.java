package com.criptext.monkeychatandroid.models;

import android.content.Context;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.query.Delete;
import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.criptext.comunication.MOKMessage;
import com.criptext.comunication.MessageTypes;
import com.criptext.monkeykitui.conversation.MonkeyConversation;
import com.criptext.monkeykitui.recycler.MonkeyItem;

import java.util.ArrayList;
import java.util.List;

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

    public static void saveMessageBatch(ArrayList<MOKMessage> messages, Context context,
                                        String userSession, Runnable runnable) {

        ActiveAndroid.beginTransaction();
        try {
            for(int i = messages.size() - 1; i > -1; i--){
                MOKMessage message = messages.get(i);
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
                    MessageItem messageItem = createMessage(message, context, userSession, !message.isMyOwnMessage(userSession));
                    messageItem.save();
                } else
                    messages.remove(i);
                //TODO VALIDATE IF CONVERSATION DOESN'T EXIST AND CREATE IT
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
        return new Select().from(MessageItem.class).where("messageId = ?", id).executeSingle() != null;
    }

    public static MessageItem createMessage(MOKMessage message, Context context, String myMonkeyId, boolean isIncoming){

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
                    item.setMessageContent(context.getCacheDir()+"/"+message.getMsg());
                break;
            case photo:
                if(!item.getMessageText().contains("/"))
                    item.setMessageContent(context.getCacheDir()+"/"+message.getMsg());
                break;
        }

        return item;
    }

    public static List<MessageItem> getMessages(String myMonkeyId, String conversationId, int rowsPerPage, int pageNumber){
            return new Select()
                    .from(MessageItem.class)
                    .where("conversationId = ?", conversationId)
                    .limit(rowsPerPage)
                    .offset(pageNumber * rowsPerPage)
                    .orderBy("timestampOrder DESC")
                    .execute();
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

    public static MessageItem unsendMessage(String messageId){
        deleteMessage(messageId);
        return new Select().from(MessageItem.class).orderBy("timestamp DESC").executeSingle();
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

    public static void updateConversationNewMessagesCount(ConversationItem conversationItem, int newMessages){
        conversationItem.setTotalNewMessage(newMessages);
        new SaveModelTask().execute(conversationItem);
    }

    public static void deleteConversation(String conversationId){
        new Delete().from(ConversationItem.class).where("idConv = ?", conversationId).execute();
    }

}
