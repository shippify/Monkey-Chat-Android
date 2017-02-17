package com.criptext.lib

import com.criptext.comunication.*
import com.google.gson.JsonObject

import com.criptext.comunication.MOKMessage;
import com.criptext.comunication.MOKUser;
import com.criptext.http.HttpSync
import com.criptext.lib.delegates.*
import java.util.*

interface MonkeyKitDelegate: SyncDelegate, GroupDelegate, ConnectionDelegate, ConversationDelegate,
        ConversationOpenDelegate, NewMessageDelegate, NewNotificationDelegate, AcknowledgeDelegate,
        FileDelegate {


    /**
     * This function is executed when you receive all your conversation messages.
     * @param messages array of the messages required.
     * *
     * @param e the exception of the result
     */
    fun onGetConversationMessages(conversationId: String, messages: ArrayList<MOKMessage>, e: Exception?)


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

}
