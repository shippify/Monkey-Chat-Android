package com.criptext.comunication

import com.google.gson.JsonObject

/**
 * Created by gesuwall on 10/5/16.
 */

class MOKNotification(val id: String, val senderId: String, val receiverId: String,
                      val params: JsonObject, val timestamp: Long) {

    constructor(remote: MOKMessage): this(remote.message_id, remote.sid, remote.rid,
            remote.params!!, remote.datetime.toLong())
}

