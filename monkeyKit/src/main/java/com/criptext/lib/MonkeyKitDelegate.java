package com.criptext.lib;

import com.criptext.comunication.MOKMessage;
import com.google.gson.JsonObject;

import org.json.JSONObject;

import java.util.ArrayList;

public interface MonkeyKitDelegate {

    /**
     * Cuando MonkeyKit se conecta, realiza varios requerimientos HTTP antes de conectar el socket.
     * Si alguno de estos falla por errores de conexion, MonkeyKit mostrara la excepcion al desarrollador
     * a traves de este callback. MonkeyKit automaticamente tratara de reconectarse. Si tienes problemas
     * de conexion, seria muy util ver las excepciones que se arrojan aqui y contactar soporte.
     * @param exception La excepcion que se arrojo durante el error de conexion.
     */
    void onNetworkError(Exception exception);

    /**
     * Callback executed when the server refused the connection because the credentials could
     * not be validated. The service won't attempt to reconnect. You should show a message to the
     * user informing that the service is unavailable. Retry after checking your credentials.
     */
    void onConnectionRefused();

    /**
     * Cuando MonkeyKit logra conectar el socket, y esta listo para enviar y recibir mensajes, ejecuta
     * este callback. Este es un buen momento para hacer "sendSet" para decirle a los otros usuarios
     * que estas online.
     */
    void onSocketConnected();

    /**
     * Periodicamente el socket de MonkeyKit se desconecta. MonkeyKit automaticamente se volvera a
     * conectar, pero este es un buen momento para mostrarle al usuario que el socket se esta
     * reconectando.
     */
    void onSocketDisconnected();

    /**
     * Despues de crear un grupo con el metodo createGroup, el servidor respondera con el ID del
     * grupo si no ocurre ningun error. Cuando la respuesta este lista, MonkeyKit ejecutara este
     * callback. Aqui se deberia persistir ese ID con los demas datos de tu grupo como el nombre y
     * la lista de miembros y ya puedes crear una vista para que el usuario comience a enviar y
     * recibir mensajes a traves de este nuevo grupo.
     * @param grupoID ID del nuevo grupo. Para enviar mensajes a este grupo los mensajes deben de
     *                enviarse con este ID como RID
     */
    void onCreateGroupOK(String grupoID);

    /**
     * Si ocurre algun error con la respuesta del servidor despues de llamar al metodo createGroup
     * en este callback se envia el mensaje de error. La implementacion de este callback mostrar un
     * mensaje de error y/o volver a intentar.
     * @param errmsg Mensaje de error en la respuesta del servidor al crear grupo
     */
    void onCreateGroupError(String errmsg);

    /**
     * Despues de eliminar un grupo con el metodo deleteGroup, el servidor borrara el grupo de la
     * base de datos remota. Cuando llegue la respuesta, y si no hay algun error se ejecutara este
     * callback. MonkeyKit garantiza que ya no se podran recibir ni enviar mensajes a este grupo
     * antes de llamar a esta funcion por lo cual en esta implementacion se puede borrar el grupo
     * de la base de datos local de la aplicacion.
     * @param grupoID id del grupo eliminado
     */
    void onDeleteGroupOK(String grupoID);

    /**
     * Si ocurre algun error con la respuesta del servidor despues de llamar al metodo deleteGroup
     * en este callback se envia el mensaje de error. La implementacion de este callback mostrar un
     * mensaje de error y/o volver a intentar.
     * @param errmsg Mensaje de error en la respuesta del servidor al eliminar grupo
     */
    void onDeleteGroupError(String errmsg);

    /**
     * Despues de pedir la informacion de un grupo con el metodo getGroupInfo(), el servidor respondera
     * con un JSON que contenga la informacion del grupo. Cuando llegue la respuesta, y si no hay
     * algun error se ejecutara este callback. La implementacion de este callback debe de guardar
     * la informacion del grupo en la base de datos local.
     * @param json JsonObject con la informacion del grupo requerida. Contiene 3 atributos:
     *             - "group_id" : un String con el ID del grupo
     *             - "members" : un JsonArray con los session ID de cada miembro del grupo
     *             - "group_info" : JSsonObject que contiene el nombre del grupo en el atributo "name"
     */
    void onGetGroupInfoOK(JsonObject json);

    /**
     * Si ocurre algun error con la respuesta del servidor despues de llamar al metodo getGroupInfo
     * en este callback se envia el mensaje de error. La implementacion de este callback mostrar un
     * mensaje de error y/o volver a intentar.
     * @param errmsg Mensaje de error en la respuesta del servidor al pedir informacion del grupo
     */
    void onGetGroupInfoError(String errmsg);

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
     * @param sessionID
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
