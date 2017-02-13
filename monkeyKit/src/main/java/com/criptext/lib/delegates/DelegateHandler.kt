package com.criptext.lib.delegates

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

class DelegateHandler(val service: MonkeyKitSocketService) {
    var acknowledgeDelegate: AcknowledgeDelegate? = null
        private set (value) { //We need some sort of delegate handler object to test this more easily
            field = value
            if (value != null) {
                pendingAcknowledgeActions.forEach(Runnable::run)
                pendingAcknowledgeActions.clear()
            }
        }
    var connectionDelegate: ConnectionDelegate? = null
        private set (value) { //We need some sort of delegate handler object to test this more easily
            field = value
            if (value != null) {
                pendingConnectionActions.forEach(Runnable::run)
                pendingConnectionActions.clear()
            }
        }
    var conversationDelegate: ConversationDelegate? = null
        private set (value) { //We need some sort of delegate handler object to test this more easily
            field = value
            if (value != null) {
                pendingConversationActions.forEach(Runnable::run)
                pendingConversationActions.clear()
            }
        }
    var fileDelegate: FileDelegate? = null
        private set (value) { //We need some sort of delegate handler object to test this more easily
            field = value
            if (value != null) {
                pendingFileActions.forEach(Runnable::run)
                pendingFileActions.clear()
            }
        }
    var groupDelegate: GroupDelegate? = null
        private set (value) { //We need some sort of delegate handler object to test this more easily
            field = value
            if (value != null) {
                pendingGroupActions.forEach(Runnable::run)
                pendingGroupActions.clear()
            }
        }
    var newMessageDelegate: NewMessageDelegate? = null
        private set (value) { //We need some sort of delegate handler object to test this more easily
            field = value
            if (value != null) {
                pendingNewMessageActions.forEach(Runnable::run)
                pendingNewMessageActions.clear()
            }
        }
    var newNotificationDelegate: NewNotificationDelegate? = null
        private set (value) { //We need some sort of delegate handler object to test this more easily
            field = value
            if (value != null) {
                pendingNotificationActions.forEach(Runnable::run)
                pendingNotificationActions.clear()
            }
        }
    var syncDelegate: SyncDelegate? = null
        private set (value) { //We need some sort of delegate handler object to test this more easily
            field = value
            if (value != null) {
                pendingSyncActions.forEach(Runnable::run)
                pendingSyncActions.clear()
            }
        }
    var monkeyKitDelegate: MonkeyKitDelegate? = null
        private set (value) { //We need some sort of delegate handler object to test this more easily
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
    private val pendingFileActions: LinkedList<Runnable> = LinkedList()
    private val pendingGroupActions: LinkedList<Runnable> = LinkedList()
    private val pendingNewMessageActions: LinkedList<Runnable> = LinkedList()
    private val pendingNotificationActions: LinkedList<Runnable> = LinkedList()
    private val pendingSyncActions: LinkedList<Runnable> = LinkedList()
    private val pendingMonkeyKitActions: LinkedList<Runnable> = LinkedList()


    private val messagesReceivedDuringSync: LinkedList<MOKMessage> = LinkedList();

    fun setDelegate(delegate: AcknowledgeDelegate) {
        acknowledgeDelegate = delegate
    }

    fun setDelegate(delegate: ConnectionDelegate) {
        connectionDelegate = delegate
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

    fun clear() {
        acknowledgeDelegate = null
        connectionDelegate = null
        fileDelegate = null
        groupDelegate = null
        newMessageDelegate = null
        newNotificationDelegate = null
        syncDelegate = null
        monkeyKitDelegate = null
    }

    private fun addMessagesToSyncResponse(response: HttpSync.SyncData){
        response.addMessages(messagesReceivedDuringSync)
        messagesReceivedDuringSync.clear()
    }

    private fun playPendingActions(){
        val totalActions = service.pendingActions.size
        for (i in 1..totalActions){
            val action = service.pendingActions.removeAt(0)
            action.run()
        }
    }


    fun processMessageFromHandler(method: CBTypes, info:Array<Any>) {
        if(status < MonkeyKitSocketService.ServiceStatus.initializing)
            return //There's no point in doing anything with the delegates if the service is dead.

        val pendingRunnable = Runnable { processMessageFromHandler(method, info) }

        when (method) {
            CBTypes.onAcknowledgeReceived -> {
                if (acknowledgeDelegate != null)
                    acknowledgeDelegate!!.onAcknowledgeRecieved(info[0] as String, info[1] as String,
                        info[2] as String , info[3] as String, info[4] as Boolean, info[5] as Int)
                else
                    pendingAcknowledgeActions.add(pendingRunnable)
            }
            CBTypes.onConversationOpenResponse -> {
                if (conversationDelegate != null)
                    conversationDelegate!!.onConversationOpenResponse(info[0] as String,
                            info[1] as Boolean, info[2] as Long, info[3] as Long?, info[4] as String)
                else
                    pendingConversationActions.add(pendingRunnable);
            }
            CBTypes.onConnectionRefused -> {
                if (connectionDelegate != null)
                    connectionDelegate!!.onConnectionRefused()
                else pendingConnectionActions.add(pendingRunnable)
            }
            CBTypes.onSocketConnected -> {
                service.resendPendingMessages()
                playPendingActions()
                if (connectionDelegate != null)
                    connectionDelegate!!.onSocketConnected()
                else
                    pendingConnectionActions.add(pendingRunnable)
                service.sendSync()
            }
            CBTypes.onSocketDisconnected -> {
                //If socket disconnected and this handler is still alive we should reconnect
                //immediately.
                service.startSocketConnection()
                if (connectionDelegate != null)
                    connectionDelegate!!.onSocketDisconnected()
                else pendingConnectionActions.add(pendingRunnable)
            }
            CBTypes.onMessageReceived -> {
                val message = info[0] as MOKMessage
                if(status == MonkeyKitSocketService.ServiceStatus.initializing)
                    messagesReceivedDuringSync.add(message)
                else {
                    val tipo = CriptextDBHandler.getMonkeyActionType(message);
                    if (tipo == MessageTypes.blMessageAudio ||
                            tipo == MessageTypes.blMessagePhoto ||
                            tipo == MessageTypes.blMessageDocument ||
                            tipo == MessageTypes.blMessageScreenCapture ||
                            tipo == MessageTypes.blMessageShareAFriend ||
                            tipo == MessageTypes.blMessageDefault)
                        service.storeReceivedMessage(message, Runnable {
                            //Message received and stored, update lastTimeSynced with with the timestamp
                            //that the server gave the message
                            service.lastTimeSynced = message.datetime.toLong();
                            if (newMessageDelegate != null)
                                newMessageDelegate!!.onMessageReceived(message)
                            else pendingNewMessageActions.add(pendingRunnable)
                            if (service.startedManually && newMessageDelegate == null)  //if service started manually, stop it manually with a timeout task
                                ServiceTimeoutTask(service).execute()
                        })
                }
            }

            CBTypes.onSyncComplete -> {
                val batch = info[0] as HttpSync.SyncData;
                status = if(syncDelegate != null) MonkeyKitSocketService.ServiceStatus.bound
                    else MonkeyKitSocketService.ServiceStatus.running
                //add messages that were received while syncing
                addMessagesToSyncResponse(batch)

                service.syncDatabase(batch, Runnable {
                    //At this point initialization is complete. We are ready to receive and send messages
                    if (syncDelegate != null)
                        syncDelegate!!.onSyncComplete(batch)
                    else pendingSyncActions.add(pendingRunnable)
                    //since status could have changed from initializing to bound, or running, let's play pending actions.
                    //this is needed for uploading photos.
                    playPendingActions()
                    if(service.startedManually && syncDelegate == null)  //if service started manually, stop it manually with a timeout task
                        ServiceTimeoutTask(service).execute()
                })
            }

            CBTypes.onDeleteReceived -> {
                service.lastTimeSynced = info[3].toString().toLong()
                service.removePendingMessage(info[0] as String)
                if (monkeyKitDelegate != null)
                    monkeyKitDelegate!!.onDeleteReceived(info[0] as String, info[1] as String,
                            info[2] as String)
                else pendingMonkeyKitActions.add(pendingRunnable)
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
            CBTypes.onFileDownloadFinished -> {
                if (fileDelegate != null)
                    fileDelegate!!.onFileDownloadFinished(info[0] as String, info[1] as Long,
                        info[2] as String, info[3] as Boolean)
                else pendingFileActions.add(pendingRunnable)
            }
            CBTypes.onContactOpenMyConversation -> {
                if (conversationDelegate != null)
                    conversationDelegate!!.onContactOpenMyConversation(info[0] as String)
                else pendingConversationActions.add(pendingRunnable)
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
            CBTypes.onGetConversations -> {
                if (monkeyKitDelegate != null)
                    monkeyKitDelegate!!.onGetConversations(info[0] as ArrayList<MOKConversation>,
                        info[1] as Exception?)
                else pendingMonkeyKitActions.add(pendingRunnable)
                if (status == MonkeyKitSocketService.ServiceStatus.initializing) {
                    //this is the first time service starts, so after adding all conversations, connect the socket
                    service.startSocketConnection()
                }
            }
            CBTypes.onDeleteConversation -> {
                if (monkeyKitDelegate != null)
                    monkeyKitDelegate!!.onDeleteConversation(info[0] as String, info[1] as Exception?)
                else pendingMonkeyKitActions.add(pendingRunnable)
            }
            CBTypes.onGetConversationMessages -> {
                if (monkeyKitDelegate != null)
                    monkeyKitDelegate!!.onGetConversationMessages(info[0] as String,
                        info[1] as ArrayList<MOKMessage>, info[2] as Exception?)
                else pendingMonkeyKitActions.add(pendingRunnable)
            }
            CBTypes.onNotificationReceived -> {
                if (newNotificationDelegate != null)
                    newNotificationDelegate!!.onNotificationReceived(info[0] as String, info[1] as String,
                        info[2] as String, info[3] as JsonObject, info[4] as String)
                else pendingNewMessageActions.add(pendingRunnable)
            }
            CBTypes.onFileFailsUpload -> {
                if (fileDelegate != null)
                    fileDelegate!!.onFileFailsUpload(info[0] as MOKMessage)
                else pendingFileActions.add(pendingRunnable)
            }
        }
    }






}
