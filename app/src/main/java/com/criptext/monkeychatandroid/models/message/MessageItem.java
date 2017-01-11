package com.criptext.monkeychatandroid.models.message;

import android.support.annotation.NonNull;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.criptext.monkeykitui.recycler.MonkeyItem;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jetbrains.annotations.NotNull;

/**
 * Created by gesuwall on 4/7/16.
 */

@Table(name = "MessageItem")
public class MessageItem extends Model implements MonkeyItem, Comparable<MessageItem> {

    @Column(name = "audioDuration")
    public long audioDuration;
    @Column(name = "conversationId", index = true)
    public String conversationId;
    @Column(name = "fileSize")
    public Long fileSize;
    @Column(name = "isIncoming")
    public boolean isIncoming;
    @Column(name = "itemType")
    public int itemType;//MonkeyItemType
    @Column(name = "messageId", unique = true)
    public String messageId;
    @Column(name = "messageContent")
    public String messageContent;
    @Column(name = "oldMessageId")
    public String oldMessageId;
    @Column(name = "params")
    public String params;//JsonObject
    @Column(name = "props")
    public String props;//JsonObject
    @Column(name = "senderMonkeyId")
    public String senderMonkeyId;
    @Column(name = "status")
    public int status;//DeliveryStatus
    @Column(name = "timestamp")
    public long timestamp;
    @Column(name = "timestampOrder", index = true)
    public long timestampOrder;

    public MessageItem(){
        super();
    }

    public MessageItem(String senderId, String conversationId, String messageId, String messageContent, long timestamp,
                       long timestampOrder, boolean isIncoming, MonkeyItemType itemType){
        super();
        this.audioDuration = 0;
        this.conversationId = conversationId;
        this.fileSize = 0L;
        this.isIncoming = isIncoming;
        this.itemType = itemType.ordinal();
        this.messageContent = messageContent;
        this.messageId = messageId;
        this.senderMonkeyId = senderId;
        this.status = DeliveryStatus.sending.ordinal();
        this.timestamp = timestamp;
        this.timestampOrder = timestampOrder;
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

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public void setOldMessageId(String oldMessageId) {
        this.oldMessageId = oldMessageId;
    }

    @NotNull
    @Override
    public String getConversationId(){
        return conversationId;
    }

    @NotNull
    @Override
    public String getSenderId() {
        return senderMonkeyId;
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
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    @Override
    public long getAudioDuration() {
        return audioDuration*1000;
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

    public boolean isGroupMessage() {
        return getSenderId().startsWith("G:");
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
