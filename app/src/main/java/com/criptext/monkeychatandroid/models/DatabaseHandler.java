package com.criptext.monkeychatandroid.models;

import android.content.Context;
import android.util.Log;

import com.criptext.comunication.MOKMessage;
import com.criptext.comunication.MessageTypes;
import com.criptext.database.CriptextDBHandler;
import com.criptext.monkeychatandroid.MonkeyChat;
import com.criptext.monkeykitui.recycler.MonkeyItem;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by daniel on 4/26/16.
 */
public class DatabaseHandler {

    public static void saveMessage(final MessageItem messageItem, final Realm.Transaction.OnSuccess onSuccess, final Realm.Transaction.OnError onError) {

        Realm realm = MonkeyChat.getInstance().getMonkeyKitRealm();
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                if (!existMessage(realm, messageItem.getMessageId())) //NO DUPLICATED
                    realm.copyToRealm(messageItem.getModel());
            }
        }, onSuccess, onError);
    }

    public static void saveMessageBatch(final ArrayList<MOKMessage> messages, final Context context, final String userSession, final Realm.Transaction.OnSuccess onSuccess, final Realm.Transaction.OnError onError) {

        final WeakReference<Context> weakContext = new WeakReference<>(context);
        Realm realm = MonkeyChat.getInstance().getMonkeyKitRealm();
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {

                for(int i = messages.size() - 1; i > -1; i--){
                    MOKMessage message = messages.get(i);
                    boolean continuar=true;
                    if(message.getProps().has("old_id")){
                        if(existMessage(realm, message.getProps().get("old_id").getAsString()))
                            continuar=false;
                    }
                    if(continuar && !existMessage(realm, message.getMessage_id())){
                        MessageItem messageItem = createMessage(message, context, userSession, true);
                        realm.copyToRealmOrUpdate(messageItem.getModel());
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
                message.getDatetimeorder(), isIncoming, type);
        item.setParams(message.getParams());
        item.setProps(message.getProps());

        switch (type){
            case audio:
                if(item.getParams().has("length"))
                    item.setDuration(MonkeyChat.milliSecondsToTimer(item.getParams().get("length").getAsLong()));
                if(!item.getMessageText().contains(context.getCacheDir().toString()))
                    item.setMessageContent(context.getCacheDir()+"/"+message.getMsg());
                break;
            case photo:
                if(!item.getMessageText().contains(context.getCacheDir().toString()))
                    item.setMessageContent(context.getCacheDir()+"/"+message.getMsg());
                break;
        }

        return item;
    }

    public static RealmResults<MessageModel> getMessages(String id, String userSession){

        Realm realm = MonkeyChat.getInstance().getMonkeyKitRealm();
        final RealmResults<MessageModel> myMessages;
        if (id.startsWith("G:")) {
            //GRUPO MESSAGES
            myMessages = realm.where(MessageModel.class).equalTo("recieverSessionId", id).or().equalTo("senderSessionId", id).findAllAsync();
        } else {
            myMessages = realm.where(MessageModel.class)
                    .beginGroup()
                    .contains("recieverSessionId", id).not().beginsWith("senderSessionId", "G:", Case.SENSITIVE)
                    .equalTo("senderSessionId", userSession)
                    .endGroup()
                    .or()
                    .beginGroup()
                    .equalTo("senderSessionId", id).not().beginsWith("recieverSessionId", "G:", Case.SENSITIVE)
                    .endGroup()
                    .findAllAsync();
        }

        return myMessages;

    }

    public static void deleteAll(){
        Realm realm = MonkeyChat.getInstance().getMonkeyKitRealm();
        realm.beginTransaction();
        realm.where(MessageModel.class).findAll().deleteAllFromRealm();
        realm.commitTransaction();
    }

    public static void updateMessageDownloadingStatus(MessageModel model, boolean isDownloading) {

        Realm realm = MonkeyChat.getInstance().getMonkeyKitRealm();
        realm.beginTransaction();
        if(model != null){
            model.setDownloading(isDownloading);
        }
        realm.commitTransaction();

    }

    public static void updateMessageOutgoingStatus(final MessageModel model, final MonkeyItem.OutgoingMessageStatus outgoingMessageStatus) {

        Realm realm = MonkeyChat.getInstance().getMonkeyKitRealm();
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                MessageModel result = realm.where(MessageModel.class).equalTo("messageId", model.getMessageId()).findFirst();
                if(result!=null)
                    result.setStatus(outgoingMessageStatus.ordinal());
            }
        }, new Realm.Transaction.OnSuccess() {
            @Override
            public void onSuccess() {
                Log.i("DB", "Success updating outgoing status");
            }
        }, new Realm.Transaction.OnError() {
            @Override
            public void onError(Throwable error) {
                Log.i("DB", error.getMessage());
            }
        });





    }

}
