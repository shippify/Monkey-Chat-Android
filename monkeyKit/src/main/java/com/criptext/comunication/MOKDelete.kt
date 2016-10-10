package com.criptext.comunication

/**
 * Created by gesuwall on 10/5/16.
 */

class MOKDelete(val messageId: String, val senderId: String, val receiverId: String, val timestamp: Long) {

    fun getConversationId(myMonkeyId: String) =
            if (receiverId.startsWith("G:")) receiverId //message from group
                else if(receiverId == myMonkeyId) senderId //message sent to user
                else receiverId //message sent by user


    constructor(remote: MOKMessage): this(remote.props!!.get("message_id").asString, remote.sid, remote.rid,
            remote.datetime.toLong())
}
