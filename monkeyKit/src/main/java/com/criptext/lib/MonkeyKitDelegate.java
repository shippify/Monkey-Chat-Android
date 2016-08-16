package com.criptext.lib;

import com.criptext.comunication.MOKConversation;
import com.criptext.comunication.MOKMessage;
import com.google.gson.JsonObject;
import java.util.ArrayList;

public interface MonkeyKitDelegate {

    /**
     * Callback executed when the server refused the connection because the credentials could
     * not be validated. The service won't attempt to reconnect. You should show a message to the
     * user informing that the service is unavailable. Retry after checking your credentials.
     */
    void onConnectionRefused();

    /**
     * When MonkeyKit connect to the socket successfully and is ready to send and receive messages.
     * After this happens it is recommend to use the sendSet function to notify all the users that you are online.
     */
    void onSocketConnected();

    /**
     * Our socket can get disconnect for network reasons but MonkeyKit will reconnect automatically. It is
     * important to notify to the users that are disconnected.
     */
    void onSocketDisconnected();

    /**
     * Callback executed when a file download operation finishes, successfully or not.
     * With this callback you should update your UI to show an error message or a download
     * complete message, depending on the result.
     * @param fileMessageId The downloaded file's message id
     * @param success true if the file was successfully downloaded, otherwise false
     */
    void onFileDownloadFinished(String fileMessageId, boolean success);

    /**
     * After create a group with createGroup method, the server responds with the group ID
     * using this delegate. Use this ID as rid to send messages.
     * @param groupID ID of the new group.
     */
    void onCreateGroup(String groupID, Exception e);

    /**
     * After add a group member with removeGroupMember method, the server will update the group from a remote DB.
     * We recommend to update your group from your local DB as well.
     * @param members new members of the group.
     */
    void onAddGroupMember(String members, Exception e);

    /**
     * After delete a group member with removeGroupMember method, the server will update the group from a remote DB.
     * We recommend to update your group from your local DB as well.
     * @param members new members of the group.
     */
    void onRemoveGroupMember(String members, Exception e);

    /**
     * This method can use it for groups and users, this function will give you a JSON with the information required.
     * @param json JsonObject con la informacion del grupo requerida. Contiene 3 atributos:
     *             - "group_id" : un String con el ID del grupo
     *             - "members" : un JsonArray con los session ID de cada miembro del grupo
     *             - "group_info" : JSsonObject que contiene el nombre del grupo en el atributo "name"
     */
    void onGetInfo(JsonObject json, Exception e);

    /**
     * This function is executed after you update the metadata of a user.
     * If exception is null the user was updated successfully.
     * @param e the exception of the result
     */
    void onUpdateUserData(Exception e);

    /**
     * This function is executed after you update the metadata of a group.
     * If exception is null the group was updated successfully.
     * @param e the exception of the result
     */
    void onUpdateGroupData(Exception e);

    /**
     * This function is executed when you receive all your conversations.
     * @param conversations array of the conversations required.
     * @param e the exception of the result
     */
    void onGetConversations(ArrayList<MOKConversation> conversations, Exception e);

    /**
     * This function is executed when you receive all your conversation messages.
     * @param messages array of the messages required.
     * @param e the exception of the result
     */
    void onGetConversationMessages(ArrayList<MOKMessage> messages, Exception e);

    /**
     * This function is executed when a message arrived and stored in the DB.
     * @param message Objeto MOKMessage que representa al mensaje recibido.
     */
    void onMessageRecieved(MOKMessage message);

    /**
     * When the message arrive to the server, MonkeyKit receive an ack. This callback come with
     * with a new ID. The implementation of this callback mark the message as sent.
     * @param senderId id of the sender
     * @param recipientId id of the recipient
     * @param newId new Id of the message
     * @param oldId old id of the message
     * @param read boolean if the message is read or not
     * @param messageType type of the message 1 text, 2 file
     */
    void onAcknowledgeRecieved(String senderId, String recipientId, String newId, String oldId, Boolean read, int messageType);

    /**
     * This function is executed after you sent an open message
     * @param senderId sender id of the conversation
     * @param isOnline Boolean if the conversation is online or not
     * @param lastSeen timestamp of the last time the conversation was online
     * @param lastOpenMe timestamp of the last time the conversation open my conversation
     */
    void onConversationOpenResponse(String senderId, Boolean isOnline, String lastSeen, String lastOpenMe, String members_online);

    /**
     * When a message is deleted from the server, MonkeyKit receive a notification.
     * @param messageId id of the message
     * @param senderId id of the sender
     * @param recipientId id of the recipient
     * @param datetime datetime of the message
     */
    void onDeleteRecieved(String messageId, String senderId, String recipientId, String datetime);

    /**
     *Cuando un contacto abre una conversacion con el usuario se ejecuta este callback. La implementacion
     * de este callback debe de marcar como leido los mensajes que se le enviaron a ese contacto.
     * @param sessionID session id of the contact
     */
    void onContactOpenMyConversation(String sessionID);

    /**
     * When you receive a notification, MonkeyKit execute this callback.
     * @param messageId id of the message
     * @param senderId id of the sender
     * @param recipientId id of the recipient
     * @param params JsonObject of the params sent in the message
     * @param datetime datetime of the message
     */
    void onNotificationReceived(String messageId, String senderId, String recipientId, JsonObject params, String datetime);

    /**
     * Despues de ejecutar un sync o un get, MonkeyKit recibe todos los mensajes que le debieron haber
     * llegado mientras estaba desconectado. En este callback se reciben todos esos mensajes tras
     * guardarlos en la base de datos. La implementacion de este metodo debe de actualizar las conversaciones
     * que tienen nuevos mensajes
     * @param messages La lista de mensajes recibidos.
     */
    void onMessageBatchReady(ArrayList<MOKMessage> messages);

    /**
     * Al recibir una notificacion, MonkeyKit ejecuta este callback. La implementacion de este metodo
     * debe de procesar la notificacion y notificar al usuario la informacion relevante.
     * @param message Objeto MOKMessage que representa el mensaje que no se pudo desencriptar.
     */
    void onMessageFailDecrypt(MOKMessage message);

    /**
     * This function is executed when you are added to a group.
     * @param groupid Group id of the group.
     * @param members Group members Ids separated by comma.
     * @param info Json with the group information: Name, admin, etc.
     */
    void onGroupAdded(String groupid, String members, JsonObject info);

    /**
     * This function is executed when a new member is added to one of your groups.
     * @param groupid Group id of the group.
     * @param new_member Id of the new member.
     */
    void onGroupNewMember(String groupid, String new_member);

    /**
     * This function is executed when a new member is removed from one of your groups.
     * @param groupid Group id of the group.
     * @param removed_member Id of the removed member.
     */
    void onGroupRemovedMember(String groupid, String removed_member);

    /**
     * This function is executed when you ask for all your groups.
     * @param groupids Group Ids separated by comma.
     */
    void onGroupsRecover(String groupids);

    /**
     * This function is executed when a file fails upload.
     * @param message MOKMessage of the file.
     */
    void onFileFailsUpload(MOKMessage message);
}
