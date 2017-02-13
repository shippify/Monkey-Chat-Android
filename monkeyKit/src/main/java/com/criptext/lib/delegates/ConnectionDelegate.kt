package com.criptext.lib.delegates

/**
 * Created by gabriel on 2/13/17.
 */

interface ConnectionDelegate {
    /**
     * Callback executed when the server refused the connection because the credentials could
     * not be validated. The service won't attempt to reconnect. You should show a message to the
     * user informing that the service is unavailable. Retry after checking your credentials.
     */
    fun onConnectionRefused()

    /**
     * When MonkeyKit connect to the socket successfully and is ready to send and receive messages.
     * After this happens it is recommend to use the sendSet function to notify all the users that you are online.
     */
    fun onSocketConnected()

    /**
     * Our socket can get disconnect for network reasons but MonkeyKit will reconnect automatically. It is
     * important to notify to the users that they are disconnected.
     */
    fun onSocketDisconnected()
}
