package com.criptext.monkeychatandroid;

import com.criptext.MonkeyFileService;
import com.criptext.comunication.MOKMessage;
import com.criptext.monkeychatandroid.models.DatabaseHandler;
import com.criptext.monkeykitui.recycler.MonkeyItem;

import org.jetbrains.annotations.NotNull;

/**
 * Created by GAumala on 8/3/16.
 */
public class MyFileUploadService extends MonkeyFileService{
    @Override
    public void onFileUploadFinished(@NotNull MOKMessage mokMessage, boolean error) {
        DatabaseHandler.updateMessageStatus(mokMessage.getMessage_id(), null,
                error ? MonkeyItem.DeliveryStatus.error : MonkeyItem.DeliveryStatus.delivered);
    }

    @Override
    public void onFileDownloadFinished(@NotNull String messageId, @NotNull String conversationId, boolean error) {
        DatabaseHandler.updateMessageStatus(messageId, null,
                error ? MonkeyItem.DeliveryStatus.error : MonkeyItem.DeliveryStatus.delivered);
    }
}
