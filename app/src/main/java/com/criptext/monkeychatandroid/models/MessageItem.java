package com.criptext.monkeychatandroid.models;

import android.content.Context;
import android.support.annotation.NonNull;

import com.criptext.monkeychatandroid.MonkeyChat;
import com.criptext.monkeykitui.recycler.MonkeyItem;
import com.criptext.monkeykitui.util.Utils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

/**
 * Created by gesuwall on 4/7/16.
 */
public class MessageItem implements MonkeyItem, Comparable<MessageItem> {

    public MessageModel model;

    private String senderSessionId, recieverSessionId, messageId, messageContent;
    private long timestamp, timestampOrder;
    private boolean isIncoming;
    private DeliveryStatus status;
    private MonkeyItemType itemType;
    /*AUDIO*/
    private long duration;
    /*PHOTO*/
    private String placeHolderFilePath;

    private JsonObject params;
    private JsonObject props;
    private boolean isDownloading;

    public MessageItem(String senderId, String recieverId, String messageId, String messageContent, long timestamp,
                       long timestampOrder, boolean isIncoming, MonkeyItemType itemType){
        this.senderSessionId = senderId;
        this.recieverSessionId = recieverId;
        this.messageId = messageId;
        this.messageContent = messageContent;
        this.timestamp = timestamp;
        this.timestampOrder = timestampOrder;
        this.isIncoming = isIncoming;
        this.itemType = itemType;
        this.placeHolderFilePath = "";
        this.duration = 0;
        this.status = DeliveryStatus.sending;
        this.isDownloading = false;

        model = new MessageModel(senderId, recieverId, messageId, messageContent, timestamp, isIncoming, itemType);
    }

    public MessageItem(MessageModel messageModel){

        this.senderSessionId = messageModel.getSenderSessionId();
        this.recieverSessionId = messageModel.getRecieverSessionId();
        this.messageId = messageModel.getMessageId();
        this.messageContent = messageModel.getMessageContent();
        this.timestamp = messageModel.getTimestamp();
        this.isIncoming = messageModel.isIncoming();
        this.itemType = MonkeyItemType.values()[messageModel.getItemType()];
        this.placeHolderFilePath = messageModel.getPlaceHolderFilePath();
        this.duration = messageModel.getDuration();
        this.status = DeliveryStatus.values()[messageModel.getStatus()];
        this.isDownloading = messageModel.isDownloading();

        if(messageModel.getParams().length()>0){
            JsonParser parser = new JsonParser();
            this.params = parser.parse(messageModel.getParams()).getAsJsonObject();
        }

        if(messageModel.getProps().length()>0){
            JsonParser parser = new JsonParser();
            this.props = parser.parse(messageModel.getProps()).getAsJsonObject();
        }

        model = messageModel;
    }

    public MessageModel getModel() {
        return model;
    }

    public void setStatus (DeliveryStatus status){
        this.status = status;
    }

    public void setDuration(long duration) {
        this.duration = duration;
        model.setDuration(duration);
    }

    public void setPlaceHolderFilePath(String placeHolderFilePath) {
        this.placeHolderFilePath = placeHolderFilePath;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
        model.setMessageContent(messageContent);
    }

    public JsonObject getParams() {
        return params;
    }

    public void setParams(JsonObject params) {
        this.params = params;
        model.setParams(params.toString());
    }

    public JsonObject getProps() {
        return props;
    }

    public void setProps(JsonObject props) {
        this.props = props;
        model.setProps(props.toString());
    }

    public boolean isDownloading() {
        return isDownloading;
    }

    public void setDownloading(boolean downloading) {
        isDownloading = downloading;
    }

    public String getSenderId(){
        return senderSessionId.startsWith("G:")?recieverSessionId:senderSessionId;
    }

    @NotNull
    @Override
    public String getContactSessionId() {
        return senderSessionId.startsWith("G:")?recieverSessionId:senderSessionId;
    }

    @Override
    public long getMessageTimestamp() {
        return timestamp;
    }

    @NotNull
    @Override
    public String getMessageId() {
        return messageId;
    }

    @Override
    public boolean isIncomingMessage() {
        return isIncoming;
    }

    @NotNull
    @Override
    public DeliveryStatus getDeliveryStatus() {
        return status;
    }

    @Override
    public int getMessageType() {
        return itemType.ordinal();
    }

    @NotNull
    @Override
    public String getMessageText() {
        return messageContent;
    }

    @NotNull
    @Override
    public String getFilePath() {
        return messageContent;
    }

    @NotNull
    @Override
    public String getPlaceholderFilePath() {
        return placeHolderFilePath;
    }

    @Override
    public long getFileSize() {
        return 0;
    }

    @Override
    public long getAudioDuration() {
        return duration;
    }

    /**CUSTOM FUNCTIONS**/

    public static ArrayList<MonkeyItem> insertSortCopy(List<MessageModel> realmlist){

        ArrayList<MonkeyItem> arrayList = new ArrayList<>();
        int total = realmlist.size();
        for(int i = 0; i < total; i++ ){
            MessageModel temp = realmlist.get(i);
            arrayList.add(new MessageItem(temp));
            int j;
            for(j = i - 1; j >= 0 && temp.getTimestamp() < realmlist.get(j).getTimestamp(); j-- )
                Collections.swap(arrayList, j, j + 1);
        }

        return arrayList;
    }

    @Override
    public long getMessageTimestampOrder() {
        return timestampOrder;
    }

    @NotNull
    @Override
    public String getOldMessageId() {
        return null;
    }

    @Override
    public int compareTo(@NonNull MessageItem another) {
        long stamp1 = getMessageTimestampOrder(), stamp2 = another.getMessageTimestampOrder();
        if(stamp1 < stamp2)
            return -1;
        else if(stamp1 > stamp2)
            return 1;
        else
            return 0;
    }
}
