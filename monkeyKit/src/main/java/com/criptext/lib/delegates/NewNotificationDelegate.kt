package com.criptext.lib.delegates

import com.google.gson.JsonObject

/**
 * Created by gabriel on 2/13/17.
 */

interface NewNotificationDelegate {
    /**
     * When you receive a notification, MonkeyKit execute this callback.
     * @param messageId id of the message
     * *
     * @param senderId id of the sender
     * *
     * @param recipientId id of the recipient
     * *
     * @param params JsonObject of the params sent in the message
     * *
     * @param datetime datetime of the message
     */
    fun onNotificationReceived(messageId: String, senderId: String, recipientId: String,
                               params: JsonObject, datetime: String)

}
