package com.criptext.lib

import com.criptext.comunication.MOKMessage
import com.criptext.comunication.PushMessage

/**
 * Created by gesuwall on 7/29/16.
 */

class DelegateMOKMessage(val message: MOKMessage, val push: PushMessage, val isEncrypted: Boolean){
     var failed: Boolean = false
}