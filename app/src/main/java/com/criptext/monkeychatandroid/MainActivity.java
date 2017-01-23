package com.criptext.monkeychatandroid;

import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.activeandroid.Model;
import com.criptext.ClientData;
import com.criptext.comunication.MOKConversation;
import com.criptext.comunication.MOKMessage;
import com.criptext.comunication.MOKNotification;
import com.criptext.comunication.MOKUser;
import com.criptext.comunication.MessageTypes;
import com.criptext.comunication.PushMessage;
import com.criptext.gcm.MonkeyRegistrationService;
import com.criptext.http.HttpSync;
import com.criptext.lib.MKDelegateActivity;
import com.criptext.monkeychatandroid.dialogs.SyncStatus;
import com.criptext.monkeychatandroid.gcm.SampleRegistrationService;
import com.criptext.monkeychatandroid.models.AsyncDBHandler;
import com.criptext.monkeychatandroid.models.conversation.ConversationItem;
import com.criptext.monkeychatandroid.models.DatabaseHandler;
import com.criptext.monkeychatandroid.models.conversation.TransactionCreator;
import com.criptext.monkeychatandroid.models.conversation.task.FindConversationTask;
import com.criptext.monkeychatandroid.models.conversation.task.GetConversationPageTask;
import com.criptext.monkeychatandroid.models.conversation.task.UpdateConversationsInfoTask;
import com.criptext.monkeychatandroid.models.message.task.GetMessagePageTask;
import com.criptext.monkeychatandroid.models.message.MessageItem;
import com.criptext.monkeychatandroid.models.SaveModelTask;
import com.criptext.monkeychatandroid.models.conversation.task.StoreNewConversationTask;
import com.criptext.monkeychatandroid.models.conversation.task.UpdateConversationsTask;

import com.criptext.monkeychatandroid.models.message.task.UpdateMessageDeliveryStatusTask;
import com.criptext.monkeychatandroid.models.UserItem;
import com.criptext.monkeykitui.MonkeyChatFragment;
import com.criptext.monkeykitui.MonkeyConversationsFragment;
import com.criptext.monkeykitui.MonkeyInfoFragment;
import com.criptext.monkeykitui.cav.EmojiHandler;
import com.criptext.monkeykitui.conversation.ConversationsActivity;
import com.criptext.monkeykitui.conversation.ConversationsList;
import com.criptext.monkeykitui.conversation.DefaultGroupData;
import com.criptext.monkeykitui.conversation.MonkeyConversation;
import com.criptext.monkeykitui.conversation.holder.ConversationTransaction;
import com.criptext.monkeykitui.info.InfoActivity;
import com.criptext.monkeykitui.input.listeners.InputListener;
import com.criptext.monkeykitui.recycler.ChatActivity;
import com.criptext.monkeykitui.recycler.GroupChat;
import com.criptext.monkeykitui.recycler.MessagesList;
import com.criptext.monkeykitui.recycler.MonkeyInfo;
import com.criptext.monkeykitui.recycler.MonkeyItem;
import com.criptext.monkeykitui.recycler.MonkeyItemTransaction;
import com.criptext.monkeykitui.recycler.audio.PlaybackNotification;
import com.criptext.monkeykitui.recycler.audio.PlaybackService;
import com.criptext.monkeykitui.toolbar.ToolbarDelegate;
import com.criptext.monkeykitui.util.MonkeyFragmentManager;
import com.criptext.monkeykitui.util.Utils;
import com.google.gson.JsonObject;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

public class MainActivity extends MKDelegateActivity implements ChatActivity, ConversationsActivity,
        InfoActivity, ToolbarDelegate{

    private static String DATA_FRAGMENT = "MainActivity.chatDataFragment";
    //Since this is the Chat activity, we need a RecyclerView and an adapter. Additionally we
    //will store the messages in our own list so that they can be accessed easily.
    MonkeyFragmentManager monkeyFragmentManager;
    MonkeyChatFragment monkeyChatFragment;
    MonkeyInfoFragment monkeyInfoFragment;

    //All the activity state is kept in this headless fragment
    ChatDataFragment state;
    static int MESS_PERPAGE = 30;

    /**
     * This class is basically a media player for our voice notes. we pass this to MonkeyAdapter
     * so that it can handle all the media playback for us. However, we must initialize it in "onStart".
     * and release it in "onStop" method.
     */
    PlaybackService.VoiceNotePlayerBinder voiceNotePlayer;

    private AsyncDBHandler asyncDBHandler;

    private File downloadDir;

    /**
     * Class used to control the status bar that shows "Connecting.." and "Connected."
     */
    private SyncStatus syncStatus;

    private ServiceConnection playbackConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            voiceNotePlayer = (PlaybackService.VoiceNotePlayerBinder) service;
            if(monkeyChatFragment != null)
                monkeyChatFragment.setVoiceNotePlayer(voiceNotePlayer);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean restored = restoreState();
        downloadDir = MonkeyChat.getDownloadDir(this);

        //Check play services. if available try to register with GCM so that we get Push notifications
        if(MonkeyRegistrationService.Companion.checkPlayServices(this))
                registerWithGCM();

        //Setup MonkeyKit UI fragments
        monkeyFragmentManager = new MonkeyFragmentManager(this,
                getResources().getString(R.string.app_name), state.mkFragmentStack);
        //this function sets the content layout for the fragments and puts a conversations fragment
        monkeyFragmentManager.setContentLayout(savedInstanceState, true);
        if (restored)
            monkeyFragmentManager.restoreToolbar(state.activeConversationItem);

        asyncDBHandler = new AsyncDBHandler();

        //wait for a timeout to show a "connecting" message
        syncStatus = new SyncStatus(monkeyFragmentManager);
        if(!isSocketConnected()) {
            syncStatus.delayConnectingMessage();
        }
    }

    public void registerWithGCM(){
        Intent intent = new Intent(this, SampleRegistrationService.class);
        intent.putExtra(ClientData.Companion.getAPP_ID_KEY(), SensitiveData.APP_ID);
        intent.putExtra(ClientData.Companion.getAPP_KEY_KEY(), SensitiveData.APP_KEY);
        intent.putExtra(ClientData.Companion.getMONKEY_ID_KEY(), state.myMonkeyID);
        startService(intent);
        Log.d("MainActivity", "Registering with GCM");
    }

    @Override
    protected void onPause() {
        super.onPause();
        //sensorHandler.onPause();
    }

    @Override
    protected void onStart(){
        super.onStart();
        //bind to the service that plays voice notes.
        startPlaybackService();

        monkeyFragmentManager.setToolbarOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               if(monkeyChatFragment != null && monkeyInfoFragment == null){
                    MonkeyInfoFragment infoFragment = MonkeyInfoFragment.Companion.newInfoInstance(
                            monkeyChatFragment.getConversationId(),
                            monkeyChatFragment.getConversationId().contains("G:"), false);
                    monkeyFragmentManager.setInfoFragment(infoFragment);
                    if(getCurrentFocus() != null){
                        getCurrentFocus().clearFocus();
                    }
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        getApplicationContext().unbindService(playbackConnection);
        //sensorHandler.onStop();
    }

    private void startPlaybackService() {
        Intent intent = new Intent(getApplicationContext(), PlaybackService.class);
        if(!PlaybackService.Companion.isRunning())
            getApplicationContext().startService(intent);
        getApplicationContext().bindService(intent, playbackConnection, Context.BIND_AUTO_CREATE);
    }

    private boolean restoreState(){
        final ChatDataFragment retainedFragment =(ChatDataFragment) getSupportFragmentManager().findFragmentByTag(DATA_FRAGMENT);
        if(retainedFragment != null) {
            if (monkeyChatFragment != null) {
                monkeyChatFragment.setInputListener(initInputListener());
                monkeyChatFragment.setVoiceNotePlayer(voiceNotePlayer);
            }
            state = retainedFragment;
            return true;
        } else {
            state = ChatDataFragment.newInstance(this);
            getSupportFragmentManager().beginTransaction().add(state, DATA_FRAGMENT).commit();
            return false;
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //sensorHandler.onDestroy();
        if(monkeyChatFragment != null)
            monkeyChatFragment.setInputListener(null);
        asyncDBHandler.cancelAll();
    }

    @Override
    protected void onRestart(){
        super.onRestart();
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                break;
        }
        return false;
    }

    /***
     * MY OWN METHODS
     */

    /**
     * Sets an InputListener to the InputView. This object listens for new messages that the user
     * wants to send, regardless of the type. They can be text, audio or photo messages. The listener
     * checks the type to figure out how to send it with MonkeyKit.
     */
    public InputListener initInputListener(){
        return new InputListener() {
            @Override
            public void onStopTyping() {
                JSONObject params = new JSONObject();
                try {
                    params.put("type", 20);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if(state.activeConversationItem != null){
                    sendTemporalNotification(state.activeConversationItem.getConvId(), params);
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
                    sendTemporalNotification(state.activeConversationItem.getConvId(), params);
                }
            }

            @Override
            public void onNewItemFileError(int type) {
                Toast.makeText(MainActivity.this, "Error writing file of type " +
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

                        mokMessage = persistFileMessageAndSend(item.getFilePath(), state.myMonkeyID, convId,
                            MessageTypes.FileTypes.Audio, params,
                            new PushMessage(EmojiHandler.encodeJavaForPush(state.myName) +
                            (textTalk==null ? " sent you an audio" : "sent an audio to " + textTalk) ), true);
                        break;
                    case photo:
                        mokMessage = persistFileMessageAndSend(item.getFilePath(), state.myMonkeyID, convId,
                            MessageTypes.FileTypes.Photo, new JsonObject(),
                            new PushMessage(EmojiHandler.encodeJavaForPush(state.myName) +
                            (textTalk==null ? " sent you a photo" : "sent a photo to " + textTalk) ), true);
                        break;
                    default:
                        mokMessage = persistMessageAndSend(item.getMessageText(), state.myMonkeyID,
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
                        break;
                }

                state.messagesMap.get(convId).smoothlyAddNewItem(newItem);
                updateConversationByMessage(newItem, false);
            }
        };
    }

    /**
     * Updates a conversation in the database and then optionally adds it to the conversation list.
     * If conversation is not found in the database, fetch the conversation from server. This
     * method should be used when we want to update a conversation that is not in our
     * 'conversations' list.
     * @param conversationId id of the conversation to update
     * @param transaction transaction that will update the conversation.
     * @param addToList if true the conversation will be added to the UI when it is found in the DB.
     *                  Conversations found in server are always added to UI.
     */
    private void updateMissingConversation(final String conversationId,
                                           ConversationTransaction transaction, final boolean addToList) {
        String[] ids = { conversationId };
        asyncDBHandler.updateMissingConversationsTask(new UpdateConversationsTask.OnQueryReturnedListener() {
            @Override
            public void onQueryReturned(List<ConversationItem> results) {
                if(results.isEmpty()) { //conversation not in DB, request server
                    getConversationInfo(conversationId);
                } else if (addToList){ //conversation found in DB, add it to list
                    ConversationItem newItem = results.get(0);
                    state.conversations.addNewConversation(newItem);
                }
            }
        }, ids, transaction);
    }

    private  void updateConversationByMessage(MessageItem message, boolean read) {
        ConversationTransaction transaction = TransactionCreator.fromSentMessage(message, read);
        ConversationItem conversation = (ConversationItem) state.conversations.findConversationById(message.conversationId);
        if (conversation != null) {
            state.conversations.updateConversation(conversation, transaction);
            DatabaseHandler.updateConversation(conversation);
        } else //find conversation elsewhere
            updateMissingConversation(message.getConversationId(), transaction, true);
    }

    /**
     * Ask old messages from server
     * @param conversationId id of the conversation messages required
     */
    public void requestMessagesFromServer(String conversationId){
        final MessagesList conversationMessages = state.messagesMap.get(conversationId);
        if (conversationId != null) {
            long firstTimestamp = 0L;
            final MonkeyItem firstItem = conversationMessages.getFirstItem();
            if (firstItem != null)
                firstTimestamp = firstItem.getMessageTimestamp();
            getConversationMessages(conversationId, MESS_PERPAGE, firstTimestamp);
        }
    }

    /**
     * Change the status bar depending on the state
     * @param status status of the connection
     */
    public void setStatusBarState(Utils.ConnectionStatus status){

        if(monkeyFragmentManager==null)
            return;
        monkeyFragmentManager.showStatusNotification(status);

    }

    /**
     * Update a status message. This is normally used after you send a message.
     * @param oldId message old id
     * @param id message id
     * @param newStatus new status to change
     */

    private void updateMessage(String id, String oldId, MonkeyItem.DeliveryStatus newStatus) {
        MessageItem messageItem = DatabaseHandler.getMessageById(id);
        if(messageItem == null){
            messageItem = DatabaseHandler.getMessageById(oldId);
        }

        if (messageItem != null) {
            messageItem.setStatus(newStatus.ordinal());
            if(oldId != null){
                messageItem.setOldMessageId(oldId);
                messageItem.setMessageId(id);
                DatabaseHandler.updateMessageStatus(id, oldId, newStatus);

            }else{
                DatabaseHandler.updateMessageStatus(id, null, newStatus);
            }

            if (state.activeConversationItem != null) {
                final String activeConversationId = state.activeConversationItem.getConvId();
                final MessagesList convMessages = state.messagesMap.get(activeConversationId);
                MessageItem message = (MessageItem) convMessages.findMonkeyItemById(oldId != null ? oldId : id);
                if (message != null) {
                    message.setStatus(newStatus.ordinal());
                    if(oldId != null){
                        message.setOldMessageId(oldId);
                        message.setMessageId(id);
                    }
                    if (monkeyChatFragment != null)
                        monkeyChatFragment.refreshDeliveryStatus(message);
                }
            }
        }

    }

    /**
     * Creates a new MonkeyChatFragment and adds it to the activity.
     * @param chat conversation to display
     */
    public void startChatWithMessages(ConversationItem chat){
        MonkeyChatFragment fragment = new MonkeyChatFragment.Builder(chat.getConvId(), chat.getName())
                                            .setAvatarURL(chat.getAvatarFilePath())
                                            .setLastRead(chat.lastRead)
                                            .setMembersIds(chat.getGroupMembers())
                                            .build();
         monkeyFragmentManager.setChatFragment(fragment);
    }
    /**
     * Updates a sent message and updates de UI so that the user can see that it has been
     * successfully delivered
     * @param oldId The old Id of the message, set locally.
     * @param newId The new id of the message, set by the server.
     * @param read true if the message was delivered and read
     */
    private void markMessageAsDelivered(String oldId, String newId, boolean read){
        updateMessage(newId, oldId, MonkeyItem.DeliveryStatus.delivered);
    }



    /**
     * adds a message to the adapter so that it can be displayed in the RecyclerView.
     * @param message a received message
     */
    private MessageItem processNewMessage(MOKMessage message) {
        String conversationID = message.getConversationID(state.myMonkeyID);
        MessagesList convMessages = state.getLoadedMessages(conversationID);
        MessageItem newItem = DatabaseHandler.createMessage(message, downloadDir.getAbsolutePath(),
                state.myMonkeyID);
        convMessages.smoothlyAddNewItem(newItem);
        return newItem;
    }

    /**
     * adds old messages to the adapter so that it can be displayed in the RecyclerView.
     * @param messages
     */
    private void processOldMessages(String conversationId, ArrayList<MOKMessage> messages){

        ArrayList<MessageItem> messageItems = new ArrayList<>();
        for(MOKMessage message: messages){
            messageItems.add(DatabaseHandler.createMessage(message, downloadDir.getAbsolutePath(), state.myMonkeyID));
        }
        Collections.sort(messageItems);
        DatabaseHandler.saveMessages(messageItems);
        final MessagesList conversationMessages = state.getLoadedMessages(conversationId);
        conversationMessages.addOldMessages(new ArrayList<MonkeyItem>(messageItems), messages.size() == 0);
    }

    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK) {
            return;
        }

        //Since our chat fragment uses different activities to take and edit photos, we must forward
        // the onActivityResult event to it so that it can react to the results of take photo, choose
        //photo and edit photo.
        if(monkeyChatFragment != null) {
            monkeyChatFragment.onActivityResult(requestCode, resultCode, data);
        }else{
            android.support.v4.app.Fragment tempChatFragment = getSupportFragmentManager().findFragmentByTag(MonkeyFragmentManager.Companion.getCHAT_FRAGMENT_TAG());
            if(tempChatFragment != null){
                tempChatFragment.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    /******
     * These are the methods that MonkeyKit calls to inform us about new events.
     */

    @Override
    public void storeSendingMessage(final MOKMessage message) {
        //TODO update conversation
        DatabaseHandler.storeNewMessage(DatabaseHandler.createMessage(message,
                downloadDir.getAbsolutePath(), state.myMonkeyID));
    }

    /******
     * These are the methods that MonkeyKit calls to inform us about new events.
     */

    @Override
    public void onSocketConnected() {
        super.onSocketConnected();
        setStatusBarState(Utils.ConnectionStatus.connected);
    }

    @Override
    public void onSocketDisconnected() {
        setStatusBarState(Utils.ConnectionStatus.connecting);
    }

    @Override
    public void onFileDownloadFinished(String fileMessageId, long fileMessageTimestamp,
                                       String conversationId, final boolean success) {
        //TODO use better search algorithm
        super.onFileDownloadFinished(fileMessageId, fileMessageTimestamp, conversationId, success);
        MessagesList convMessages = state.messagesMap.get(conversationId);
        if (convMessages != null)
            convMessages.updateMessage(fileMessageId, fileMessageTimestamp, new MonkeyItemTransaction() {
                @Override
                public MonkeyItem invoke(MonkeyItem monkeyItem) {
                    MessageItem item = (MessageItem) monkeyItem;
                    item.setStatus(success ? MonkeyItem.DeliveryStatus.delivered.ordinal() :
                            MonkeyItem.DeliveryStatus.error.ordinal());
                    return item;
                }
            });
        //since this is an update, we don't care what happens if the loaded messages list is null
    }

    @Override
    public void onAcknowledgeRecieved(@NotNull final String senderId, @NotNull final String recipientId,
                                      final @NotNull String newId, final @NotNull String oldId, final boolean read,
                                      final int messageType) {
        //Always call super so that MKDelegate knows that it should not attempt to retry this message anymore
        super.onAcknowledgeRecieved(senderId, recipientId, newId, oldId, read, messageType);

        asyncDBHandler.updateMessageDeliveryStatus(new UpdateMessageDeliveryStatusTask.OnQueryReturnedListener() {
            @Override
            public void onQueryReturned(final MessageItem result) {
                if(result != null){
                    if(read && monkeyChatFragment != null && senderId.equals(monkeyChatFragment.getConversationId()))
                        monkeyChatFragment.setLastRead(result.getMessageTimestampOrder()); //update checkmarks in fragment

                    updateConversationByMessage(result, read);

                    MessagesList convMessageList = state.getLoadedMessages(senderId);
                    convMessageList.updateMessage(result, new MonkeyItemTransaction() {
                        @Override
                        public MonkeyItem invoke(MonkeyItem monkeyItem) {
                            return result;
                        }
                    });

                } else if((messageType == Integer.parseInt(MessageTypes.MOKText)
                        || messageType == Integer.parseInt(MessageTypes.MOKFile))){
                    //If we use the same monkeyId for several devices (multisession) we receive an
                    // acknowledge for each message sent. So to validate if we have the message
                    // sent, we can send a sync message.
                    sendSync();
                }
            }
        }, oldId, newId);
    }

    @Override
    public void onCreateGroup(String groupMembers, String groupName, String groupID, Exception e) {
        if(e==null){
            ConversationItem conversationItem = new ConversationItem(groupID,
                    groupName, System.currentTimeMillis(), "Write to this group",
                    0, true, groupMembers, "", MonkeyConversation.ConversationStatus.empty.ordinal());
            state.conversations.addNewConversation(conversationItem); //TODO update silently??
        }
    }

    @Override
    public void onAddGroupMember(@Nullable String groupID, @Nullable String newMember, @Nullable String members, @Nullable Exception e) {
    }

    @Override
    public void onRemoveGroupMember(@Nullable String groupID, @Nullable String removedMember, @Nullable String members, @Nullable Exception e) {
    }

    @Override
    public void onUpdateUserData(@NotNull String monkeyId, @Nullable Exception e) {

    }

    @Override
    public void onUpdateGroupData(@NotNull String groupId, @Nullable Exception e) {

    }

    @Override
    public void onMessageReceived(@NonNull MOKMessage message) {
        MessageItem newItem = processNewMessage(message);
        updateConversationByMessage(newItem, monkeyChatFragment != null &&
                state.activeConversationItem != null &&
                state.activeConversationItem.getConvId().equals(newItem.getConversationId()));
    }

    /**
     * Update conversations in memory and the update the MonkeyConversationsFragment.
     */
    private void syncConversationsFragment() {
        int totalConversations = state.conversations.size();
        asyncDBHandler.getConversationPage(new GetConversationPageTask.OnQueryReturnedListener() {
            @Override
            public void onQueryReturned(List<ConversationItem> conversationPage) {
                state.conversations.setHasReachedEnd(true);
                state.conversations.insertConversations(conversationPage, conversationPage.size() == 0);
            }
        }, totalConversations, 0);
    }

    /**
     * Updates the conversation that user may have closed. This assumes that this conversation is
     * still referenced by state as activeConversationItem, will throw exception if this isn't true.
     * This updates the lastOpen, secondaryText and totalNewMessages, so that when the user goes
     * back to the conversation list, he/she sees up-to-date data.
     */
    public void updateClosedConversation() {
        final MessagesList messages = state.getActiveConversationMessages();
        if (messages != null && !messages.isEmpty()) {
            final MonkeyItem lastItem = messages.get(messages.size() - 1);
            final long lastOpenValue = lastItem.getMessageTimestampOrder();
            final String lastText = DatabaseHandler.getSecondaryTextByMessageType(lastItem, false);

            final ConversationItem activeConv = state.activeConversationItem;
            if (activeConv == null) throw new IllegalStateException("Closed conversation is null");
            ConversationTransaction t = new ConversationTransaction() {
                @Override
                public void updateConversation(@NotNull MonkeyConversation conversation) {
                    ConversationItem conversationItem = (ConversationItem) conversation;
                    conversationItem.lastOpen = lastOpenValue;
                    conversationItem.setTotalNewMessage(0);
                    conversationItem.setSecondaryText(lastText);
                }
            };
            //Apply the transaction on the UI
            state.conversations.updateConversation(activeConv, t);
            //Apply same transaction on the DB
            DatabaseHandler.updateConversation(activeConv);
        }
    }

    private void syncNotifications(List<MOKNotification> notifications) {
        Iterator<MOKNotification> notificationIterator = notifications.iterator();
        while (notificationIterator.hasNext()) {
            MOKNotification not = notificationIterator.next();
            if (not.getProps().has("monkey_action")) {
                int type = not.getProps().get("monkey_action").getAsInt();
                try {
                    switch (type) {
                        case com.criptext.comunication.MessageTypes.MOKGroupNewMember:
                            onGroupNewMember(not.getReceiverId(), not.getProps().get("new_member").getAsString());
                            break;
                        case com.criptext.comunication.MessageTypes.MOKGroupRemoveMember:
                            onGroupRemovedMember(not.getReceiverId(), not.getSenderId());
                            break;
                        case com.criptext.comunication.MessageTypes.MOKGroupCreate:
                            onGroupAdded(not.getReceiverId(), not.getProps().get("members").getAsString(), not.getProps().getAsJsonObject("info"));

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onSyncComplete(@NonNull HttpSync.SyncData syncData) {
        syncStatus.cancelMessage();

        final String activeConversationId = state.getActiveConversationId();
        boolean activeConversationNeedsUpdate = false;
        final HashMap<String, List<MOKMessage>> newMessagesMap = syncData.getNewMessages();

        Iterator<String> iterator = syncData.getConversationsToUpdate().iterator();
        while (iterator.hasNext()) {
            String convId = iterator.next();

            if((activeConversationId != null) && activeConversationId.equals(convId)) {
                activeConversationNeedsUpdate = true;
            }
            //clear cached messages of updated conversations
            state.messagesMap.remove(convId);
        }

        final List<MOKMessage> activeConversationMessages = newMessagesMap.get(activeConversationId);
        //if active conversation has been updated, update the chat fragment's messages
        if(activeConversationNeedsUpdate && activeConversationMessages != null) {
            final MessagesList convMessages = state.getLoadedMessages(activeConversationId);
            asyncDBHandler.getMessagePage(new GetMessagePageTask.OnQueryReturnedListener() {
                @Override
                public void onQueryReturned(List<MessageItem> messagePage) {
                    convMessages.smoothlyAddNewItems(messagePage);
                }
            }, activeConversationId, activeConversationMessages.size(), 0);
        }

        if(!syncData.getUsers().isEmpty()){
            getUsersInfo(StringUtils.join(syncData.getUsers(), ","));
        }

        syncConversationsFragment();
        syncNotifications(syncData.getNotifications());
    }

    @Override
    public void onGroupAdded(final String groupid, final String members, final JsonObject info) {
        asyncDBHandler.getConversationById(new FindConversationTask.OnQueryReturnedListener() {
            @Override
            public void onQueryReturned(ConversationItem result) {
                if(result == null) {
                    result = new ConversationItem(groupid, info.has("name") ?
                        info.get("name").getAsString() : "Uknown Group", System.currentTimeMillis(),
                        "Write to this group", 0, true, members, info.has("avatar") ?
                        info.get("avatar").getAsString() : "", MonkeyConversation.ConversationStatus.empty.ordinal());
                    DatabaseHandler.syncConversation(result);
                    state.conversations.addNewConversation(result);
                }
            }
        }, groupid);
    }

    @Override
    public void onGroupNewMember(String groupid, final String new_member) {


    }

    @Override
    public void onGroupRemovedMember(String groupid, final String removed_member) {

        ConversationTransaction transaction = new ConversationTransaction() {
            @Override
            public void updateConversation(@NotNull MonkeyConversation conversation) {
                ConversationItem convToUpdate = (ConversationItem) conversation;
                convToUpdate.removeMember(removed_member);
            }
        };

        ConversationItem group = (ConversationItem) state.conversations.findConversationById(groupid);
        final DefaultGroupData groupData = state.groupData;
        if(group != null) {

            if(groupData!=null && groupData.getConversationId().equals(groupid)){
                groupData.removeMember(removed_member);
                groupData.setInfoList(state.myMonkeyID, state.myName);
            }
            state.conversations.updateConversation(group, transaction);
            DatabaseHandler.updateConversation(group);

        } else {
            if(!removed_member.equals(state.myMonkeyID)) {
                updateMissingConversation(groupid, transaction, false);
            }
        }
    }

    @Override
    public void onDeleteConversation(@NotNull String conversationId, @Nullable Exception e) {
        //not supported
    }

    @Override
    public void onGetGroupInfo(@NotNull MOKConversation mokConversation, @Nullable Exception e) {
        if(e==null){
            String convName = "Unknown group";
            String admins = "";
            JsonObject userInfo = mokConversation.getInfo();
            if(userInfo!=null && userInfo.has("name"))
                convName = userInfo.get("name").getAsString();
            ConversationItem conversationItem = new ConversationItem(mokConversation.getConversationId(),
                    convName, System.currentTimeMillis(), "Write to this group",
                    1, true, mokConversation.getMembers()!=null? TextUtils.join("," ,mokConversation.getMembers()):"",
                    mokConversation.getAvatarURL(), MonkeyConversation.ConversationStatus.empty.ordinal());
            if(userInfo!=null && userInfo.has("admin")) {
                admins = userInfo.get("admin").getAsString();
                conversationItem.setAdmins(admins);
            }

            asyncDBHandler.storeNewConversation(new StoreNewConversationTask.OnQueryReturnedListener() {
                @Override
                public void onQueryReturned(ConversationItem result) {
                state.conversations.addNewConversation(result); //TODO update silently?
                }
            }, conversationItem);

        }
    }

    @Override
    public void onGetUserInfo(@NotNull MOKUser mokUser, @Nullable Exception e) {

        if(e==null){
            String convName = "Unknown";
            JsonObject userInfo = mokUser.getInfo();
            if(userInfo!=null && userInfo.has("name"))
                convName = userInfo.get("name").getAsString();
            ConversationItem conversationItem = new ConversationItem(mokUser.getMonkeyId(),
                    convName, System.currentTimeMillis(), "Write to this contact",
                    1, false, "", mokUser.getAvatarURL(), MonkeyConversation.ConversationStatus.empty.ordinal());
            asyncDBHandler.storeNewConversation(new StoreNewConversationTask.OnQueryReturnedListener() {
                @Override
                public void onQueryReturned(ConversationItem result) {
                state.conversations.addNewConversation(result); //TODO update silently??
                }
            }, conversationItem);
        }

    }

    @Override
    public void onGetUsersInfo(@NotNull ArrayList<MOKUser> mokUsers, @Nullable Exception e) {
        final DefaultGroupData groupData = state.groupData;
        if(e==null && groupData!=null && monkeyChatFragment!=null) {
            ArrayList<MonkeyInfo> users = new ArrayList<>();
            for(MOKUser mokUser : mokUsers){
                users.add(new UserItem(mokUser));
            }
            groupData.setMembers(users);
            groupData.setAdmins(DatabaseHandler.getConversationById(monkeyChatFragment.getConversationId()).getAdmins());
            groupData.setInfoList(state.myMonkeyID, state.myName);
            monkeyChatFragment.reloadAllMessages();
            if(monkeyInfoFragment != null){
                monkeyInfoFragment.setInfo(groupData.getInfoList());
            }
        }

        asyncDBHandler.updateConversationsInfo(new UpdateConversationsInfoTask.OnQueryReturnedListener() {
            @Override
            public void onQueryReturned(ArrayList<ConversationItem> conversationsUpdated) {
                for (ConversationItem conversation : conversationsUpdated) {
                    state.conversations.updateConversation(conversation);
                }
            }
        }, mokUsers);
    }

    @Override
    public void onGetConversations(@NotNull ArrayList<MOKConversation> fetchedConversations, @Nullable Exception e) {
        //ALWAYS CALL SUPER FOR THIS CALLBACK!!
        super.onGetConversations(fetchedConversations, e);
        if(e!=null) {
            e.printStackTrace();
            return;
        }

        if(fetchedConversations.isEmpty())
          state.conversations.setHasReachedEnd(true);
        else
            Log.d("MainActvity", "getconversations. first is " + fetchedConversations.get(0).getConversationId());

        ArrayList<ConversationItem> monkeyConversations = new ArrayList<>();
        for(MOKConversation mokConversation : fetchedConversations){
            String convName = "Unknown";
            String admins = null;
            String secondaryText = "Write to this conversation";
            if(mokConversation.isGroup())
                secondaryText = "Write to this group";
            JsonObject convInfo = mokConversation.getInfo();
            if(convInfo!=null && convInfo.has("name"))
                convName = convInfo.get("name").getAsString();
            MessageItem lastItem = null;
            if(mokConversation.getLastMessage() != null)
            lastItem = DatabaseHandler.createMessage(mokConversation.getLastMessage(),
                    downloadDir.getAbsolutePath(), state.myMonkeyID);
            ConversationItem conversationItem = new ConversationItem(mokConversation.getConversationId(),
                    convName, mokConversation.getLastModified(),
                    DatabaseHandler.getSecondaryTextByMessageType(lastItem, mokConversation.isGroup()),
                    mokConversation.getUnread(),
                    mokConversation.isGroup(), mokConversation.getMembers()!=null? TextUtils.join("," ,mokConversation.getMembers()):"",
                    mokConversation.getAvatarURL(),
                    MonkeyConversation.ConversationStatus.receivedMessage.ordinal());
            if(convInfo!=null && convInfo.has("admin")) {
                admins = convInfo.get("admin").getAsString();
                conversationItem.setAdmins(admins);
            }
            if(mokConversation.getUnread()>0) {
                conversationItem.status = MonkeyConversation.ConversationStatus.receivedMessage.ordinal();
            }
            else if(mokConversation.getLastMessage()!=null){
                if(mokConversation.getLastMessage().isMyOwnMessage(state.myMonkeyID)){
                    conversationItem.status = MonkeyConversation.ConversationStatus.deliveredMessage.ordinal();
                }
                else{
                    conversationItem.status = MonkeyConversation.ConversationStatus.receivedMessage.ordinal();
                }
            }
            monkeyConversations.add(conversationItem);
        }
        final ConversationItem[] conversationItems = new ConversationItem[monkeyConversations.size()];
        for(int i = 0; i < monkeyConversations.size(); i++){
            conversationItems[i] = new ConversationItem(monkeyConversations.get(i));
        }
        if(conversationItems.length > 0)
            asyncDBHandler.storeConversationPage(new SaveModelTask.OnQueryReturnedListener() {
                @Override
                public void onQueryReturned(Model[] storedModels) {
                    ConversationItem[] storedConversations = (ConversationItem[]) storedModels;
                    state.conversations.addOldConversations(Arrays.asList(storedConversations),
                            conversationItems.length == 0);
                }
            }, conversationItems);
    }

    @Override
    public void onGetConversationMessages(@NotNull String conversationId, @NotNull ArrayList<MOKMessage> messages, @Nullable Exception e) {
        //ALWAYS CALL SUPER FOR THIS CALLBACK
        super.onGetConversationMessages(conversationId, messages, e);
        if(e!=null) {
            e.printStackTrace();
            return;
        }
        processOldMessages(conversationId, messages);
    }

    @Override
    public void onFileFailsUpload(MOKMessage message) {
        super.onFileFailsUpload(message);
        updateMessage(message.getMessage_id(), null, MonkeyItem.DeliveryStatus.error);
    }


    @Override
    public void onConversationOpenResponse(String senderId, Boolean isOnline, String lastSeen,
                                           String lastOpenMe, String members_online) {
        if(monkeyFragmentManager!=null && monkeyChatFragment!=null) {
            if(!monkeyChatFragment.getConversationId().equals(senderId)){
                return;
            }

            String subtitle = isOnline? "Online":"";

            long lastSeenValue = -1L;
            boolean isGroupConversation = senderId.contains("G:");
            final DefaultGroupData groupData = state.groupData;
            if(isGroupConversation){
                groupData.setMembersOnline(members_online);
                int membersOnline = members_online != null ? members_online.split(",").length : 0;
                if(membersOnline > 0) {
                    subtitle = membersOnline + " " + (membersOnline > 1 ? "members online" : "member online");
                }
                groupData.setInfoList(state.myMonkeyID, state.myName);
                if(monkeyInfoFragment != null){
                    monkeyInfoFragment.setInfo(groupData.getInfoList());
                }
            }
            else if(!isOnline){
                if(lastSeen.isEmpty())
                    lastSeenValue = 0L;
                else
                    lastSeenValue = Long.valueOf(lastSeen) * 1000;
                subtitle = "Last seen: "+Utils.Companion.getFormattedDate(lastSeenValue, this);
            }
            if(!subtitle.isEmpty()) {
                monkeyFragmentManager.setSubtitle(subtitle);
            }

            final ConversationItem activeConv = state.activeConversationItem;
            lastSeenValue = (activeConv.lastRead > lastSeenValue ? activeConv.lastRead : lastSeenValue);
            updateConversationLastRead(senderId, lastSeenValue);
        }
    }

    /**
     * Updates the lastRead value of a conversation. If the  conversation is active, the chatFragment
     * is updated to reflect the new value. if conversation does not exist
     * @param conversationId
     * @param newLastReadValue
     */
    public void updateConversationLastRead(String conversationId, long newLastReadValue) {
        if(conversationId.startsWith("G:"))
            return; //don't update group conversations

        ConversationTransaction transaction = TransactionCreator.fromContactOpenedConversation(newLastReadValue);
        ConversationItem openedConversation =
                (ConversationItem) state.conversations.findConversationById(conversationId);

        if(openedConversation != null) {
            if (newLastReadValue > openedConversation.lastRead) {
                //Only update conversation's last read if the new last read value is greater than the
                // current one.

                if(monkeyChatFragment!=null && monkeyChatFragment.getConversationId().equals(conversationId))
                    monkeyChatFragment.setLastRead(newLastReadValue);

                state.conversations.updateConversation(openedConversation, transaction);
                DatabaseHandler.updateConversation(openedConversation);
            }
        }
         else updateMissingConversation(conversationId, transaction, false);
    }

    @Override
    public void onDeleteReceived(String messageId, String senderId, String recipientId) {

    }

    @Override
    public void onContactOpenMyConversation(String monkeyId) {
        //Update the conversation status
        final long newLastReadValue = System.currentTimeMillis();
        updateConversationLastRead(monkeyId, newLastReadValue);

    }

    @Override
    public void onNotificationReceived(String messageId, String senderId, String recipientId, JsonObject params, String datetime) {
        int type = params.get("type").getAsInt();
        final DefaultGroupData groupData = state.groupData;
        if(recipientId.contains("G:")){
            if(monkeyChatFragment != null && monkeyChatFragment.getConversationId().equals(recipientId)){
                if(type == 21) {
                    groupData.addMemberTyping(senderId);
                    monkeyFragmentManager.setSubtitle(groupData.getMembersNameTyping());
                }else if (type ==20){
                    groupData.removeMemberTyping(senderId);
                    monkeyFragmentManager.setSubtitle(groupData.getMembersNameTyping());
                }
            }
        }else{
            if(monkeyChatFragment != null && monkeyChatFragment.getConversationId().equals(senderId)){
                if(type == 21) {
                    monkeyFragmentManager.setSubtitle("Typing...");
                }else if (type ==20){
                    monkeyFragmentManager.setSubtitle("Online");
                }
            }
        }
    }

    @NotNull
    @Override
    public Class<?> getServiceClassName() {
        //Provide the class of the service that we subclassed so that MKActivityDelegate can automatically
        //handle the binding and unbinding for us.
        return MyServiceClass.class;
    }

    @Override
    public  void onConnectionRefused(){
        setStatusBarState(Utils.ConnectionStatus.connected);
        Toast.makeText(this, "Login failed. Please check your Monkey Kit credentials", Toast.LENGTH_LONG).show();
    }

    /** CHAT ACTIVITY METHODS **/

    @Override
    public boolean isOnline() {
        //Use connectivity service to check if there's an active internet connection.
        ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager
                .getActiveNetworkInfo();
        return activeNetworkInfo != null;
    }

    @Override
    public void onFileUploadRequested(@NotNull MonkeyItem item) {
        if(item.getDeliveryStatus() == MonkeyItem.DeliveryStatus.error) {
            updateMessage(item.getMessageId(), null, MonkeyItem.DeliveryStatus.sending);
            if(monkeyChatFragment != null)
                monkeyChatFragment.rebindMonkeyItem(item);
        }
        boolean msgIsResending = resendFile(item.getMessageId());
        if(!msgIsResending){
            MessageItem message = (MessageItem) item;
            MOKMessage resendMessage = new MOKMessage(message.getMessageId(), state.myMonkeyID,
                    state.getActiveConversationId(), message.getMessageText(), "" + message.getMessageTimestamp(),
                    "" + message.getMessageType(), message.getJsonParams(), message.getJsonProps());
            resendFile(resendMessage, new PushMessage("You have a new message from the sample app"), true);
        }
    }

    @Override
    public void onFileDownloadRequested(@NotNull MonkeyItem item) {

        if(item.getDeliveryStatus() == MonkeyItem.DeliveryStatus.error) {
            //If the message failed to download previously, mark it as sending and rebind.
            //Rebinding will update the UI to a loading view and call this method again to start
            //the download
            MessageItem message = (MessageItem) item;
            message.setStatus(MonkeyItem.DeliveryStatus.sending.ordinal());
            if(monkeyChatFragment != null) {
                monkeyChatFragment.rebindMonkeyItem(message);
            }
        } else { //Not error status, download the file.
            final MessageItem messageItem = (MessageItem) item;
            downloadFile(messageItem.getMessageId(), messageItem.getFilePath(),
                    messageItem.getJsonProps(), messageItem.getSenderId(),
                    messageItem.getMessageTimestampOrder(), messageItem.getConversationId());
        }

    }

    @Override
    public void onLoadMoreMessages(final String conversationId, int currentMessageCount) {
        asyncDBHandler.getMessagePage(new GetMessagePageTask.OnQueryReturnedListener() {
            @Override
            public void onQueryReturned(List<MessageItem> messageItems) {
                if(messageItems.size() > 0){
                    final MessagesList messages = state.getLoadedMessages(conversationId);
                    messages.addOldMessages(new ArrayList<MonkeyItem>(messageItems), false);
                }
                else {
                    requestMessagesFromServer(conversationId);
                }
            }
        }, conversationId, MESS_PERPAGE, currentMessageCount);
    }

    @Override
    public void onDestroyWithPendingMessages(@NotNull ArrayList<MOKMessage> errorMessages) {
        DatabaseHandler.markMessagesAsError(errorMessages);
    }

    @NotNull
    @Override
    public MessagesList getInitialMessages(String conversationId) {
        return state.getLoadedMessages(conversationId);
    }

    @Override
    public GroupChat getGroupChat(@NotNull String conversationId, @NonNull String membersIds) {
        final DefaultGroupData _groupData = state.groupData;
        if(conversationId.contains("G:") && (_groupData == null || !_groupData.getConversationId().equals(conversationId))) {
            state.groupData = new DefaultGroupData(conversationId, membersIds, this);
            getUsersInfo(membersIds);
        }
        else if(_groupData!=null && !_groupData.getConversationId().equals(conversationId)){
            state.groupData = null;
        }
        return state.groupData;
    }

    @Override
    public void onBackPressed() {
        final Stack stack = state.mkFragmentStack;
        if (!stack.isEmpty() && stack.peek() == MonkeyFragmentManager.FragmentTypes.chat) { //user exit chat, clear active conversation
            updateClosedConversation();
            state.activeConversationItem = null;
        }

        super.onBackPressed();
    }

    @Override
    public void onStartChatFragment(@NonNull MonkeyChatFragment fragment, @NonNull String conversationId) {
        setOpenConversation(conversationId);
        monkeyChatFragment = fragment;
        monkeyChatFragment.setVoiceNotePlayer(voiceNotePlayer);
        monkeyChatFragment.setInputListener(initInputListener());
    }

    @Override
    public void onStopChatFragment(@NonNull String conversationId) {
        setOpenConversation(null);

        if(voiceNotePlayer != null)
            voiceNotePlayer.setupNotificationControl(new PlaybackNotification(R.mipmap.ic_launcher,
                    " Playing voice note"));
        //monkeyChatFragment.setInputListener(null);
        monkeyChatFragment = null;
    }

    /** CONVERSATION ACTIVITY METHODS **/
    @Override
    public ConversationsList onRequestConversations() {
        //TODO rewrite async
        final ConversationsList conversations = state.conversations;
        if(!conversations.isEmpty())
            return conversations;
        else {
            int firstBatchSize = 20;
            List<ConversationItem> firstConversations = DatabaseHandler.getConversations(firstBatchSize, 0);
            //INSER CONVERSATIONS SHOULD BE CALLED ONLY HERE!!!
            conversations.insertConversations(firstConversations, firstConversations.size() < firstBatchSize);
            return conversations;
        }
    }

    @Override
    public void onConversationClicked(final @NotNull MonkeyConversation conversation) {

        state.activeConversationItem = (ConversationItem) conversation;
        List<MonkeyItem> messages = state.getLoadedMessages(conversation.getConvId());
        if(!messages.isEmpty()){
            //Get initial messages from memory
            startChatWithMessages((ConversationItem) conversation);
        }
        else{
            //Get initial messages from DB
            asyncDBHandler.getMessagePage(new GetMessagePageTask.OnQueryReturnedListener() {
                @Override
                public void onQueryReturned(List<MessageItem> messageItems) {
                    state.addNewMessagesList(conversation.getConvId(), messageItems);
                    startChatWithMessages((ConversationItem) conversation);
                }
            }, conversation.getConvId(), MESS_PERPAGE, 0);
        }
    }

    @Override
    public void setConversationsFragment(@Nullable MonkeyConversationsFragment monkeyConversationsFragment) {
        state.conversations.setListUI(monkeyConversationsFragment);

    }

    @Override
    public void onLoadMoreConversations(int loadedConversations) {
        final int conversationsToLoad = 50;
        final int conversationsToRequest = 20;
        asyncDBHandler.getConversationPage(new GetConversationPageTask.OnQueryReturnedListener() {
            @Override
            public void onQueryReturned(List<ConversationItem> conversationPage) {
                if(conversationPage.isEmpty()) {
                    MonkeyConversation lastItem;
                    if((lastItem = state.conversations.getLastConversation()) != null) {
                        getConversationsFromServer(conversationsToRequest, lastItem.getDatetime() / 1000);
                    } else {
                        getConversationsFromServer(conversationsToRequest, 0);
                    }
                } else {
                    state.conversations.addOldConversations(conversationPage, false);
                }
            }
        }, conversationsToLoad, loadedConversations);
    }

    @Override
    public void onConversationDeleted(@NotNull MonkeyConversation conversation) {

        if (conversation.isGroup()) {
            removeGroupMember(conversation.getConvId(), state.myMonkeyID);
        } else {
            deleteConversation(conversation.getConvId());
        }
        DatabaseHandler.deleteConversation((ConversationItem) conversation);
        state.conversations.remove(conversation);

    }

    @Override
    public void onMessageRemoved(@NotNull MonkeyItem item, boolean unsent) {
        if(unsent){
            unsendMessage(item.getSenderId(), item.getConversationId(), item.getMessageId());
        }else{
            DatabaseHandler.deleteMessage((MessageItem)item);
        }
    }

    @Override
    public void onClickToolbar(@NotNull String monkeyID, @NotNull String name,
                               @NotNull String lastSeen, @NotNull String avatarURL){

    }

    @Override
    public void setInfoFragment(@Nullable MonkeyInfoFragment infoFragment) {
        monkeyInfoFragment = infoFragment;
    }

    @Override
    public void requestUsers() {

    }

    @Nullable
    @Override
    public ArrayList<MonkeyInfo> getInfo(String conversationId) {

        if(conversationId.contains("G:")){
            return state.groupData.getInfoList();
        }
        Iterator it = state.conversations.iterator();
        ArrayList<MonkeyInfo> infoList = new ArrayList<>();

        while(it.hasNext()){
            try {
                ConversationItem conversation = (ConversationItem) it.next();
                if (conversation.getConvId().contains("G:") && conversation.getGroupMembers().contains(state.myMonkeyID) &&
                        conversation.getGroupMembers().contains(conversationId)) {
                    infoList.add(conversation);
                }
            }catch (Exception e){

            }
        }

        Collections.sort(infoList, new Comparator<MonkeyInfo>() {
            @Override
            public int compare(MonkeyInfo lhs, MonkeyInfo rhs) {
                return lhs.getTitle().toLowerCase().compareTo(rhs.getTitle().toLowerCase());
            }
        });

        return infoList;

    }

    @Override
    public void onInfoItemClick(@NotNull MonkeyInfo infoItem) {
        if(infoItem.getInfoId().contains("G:")){
            state.activeConversationItem = (ConversationItem) infoItem;
            final ConversationItem conversation = (ConversationItem) infoItem;
            List<MonkeyItem> messages = state.messagesMap.get(conversation.getConvId());
            if(messages!=null && !messages.isEmpty()){
                startChatFromInfo(conversation);
            }else{
                //Get initial messages from DB
                asyncDBHandler.getMessagePage(new GetMessagePageTask.OnQueryReturnedListener() {
                    @Override
                    public void onQueryReturned(List<MessageItem> messageItems) {
                        state.addNewMessagesList(conversation.getConvId(), messageItems);
                        startChatFromInfo(conversation);
                    }
                }, conversation.getConvId(), MESS_PERPAGE, 0);
            }
        }else{
            ArrayList<MonkeyConversation> conversationsList = new ArrayList<>(state.conversations);
            ConversationItem conversationUser = null;
            for(MonkeyConversation conv : conversationsList) {
                if(conv.getConvId().equals(infoItem.getInfoId())) {
                    conversationUser = (ConversationItem)conv;
                    break;
                }
            }
            if(conversationUser == null){
                //return;
                ConversationItem conversationItem = new ConversationItem(infoItem.getInfoId(),
                        infoItem.getTitle(), System.currentTimeMillis(), "Write to this Conversation",
                        0, false, "", infoItem.getAvatarUrl(), MonkeyConversation.ConversationStatus.empty.ordinal());
                conversationsList.add(conversationItem);
                MessagesList newMessagesList = new MessagesList(infoItem.getInfoId());
                newMessagesList.setHasReachedEnd(true);
                state.messagesMap.put(infoItem.getInfoId(), newMessagesList);
                startChatFromInfo(conversationItem);
                DatabaseHandler.saveConversations(new ConversationItem[]{conversationItem});
                return;
            }
            final ConversationItem conversationUserCopy = conversationUser;
            List<MonkeyItem> messages = state.messagesMap.get(conversationUser.getConvId());
            if(messages!=null && !messages.isEmpty()){
                startChatFromInfo(conversationUser);
            }else{
                //Get initial messages from DB
                asyncDBHandler.getMessagePage(new GetMessagePageTask.OnQueryReturnedListener() {
                    @Override
                    public void onQueryReturned(List<MessageItem> messageItems) {
                        state.addNewMessagesList(conversationUserCopy.getConvId(), messageItems);
                        startChatFromInfo(conversationUserCopy);
                    }
                }, conversationUser.getConvId(), MESS_PERPAGE, 0);
            }


        }
    }

    public void startChatFromInfo(ConversationItem chat){
        MonkeyChatFragment fragment = new MonkeyChatFragment.Builder(chat.getConvId(), chat.getName())
                .setAvatarURL(chat.getAvatarFilePath())
                .setLastRead(chat.lastRead)
                .setMembersIds(chat.getGroupMembers())
                .build();

        state.activeConversationItem = chat;
        monkeyFragmentManager.setChatFragmentFromInfo(fragment, initInputListener(), voiceNotePlayer);
        monkeyInfoFragment = null;
    }

    @Override
    public void onExitGroup(@NotNull String conversationId) {

        removeGroupMember(conversationId, state.myMonkeyID);
        ConversationItem group = (ConversationItem) state.conversations.findConversationById(conversationId);
        int pos = state.conversations.getConversationPositionByTimestamp(group);
        state.conversations.removeConversationAt(pos);
        DatabaseHandler.deleteConversation(group);

        monkeyFragmentManager.popStack(2);
    }

    @Override
    public void onAddParticipant() {
        Log.d("INFO FRAGMENT", "Add Participant Clicked");
    }

    @Override
    public void deleteAllMessages(@NotNull String conversationId) {
        DatabaseHandler.deleteAll(conversationId);
        MessagesList messages = state.getLoadedMessages(conversationId);
        if(!messages.isEmpty()) messages.removeAllMessages();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(monkeyChatFragment != null)
            monkeyChatFragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void removeMember(@NotNull String monkeyId) {

   }
}
