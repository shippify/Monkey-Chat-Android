package com.criptext.lib.delegates

import com.criptext.MonkeyKitSocketService
import com.criptext.comunication.CBTypes
import com.criptext.comunication.MOKConversation
import com.criptext.comunication.MOKMessage
import com.criptext.comunication.MOKUser
import com.criptext.http.HttpSync
import com.criptext.lib.MonkeyKitDelegate
import com.google.gson.JsonObject
import org.amshove.kluent.`should equal`
import org.junit.Before
import org.junit.Test
import java.util.*

/**
 * Created by gesuwall on 2/13/17.
 */

class `DelegateHandler Test` {
    lateinit var delegateHandler: DelegateHandler

    @Before
    fun initialize() {
        delegateHandler = DelegateHandler()
        MonkeyKitSocketService.status = MonkeyKitSocketService.ServiceStatus.running
    }

    @Test
    fun `should process acknowledge received`() {
        var lastAcknowledged = ""
        val delegate = object : AcknowledgeDelegate {
            override fun onAcknowledgeRecieved(senderId: String, recipientId: String, newId: String,
                                               oldId: String, read: Boolean, messageType: Int) {
                lastAcknowledged = newId
            }
        }

        delegateHandler.processMessageFromHandler(CBTypes.onAcknowledgeReceived,
                arrayOf("1", "1", "1", "1", false, 1))
        lastAcknowledged `should equal` ""

        delegateHandler.setDelegate(delegate)
        lastAcknowledged `should equal` "1"

        delegateHandler.processMessageFromHandler(CBTypes.onAcknowledgeReceived,
                arrayOf("2", "2", "2", "2", false, 1))
        lastAcknowledged `should equal` "2"
    }


    @Test
    fun `should process connection refused`() {
        var timesDisconnected = 0
        val delegate = object : ConnectionDelegate {
            override fun onConnectionRefused() {
                timesDisconnected++
            }

            override fun onSocketConnected() {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onSocketDisconnected() {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

        }

        delegateHandler.processMessageFromHandler(CBTypes.onConnectionRefused,
                arrayOf(Any()))
        timesDisconnected `should equal` 0

        delegateHandler.setDelegate(delegate)
        timesDisconnected `should equal` 1

        delegateHandler.processMessageFromHandler(CBTypes.onConnectionRefused,
                arrayOf(Any()))
        timesDisconnected `should equal` 2
    }

    @Test
    fun `should process socketConnected`() {
        var timesConnected = 0
        val delegate = object : ConnectionDelegate {
            override fun onConnectionRefused() {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onSocketConnected() {
                timesConnected++
            }

            override fun onSocketDisconnected() {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

        }

        delegateHandler.processMessageFromHandler(CBTypes.onSocketConnected,
                arrayOf(Any()))
        timesConnected `should equal` 0

        delegateHandler.setDelegate(delegate)
        timesConnected `should equal` 1

        delegateHandler.processMessageFromHandler(CBTypes.onSocketConnected,
                arrayOf(Any()))
        timesConnected `should equal` 2
    }

    @Test
    fun `should process socketDisconnected`() {
        var timesDisconnected = 0
        val delegate = object : ConnectionDelegate {
            override fun onConnectionRefused() {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onSocketConnected() {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onSocketDisconnected() {
                timesDisconnected++
            }

        }

        delegateHandler.processMessageFromHandler(CBTypes.onSocketDisconnected,
                arrayOf(Any()))
        timesDisconnected `should equal` 0

        delegateHandler.setDelegate(delegate)
        timesDisconnected `should equal` 1

        delegateHandler.processMessageFromHandler(CBTypes.onSocketDisconnected,
                arrayOf(Any()))
        timesDisconnected `should equal` 2
    }

    @Test
    fun `should process messageReceived`() {
        var lastMessage = ""
        val delegate = object : NewMessageDelegate {
            override fun onMessageReceived(message: MOKMessage) {
                lastMessage = message.message_id
            }
        }

        delegateHandler.processMessageFromHandler(CBTypes.onMessageReceived,
                arrayOf(MOKMessage("1", "1", "1", "1", "1", "1")))
        lastMessage `should equal` ""

        delegateHandler.setDelegate(delegate)
        lastMessage `should equal` "1"

        delegateHandler.processMessageFromHandler(CBTypes.onMessageReceived,
                arrayOf(MOKMessage("2", "2", "2", "2", "2", "2")))
        lastMessage `should equal` "2"
    }

    @Test
    fun `should process syncComplete`() {
        var batchesReceived = 0
        val delegate = object : SyncDelegate {
            override fun onSyncComplete(data: HttpSync.SyncData) {
                batchesReceived++
            }
        }

        delegateHandler.processMessageFromHandler(CBTypes.onSyncComplete,
                arrayOf(HttpSync.SyncData("1", null)))
        batchesReceived `should equal` 0

        delegateHandler.setDelegate(delegate)
        batchesReceived `should equal` 1

        delegateHandler.processMessageFromHandler(CBTypes.onSyncComplete,
                arrayOf(HttpSync.SyncData("1", null)))
        batchesReceived `should equal` 2
    }

    @Test
    fun `should process onConversationOpenResponse`() {
        var mLastSeen = ""
        val delegate = object : ConversationDelegate {
            override fun onUpdateUserData(monkeyId: String, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
            override fun onUpdateGroupData(groupId: String, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
            override fun onGetUserInfo(mokUser: MOKUser, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
            override fun onConversationOpenResponse(senderId: String, isOnline: Boolean, lastSeen: String, lastOpenMe: String?, members_online: String) {
                mLastSeen = lastSeen
            }

            override fun onContactOpenMyConversation(monkeyId: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onDeleteConversation(conversationId: String, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGetGroupInfo(mokConversation: MOKConversation, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGetConversations(conversations: ArrayList<MOKConversation>, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGetUsersInfo(mokUsers: ArrayList<MOKUser>, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }

        delegateHandler.processMessageFromHandler(CBTypes.onConversationOpenResponse,
                arrayOf("1", false, "1", "1", ""))
        mLastSeen `should equal` ""

        delegateHandler.setDelegate(delegate)
        mLastSeen `should equal` "1"

        delegateHandler.processMessageFromHandler(CBTypes.onConversationOpenResponse,
                arrayOf("2", false, "2", "2", ""))
        mLastSeen `should equal` "2"
    }

    @Test
    fun `should process updateUserData`() {
        var lastUpdated = ""
        val delegate = object : ConversationDelegate {
            override fun onUpdateUserData(monkeyId: String, e: Exception?) {
                lastUpdated = monkeyId
            }
            override fun onUpdateGroupData(groupId: String, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
            override fun onGetUserInfo(mokUser: MOKUser, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
            override fun onConversationOpenResponse(senderId: String, isOnline: Boolean, lastSeen: String, lastOpenMe: String?, members_online: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onContactOpenMyConversation(monkeyId: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onDeleteConversation(conversationId: String, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGetGroupInfo(mokConversation: MOKConversation, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGetConversations(conversations: ArrayList<MOKConversation>, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGetUsersInfo(mokUsers: ArrayList<MOKUser>, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }

        delegateHandler.processMessageFromHandler(CBTypes.onUpdateUserData,
                arrayOf("1", Exception()))
        lastUpdated `should equal` ""

        delegateHandler.setDelegate(delegate)
        lastUpdated `should equal` "1"

        delegateHandler.processMessageFromHandler(CBTypes.onUpdateUserData,
                arrayOf("2", Exception()))
        lastUpdated `should equal` "2"
    }

    @Test
    fun `should process updateGroupData`() {
        var lastUpdated = ""
        val delegate = object : ConversationDelegate {
            override fun onUpdateUserData(monkeyId: String, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
            override fun onUpdateGroupData(groupId: String, e: Exception?) {
                lastUpdated = groupId
            }
            override fun onGetUserInfo(mokUser: MOKUser, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
            override fun onConversationOpenResponse(senderId: String, isOnline: Boolean, lastSeen: String, lastOpenMe: String?, members_online: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onContactOpenMyConversation(monkeyId: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onDeleteConversation(conversationId: String, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGetGroupInfo(mokConversation: MOKConversation, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGetConversations(conversations: ArrayList<MOKConversation>, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGetUsersInfo(mokUsers: ArrayList<MOKUser>, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }

        delegateHandler.processMessageFromHandler(CBTypes.onUpdateGroupData,
                arrayOf("1", Exception()))
        lastUpdated `should equal` ""

        delegateHandler.setDelegate(delegate)
        lastUpdated `should equal` "1"

        delegateHandler.processMessageFromHandler(CBTypes.onUpdateGroupData,
                arrayOf("2", Exception()))
        lastUpdated `should equal` "2"
    }

    @Test
    fun `should process contactOpenMyConversation`() {
        var latestId = ""
        val delegate = object : ConversationDelegate {
            override fun onContactOpenMyConversation(monkeyId: String) {
                latestId = monkeyId
            }

            override fun onConversationOpenResponse(senderId: String, isOnline: Boolean, lastSeen: String, lastOpenMe: String?, members_online: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onDeleteConversation(conversationId: String, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGetConversations(conversations: ArrayList<MOKConversation>, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGetGroupInfo(mokConversation: MOKConversation, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGetUserInfo(mokUser: MOKUser, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGetUsersInfo(mokUsers: ArrayList<MOKUser>, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onUpdateUserData(monkeyId: String, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onUpdateGroupData(groupId: String, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }

        delegateHandler.processMessageFromHandler(CBTypes.onContactOpenMyConversation,
                arrayOf("1"))
        latestId `should equal` ""

        delegateHandler.setDelegate(delegate)
        latestId `should equal` "1"

        delegateHandler.processMessageFromHandler(CBTypes.onContactOpenMyConversation,
                arrayOf("2"))
        latestId `should equal` "2"
    }

    @Test
    fun `should process getUserInfo`() {
        var latestId = ""
        val delegate = object : ConversationDelegate {
            override fun onContactOpenMyConversation(monkeyId: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onConversationOpenResponse(senderId: String, isOnline: Boolean, lastSeen: String, lastOpenMe: String?, members_online: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onDeleteConversation(conversationId: String, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGetConversations(conversations: ArrayList<MOKConversation>, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGetGroupInfo(mokConversation: MOKConversation, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGetUserInfo(mokUser: MOKUser, e: Exception?) {
                latestId = mokUser.monkeyId
            }

            override fun onGetUsersInfo(mokUsers: ArrayList<MOKUser>, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onUpdateUserData(monkeyId: String, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onUpdateGroupData(groupId: String, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }

        delegateHandler.processMessageFromHandler(CBTypes.onGetUserInfo,
                arrayOf(MOKUser("1"), Exception()))
        latestId `should equal` ""

        delegateHandler.setDelegate(delegate)
        latestId `should equal` "1"

        delegateHandler.processMessageFromHandler(CBTypes.onGetUserInfo,
                arrayOf(MOKUser("2"), Exception()))
        latestId `should equal` "2"
    }

    @Test
    fun `should process getUsersInfo`() {
        var latestId = ""
        val delegate = object : ConversationDelegate {
            override fun onContactOpenMyConversation(monkeyId: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onConversationOpenResponse(senderId: String, isOnline: Boolean, lastSeen: String, lastOpenMe: String?, members_online: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onDeleteConversation(conversationId: String, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGetConversations(conversations: ArrayList<MOKConversation>, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGetGroupInfo(mokConversation: MOKConversation, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGetUserInfo(mokUser: MOKUser, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGetUsersInfo(mokUsers: ArrayList<MOKUser>, e: Exception?) {
                latestId = mokUsers.first().monkeyId
            }

            override fun onUpdateUserData(monkeyId: String, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onUpdateGroupData(groupId: String, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }

        delegateHandler.processMessageFromHandler(CBTypes.onGetUsersInfo,
                arrayOf(mutableListOf(MOKUser("1")), Exception()))
        latestId `should equal` ""

        delegateHandler.setDelegate(delegate)
        latestId `should equal` "1"

        delegateHandler.processMessageFromHandler(CBTypes.onGetUsersInfo,
                arrayOf(mutableListOf(MOKUser("2")), Exception()))
        latestId `should equal` "2"
    }

    @Test
    fun `should process getGroupInfo`() {
        var latestId = ""
        val delegate = object : ConversationDelegate {
            override fun onContactOpenMyConversation(monkeyId: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onConversationOpenResponse(senderId: String, isOnline: Boolean, lastSeen: String, lastOpenMe: String?, members_online: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onDeleteConversation(conversationId: String, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGetConversations(conversations: ArrayList<MOKConversation>, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGetGroupInfo(mokConversation: MOKConversation, e: Exception?) {
                latestId = mokConversation.conversationId
            }

            override fun onGetUserInfo(mokUser: MOKUser, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGetUsersInfo(mokUsers: ArrayList<MOKUser>, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onUpdateUserData(monkeyId: String, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onUpdateGroupData(groupId: String, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }

        delegateHandler.processMessageFromHandler(CBTypes.onGetGroupInfo,
                arrayOf(MOKConversation("1"), Exception()))
        latestId `should equal` ""

        delegateHandler.setDelegate(delegate)
        latestId `should equal` "1"

        delegateHandler.processMessageFromHandler(CBTypes.onGetGroupInfo,
                arrayOf(MOKConversation("2"), Exception()))
        latestId `should equal` "2"
    }


    @Test
    fun `should process createGroup`() {
        var latestGroup : String? = ""
        val delegate = object : GroupDelegate {
            override fun onCreateGroup(groupMembers: String?, groupName: String?, groupID: String?, e: Exception?) {
                latestGroup = groupID
            }

            override fun onAddGroupMember(groupID: String?, newMember: String?, members: String?, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGroupAdded(groupid: String, members: String, info: JsonObject) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGroupNewMember(groupid: String, new_member: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onRemoveGroupMember(groupID: String?, removedMember: String?, members: String?, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGroupRemovedMember(groupid: String, removed_member: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

        }

        delegateHandler.processMessageFromHandler(CBTypes.onCreateGroup,
                arrayOf("1", "1", "1", Exception()))
        latestGroup `should equal` ""

        delegateHandler.setDelegate(delegate)
        latestGroup `should equal` "1"

        delegateHandler.processMessageFromHandler(CBTypes.onCreateGroup,
                arrayOf("2", "2", "2", Exception()))
        latestGroup `should equal` "2"
    }

    @Test
    fun `should process removeGroupMember`() {
        var latestGroup : String? = ""
        val delegate = object : GroupDelegate {
            override fun onCreateGroup(groupMembers: String?, groupName: String?, groupID: String?, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onAddGroupMember(groupID: String?, newMember: String?, members: String?, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGroupAdded(groupid: String, members: String, info: JsonObject) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGroupNewMember(groupid: String, new_member: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onRemoveGroupMember(groupID: String?, removedMember: String?, members: String?, e: Exception?) {
                latestGroup = groupID
            }

            override fun onGroupRemovedMember(groupid: String, removed_member: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

        }

        delegateHandler.processMessageFromHandler(CBTypes.onRemoveGroupMember,
                arrayOf("1", "1", "1", Exception()))
        latestGroup `should equal` ""

        delegateHandler.setDelegate(delegate)
        latestGroup `should equal` "1"

        delegateHandler.processMessageFromHandler(CBTypes.onRemoveGroupMember,
                arrayOf("2", "2", "2", Exception()))
        latestGroup `should equal` "2"
    }

    @Test
    fun `should process addGroupMember`() {
        var latestGroup : String? = ""
        val delegate = object : GroupDelegate {
            override fun onCreateGroup(groupMembers: String?, groupName: String?, groupID: String?, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onAddGroupMember(groupID: String?, newMember: String?, members: String?, e: Exception?) {
                latestGroup = groupID
            }

            override fun onGroupAdded(groupid: String, members: String, info: JsonObject) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGroupNewMember(groupid: String, new_member: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onRemoveGroupMember(groupID: String?, removedMember: String?, members: String?, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGroupRemovedMember(groupid: String, removed_member: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

        }

        delegateHandler.processMessageFromHandler(CBTypes.onAddGroupMember,
                arrayOf("1", "1", "1", Exception()))
        latestGroup `should equal` ""

        delegateHandler.setDelegate(delegate)
        latestGroup `should equal` "1"

        delegateHandler.processMessageFromHandler(CBTypes.onAddGroupMember,
                arrayOf("2", "2", "2", Exception()))
        latestGroup `should equal` "2"
    }

    @Test
    fun `should process onGroupAdded`() {
        var latestGroup : String? = ""
        val delegate = object : GroupDelegate {
            override fun onCreateGroup(groupMembers: String?, groupName: String?, groupID: String?, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onAddGroupMember(groupID: String?, newMember: String?, members: String?, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGroupAdded(groupID: String, members: String, info: JsonObject) {
                latestGroup = groupID
            }

            override fun onGroupNewMember(groupid: String, new_member: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onRemoveGroupMember(groupID: String?, removedMember: String?, members: String?, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGroupRemovedMember(groupid: String, removed_member: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

        }

        delegateHandler.processMessageFromHandler(CBTypes.onGroupAdded,
                arrayOf("1", "1", JsonObject()))
        latestGroup `should equal` ""

        delegateHandler.setDelegate(delegate)
        latestGroup `should equal` "1"

        delegateHandler.processMessageFromHandler(CBTypes.onGroupAdded,
                arrayOf("2", "2", JsonObject()))
        latestGroup `should equal` "2"
    }

    @Test
    fun `should process onGroupNewMember`() {
        var latestGroup : String? = ""
        val delegate = object : GroupDelegate {
            override fun onCreateGroup(groupMembers: String?, groupName: String?, groupID: String?, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onAddGroupMember(groupID: String?, newMember: String?, members: String?, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGroupAdded(groupID: String, members: String, info: JsonObject) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGroupNewMember(groupID: String, new_member: String) {
                latestGroup = groupID
            }

            override fun onRemoveGroupMember(groupID: String?, removedMember: String?, members: String?, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGroupRemovedMember(groupid: String, removed_member: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

        }

        delegateHandler.processMessageFromHandler(CBTypes.onGroupNewMember,
                arrayOf("1", "1"))
        latestGroup `should equal` ""

        delegateHandler.setDelegate(delegate)
        latestGroup `should equal` "1"

        delegateHandler.processMessageFromHandler(CBTypes.onGroupNewMember,
                arrayOf("2", "2"))
        latestGroup `should equal` "2"
    }

    @Test
    fun `should process onGroupRemovedMember`() {
        var latestGroup : String? = ""
        val delegate = object : GroupDelegate {
            override fun onCreateGroup(groupMembers: String?, groupName: String?, groupID: String?, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onAddGroupMember(groupID: String?, newMember: String?, members: String?, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGroupAdded(groupID: String, members: String, info: JsonObject) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGroupNewMember(groupID: String, new_member: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onRemoveGroupMember(groupID: String?, removedMember: String?, members: String?, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGroupRemovedMember(groupID: String, removed_member: String) {
                latestGroup = groupID
            }

        }

        delegateHandler.processMessageFromHandler(CBTypes.onGroupRemovedMember,
                arrayOf("1", "1", "1", Exception()))
        latestGroup `should equal` ""

        delegateHandler.setDelegate(delegate)
        latestGroup `should equal` "1"

        delegateHandler.processMessageFromHandler(CBTypes.onGroupRemovedMember,
                arrayOf("2", "2", "2", Exception()))
        latestGroup `should equal` "2"
    }

    @Test
    fun `should process getConversations`() {
        var latestId = ""
        val delegate = object : MonkeyKitDelegate {
            override fun onDeleteReceived(messageId: String, senderId: String, recipientId: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGetConversationMessages(conversationId: String, messages: ArrayList<MOKMessage>, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onSyncComplete(data: HttpSync.SyncData) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onConnectionRefused() {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onSocketConnected() {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onSocketDisconnected() {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onContactOpenMyConversation(monkeyId: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onDeleteConversation(conversationId: String, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onConversationOpenResponse(senderId: String, isOnline: Boolean, lastSeen: String, lastOpenMe: String?, members_online: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGetGroupInfo(mokConversation: MOKConversation, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGetConversations(conversations: ArrayList<MOKConversation>, e: Exception?) {
                latestId = conversations.first().conversationId
            }

            override fun onGetUserInfo(mokUser: MOKUser, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGetUsersInfo(mokUsers: ArrayList<MOKUser>, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onUpdateUserData(monkeyId: String, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onUpdateGroupData(groupId: String, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onMessageReceived(message: MOKMessage) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onNotificationReceived(messageId: String, senderId: String, recipientId: String, params: JsonObject, datetime: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onAcknowledgeRecieved(senderId: String, recipientId: String, newId: String, oldId: String, read: Boolean, messageType: Int) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onFileDownloadFinished(fileMessageId: String, fileMessageTimestamp: Long, conversationId: String, success: Boolean) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onFileFailsUpload(message: MOKMessage) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onCreateGroup(groupMembers: String?, groupName: String?, groupID: String?, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onAddGroupMember(groupID: String?, newMember: String?, members: String?, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGroupAdded(groupID: String, members: String, info: JsonObject) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGroupNewMember(groupID: String, new_member: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onRemoveGroupMember(groupID: String?, removedMember: String?, members: String?, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGroupRemovedMember(groupID: String, removed_member: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

        }

        delegateHandler.processMessageFromHandler(CBTypes.onGetConversations,
                arrayOf(mutableListOf(MOKConversation("1")), Exception()))
        latestId `should equal` ""

        delegateHandler.setDelegate(delegate)
        latestId `should equal` "1"

        delegateHandler.processMessageFromHandler(CBTypes.onGetConversations,
                arrayOf(mutableListOf(MOKConversation("2")), Exception()))
        latestId `should equal` "2"
    }

    @Test
    fun `should process deleteReceived`() {
        var latestId = ""
        val delegate = object : MonkeyKitDelegate {
            override fun onDeleteReceived(messageId: String, senderId: String, recipientId: String) {
                latestId = messageId
            }

            override fun onGetConversationMessages(conversationId: String, messages: ArrayList<MOKMessage>, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onSyncComplete(data: HttpSync.SyncData) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onConnectionRefused() {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onSocketConnected() {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onSocketDisconnected() {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onContactOpenMyConversation(monkeyId: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onDeleteConversation(conversationId: String, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onConversationOpenResponse(senderId: String, isOnline: Boolean, lastSeen: String, lastOpenMe: String?, members_online: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGetGroupInfo(mokConversation: MOKConversation, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGetConversations(conversations: ArrayList<MOKConversation>, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGetUserInfo(mokUser: MOKUser, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGetUsersInfo(mokUsers: ArrayList<MOKUser>, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onUpdateUserData(monkeyId: String, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onUpdateGroupData(groupId: String, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onMessageReceived(message: MOKMessage) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onNotificationReceived(messageId: String, senderId: String, recipientId: String, params: JsonObject, datetime: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onAcknowledgeRecieved(senderId: String, recipientId: String, newId: String, oldId: String, read: Boolean, messageType: Int) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onFileDownloadFinished(fileMessageId: String, fileMessageTimestamp: Long, conversationId: String, success: Boolean) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onFileFailsUpload(message: MOKMessage) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onCreateGroup(groupMembers: String?, groupName: String?, groupID: String?, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onAddGroupMember(groupID: String?, newMember: String?, members: String?, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGroupAdded(groupID: String, members: String, info: JsonObject) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGroupNewMember(groupID: String, new_member: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onRemoveGroupMember(groupID: String?, removedMember: String?, members: String?, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGroupRemovedMember(groupID: String, removed_member: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

        }

        delegateHandler.processMessageFromHandler(CBTypes.onDeleteReceived,
                arrayOf("1", "1", "1"))
        latestId `should equal` ""

        delegateHandler.setDelegate(delegate)
        latestId `should equal` "1"

        delegateHandler.processMessageFromHandler(CBTypes.onDeleteReceived,
                arrayOf("2", "2", "2"))
        latestId `should equal` "2"
    }

    @Test
    fun `should process deleteConversation`() {
        var latestId = ""
        val delegate = object : MonkeyKitDelegate {
            override fun onDeleteReceived(messageId: String, senderId: String, recipientId: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGetConversationMessages(conversationId: String, messages: ArrayList<MOKMessage>, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onSyncComplete(data: HttpSync.SyncData) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onConnectionRefused() {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onSocketConnected() {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onSocketDisconnected() {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onContactOpenMyConversation(monkeyId: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onDeleteConversation(conversationId: String, e: Exception?) {
                latestId = conversationId
            }

            override fun onConversationOpenResponse(senderId: String, isOnline: Boolean, lastSeen: String, lastOpenMe: String?, members_online: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGetGroupInfo(mokConversation: MOKConversation, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGetConversations(conversations: ArrayList<MOKConversation>, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGetUserInfo(mokUser: MOKUser, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGetUsersInfo(mokUsers: ArrayList<MOKUser>, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onUpdateUserData(monkeyId: String, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onUpdateGroupData(groupId: String, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onMessageReceived(message: MOKMessage) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onNotificationReceived(messageId: String, senderId: String, recipientId: String, params: JsonObject, datetime: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onAcknowledgeRecieved(senderId: String, recipientId: String, newId: String, oldId: String, read: Boolean, messageType: Int) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onFileDownloadFinished(fileMessageId: String, fileMessageTimestamp: Long, conversationId: String, success: Boolean) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onFileFailsUpload(message: MOKMessage) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onCreateGroup(groupMembers: String?, groupName: String?, groupID: String?, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onAddGroupMember(groupID: String?, newMember: String?, members: String?, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGroupAdded(groupID: String, members: String, info: JsonObject) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGroupNewMember(groupID: String, new_member: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onRemoveGroupMember(groupID: String?, removedMember: String?, members: String?, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGroupRemovedMember(groupID: String, removed_member: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

        }

        delegateHandler.processMessageFromHandler(CBTypes.onDeleteConversation,
                arrayOf("1", Exception()))
        latestId `should equal` ""

        delegateHandler.setDelegate(delegate)
        latestId `should equal` "1"

        delegateHandler.processMessageFromHandler(CBTypes.onDeleteConversation,
                arrayOf("2", Exception()))
        latestId `should equal` "2"
    }

    @Test
    fun `should process getConversationMessages`() {
        var latestId = ""
        val delegate = object : MonkeyKitDelegate {
            override fun onDeleteReceived(messageId: String, senderId: String, recipientId: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGetConversationMessages(conversationId: String, messages: ArrayList<MOKMessage>, e: Exception?) {
                latestId = conversationId
            }

            override fun onSyncComplete(data: HttpSync.SyncData) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onConnectionRefused() {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onSocketConnected() {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onSocketDisconnected() {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onContactOpenMyConversation(monkeyId: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onDeleteConversation(conversationId: String, e: Exception?) {
            }

            override fun onConversationOpenResponse(senderId: String, isOnline: Boolean, lastSeen: String, lastOpenMe: String?, members_online: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGetGroupInfo(mokConversation: MOKConversation, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGetConversations(conversations: ArrayList<MOKConversation>, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGetUserInfo(mokUser: MOKUser, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGetUsersInfo(mokUsers: ArrayList<MOKUser>, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onUpdateUserData(monkeyId: String, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onUpdateGroupData(groupId: String, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onMessageReceived(message: MOKMessage) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onNotificationReceived(messageId: String, senderId: String, recipientId: String, params: JsonObject, datetime: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onAcknowledgeRecieved(senderId: String, recipientId: String, newId: String, oldId: String, read: Boolean, messageType: Int) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onFileDownloadFinished(fileMessageId: String, fileMessageTimestamp: Long, conversationId: String, success: Boolean) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onFileFailsUpload(message: MOKMessage) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onCreateGroup(groupMembers: String?, groupName: String?, groupID: String?, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onAddGroupMember(groupID: String?, newMember: String?, members: String?, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGroupAdded(groupID: String, members: String, info: JsonObject) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGroupNewMember(groupID: String, new_member: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onRemoveGroupMember(groupID: String?, removedMember: String?, members: String?, e: Exception?) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGroupRemovedMember(groupID: String, removed_member: String) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

        }

        delegateHandler.processMessageFromHandler(CBTypes.onGetConversationMessages,
                arrayOf("1", ArrayList<MOKMessage>(), Exception()))
        latestId `should equal` ""

        delegateHandler.setDelegate(delegate)
        latestId `should equal` "1"

        delegateHandler.processMessageFromHandler(CBTypes.onGetConversationMessages,
                arrayOf("2", ArrayList<MOKMessage>(), Exception()))
        latestId `should equal` "2"
    }

    @Test
    fun `should process notificationReceived`() {
        var latestId = ""
        val delegate = object : NewNotificationDelegate {
            override fun onNotificationReceived(messageId: String, senderId: String,
                                        recipientId: String, params: JsonObject, datetime: String) {
                latestId = messageId
            }
        }

        delegateHandler.processMessageFromHandler(CBTypes.onNotificationReceived,
                arrayOf("1", "1", "1", JsonObject(), "1"))
        latestId `should equal` ""

        delegateHandler.setDelegate(delegate)
        latestId `should equal` "1"

        delegateHandler.processMessageFromHandler(CBTypes.onNotificationReceived,
                arrayOf("2", "2", "2", JsonObject(), "2"))
        latestId `should equal` "2"
    }

    @Test
    fun `should process fileDownloadFinish`() {
        var latestId = ""
        val delegate = object : FileDelegate {
            override fun onFileDownloadFinished(fileMessageId: String, fileMessageTimestamp: Long, conversationId: String, success: Boolean) {
                latestId = fileMessageId
            }

            override fun onFileFailsUpload(message: MOKMessage) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }

        delegateHandler.processMessageFromHandler(CBTypes.onFileDownloadFinished,
                arrayOf("1", 1L, "1", true))
        latestId `should equal` ""

        delegateHandler.setDelegate(delegate)
        latestId `should equal` "1"

        delegateHandler.processMessageFromHandler(CBTypes.onFileDownloadFinished,
                arrayOf("2", 2L, "2", true))
        latestId `should equal` "2"
    }

    @Test
    fun `should process fileUploadFinish`() {
        var latestId = ""
        val delegate = object : FileDelegate {
            override fun onFileDownloadFinished(fileMessageId: String, fileMessageTimestamp: Long, conversationId: String, success: Boolean) {
                throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onFileFailsUpload(message: MOKMessage) {
                latestId = message.message_id
            }
        }

        delegateHandler.processMessageFromHandler(CBTypes.onFileFailsUpload,
            arrayOf(MOKMessage("1", "1", "1", "1", "1", "1")))
        latestId `should equal` ""

        delegateHandler.setDelegate(delegate)
        latestId `should equal` "1"

        delegateHandler.processMessageFromHandler(CBTypes.onFileFailsUpload,
                arrayOf(MOKMessage("2", "2", "2", "2", "2", "2")))
        latestId `should equal` "2"
    }



}