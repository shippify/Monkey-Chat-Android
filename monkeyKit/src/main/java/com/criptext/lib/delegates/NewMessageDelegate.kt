package com.criptext.lib.delegates

import com.criptext.comunication.MOKMessage
/**
 * Created by gabriel on 2/13/17.
 */

interface NewMessageDelegate {
    /**
     * This function is executed when a message arrived and stored in the DB.
     * @param message Objeto MOKMessage que representa al mensaje recibido.
     */
    fun onMessageReceived(message: MOKMessage)
}
