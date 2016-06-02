package com.criptext.comunication

/**
 * Created by gesuwall on 6/2/16.
 */

enum class CBTypes {
    onMessageReceived, onAcknowledgeReceived, onSocketConnected, onSocketDisconnected,
        onConnectOK, onNetworkError, onDeleteReceived, onCreateGroupOK, onCreateGroupError,
        onDeleteGroupOK, onDeleteGroupError, onAddMemberToGroupOK, onAddMemberToGroupError,
        onContactOpenMyConversation, onGetGroupInfoOK, onGetGroupInfoError, onNotificationReceived,
        onMessageBatchReady
}