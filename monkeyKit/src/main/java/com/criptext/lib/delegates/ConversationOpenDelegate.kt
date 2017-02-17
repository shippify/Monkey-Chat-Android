package com.criptext.lib.delegates

/**
 * Created by gesuwall on 2/16/17.
 */

interface ConversationOpenDelegate {
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
    fun onConversationOpenResponse(senderId: String, isOnline: Boolean, lastSeen: String,
                                   lastOpenMe: String?, members_online: String)

    /**
     * When a MonkeyKit user opens a conversation with the current user, a special notification
     * is received from server triggering this callback. In this callback you should mark as read
     * all the messages that have been successfully delivered to that user.
     * @param monkeyId monkey id of the user that opened the conversation.
     */
    fun onContactOpenMyConversation(monkeyId: String)
}