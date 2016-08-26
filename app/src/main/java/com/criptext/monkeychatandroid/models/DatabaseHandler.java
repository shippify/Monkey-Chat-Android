package com.criptext.monkeychatandroid.models;

import android.content.Context;
import android.util.Log;

import com.criptext.comunication.MOKMessage;
import com.criptext.monkeykitui.recycler.MonkeyItem;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;

/**
 * Created by daniel on 4/26/16.
 */
public class DatabaseHandler {

    public static void saveIncomingMessage(Realm realm, final MessageItem messageItem, final Realm.Transaction.OnSuccess onSuccess, final Realm.Transaction.OnError onError) {

        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                if (!existMessage(realm, messageItem.getMessageId())) //NO DUPLICATED
                    realm.copyToRealm(messageItem.getModel());
                else
                    throw new IllegalArgumentException("messageItem: " + messageItem.getMessageId() + " already exists");
            }
        }, onSuccess, onError);
    }
    public static void storeSendingMessage(Realm realm, final MessageItem messageItem, final Realm.Transaction.OnSuccess onSuccess, final Realm.Transaction.OnError onError) {

        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                if (!existMessage(realm, messageItem.getMessageId())) { //NO DUPLICATED
                    //Since this is a mirror chat, all messages have the same sender ID.
                    //We have to explicitly sent this message as Outgoing, otherwise it
                    // will be stored differently.
                    messageItem.getModel().setIncoming(false);
                    realm.copyToRealm(messageItem.getModel());
                } else
                    throw new IllegalArgumentException("messageItem already exists");
            }
        }, onSuccess, onError);
    }
    public static void saveMessageBatch(Realm realm, final ArrayList<MOKMessage> messages, final Context context, final String userSession, final Realm.Transaction.OnSuccess onSuccess, final Realm.Transaction.OnError onError) {

        final WeakReference<Context> weakContext = new WeakReference<>(context);
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {

                for(int i = messages.size() - 1; i > -1; i--){
                    MOKMessage message = messages.get(i);
                    if(!existMessage(realm, message.getMessage_id())){
                        MessageItem messageItem = createMessage(message, context, userSession, !message.isMyOwnMessage(userSession));
                        realm.copyToRealm(messageItem.getModel());
                    } else
                        messages.remove(i);
                }
            }
        }, onSuccess, onError);

    }

    public static boolean existMessage(Realm realm, String id) {
        MessageModel mess = realm.where(MessageModel.class).equalTo("messageId", id).findFirst();
        return mess != null;
    }

    public static MessageItem createMessage(MOKMessage message, Context context, String userSession, boolean isIncoming){

        //VERIFY IF IT IS A GROUP MESSAGE
        String sid=message.getRid().contains("G")?message.getRid():message.getSid();
        String rid=message.getRid().contains("G")?message.getSid():message.getRid();

        boolean msgIsMyOwn = message.getSid().equals(userSession);
        if(msgIsMyOwn) {//VERIFY IF IS IT MY OWN MESSAGE
            sid = message.getSid();
            rid = message.getRid();
        }

        MonkeyItem.MonkeyItemType type = MonkeyItem.MonkeyItemType.text;
        if (message.getProps().has("file_type")) {
            if(Integer.parseInt(message.getProps().get("file_type").getAsString())==1)
                type = MonkeyItem.MonkeyItemType.audio;
            else if(Integer.parseInt(message.getProps().get("file_type").getAsString())==3)
                type = MonkeyItem.MonkeyItemType.photo;
        }

        MessageItem item = new MessageItem(sid, rid, message.getMessage_id(), message.getMsg(),
                Long.parseLong(message.getDatetime()), message.getDatetimeorder(), isIncoming, type);
        item.setParams(message.getParams());
        item.setProps(message.getProps());
        if(!item.getMessageId().contains("-"))
            item.setStatus(MonkeyItem.DeliveryStatus.delivered);

        switch (type){
            case audio:
                if(item.getParams().has("length"))
                    item.setDuration(item.getParams().get("length").getAsLong());
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

    public static RealmResults<MessageModel> getMessages(Realm realm, String mySessionId, String conversationId){

        final RealmResults<MessageModel> myMessages;
        if (conversationId.startsWith("G:")) {
            //In a group, all messages are either sent from or sent to an ID that starts with ':G'
            //Let's get all those messages.
            myMessages = realm.where(MessageModel.class).equalTo("recieverSessionId", conversationId).or().equalTo("senderSessionId", conversationId).findAllAsync();
        } else {
            //If this is not a group, then it's probably the mirror chat,
            //Let's just get all messages from DB.
            myMessages = realm.where(MessageModel.class)
                    .beginGroup()
                    .contains("recieverSessionId", conversationId).not().beginsWith("senderSessionId", "G:", Case.SENSITIVE)
                    .equalTo("senderSessionId", mySessionId)
                    .endGroup()
                    .or()
                    .beginGroup()
                    .equalTo("senderSessionId", conversationId).not().beginsWith("recieverSessionId", "G:", Case.SENSITIVE)
                    .endGroup()
                    .findAllAsync();
        }

        return myMessages;

    }

    public static void deleteAll(Realm realm){
        realm.beginTransaction();
        realm.where(MessageModel.class).findAll().deleteAllFromRealm();
        realm.commitTransaction();
    }

    public static void updateMessageDownloadingStatus(Realm realm, MessageModel model, boolean isDownloading) {
        realm.beginTransaction();
        if(model != null){
            model.setDownloading(isDownloading);
        }
        realm.commitTransaction();

    }

    public static void updateMessageOutgoingStatusBlocking(Realm realm, final String messageId,
                                                   final MonkeyItem.DeliveryStatus outgoingMessageStatus){
        MessageModel result = realm.where(MessageModel.class).equalTo("messageId", messageId).findFirst();
        if (result != null)
            result.setStatus(outgoingMessageStatus.ordinal());
    }

    public static void updateMessageOutgoingStatus(Realm realm, final String messageId, final MonkeyItem.DeliveryStatus outgoingMessageStatus) {
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                updateMessageOutgoingStatusBlocking(realm, messageId, outgoingMessageStatus);
            }
        }, new Realm.Transaction.OnSuccess() {
            @Override
            public void onSuccess() {
            }
        }, new Realm.Transaction.OnError() {
            @Override
            public void onError(Throwable error) {
                Log.i("DB", error.getMessage());
            }
        });
    }

    public static void updateMessageOutgoingStatus(Realm realm, final MessageModel model, final MonkeyItem.DeliveryStatus outgoingMessageStatus) {
        updateMessageOutgoingStatus(realm, model.getMessageId(), outgoingMessageStatus);
    }

    public static void markMessagesAsError(Realm realm, final ArrayList<MOKMessage> errorMessages) {

        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                RealmQuery<MessageModel> query = realm.where(MessageModel.class);
                for(MOKMessage errorMsg : errorMessages)
                    query.equalTo("messageId", errorMsg.getMessage_id());

                RealmResults<MessageModel> results = query.findAll();
                for(MessageModel model : results)
                    model.setStatus(MonkeyItem.DeliveryStatus.error.ordinal());
            }
        }, new Realm.Transaction.OnSuccess() {
            @Override
            public void onSuccess() {
            }
        }, new Realm.Transaction.OnError() {
            @Override
            public void onError(Throwable error) {
                Log.i("DB", error.getMessage());
            }
        });


    }

}
