package com.criptext.monkeychatandroid;

import android.graphics.Color;

import com.criptext.MonkeyKitSocketService;
import com.criptext.comunication.MOKUser;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by daniel on 8/29/16.
 */

public class GroupData implements com.criptext.monkeykitui.recycler.GroupChat{

    private String conversationId;
    private String membersIds;
    private HashMap<String, MOKUser> mokUserHashMap;
    private boolean askingUsers = false;
    private MonkeyKitSocketService service;

    public GroupData(String conversationId, String members, MonkeyKitSocketService service){
        this.conversationId = conversationId;
        this.membersIds = members;
        this.service = service;
        mokUserHashMap = new HashMap<>();
        //TODO GET MEMBERS FROM DB
        for(String memberId: membersIds.split(",")){
            if(mokUserHashMap.get(memberId)==null){
                getMembers();
                askingUsers = true;
                break;
            }
        }
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setMembers(String conversationId, ArrayList<MOKUser> mokUsers){
        if(conversationId.equals(this.conversationId)) {
            askingUsers = false;
            mokUserHashMap.clear();
            for (MOKUser mokUser : mokUsers) {
                mokUserHashMap.put(mokUser.getMonkeyId(), mokUser);
            }
        }
    }

    private void getMembers(){
        if(!askingUsers && service!=null){
            service.getUsersInfo(membersIds);
        }
    }

    @NotNull
    @Override
    public String getMemberName(@NotNull String monkeyId) {
        MOKUser mokUser = mokUserHashMap.get(monkeyId);
        String finalName = "Unknown";
        if(mokUser!=null && mokUser.getInfo()!=null && mokUser.getInfo().has("name")){
            finalName = mokUser.getInfo().get("name").getAsString();
        }
        return finalName;
    }

    @Override
    public int getMemberColor(@NotNull String monkeyId) {
        return Color.BLUE;
    }
}
