package com.criptext.monkeychatandroid;

import android.provider.ContactsContract;
import android.util.Log;

import com.criptext.MonkeyFileService;
import com.criptext.comunication.MOKMessage;
import com.criptext.monkeychatandroid.models.DatabaseHandler;
import com.criptext.monkeychatandroid.models.MessageModel;
import com.criptext.monkeykitui.recycler.MonkeyItem;

import org.jetbrains.annotations.NotNull;

import io.realm.Realm;

/**
 * Created by GAumala on 8/3/16.
 */
public class MyFileUploadService extends MonkeyFileService{
    @Override
    public void onFileUploadFinished(@NotNull MOKMessage mokMessage, boolean error) {
        Realm realm = MonkeyChat.getInstance().getNewMonkeyRealm();
        realm.beginTransaction();
        DatabaseHandler.updateMessageOutgoingStatusBlocking(realm, mokMessage.getMessage_id(),
                error ? MonkeyItem.DeliveryStatus.error : MonkeyItem.DeliveryStatus.delivered);
        realm.commitTransaction();
        realm.close();

    }

    @Override
    public void onFileDownloadFinished(@NotNull String messageId, @NotNull String conversationId, boolean error) {
        Realm realm = MonkeyChat.getInstance().getNewMonkeyRealm();
        realm.beginTransaction();
        DatabaseHandler.updateMessageOutgoingStatusBlocking(realm, messageId,
                error ? MonkeyItem.DeliveryStatus.error : MonkeyItem.DeliveryStatus.delivered);
        realm.commitTransaction();
        realm.close();
    }
}
