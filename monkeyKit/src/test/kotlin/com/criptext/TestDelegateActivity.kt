package com.criptext

import com.criptext.comunication.MOKConversation
import com.criptext.comunication.MOKMessage
import com.criptext.comunication.MOKUser
import com.criptext.http.HttpSync
import com.criptext.lib.MKDelegateActivity
import com.google.gson.JsonObject
import java.util.*

/**
 * Created by gesuwall on 12/22/16.
 */

class TestDelegateActivity() : MKDelegateActivity() {

    private val messagesDB: HashMap<String, MOKMessage> = hashMapOf()
    private val pendingMessages: HashMap<String, MOKMessage> = hashMapOf()

    fun getStoredMessage(messageID: String) = messagesDB[messageID]
    fun getStoredPendingMessage(messageID: String) = pendingMessages[messageID]

    override fun onUpdateGroupData(groupId: String, e: Exception?) {
    }

    override fun onUpdateUserData(monkeyId: String, e: Exception?) {
    }

    override fun storeSendingMessage(message: MOKMessage) {
        messagesDB.put(message.message_id, message)
    }

    override fun onDestroyWithPendingMessages(errorMessages: ArrayList<MOKMessage>) {
        for(m in errorMessages) {
            pendingMessages.put(m.message_id, m)
        }
    }

    override fun onConnectionRefused() {
    }

    override fun onAddGroupMember(groupID: String?, newMember : String?, members: String?, e: Exception?) {
    }

    override fun onContactOpenMyConversation(monkeyId: String) {
    }

    override fun onConversationOpenResponse(senderId: String, isOnline: Boolean?, lastSeen: String?, lastOpenMe: String?, members_online: String) {
    }

    override fun onCreateGroup(groupMembers: String?, groupName: String?, groupID: String?, e: Exception?) {
    }

    override fun onDeleteConversation(conversationId: String, e: Exception?) {
    }

    override fun onGetGroupInfo(mokConversation: MOKConversation, e: Exception?) {
    }

    override fun onDeleteReceived(messageId: String, senderId: String, recipientId: String) {
    }

    override fun onGetConversationMessages(conversationId: String, messages: ArrayList<MOKMessage>, e: Exception?) {
    }

    override fun onGetUserInfo(mokUser: MOKUser, e: Exception?) {
    }

    override fun onGetUsersInfo(mokUsers: ArrayList<MOKUser>, e: Exception?) {
    }

    override fun onGroupAdded(groupid: String, members: String, info: JsonObject) {
    }

    override fun onGroupNewMember(groupid: String, new_member: String) {
    }

    override fun onGroupRemovedMember(groupid: String, removed_member: String) {
    }

    override fun onMessageReceived(message: MOKMessage) {
    }

    override fun onNotificationReceived(messageId: String, senderId: String, recipientId: String, params: JsonObject, datetime: String) {
    }

    override fun onRemoveGroupMember(groupID: String?, removedMember : String?, members: String?, e: Exception?) {
    }

    override fun onSocketDisconnected() {
    }

    override fun onSyncComplete(data: HttpSync.SyncData) {
    }

    override val serviceClassName: Class<*> = TestService::class.java
}
