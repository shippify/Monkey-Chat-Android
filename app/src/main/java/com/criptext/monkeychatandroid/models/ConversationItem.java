package com.criptext.monkeychatandroid.models;

import com.criptext.monkeykitui.conversation.MonkeyConversation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by daniel on 8/24/16.
 */
public class ConversationItem implements MonkeyConversation{

    private String id;
    private String name;
    private long datetime;
    private String secondaryText;
    private int totalNewMessage;
    private boolean isGroup;
    public String groupMembers;
    public String avatarFilePath;
    public int status;

    public ConversationItem(String id, String name, long datetime, String secondaryText, int totalNewMessage, boolean isGroup, String groupMembers, String avatarFilePath, int status) {
        this.id = id;
        this.name = name;
        this.datetime = datetime;
        this.secondaryText = secondaryText;
        this.totalNewMessage = totalNewMessage;
        this.isGroup = isGroup;
        this.groupMembers = groupMembers;
        this.avatarFilePath = avatarFilePath;
        this.status = status;
    }

    @NotNull
    @Override
    public String getId() {
        return id;
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
}
