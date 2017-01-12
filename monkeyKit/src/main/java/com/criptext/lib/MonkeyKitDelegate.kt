package com.criptext.lib

import com.criptext.comunication.*
import com.google.gson.JsonObject

import com.criptext.comunication.MOKMessage;
import com.criptext.comunication.MOKUser;
import com.criptext.http.HttpSync
import java.util.*

interface MonkeyKitDelegate {

    /**
     * Callback executed when the server refused the connection because the credentials could
     * not be validated. The service won't attempt to reconnect. You should show a message to the
     * user informing that the service is unavailable. Retry after checking your credentials.
     */
    fun onConnectionRefused()

    /**
     * When MonkeyKit connect to the socket successfully and is ready to send and receive messages.
     * After this happens it is recommend to use the sendSet function to notify all the users that you are online.
     */
    fun onSocketConnected()

    /**
     * Our socket can get disconnect for network reasons but MonkeyKit will reconnect automatically. It is
     * important to notify to the users that they are disconnected.
     */
    fun onSocketDisconnected()

    /**
     * Callback executed when a file download operation finishes, successfully or not.
     * With this callback you should update your UI to show an error message or a download
     * complete message, depending on the result.
     * @param fileMessageId The downloaded file's message id
     * @param fileMessageTimestamp Unique identiffier of the downloaded file's conversation, this
     * might make it easier to search for the message.
     * @param fileMessageTimestamp The downloaded file's timestamp, this might make it easier to
     * search for the message.
     * *
     * @param success true if the file was successfully downloaded, otherwise false
     */
    fun onFileDownloadFinished(fileMessageId: String, fileMessageTimestamp: Long,
                               conversationId: String, success: Boolean)

    /**
     * After create a group with createGroup method, the server responds with the group ID
     * using this delegate. Use this ID as rid to send messages.
     * @param groupMembers monkey ID's of the new group's members separated by commas. It is null
     * if the group could not be created.
     * @param groupName Name of the new group. It is null if the group could not be created.
     * @param groupID ID of the new group. It is null if the group could not be created.
     * @param e the exception of the result
     */
    fun onCreateGroup(groupMembers: String?, groupName: String?, groupID: String?, e: Exception?)

    /**
     * After add a group member with removeGroupMember method, the server will update the group from a remote DB.
     * We recommend to update your group from your local DB as well.
     * @param groupID group id
     * @param members new members of the group.
     * @param e the exception of the result
     */
    fun onAddGroupMember(groupID: String?, newMember : String?, members: String?, e: Exception?)

    /**
     * After delete a group member with removeGroupMember method, the server will update the group from a remote DB.
     * We recommend to update your group from your local DB as well.
     * @param groupID group id
     * @param members new members of the group.
     * @param e the exception of the result
     */
    fun onRemoveGroupMember(groupID: String?, removedMember : String?, members: String?, e: Exception?)

    /**
     * This function will give you a MOKConversation with the information required.
     * @param mokConversation object with the required information.
     * @param e the exception of the result
     */
    fun onGetGroupInfo(mokConversation: MOKConversation, e: Exception?);

    /**
     * This function will give you a MOKUser with the information required.
     * @param mokUser object with the required information.
     * @param e the exception of the result
     */
    fun onGetUserInfo(mokUser: MOKUser, e: Exception?);

    /**
     * This function will give you a MOKUser with the information required.
     * @param mokUserArrayList list of MOKUser with the required information.
     * @param e the exception of the result
     */
    fun onGetUsersInfo(mokUsers: ArrayList<MOKUser>, e: Exception?);

    /**
     * This function is executed after you update the metadata of a user.
     * If exception is null the user was updated successfully.
     * @param e the exception of the result
     */
    fun onUpdateUserData(monkeyId: String, e: Exception?)

    /**
     * This function is executed after you update the metadata of a group.
     * If exception is null the group was updated successfully.
     * @param e the exception of the result
     */
    fun onUpdateGroupData(groupId: String, e: Exception?)

    /**
     * This function is executed when you receive all your conversations.
     * @param conversations array of the conversations required.
     * *
     * @param e the exception of the result
     */
    fun onGetConversations(conversations: ArrayList<MOKConversation>, e: Exception?)

    /**
     * This function is executed when you delete a conversation.
     * @param conversationId id of the conversation deleted
     * @param e the exception of the result
     */
    fun onDeleteConversation(conversationId: String, e: Exception?)

    /**
     * This function is executed when you receive all your conversation messages.
     * @param messages array of the messages required.
     * *
     * @param e the exception of the result
     */
    fun onGetConversationMessages(conversationId: String, messages: ArrayList<MOKMessage>, e: Exception?)

    /**
     * This function is executed when a message arrived and stored in the DB.
     * @param message Objeto MOKMessage que representa al mensaje recibido.
     */
    fun onMessageReceived(message: MOKMessage)

    /**
     * When the message arrive to the server, MonkeyKit receive an ack. This callback come with
     * with a new ID. The implementation of this callback mark the message as sent.
     * @param senderId id of the sender
     * *
     * @param recipientId id of the recipient
     * *
     * @param newId new Id of the message
     * *
     * @param oldId old id of the message
     * *
     * @param read boolean if the message is read or not
     * *
     * @param messageType type of the message 1 text, 2 file
     */
    fun onAcknowledgeRecieved(senderId: String, recipientId: String, newId: String, oldId: String, read: Boolean, messageType: Int)

    /**
     * This function is executed after you sent an open message
     * @param senderId sender id of the conversation
     * *
     * @param isOnline Boolean if the conversation is online or not
     * *
     * @param lastSeen timestamp of the last time the conversation was online
     * *
     * @param lastOpenMe timestamp of the last time the conversation open my conversation
     */
    fun onConversationOpenResponse(senderId: String, isOnline: Boolean?, lastSeen: String?, lastOpenMe: String?, members_online: String)

    /**
     * When a message is deleted from the server, MonkeyKit receive a notification.
     * @param messageId id of the message
     * *
     * @param senderId id of the sender
     * *
     * @param recipientId id of the recipient
     * *
     * @param datetime datetime of the message
     */
    fun onDeleteReceived(messageId: String, senderId: String, recipientId: String)

    /**
     * When a MonkeyKit user opens a conversation with the current user, a special notification
     * is received from server triggering this callback. In this callback you should mark as read
     * all the messages that have been successfully delivered to that user.
     * @param monkeyId monkey id of the user that opened the conversation.
     */
    fun onContactOpenMyConversation(monkeyId: String)

    /**
     * When you receive a notification, MonkeyKit execute this callback.
     * @param messageId id of the message
     * *
     * @param senderId id of the sender
     * *
     * @param recipientId id of the recipient
     * *
     * @param params JsonObject of the params sent in the message
     * *
     * @param datetime datetime of the message
     */
    fun onNotificationReceived(messageId: String, senderId: String, recipientId: String, params: JsonObject, datetime: String)

    /**
     * After the SyncDatabase() function of MonkeyKitSocketService finishes updating the database,
     * this callback is executed so that you can use that same data to update the UI.
     * guardarlos en la base de datos. La implementacion de este metodo debe de actualizar las conversaciones
     * @param data The data used to sync the database. With this callback your UI should reflect
     * the new state of your database.
    */
     fun onSyncComplete(data: HttpSync.SyncData)

    /**
     * This function is executed when you are added to a group.
     * @param groupid Group id of the group.
     * *
     * @param members Group members Ids separated by comma.
     * *
     * @param info Json with the group information: Name, admin, etc.
     */
    fun onGroupAdded(groupid: String, members: String, info: JsonObject)

    /**
     * This function is executed when a new member is added to one of your groups.
     * @param groupid Group id of the group.
     * *
     * @param new_member Id of the new member.
     */
    fun onGroupNewMember(groupid: String, new_member: String)

    /**
     * This function is executed when a new member is removed from one of your groups.
     * @param groupid Group id of the group.
     * *
     * @param removed_member Id of the removed member.
     */
    fun onGroupRemovedMember(groupid: String, removed_member: String)

    /**
     * This function is executed when a file fails upload.
     * @param message MOKMessage of the file.
     */
    fun onFileFailsUpload(message: MOKMessage)
}
