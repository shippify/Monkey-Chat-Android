package com.criptext.comunication

/**
 * Created by gesuwall on 10/5/16.
 */

class MOKDelete(val messageId: String, val senderId: String, val receiverId: String, val timestamp: Long) {

    constructor(remote: MOKMessage): this(remote.message_id, remote.sid, remote.rid,
            remote.datetime.toLong())
}
