package com.criptext.lib.delegates

import android.util.Log
import com.criptext.MonkeyKitSocketService
import com.criptext.MonkeyKitSocketService.Companion.status
import com.criptext.comunication.*
import com.criptext.database.CriptextDBHandler
import com.criptext.http.HttpSync
import com.criptext.lib.MonkeyKitDelegate
import com.criptext.lib.ServiceTimeoutTask
import com.google.gson.JsonObject
import java.io.File
import java.util.*

/**
 * Created by gabriel on 2/13/17.
 */

class DelegateHandler() {
    var acknowledgeDelegate: AcknowledgeDelegate? = null
        private set (value) {
            field = value
            if (value != null) {
                pendingAcknowledgeActions.forEach(Runnable::run)
                pendingAcknowledgeActions.clear()
            }
        }
    var connectionDelegate: ConnectionDelegate? = null
        private set (value) {
            field = value
            if (value != null) {
                pendingConnectionActions.forEach(Runnable::run)
                pendingConnectionActions.clear()
            }
        }
    var conversationDelegate: ConversationDelegate? = null
        private set (value) {
            field = value
            if (value != null) {
                pendingConversationActions.forEach(Runnable::run)
                pendingConversationActions.clear()
            }
        }
    var conversationOpenDelegate: ConversationOpenDelegate? = null
    private set (value) {
        field = value
        if (value != null) {
            pendingConversationOpenActions.forEach(Runnable::run)
            pendingConversationOpenActions.clear()
        }
    }
    var fileDelegate: FileDelegate? = null
        private set (value) {
            field = value
            if (value != null) {
                pendingFileActions.forEach(Runnable::run)
                pendingFileActions.clear()
            }
        }
    var groupDelegate: GroupDelegate? = null
        private set (value) {
            field = value
            if (value != null) {
                pendingGroupActions.forEach(Runnable::run)
                pendingGroupActions.clear()
            }
        }
    var newMessageDelegate: NewMessageDelegate? = null
        private set (value) {
            field = value
            if (value != null) {
                pendingNewMessageActions.forEach(Runnable::run)
                pendingNewMessageActions.clear()
            }
        }
    var newNotificationDelegate: NewNotificationDelegate? = null
        private set (value) {
            field = value
            if (value != null) {
                pendingNotificationActions.forEach(Runnable::run)
                pendingNotificationActions.clear()
            }
        }
    var syncDelegate: SyncDelegate? = null
        private set (value) {
            field = value
            if (value != null) {
                pendingSyncActions.forEach(Runnable::run)
                pendingSyncActions.clear()
            }
        }
    var monkeyKitDelegate: MonkeyKitDelegate? = null
        private set (value) {
            field = value
            if (value != null) {
                pendingMonkeyKitActions.forEach(Runnable::run)
                pendingMonkeyKitActions.clear()
            }
        }

    /**
     * List of actions to execute after delegate rebinds
     */
    private val pendingAcknowledgeActions: LinkedList<Runnable> = LinkedList()
    private val pendingConnectionActions: LinkedList<Runnable> = LinkedList()
    private val pendingConversationActions: LinkedList<Runnable> = LinkedList()
    private val pendingConversationOpenActions: LinkedList<Runnable> = LinkedList()
    private val pendingFileActions: LinkedList<Runnable> = LinkedList()
    private val pendingGroupActions: LinkedList<Runnable> = LinkedList()
    private val pendingNewMessageActions: LinkedList<Runnable> = LinkedList()
    private val pendingNotificationActions: LinkedList<Runnable> = LinkedList()
    private val pendingSyncActions: LinkedList<Runnable> = LinkedList()
    private val pendingMonkeyKitActions: LinkedList<Runnable> = LinkedList()



    fun setDelegate(delegate: AcknowledgeDelegate) {
        acknowledgeDelegate = delegate
    }

    fun setDelegate(delegate: ConnectionDelegate) {
        connectionDelegate = delegate
    }

    fun setDelegate(delegate: ConversationDelegate) {
        conversationDelegate = delegate
    }

    fun setDelegate(delegate: ConversationOpenDelegate) {
        conversationOpenDelegate = delegate
    }

    fun setDelegate(delegate: FileDelegate) {
        fileDelegate = delegate
    }

    fun setDelegate(delegate: GroupDelegate) {
        groupDelegate = delegate
    }

    fun setDelegate(delegate: NewMessageDelegate) {
        newMessageDelegate = delegate
    }

    fun setDelegate(delegate: NewNotificationDelegate) {
        newNotificationDelegate = delegate
    }

    fun setDelegate(delegate: SyncDelegate) {
        syncDelegate = delegate
    }

    fun setDelegate(delegate: MonkeyKitDelegate) {
        monkeyKitDelegate = delegate
    }

    val hasDelegate: Boolean
    get() = acknowledgeDelegate != null
        || connectionDelegate != null
        || conversationDelegate != null
        || conversationOpenDelegate != null
        || fileDelegate != null
        || groupDelegate != null
        || newMessageDelegate != null
        || newNotificationDelegate != null
        || syncDelegate != null
        || monkeyKitDelegate != null

    fun clear() {
        acknowledgeDelegate = null
        connectionDelegate = null
        conversationDelegate == null
        conversationOpenDelegate == null
        fileDelegate = null
        groupDelegate = null
        newMessageDelegate = null
        newNotificationDelegate = null
        syncDelegate = null
        monkeyKitDelegate = null
    }

    fun processMessageFromHandler(method: CBTypes, info:Array<Any?>) {
        if(status < MonkeyKitSocketService.ServiceStatus.initializing)
            return //There's no point in doing anything with the delegates if the service is dead.

        if(info==null || info[0]==null)
            return //There were several crashes where info[0] were null at onGetConversations

        val pendingRunnable = Runnable { processMessageFromHandler(method, info) }

        when (method) {
            CBTypes.onAcknowledgeReceived -> {
                if (acknowledgeDelegate != null)
                    acknowledgeDelegate!!.onAcknowledgeRecieved(info[0] as String, info[1] as String,
                        info[2] as String , info[3] as String, info[4] as Boolean, info[5] as Int)
                else {
                    pendingAcknowledgeActions.add(pendingRunnable)
                }
            }
            CBTypes.onConnectionRefused -> {
                if (connectionDelegate != null)
                    connectionDelegate!!.onConnectionRefused()
                else pendingConnectionActions.add(pendingRunnable)
            }
            CBTypes.onSocketConnected -> {
                if (connectionDelegate != null)
                    connectionDelegate!!.onSocketConnected()
                else pendingConnectionActions.add(pendingRunnable)
            }
            CBTypes.onSocketDisconnected -> {
                if (connectionDelegate != null)
                    connectionDelegate!!.onSocketDisconnected()
                else pendingConnectionActions.add(pendingRunnable)
            }
            CBTypes.onMessageReceived -> {
                val message = info[0] as MOKMessage
                if (newMessageDelegate != null)
                    newMessageDelegate!!.onMessageReceived(message)
                else pendingNewMessageActions.add(pendingRunnable)
            }
            CBTypes.onSyncComplete -> {
                val batch = info[0] as HttpSync.SyncData;
                if (syncDelegate != null)
                    syncDelegate!!.onSyncComplete(batch)
                else pendingSyncActions.add(pendingRunnable)
            }
            CBTypes.onUpdateUserData -> {
                if (conversationDelegate != null)
                    conversationDelegate!!.onUpdateUserData(info[0] as String, info[1] as Exception?)
                else pendingConversationActions.add(pendingRunnable)
            }
            CBTypes.onUpdateGroupData -> {
                if (conversationDelegate != null)
                    conversationDelegate!!.onUpdateGroupData(info[0] as String, info[1] as Exception?)
                else pendingConversationActions.add(pendingRunnable)
            }
            CBTypes.onConversationOpenResponse -> {
                if (conversationOpenDelegate != null)
                    conversationOpenDelegate!!.onConversationOpenResponse(info[0] as String,
                            info[1] as Boolean, info[2] as String, info[3] as String?, info[4] as String)
                else
                    pendingConversationOpenActions.add(pendingRunnable);
            }
            CBTypes.onContactOpenMyConversation -> {
                if (conversationOpenDelegate != null)
                    conversationOpenDelegate!!.onContactOpenMyConversation(info[0] as String)
                else pendingConversationOpenActions.add(pendingRunnable)
            }
            CBTypes.onGetUserInfo-> {
                if (conversationDelegate != null)
                    conversationDelegate!!.onGetUserInfo( info[0] as MOKUser, info[1] as Exception?)
                else pendingConversationActions.add(pendingRunnable)
            }
            CBTypes.onGetUsersInfo-> {
                if (conversationDelegate != null)
                    conversationDelegate!!.onGetUsersInfo( info[0] as ArrayList<MOKUser>,
                        info[1] as Exception?)
                else pendingConversationActions.add(pendingRunnable)
            }
            CBTypes.onGetGroupInfo-> {
                if (conversationDelegate != null)
                    conversationDelegate!!.onGetGroupInfo(info[0] as MOKConversation, info[1] as Exception?)
                else pendingConversationActions.add(pendingRunnable)
            }
            CBTypes.onCreateGroup -> {
                if (groupDelegate != null)
                    groupDelegate!!.onCreateGroup(info[0] as String?, info[1] as String?,
                        info[2] as String?, info[3] as Exception?)
                else pendingGroupActions.add(pendingRunnable)
            }
            CBTypes.onRemoveGroupMember -> {
                if (groupDelegate != null)
                    groupDelegate!!.onRemoveGroupMember(info[0] as String, info[1] as String?,
                            info[2] as String?, info[3] as Exception?)
                else pendingGroupActions.add(pendingRunnable)
            }
            CBTypes.onAddGroupMember -> {
                if (groupDelegate != null)
                    groupDelegate!!.onAddGroupMember(info[0] as String, info[1] as String?,
                            info[2] as String?, info[3] as Exception?)
                else pendingGroupActions.add(pendingRunnable)
            }
            CBTypes.onGroupAdded -> {
                if (groupDelegate != null)
                    groupDelegate!!.onGroupAdded(info[0] as String, info[1] as String, info[2] as JsonObject)
                else pendingGroupActions.add(pendingRunnable)
            }
            CBTypes.onGroupNewMember -> {
                if (groupDelegate != null)
                    groupDelegate!!.onGroupNewMember(info[0] as String, info[1] as String)
                else pendingGroupActions.add(pendingRunnable)
            }
            CBTypes.onGroupRemovedMember -> {
                if (groupDelegate != null)
                    groupDelegate!!.onGroupRemovedMember(info[0] as String, info[1] as String)
                else pendingGroupActions.add(pendingRunnable)
            }
            CBTypes.onGetConversations -> {

                if (conversationDelegate != null)
                    conversationDelegate!!.onGetConversations(info[0] as ArrayList<MOKConversation>,
                        info[1] as Exception?)
                else pendingConversationActions.add(pendingRunnable)
            }
            CBTypes.onDeleteReceived -> {
                if (monkeyKitDelegate != null)
                    monkeyKitDelegate!!.onDeleteReceived(info[0] as String, info[1] as String,
                            info[2] as String)
                else pendingMonkeyKitActions.add(pendingRunnable)
            }
            CBTypes.onDeleteConversation -> {
                if (monkeyKitDelegate != null)
                    monkeyKitDelegate!!.onDeleteConversation(info[0] as String, info[1] as Exception?)
                else pendingMonkeyKitActions.add(pendingRunnable)
            }
            CBTypes.onGetConversationMessages -> {
                if (monkeyKitDelegate != null) {
                    print(info[0])
                    monkeyKitDelegate!!.onGetConversationMessages(info[0] as String,
                            info[1] as ArrayList<MOKMessage>, info[2] as Exception?)
                }
                else pendingMonkeyKitActions.add(pendingRunnable)
            }
            CBTypes.onNotificationReceived -> {
                val messageId = info[0] as String
                val senderId = info[1] as String
                val receipientId = info[2] as String
                val params = info[3] as JsonObject
                val datetime = info[4] as String

                if (newNotificationDelegate != null)
                    newNotificationDelegate!!.onNotificationReceived(messageId, senderId,
                        receipientId, params, datetime)
                else pendingNotificationActions.add(pendingRunnable)
            }
            CBTypes.onFileDownloadFinished -> {
                if (fileDelegate != null)
                    fileDelegate!!.onFileDownloadFinished(info[0] as String, info[1] as Long,
                            info[2] as String, info[3] as Boolean)
                else pendingFileActions.add(pendingRunnable)
            }
            CBTypes.onFileFailsUpload -> {
                if (fileDelegate != null)
                    fileDelegate!!.onFileFailsUpload(info[0] as MOKMessage)
                else pendingFileActions.add(pendingRunnable)
            }
            else -> Log.d("DelegateHandler", "unsupported: $method")
        }
    }
}
