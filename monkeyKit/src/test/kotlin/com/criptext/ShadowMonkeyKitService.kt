package com.criptext

import android.os.Message
import com.criptext.comunication.CBTypes
import com.criptext.comunication.MOKMessage
import com.criptext.comunication.MessageTypes
import com.criptext.comunication.PushMessage
import com.criptext.http.HttpSync
import org.amshove.kluent.`should equal`
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import java.util.*

/**
 * Created by gesuwall on 12/23/16.
 */

@Implements(MonkeyKitSocketService::class)
class ShadowMonkeyKitService {

    companion object {
        var isOnline = false
        private set
        var requestedSync = false
        private set
        var _asyncSocketConnected = false
        private set

        fun clearRequestedSync() {
            requestedSync = false
        }

        fun cleanup() {
            isOnline = false
            requestedSync = false
            _asyncSocketConnected = false
        }

        private val sentMessages: HashMap<String, MOKMessage> = hashMapOf()

        /**
         * When the asyncConnSocket conects, the isAsyncSocketConnected() function must return true
         * afterwards. then a 'MessageSocketConnected' is passed to the service's message handler.
         * This eventually triggers the sendSync() function. the next step to finish the connection
         * simulation is to simulate the syncComplete event.
         */
        fun simulateSocketConnection(service: MonkeyKitSocketService) {
            _asyncSocketConnected = true
            val msg = Message();
            msg.what = MessageTypes.MessageSocketConnected
            service.messageHandler.handleMessage(msg)
        }

        /**
         * Before simulating a syncComplete, the service should first request a sync response, otherwise
         * this will fail. When the sync response is received a message of type MOKProtocolSync
         * is passed to the processMessageFromHandler() function with the simulated response. This
         * finishes the connection process and calls the setOnline() method
         */
        fun simulateSyncComplete(service: MonkeyKitSocketService, nextResponse: HttpSync.SyncData) {
            val msg = Message();
            ShadowMonkeyKitService.requestedSync `should equal` true
            ShadowMonkeyKitService.clearRequestedSync()
            msg.what = MessageTypes.MOKProtocolSync
            msg.obj = nextResponse
            service.processMessageFromHandler(CBTypes.onSyncComplete, arrayOf(nextResponse))
        }

        /**
         * Simulates the complete socket connection by calling simulateSocketConnection() and then
         * simulateSyncComplete().
         */
        fun simulateConnectionProcess(service: MonkeyKitSocketService, nextResponse: HttpSync.SyncData) {
            simulateSocketConnection(service)
            simulateSyncComplete(service, nextResponse)
        }

        fun findSentMessage(id: String) = sentMessages.get(id)

    }

    @Implementation
    fun isAsyncSocketConnected() : Boolean {
        return _asyncSocketConnected
    }

    @Implementation
    fun sendSync(){
        requestedSync = true
    }

    @Implementation
    fun sendSync(since: Long, qty: Int){
        requestedSync = true
    }

    @Implementation
    fun setOnline(online: Boolean) {
        isOnline = online
    }

    @Implementation
    fun sendMessage(newMessage: MOKMessage, pushMessage: PushMessage, encrypted: Boolean): MOKMessage {
        sentMessages.put(newMessage.message_id, newMessage)
        return newMessage
    }

    @Implementation
    fun sendFileMessage(newMessage: MOKMessage, pushMessage: PushMessage, encrypted: Boolean) {
        sentMessages.put(newMessage.message_id, newMessage)
    }

}