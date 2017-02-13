package com.criptext.lib.delegates

/**
 * Created by gabriel on 2/13/17.
 */

interface AcknowledgeDelegate {
    /**
     * When the message arrive to the server, MonkeyKit receive an ack. This callback come with
     * with a new ID. The implementation of this callback mark the message as sent.
     * @param senderId id of the sender
     * *
     * @param recipientId id of the recipient
     * *
     * @param newId new Id of the message
     * *
     * @param oldId old id of the message
     * *
     * @param read boolean if the message is read or not
     * *
     * @param messageType type of the message 1 text, 2 file
     */
    fun onAcknowledgeRecieved(senderId: String, recipientId: String, newId: String, oldId: String,
                              read: Boolean, messageType: Int)
}
