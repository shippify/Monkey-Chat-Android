package com.criptext

import com.criptext.ClientData
import com.criptext.MonkeyKitSocketService
import com.criptext.comunication.MOKMessage
import com.criptext.http.HttpSync
import java.util.*

/**
 * Created by gesuwall on 12/22/16.
 */

class TestService: MonkeyKitSocketService() {
    override fun loadClientData() = ClientData("John Smith", "TEST_APP_ID", "TEST_APP_KEY", "TestMonkeyId", "sdomain", 1139)
    val messagesDB: HashMap<String, MOKMessage> = hashMapOf()

    fun isMessageStored(messageID: String) = messagesDB.containsKey(messageID)

    override fun storeReceivedMessage(message: MOKMessage, runnable: Runnable) {
        messagesDB.put(message.message_id, message)
        runnable.run()
    }

    override fun syncDatabase(syncData: HttpSync.SyncData, runnable: Runnable) {
        runnable.run()
    }

    override val uploadServiceClass: Class<*>
        get() = TestService::class.java

}