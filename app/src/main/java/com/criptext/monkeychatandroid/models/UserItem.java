package com.criptext.monkeychatandroid.models;

import com.activeandroid.annotation.Column;
import com.criptext.monkeykitui.conversation.MonkeyConversation;
import com.criptext.monkeykitui.recycler.MonkeyInfo;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by hirobreak on 05/10/16.
 */
public class UserItem implements MonkeyInfo {
    @Column(name = "idUser", unique = true)
    private String monkeyId;
    @Column(name = "name")
    private String name;
    @Column(name = "rol")
    private String rol;
    @Column(name = "avatarFilePath")
    public String avatarFilePath;
    @Column(name = "status")
    public String status;

    public UserItem(){
        super();
    }

    public UserItem(String idUser, String name, String rol, String avatarFilePath, String status) {
        super();
        this.monkeyId = idUser;
        this.name = name;
        this.rol = rol;
        this.avatarFilePath = avatarFilePath;
        this.status = status;
    }

    public UserItem(String monkeyId, MonkeyInfo user){
        this.monkeyId = monkeyId;
        this.name = user.getTitle();
        this.rol = user.getSubtitle();
        this.avatarFilePath = user.getAvatarUrl();
        this.status = user.getSubtitle();
    }

    public void setId(String monkeyId) {
        this.monkeyId = monkeyId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void rol(String rol) { this.rol = rol; }

    public void setAvatarFilePath(String avatarFilePath) {
        this.avatarFilePath = avatarFilePath;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMonkeyId() {
        return monkeyId;
    }

    @Nullable
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
        return status;
    }

    @NotNull
    @Override
    public String getRightTitle() {
        return rol;
    }

    @NotNull
    @Override
    public String getInfoId() {
        return monkeyId;
    }
}