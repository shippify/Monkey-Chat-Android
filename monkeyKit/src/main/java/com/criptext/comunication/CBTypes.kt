package com.criptext.comunication

/**
 * Created by gesuwall on 6/2/16.
 */

enum class CBTypes {
    onMessageReceived, onAcknowledgeReceived, onSocketConnected, onSocketDisconnected,
        onDeleteReceived, onCreateGroupOK, onCreateGroupError,
        onDeleteGroupOK, onDeleteGroupError, onAddMemberToGroupOK, onAddMemberToGroupError,
        onContactOpenMyConversation, onGetGroupInfoOK, onGetGroupInfoError, onNotificationReceived,
        onMessageBatchReady, onMessageFailDecrypt, onGroupAdded, onGroupNewMember, onGroupRemovedMember,
        onGroupsRecover, onFileFailsUpload, onConversationOpenResponse, onConnectionRefused
}