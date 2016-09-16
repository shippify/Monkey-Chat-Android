package com.criptext.monkeychatandroid.models;

import android.support.annotation.NonNull;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.criptext.monkeykitui.recycler.MonkeyItem;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by gesuwall on 4/7/16.
 */

@Table(name = "MessageItem")
public class MessageItem extends Model implements MonkeyItem, Comparable<MessageItem> {

    @Column(name = "senderSessionId", index = true)
    public String senderSessionId;
    @Column(name = "receiverSessionId", index = true)
    public String receiverSessionId;
    @Column(name = "messageId")
    public String messageId;
    @Column(name = "oldMessageId")
    public String oldMessageId;
    @Column(name = "messageContent")
    public String messageContent;
    @Column(name = "timestamp")
    public long timestamp;
    @Column(name = "timestampOrder")
    public long timestampOrder;
    @Column(name = "isIncoming")
    public boolean isIncoming;
    @Column(name = "status")
    public int status;//DeliveryStatus
    @Column(name = "itemType")
    public int itemType;//MonkeyItemType
    @Column(name = "audioDuration")
    public long audioDuration;
    @Column(name = "params")
    public String params;//JsonObject
    @Column(name = "props")
    public String props;//JsonObject

    public MessageItem(){
        super();
    }

    public MessageItem(String senderId, String receiverId, String messageId, String messageContent, long timestamp,
                       long timestampOrder, boolean isIncoming, MonkeyItemType itemType){
        super();
        this.senderSessionId = senderId;
        this.receiverSessionId = receiverId;
        this.messageId = messageId;
        this.messageContent = messageContent;
        this.timestamp = timestamp;
        this.timestampOrder = timestampOrder;
        this.isIncoming = isIncoming;
        this.itemType = itemType.ordinal();
        this.audioDuration = 0;
        this.status = DeliveryStatus.sending.ordinal();
    }

    public void setStatus (int status){
        this.status = status;
    }

    public void setAudioDuration(long audioDuration) {
        this.audioDuration = audioDuration;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public String getParams() {
        return params;
    }

    public JsonObject getJsonParams() {
        return new JsonParser().parse(params).getAsJsonObject();
    }

    public void setParams(String params) {
        this.params = params;
    }

    public String getProps() {
        return props;
    }

    public JsonObject getJsonProps() {
        return new JsonParser().parse(props).getAsJsonObject();
    }

    public void setProps(String props) {
        this.props = props;
    }

    public String getConversationId(){
        return receiverSessionId.startsWith("G:")? receiverSessionId :senderSessionId;
    }

    public String getReceiverSessionId(){
        return receiverSessionId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public void setOldMessageId(String oldMessageId) {
        this.oldMessageId = oldMessageId;
    }

    @NotNull
    @Override
    public String getSenderId() {
        return senderSessionId.startsWith("G:")? receiverSessionId :senderSessionId;
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
        return DeliveryStatus.values()[status];
    }

    @Override
    public int getMessageType() {
        return itemType;
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
        return "";
    }

    @Override
    public long getFileSize() {
        return 0;
    }

    @Override
    public long getAudioDuration() {
        return audioDuration*1000;
    }

    /**CUSTOM FUNCTIONS**/

    public static ArrayList<MonkeyItem> insertSortCopy(List<MessageItem> realmlist){

        ArrayList<MonkeyItem> arrayList = new ArrayList<>();
        int total = realmlist.size();
        for(int i = 0; i < total; i++ ){
            MessageItem temp = realmlist.get(i);
            arrayList.add(temp);
            int j;
            for(j = i - 1; j >= 0 && temp.getMessageTimestamp() < realmlist.get(j).getMessageTimestamp(); j-- )
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
        return oldMessageId;
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
