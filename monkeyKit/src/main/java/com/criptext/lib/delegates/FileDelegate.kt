package com.criptext.lib.delegates

import com.criptext.comunication.MOKMessage

/**
 * Created by gabriel on 2/13/17.
 */

interface FileDelegate {

    /**
     * This function is executed when a file fails upload.
     * @param message MOKMessage of the file.
     */
    fun onFileFailsUpload(message: MOKMessage)

    /**
     * Callback executed when a file download operation finishes, successfully or not.
     * With this callback you should update your UI to show an error message or a download
     * complete message, depending on the result.
     * @param fileMessageId The downloaded file's message id
     * @param fileMessageTimestamp Unique identiffier of the downloaded file's conversation, this
     * might make it easier to search for the message.
     * @param fileMessageTimestamp The downloaded file's timestamp, this might make it easier to
     * search for the message.
     * *
     * @param success true if the file was successfully downloaded, otherwise false
     */
    fun onFileDownloadFinished(fileMessageId: String, fileMessageTimestamp: Long,
                               conversationId: String, success: Boolean)
}
