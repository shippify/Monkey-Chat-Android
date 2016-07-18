package com.criptext.database;

import com.criptext.comunication.MOKMessage;
import com.criptext.comunication.MessageTypes;

/**
 * Created by daniel on 12/28/15.
 */
public class CriptextDBHandler {

    /** Retorna el tipo de accion de un mensaje de MonkeyKit. La acción puede estar en props o en params.
     * props tiene prioridad, si no encuentra el action ahi, lo busca en params. Después se mapean los tipos
     * de MonkeyKit con los valores de Criptext
     * @param message el mensaje recibido desde MonkeyKit
     * @return el tipo de acción del mensaje
     */
    public static int getMonkeyActionType(MOKMessage message){
        int tipo=0;
        if(message.getProps() != null) {
            if (message.getProps().has("type"))
                tipo = Integer.parseInt(message.getProps().get("type").getAsString());
            if (message.getProps().has("file_type"))
                tipo = Integer.parseInt(message.getProps().get("file_type").getAsString());
            if (message.getProps().has("monkey_action"))
                tipo = message.getProps().get("monkey_action").getAsInt();
        }
        //El tipo de shareContat se encuentra en params
        if(message.getParams() != null && message.getParams().has("type")){
            return message.getParams().get("type").getAsInt();//Puede ser blMessageShareAFriend, blMessageChangeAvatar, etc
        }
        System.out.println("msj de MonkeyActionType: " + message.getMonkeyAction());
        switch(message.getMonkeyAction()) {
            case com.criptext.comunication.MessageTypes.MOKGroupCreate: //MOKGroupCreate
                return MessageTypes.blMessageGroupAdded;
            case 2: //
                break;
            case com.criptext.comunication.MessageTypes.MOKGroupNewMember:
                return MessageTypes.blMessageGroupNewMember;
            case com.criptext.comunication.MessageTypes.MOKGroupRemoveMember:
                return MessageTypes.blMessageGroupRemovedMember;
            case com.criptext.comunication.MessageTypes.MOKGroupJoined:
                return MessageTypes.blMessageUserGroupsUpdate;
        }

        return tipo;
    }

}
