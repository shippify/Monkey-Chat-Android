package com.criptext.monkeychatandroid;

import android.widget.Toast;

import com.criptext.comunication.MOKMessage;
import com.criptext.comunication.MessageTypes;
import com.criptext.comunication.PushMessage;
import com.criptext.monkeychatandroid.models.conversation.ConversationItem;
import com.criptext.monkeychatandroid.models.message.MessageItem;
import com.criptext.monkeychatandroid.state.ChatState;
import com.criptext.monkeykitui.cav.EmojiHandler;
import com.criptext.monkeykitui.input.listeners.InputListener;
import com.criptext.monkeykitui.recycler.MessagesList;
import com.criptext.monkeykitui.recycler.MonkeyItem;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by gesuwall on 2/24/17.
 */
public class CustomInputListener implements InputListener {
    private MainActivity act;
    private ChatState state;
    private MessageItem nextFileMessage;

    public CustomInputListener (MainActivity act, ChatState state) {
        this.act = act;
        this.state = state;
    }


    @Override
            public void onStopTyping() {
                JSONObject params = new JSONObject();
                try {
                    params.put("type", 20);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if(state.activeConversationItem != null){
                    act.sendTemporalNotification(state.activeConversationItem.getConvId(), params);
                }
            }

            @Override
            public void onTyping(@NotNull String text) {
                JSONObject params = new JSONObject();
                try {
                    params.put("type", 21);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if(state.activeConversationItem != null){
                    act.sendTemporalNotification(state.activeConversationItem.getConvId(), params);
                }
            }

            @Override
            public void onNewItemFileError(int type) {
                Toast.makeText(act, "Error writing file of type " +
                        MonkeyItem.MonkeyItemType.values()[type], Toast.LENGTH_LONG).show();
            }

            @Override
            public void onNewItem(@NotNull MonkeyItem item) {

                String textTalk = null;
                JsonObject params = new JsonObject();
                MOKMessage mokMessage;

                ConversationItem activeConv = state.activeConversationItem; //should not be null
                final String convId = activeConv.getConvId();
                if(activeConv.isGroup())
                    textTalk = activeConv.getName();


                //Store the message in the DB and send it via MonkeyKit
                switch (MonkeyItem.MonkeyItemType.values()[item.getMessageType()]) {
                    case audio:
                        params = new JsonObject();
                        params.addProperty("length",""+item.getAudioDuration()/1000);

                        mokMessage = act.persistFileMessageAndSend(item.getFilePath(), state.myMonkeyID, convId,
                            MessageTypes.FileTypes.Audio, params,
                            new PushMessage(EmojiHandler.encodeJavaForPush(state.myName) +
                            (textTalk==null ? " sent you an audio" : "sent an audio to " + textTalk) ), true);
                        break;
                    case photo:
                        mokMessage = act.persistFileMessageAndSend(item.getFilePath(), state.myMonkeyID, convId,
                            MessageTypes.FileTypes.Photo, new JsonObject(),
                            new PushMessage(EmojiHandler.encodeJavaForPush(state.myName) +
                            (textTalk==null ? " sent you a photo" : "sent a photo to " + textTalk) ), true);
                        break;
                    default:
                        mokMessage = act.persistMessageAndSend(item.getMessageText(), state.myMonkeyID,
                            convId, params, new PushMessage(EmojiHandler.encodeJavaForPush(state.myName) +
                            (textTalk==null ? " sent you a message" : " sent a message to " + textTalk) ), true);
                        break;
                }

                //Now that the message was sent, create a MessageItem using the MOKMessage that MonkeyKit
                //created. This MessageItem will be added to MonkeyAdapter so that it can be shown in
                //the screen.
                //USE THE DATETIMEORDER FROM MOKMESSAGE, NOT THE ONE FROM MONKEYITEM
                MessageItem newItem = new MessageItem(state.myMonkeyID, convId, mokMessage.getMessage_id(),
                        item.getMessageText(), item.getMessageTimestamp(), mokMessage.getDatetimeorder(), item.isIncomingMessage(),
                        MonkeyItem.MonkeyItemType.values()[item.getMessageType()]);
                newItem.setParams(params.toString());
                newItem.setProps(mokMessage.getProps().toString());

                switch (MonkeyItem.MonkeyItemType.values()[item.getMessageType()]) {
                    case audio:
                        newItem.setAudioDuration(item.getAudioDuration()/1000);
                        newItem.setMessageContent(item.getFilePath());
                        newItem.setFileSize(mokMessage.getFileSize());
                        break;
                    case photo:
                        newItem.setMessageContent(item.getFilePath());
                        newItem.setFileSize(mokMessage.getFileSize());
                        nextFileMessage = newItem; //photos are special, don't add to list yet
                        act.updateConversationByMessage(newItem, false);
                        return;
                }

                state.messagesMap.get(convId).smoothlyAddNewItem(newItem);
                act.updateConversationByMessage(newItem, false);
            }

    public MessageItem popNewFileMessage() {
        final MessageItem m = nextFileMessage;
        nextFileMessage = null;
        return m;
    }
}
