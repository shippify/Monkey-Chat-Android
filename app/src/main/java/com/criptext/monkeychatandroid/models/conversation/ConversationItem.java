package com.criptext.monkeychatandroid.models.conversation;

import android.util.Log;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.criptext.monkeykitui.conversation.MonkeyConversation;
import com.criptext.monkeykitui.recycler.MonkeyInfo;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


@Table(name = "ConversationItem")
public class ConversationItem extends Model implements MonkeyConversation, MonkeyInfo{

    @Column(name = "idConv", unique = true)
    private String idConv;
    @Column(name = "name")
    private String name;
    @Column(name = "datetime")
    private long datetime;
    @Column(name = "secondaryText")
    private String secondaryText;
    @Column(name = "totalNewMessage")
    private int totalNewMessage;
    @Column(name = "isGroup")
    private boolean isGroup;
    @Column(name = "groupMembers")
    public String groupMembers;
    @Column(name = "avatarFilePath")
    public String avatarFilePath;
    @Column(name = "status")
    public int status;
    @Column(name = "lastRead")
    public long lastRead;
    @Column(name = "lastOpen")
    public long lastOpen;
    @Column(name = "admins")
    public String admins;

    public String membersTyping;

    public ConversationItem(){
        super();
    }


    public ConversationItem(String idConv, String name, long datetime, String secondaryText, int totalNewMessage,
                            boolean isGroup, String groupMembers, String avatarFilePath, int status) {
        super();
        this.idConv = idConv;
        this.name = name;
        this.datetime = datetime;
        this.secondaryText = secondaryText;
        this.totalNewMessage = totalNewMessage;
        this.isGroup = isGroup;
        this.groupMembers = groupMembers;
        this.avatarFilePath = avatarFilePath;
        if(status != ConversationStatus.moreConversations.ordinal())
            this.status = status;
        else
            throw new IllegalArgumentException("ConversationItem should never have moreConversations status");
        this.admins = "";
        lastRead = 0;
        lastOpen = 0;
        membersTyping = "";
    }

    public ConversationItem(MonkeyConversation conversation){
        super();
        this.idConv = conversation.getConvId();
        this.name = conversation.getName();
        this.datetime = conversation.getDatetime();
        this.secondaryText = conversation.getSecondaryText();
        this.totalNewMessage = conversation.getTotalNewMessages();
        this.isGroup = conversation.isGroup();
        this.groupMembers = conversation.getGroupMembers();
        this.avatarFilePath = conversation.getAvatarFilePath();
        this.admins = conversation.getAdmins();
        this.status = conversation.getStatus();
        membersTyping = "";
        if(status == ConversationStatus.moreConversations.ordinal())
            throw new IllegalArgumentException("ConversationItem should never have moreConversations status");
    }

    public void setId(String idConv) {
        this.idConv = idConv;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDatetime(long datetime) {
        this.datetime = datetime;
    }

    public void setSecondaryText(String secondaryText) {
        this.secondaryText = secondaryText;
    }

    public void setTotalNewMessage(int totalNewMessage) {
        this.totalNewMessage = totalNewMessage;
    }

    public void setGroup(boolean group) {
        isGroup = group;
    }

    public boolean isDeletable() {
        return true;
    }

    public void setGroupMembers(String groupMembers) {
        this.groupMembers = groupMembers;
    }

    public void setAvatarFilePath(String avatarFilePath) {
        this.avatarFilePath = avatarFilePath;
    }

    public void setStatus(int status) {
        if(status == ConversationStatus.moreConversations.ordinal())
            throw new IllegalArgumentException("ConversationItem should never have moreConversations status");
        this.status = status;
    }

    public void setAdmins(String admins) {
        this.admins = admins;
    }

    public String getAdmins() {
        return admins;
    }

    public void removeMember(String memberId){
        groupMembers = groupMembers.replace(memberId, "");
        groupMembers = groupMembers.replace(",,", ",");
        if (groupMembers.endsWith(",")) {
            groupMembers = groupMembers.substring(0, groupMembers.length()-1);
        }
    }

    public void addMember(String memberId){
        if(groupMembers.contains(memberId)){
            return;
        }
        if(groupMembers.length() <= 0){
            groupMembers = memberId;
        }else{
            groupMembers = groupMembers.concat("," + memberId);
        }
    }

    @NotNull
    @Override
    public String getConvId() {
        return idConv;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getDatetime() {
        return datetime;
    }

    @NotNull
    @Override
    public String getSecondaryText() {
        if(membersTyping!=null && !membersTyping.isEmpty()){
            return "Someone is typing...";
        }
        return secondaryText;
    }

    @Override
    public int getTotalNewMessages() {
        return totalNewMessage;
    }

    @Override
    public boolean isGroup() {
        return isGroup;
    }

    @NotNull
    @Override
    public String getGroupMembers() {
        return groupMembers;
    }

    @Nullable
    @Override
    public String getAvatarFilePath() {
        return avatarFilePath;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @NotNull
    @Override
    public String getAvatarUrl() {
        return avatarFilePath;
    }

    @NotNull
    @Override
    public String getTitle() {
        return name;
    }

    @NotNull
    @Override
    public String getSubtitle() {
        if(membersTyping != null && !membersTyping.isEmpty()){
            return "Someone is typing...";
        }
        return secondaryText;
    }

    @NotNull
    @Override
    public String getRightTitle() {
        return "";
    }

    @NotNull
    @Override
    public String getInfoId() {
        return idConv;
    }

    @Override
    public void setRightTitle(@NotNull String rightTitle) {

    }

    @Override
    public void setSubtitle(@NotNull String subtitle) {

    }

    @Override
    public int getColor() {
        return 0;
    }

    @Override
    public void setColor(int color) {

    }
}
