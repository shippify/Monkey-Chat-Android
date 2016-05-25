package com.criptext.lib;

import com.criptext.comunication.MOKMessage;
import com.google.gson.JsonObject;
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
	 * Al recibir un mensaje, MonkeyKit ejecuta este callback. El mensaje ya fue guardado en la base.
	 * La implementacion de este metodo debe de notificar al usuario que ha recibido el mensaje.
	 * @param message Objeto MOKMessage que representa al mensaje recibido.
	 */
	void onMessageRecieved(MOKMessage message);

	/**
	 * Cuando el mensaje es recibido en en servidor, MonkeyKit recibe un Ack.  En este callback el
	 * mensaje llega con un nuevo ID, uno que es positivo a diferencia del valor negativo que se
	 * coloca cuando el mensaje esta transito. se puede obtener el ID anterior con el metodo getOldID()
	 * del MOKMessage.$La implementacion de este callback debe de marcar el  mensaje como "enviado" o
	 * leido dependiendo del valor de getStatus() del MOKMessage y actualizar el ID.
	 * @param message mensaje transmitido satisfactoriamente al servidor.
	 */
	void onAcknowledgeRecieved(MOKMessage message);

	/**
	 * Cuando un mensaje es borrado en el servidor, MonkeyKit recibe una notificacion. La
	 * implementacion de este callback puede optar por borrar el mensaje de la base de datos local
	 * del dispositivo.
	 * @param message MOKMessage representando el mensaje a borrar. EL MOKMesssage no tiene datos del
	 *                mensaje, unicamente el ID.
	 */
	void onDeleteRecieved(MOKMessage message);


	/**
	 *Cuando un contacto abre una conversacion con el usuario se ejecuta este callback. La implementacion
	 * de este callback debe de marcar como leido los mensajes que se le enviaron a ese contacto.
	 * @param sessionID
	 */
	void onContactOpenMyConversation(String sessionID);

	/**
	 * Al recibir una notificacion, MonkeyKit ejecuta este callback. La implementacion de este metodo
	 * debe de procesar la notificacion y notificar al usuario la informacion relevante.
	 * @param notification Objeto MOKMessage que representa a la notificacion recibida.
	 */
	void onNotificationReceived(MOKMessage notification);

	/**
	 * Despues de ejecutar un sync o un get, MonkeyKit recibe todos los mensajes que le debieron haber
	 * llegado mientras estaba desconectado. En este callback se reciben todos esos mensajes tras
	 * guardarlos en la base de datos. La implementacion de este metodo debe de actualizar las conversaciones
	 * que tienen nuevos mensajes
	 * @param messages La lista de mensajes recibidos.
	 */
	void onMessageBatchReady(ArrayList<MOKMessage> messages);
}
