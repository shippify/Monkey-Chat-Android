package com.criptext.lib.delegates

import com.criptext.comunication.MOKConversation
import com.criptext.comunication.MOKUser
import java.util.*

/**
 * Created by gabriel on 2/13/17.
 */

interface ConversationDelegate {
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


}
