package com.criptext

import com.criptext.ClientData
import com.criptext.MonkeyKitSocketService
import com.criptext.comunication.MOKMessage
import com.criptext.http.HttpSync

/**
 * Created by gesuwall on 12/22/16.
 */

class TestService: MonkeyKitSocketService() {
    override fun loadClientData() = ClientData("John Smith", "TEST_APP_ID", "TEST_APP_KEY", "TestMonkeyId", "sdomain", 1139)

    override fun storeReceivedMessage(message: MOKMessage, runnable: Runnable) {
    }

    override fun syncDatabase(syncData: HttpSync.SyncData, runnable: Runnable) {
    }

    override val uploadServiceClass: Class<*>
        get() = TestService::class.java

}