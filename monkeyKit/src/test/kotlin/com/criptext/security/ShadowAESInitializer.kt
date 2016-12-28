package com.criptext.security

import com.criptext.ClientData
import com.criptext.MonkeyKitSocketService
import com.google.gson.JsonObject
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject

/**
 * Created by gesuwall on 12/23/16.
 */

@Implements(AsyncAESInitializer::class)
class ShadowAESInitializer {
    @RealObject
    private var task: AsyncAESInitializer? = null

    companion object {
        private var runnable: Runnable? = null
        var nextLastSync: Long = 0L
        var nextHasSyncedBefore: Boolean = false
        var nextClientData: ClientData? = null
        var nextPendingMessages: List<JsonObject>? = null

        fun postResult(pendingMessages: List<JsonObject>, hasSyncedBefore: Boolean, lastSync: Long,
                       clientData: ClientData) {
            nextPendingMessages = pendingMessages
            nextHasSyncedBefore = hasSyncedBefore
            nextLastSync = lastSync
            nextClientData = clientData
            runnable!!.run()
        }
    }

    @Implementation
    fun __constructor__(socketService: MonkeyKitSocketService) {

    }

    @Implementation
    fun initialize() {
        runnable = Runnable {
            val result = AsyncAESInitializer.InitializerResult(nextPendingMessages, AESUtil(null, "key"),
                    nextHasSyncedBefore, nextLastSync, nextClientData!!)
            task!!.onPostExecute(result)
        }


    }

}