package com.criptext.lib.delegates

import com.google.gson.JsonObject

/**
 * Created by gabriel on 2/13/17.
 */

interface GroupDelegate {
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
}