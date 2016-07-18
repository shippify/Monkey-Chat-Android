package com.criptext.security

import com.criptext.comunication.MOKMessage

/**
 * Created by gesuwall on 6/6/16.
 */

data class EncryptedMsg(val message: MOKMessage, var key: String, var iv: String) {

    companion object {
        fun fromSecret(message: MOKMessage, secret: String): EncryptedMsg{
            val array = secret.split(":")
            return EncryptedMsg(message, array[0], array[1])
        }

    }
}
