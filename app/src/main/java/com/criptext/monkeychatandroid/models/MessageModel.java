package com.criptext.monkeychatandroid.models;

import com.criptext.monkeykitui.recycler.MonkeyItem;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by daniel on 4/26/16.
 */
public class MessageModel extends RealmObject {

    @PrimaryKey
    private String messageId;
    private String senderSessionId;
    private String recieverSessionId;
    private String messageContent;
    private long timestamp;
    private boolean isIncoming;
    private Integer status;
    private Integer itemType;

    /*AUDIO*/
    private long duration;
    /*PHOTO*/
    private String placeHolderFilePath;

    private String params;
    private String props;
    private boolean isDownloading;

    public MessageModel(){

    }

    public MessageModel(String senderId, String recieverId, String messageId, String messageContent, long timestamp,
                            boolean isIncoming, MonkeyItem.MonkeyItemType itemType){
        setSenderSessionId(senderId);
        setRecieverSessionId(recieverId);
        setMessageId(messageId);
        setMessageContent(messageContent);
        setTimestamp(timestamp);
        setIncoming(isIncoming);
        setItemType(itemType.ordinal());
        setStatus(0);
        setDuration(0);
        setPlaceHolderFilePath("");
        setDownloading(false);
        setParams("");
        setProps("");
    }

    public MessageModel(String senderId, String recieverId, String messageId, String messageContent, long timestamp,
                        boolean isIncoming, int itemType){
        setSenderSessionId(senderId);
        setRecieverSessionId(recieverId);
        setMessageId(messageId);
        setMessageContent(messageContent);
        setTimestamp(timestamp);
        setIncoming(isIncoming);
        setItemType(itemType);
        setStatus(0);
        setDuration(0);
        setPlaceHolderFilePath("");
        setDownloading(false);
        setParams("");
        setProps("");
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderSessionId() {
        return senderSessionId;
    }

    public void setSenderSessionId(String senderSessionId) {
        this.senderSessionId = senderSessionId;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isIncoming() {
        return isIncoming;
    }

    public void setIncoming(boolean incoming) {
        isIncoming = incoming;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getItemType() {
        return itemType;
    }

    public void setItemType(Integer itemType) {
        this.itemType = itemType;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getPlaceHolderFilePath() {
        return placeHolderFilePath;
    }

    public void setPlaceHolderFilePath(String placeHolderFilePath) {
        this.placeHolderFilePath = placeHolderFilePath;
    }

    public String getRecieverSessionId() {
        return recieverSessionId;
    }

    public void setRecieverSessionId(String recieverSessionId) {
        this.recieverSessionId = recieverSessionId;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public String getProps() {
        return props;
    }

    public void setProps(String props) {
        this.props = props;
    }

    public boolean isDownloading() {
        return isDownloading;
    }

    public void setDownloading(boolean downloading) {
        isDownloading = downloading;
    }
}
