package com.criptext.monkeychatandroid;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuInflater;
import android.widget.Toast;

import com.criptext.ClientData;
import com.criptext.comunication.MOKConversation;
import com.criptext.comunication.MOKMessage;
import com.criptext.comunication.MOKUser;
import com.criptext.comunication.MessageTypes;
import com.criptext.comunication.PushMessage;
import com.criptext.gcm.MonkeyRegistrationService;
import com.criptext.lib.MKDelegateActivity;
import com.criptext.monkeychatandroid.dialogs.NewGroupDialog;
import com.criptext.monkeychatandroid.gcm.SampleRegistrationService;
import com.criptext.monkeychatandroid.models.ConversationItem;
import com.criptext.monkeychatandroid.models.DatabaseHandler;
import com.criptext.monkeychatandroid.models.MessageItem;
import com.criptext.monkeychatandroid.models.MessageLoader;
import com.criptext.monkeykitui.MonkeyChatFragment;
import com.criptext.monkeykitui.MonkeyConversationsFragment;
import com.criptext.monkeykitui.conversation.ConversationsActivity;
import com.criptext.monkeykitui.conversation.MonkeyConversation;
import com.criptext.monkeykitui.conversation.holder.ConversationTransaction;
import com.criptext.monkeykitui.input.listeners.InputListener;
import com.criptext.monkeykitui.recycler.ChatActivity;
import com.criptext.monkeykitui.recycler.GroupChat;
import com.criptext.monkeykitui.recycler.MonkeyItem;
import com.criptext.monkeykitui.recycler.MonkeyItemTransaction;
import com.criptext.monkeykitui.recycler.audio.DefaultVoiceNotePlayer;
import com.criptext.monkeykitui.recycler.audio.VoiceNotePlayer;
import com.criptext.monkeykitui.util.MonkeyFragmentManager;
import com.criptext.monkeykitui.util.Utils;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.realm.Realm;

public class MainActivity extends MKDelegateActivity implements ChatActivity, ConversationsActivity{

    private static String DATA_FRAGMENT = "MainActivity.chatDataFragment";
    //Since this is the Chat activity, we need a RecyclerView and an adapter. Additionally we
    //will store the messages in our own list so that they can be accessed easily.
    MessageLoader messageLoader;
    MonkeyFragmentManager monkeyFragmentManager;

    MonkeyChatFragment monkeyChatFragment;

    MonkeyConversationsFragment monkeyConversationsFragment;
    HashMap<String, List<MonkeyItem>> messagesMap = new HashMap<>();
    Collection<MonkeyConversation> conversationsList = null;
    ChatDataFragment dataFragment;
    /**
     * This class is basically a media player for our voice notes. we pass this to MonkeyAdapter
     * so that it can handle all the media playback for us. However, we must initialize it in "onStart".
     * and release it in "onStop" method.
     */
    VoiceNotePlayer voiceNotePlayer;

    private SharedPreferences prefs;
    /**
     * Monkey ID of the current user. This is stored in Shared Preferences, so we use this
     * property to cache it so that we don't have to read from disk every time we need it.
     */
    private String myMonkeyID;
    /**
     * Monkey ID of the user that we are going to talk with.
     */
    private String myFriendID;
    /**
     * Object that uses the device's sensor to turn off the screen whenever the user has the device
     * close to his/her face, like when they are making a phone call.
     */
    private  SensorHandler sensorHandler;

    /**
     * Realm instance. We use this to retreive messages. it must be initialized on the 'onStart" method
     * and released on the "onStop".
     */
    private Realm realm;

    /**
     * This class is used to handle group methods.
     */
    private GroupData groupData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getRetainedData();
        //First, initialize the database and the constants from SharedPreferences.
        initRealm();
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        myMonkeyID = prefs.getString(MonkeyChat.MONKEY_ID, null);
        Log.d("MonkeyId", myMonkeyID);

        //Check play services. if available try to register with GCM so that we get Push notifications
        if(MonkeyRegistrationService.Companion.checkPlayServices(this))
                registerWithGCM();


        //Create a voice note player so that it can play voice note messages in the recycler view
        //when the user clicks on them
        if(voiceNotePlayer==null)
            voiceNotePlayer = new DefaultVoiceNotePlayer(this);
        //Add a sensor handler so the activity can turn off the screen when the sensors detect that
        //the user is too close to the phone and change the audio output device.
        sensorHandler = new SensorHandler(voiceNotePlayer, this);
        //finally load one page of messages.

        monkeyFragmentManager = new MonkeyFragmentManager(this);
        monkeyFragmentManager.setContentLayout(savedInstanceState);
        monkeyFragmentManager.setConversationsTitle(getResources().getString(R.string.app_name));
    }

    private void initRealm(){
        if(realm == null)
            realm = MonkeyChat.getInstance().getNewMonkeyRealm();
    }

    public void registerWithGCM(){
        Intent intent = new Intent(this, SampleRegistrationService.class);
        intent.putExtra(ClientData.Companion.getAPP_ID_KEY(), SensitiveData.APP_ID);
        intent.putExtra(ClientData.Companion.getAPP_KEY_KEY(), SensitiveData.APP_KEY);
        intent.putExtra(ClientData.Companion.getMONKEY_ID_KEY(), myMonkeyID);
        startService(intent);
        Log.d("MainActivity", "Registering with GCM");
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorHandler.onPause();
    }

    @Override
    protected void onStop() {

        super.onStop();
        realm.close();
        realm = null;
        voiceNotePlayer.releasePlayer();
        sensorHandler.onStop();
    }

    private void getRetainedData(){
        final ChatDataFragment retainedFragment =(ChatDataFragment) getSupportFragmentManager().findFragmentByTag(DATA_FRAGMENT);
        if(retainedFragment != null) {
            messageLoader = retainedFragment.messageLoader;
            messagesMap = retainedFragment.chatMap!=null?retainedFragment.chatMap:new HashMap<String, List<MonkeyItem>>();
            conversationsList = retainedFragment.conversationsList;
            groupData = retainedFragment.groupData;
            if (monkeyChatFragment != null) {
                monkeyChatFragment.setInputListener(initInputListener());
                if(voiceNotePlayer == null)
                    voiceNotePlayer = new DefaultVoiceNotePlayer(this);
                monkeyChatFragment.setVoiceNotePlayer(voiceNotePlayer);
            }
            dataFragment = retainedFragment;
        } else {
            dataFragment = new ChatDataFragment();
            messagesMap = new HashMap<>();
            getSupportFragmentManager().beginTransaction().add(dataFragment, DATA_FRAGMENT).commit();
        }

    }

    private void retainDataInFragment(){
        dataFragment.chatMap = this.messagesMap;
        dataFragment.conversationsList = this.conversationsList;
        dataFragment.messageLoader = this.messageLoader;
        dataFragment.groupData = this.groupData;
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorHandler.onDestroy();
        retainDataInFragment();
    }

    @Override
    protected void onRestart(){
        super.onRestart();
        initRealm();
    }
    @Override
    protected void onStart(){
        super.onStart();
        voiceNotePlayer.initPlayer();
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_conversations, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.action_newgroup:
            {
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.addToBackStack(null);

                // Create and show the dialog.
                NewGroupDialog newFragment = NewGroupDialog.newInstance();
                newFragment.show(ft, "dialog");

            } break;
            case R.id.action_deleteall:
                DatabaseHandler.deleteAll(realm);
                if(monkeyChatFragment != null) monkeyChatFragment.clearMessages();
                break;
            default:
                break;
        }
        return true;
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
            public void onNewItem(@NotNull MonkeyItem item) {

                JsonObject params = new JsonObject();
                MOKMessage mokMessage;

                //Store the message in the DB and send it via MonkeyKit
                switch (MonkeyItem.MonkeyItemType.values()[item.getMessageType()]) {
                    case audio:
                        params = new JsonObject();
                        params.addProperty("length",""+item.getAudioDuration()/1000);

                        mokMessage = persistFileMessageAndSend(item.getFilePath(), myMonkeyID, myFriendID,
                                MessageTypes.FileTypes.Audio, params, new PushMessage("You have a new message from the sample app"), true);
                        break;
                    case photo:
                        mokMessage = persistFileMessageAndSend(item.getFilePath(), myMonkeyID, myFriendID,
                                MessageTypes.FileTypes.Photo, new JsonObject(), new PushMessage("You have a new message from the sample app"), true);
                        break;
                    default:
                        mokMessage = persistMessageAndSend(item.getMessageText(), myMonkeyID,
                                myFriendID, params, new PushMessage("You have a new message from the sample app"), true);
                        break;
                }

                //Now that the message was sent, create a MessageItem using the MOKMessage that MonkeyKit
                //created. This MessageItem will be added to MonkeyAdapter so that it can be shown in
                //the screen.
                MessageItem newItem = new MessageItem(myMonkeyID, myFriendID, mokMessage.getMessage_id(),
                        item.getMessageText(), item.getMessageTimestamp(), item.getMessageTimestampOrder(), item.isIncomingMessage(),
                        MonkeyItem.MonkeyItemType.values()[item.getMessageType()]);
                newItem.setParams(params);

                switch (MonkeyItem.MonkeyItemType.values()[item.getMessageType()]) {
                    case audio:
                        newItem.setDuration(item.getAudioDuration()/1000);
                        newItem.setMessageContent(item.getFilePath());
                        break;
                    case photo:
                        newItem.setMessageContent(item.getFilePath());
                        break;
                }
                if(monkeyChatFragment != null)
                    monkeyChatFragment.smoothlyAddNewItem(newItem); // Add to recyclerView
            }
        };
    }

    /**
     * Add messages retrieved from DB to the messages list
     * @param oldMessages list of messages
     * @param hasReachedEnd boolean if messages has reached end
     */
    public void addOldMessages(ArrayList<MonkeyItem> oldMessages, boolean hasReachedEnd){
        if(oldMessages != null && monkeyChatFragment != null)
            monkeyChatFragment.addOldMessages(oldMessages, hasReachedEnd);
        if(monkeyChatFragment != null)
            monkeyChatFragment.addOldMessages(new ArrayList<MonkeyItem>(), hasReachedEnd);
    }

    /**
     * Ask old messages from server
     * @param conversationId id of the conversation messages required
     */
    public void addOldMessagesFromServer(String conversationId){
        if(monkeyChatFragment!=null) {
            String firstTimestamp = "0";
            if(monkeyChatFragment.getFirstMessage()!=null)
                firstTimestamp = ""+monkeyChatFragment.getFirstMessage().getMessageTimestamp();
            else if(messagesMap.get(conversationId)!=null && messagesMap.get(conversationId).size()>0)
                firstTimestamp = ""+new ArrayList<MonkeyItem>(messagesMap.get(conversationId)).get(0).getMessageTimestamp();
            getConversationMessages(conversationId, 30, firstTimestamp);
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
     * @param id message id
     * @param newStatus new status to change
     */
    private void updateMessage(String id, MonkeyItem.DeliveryStatus newStatus) {
        //DatabaseHandler.updateMessageOutgoingStatus(realm, id, newStatus);
        if (monkeyChatFragment != null) {
            MessageItem monkeyItem = (MessageItem) monkeyChatFragment.findMonkeyItemById(id);
            if (monkeyItem != null) {
                monkeyItem.setStatus(newStatus);
                monkeyChatFragment.rebindMonkeyItem(monkeyItem);
            }
        }
    }

    private void updateMessage(String id, long dateorder, String conversationId, final MonkeyItem.DeliveryStatus newStatus) {
        MonkeyItemTransaction transaction = new MonkeyItemTransaction() {
                @Override
                public MonkeyItem invoke(MonkeyItem monkeyItem) {
                    MessageItem message = (MessageItem) monkeyItem;
                    message.setStatus(newStatus);
                    return message;
                }
            };

        if (monkeyChatFragment != null && monkeyChatFragment.getConversationId().equals(conversationId)) {
            //If conversation is currently open
            monkeyChatFragment.updateMessage(id, dateorder, transaction);
        } else {
            //Look for the conversation
            List<MonkeyItem> conversationMessages = messagesMap.get(conversationId);
            if(conversationMessages != null){
                int position = MonkeyItem.Companion.findItemPositionInList(id, dateorder, conversationMessages);
                if(position > -1){
                    MonkeyItem updateItem = conversationMessages.remove(position);
                    conversationMessages.add(position, transaction.invoke(updateItem));
                }
            }
        }
    }
    /**
     * Update a conversation according to the params received.
     * @param conversationId conversation id to change
     * @param secondaryText secondary text
     * @param status new status
     * @param unread number of messages without read
     * @param dateTime new date time of the conversation
     */
    private void updateConversation(final String conversationId, final String secondaryText, final MonkeyConversation.ConversationStatus status,
                                    final int unread, final long dateTime){
        if(monkeyConversationsFragment!=null) {
            final ConversationItem conversationItem = (ConversationItem) monkeyConversationsFragment.findConversationById(conversationId);
            if(conversationItem!=null) {
                monkeyConversationsFragment.updateConversation(conversationItem, new ConversationTransaction() {
                    @Override
                    public void updateConversation(@NotNull MonkeyConversation conversation) {
                        ConversationItem newConversationItem = (ConversationItem)conversation;
                        newConversationItem.setSecondaryText(secondaryText!=null?secondaryText:conversationItem.getSecondaryText());
                        if(status != MonkeyConversation.ConversationStatus.empty && (conversationItem.getStatus() != MonkeyConversation.ConversationStatus.receivedMessage.ordinal()
                                || status != MonkeyConversation.ConversationStatus.sentMessageRead)){
                            newConversationItem.setStatus(status.ordinal());
                            newConversationItem.setTotalNewMessage(unread == 0 ? 0 : conversationItem.getTotalNewMessages()+unread);
                        }
                        if(status == MonkeyConversation.ConversationStatus.empty ||
                                (monkeyChatFragment!=null && monkeyChatFragment.getConversationId().equals(conversationId))){
                            newConversationItem.setTotalNewMessage(0);
                        }
                        newConversationItem.setDatetime(dateTime>-1?dateTime:conversationItem.getDatetime());
                    }
                });
            }
        }
    }

    private void updateConversationBadge(String conversationId, int unread){
        if(monkeyConversationsFragment!=null) {
            final ConversationItem conversationItem = (ConversationItem) monkeyConversationsFragment.findConversationById(conversationId);
            if(conversationItem!=null) {
                conversationItem.setTotalNewMessage(unread);
                monkeyConversationsFragment.updateConversation(conversationItem);
            }
        }
    }

    /**
     * Search a conversation by its message id and update its status.
     * @param messageId message id
     * @param read boolean if the message is read or not
     */
    private void updateConversationByMessage(String messageId, boolean read){
        if (monkeyChatFragment != null) {
            MessageItem monkeyItem = (MessageItem) monkeyChatFragment.findMonkeyItemById(messageId);
            if (monkeyItem != null) {
                updateConversation(monkeyItem.getRecieverSessionId(), getSecondaryTextByMessageType(monkeyItem),
                        read?MonkeyConversation.ConversationStatus.sentMessageRead:MonkeyConversation.ConversationStatus.deliveredMessage,
                        0, monkeyItem.getMessageTimestampOrder());
            }
        }
    }

    private void createNewConversation(){

    }

    /**
     * Creates a new MonkeyChatFragment and adds it to the activity.
     * @param conversationId unique identifier of the conversation of the fragment
     * @param membersIds String separate by coma of the member ids
     * @param initialMessages a list of the first messages to draw in the chat
     * @param hasReachedEnd true of the initial messages are the only existing messages of the chat
     */
    public void startChatWithMessages(String conversationId, String membersIds, ArrayList<MonkeyItem> initialMessages,
                                      boolean hasReachedEnd){
        messagesMap.put(conversationId, initialMessages);
        MonkeyChatFragment fragment = MonkeyChatFragment.Companion.newInstance(conversationId, membersIds, hasReachedEnd);
        conversationsList = monkeyFragmentManager.setChatFragment(fragment, initInputListener(), voiceNotePlayer);
    }
    /**
     * Updates a sent message and updates de UI so that the user can see that it has been
     * successfully delivered
     * @param oldId The old Id of the message.
     */
    private void markMessageAsDelivered(String oldId, boolean read){
        updateMessage(oldId, read?MonkeyItem.DeliveryStatus.read:MonkeyItem.DeliveryStatus.delivered);
    }

    /**
     * adds a message to the adapter so that it can be displayed in the RecyclerView.
     * @param message
     */
    private void processNewMessage(MOKMessage message){
        MessageItem newItem = DatabaseHandler.createMessage(message, this, myMonkeyID, !message.isMyOwnMessage(myMonkeyID));
        if(monkeyChatFragment != null && messageLoader!=null
                && monkeyChatFragment.getConversationId().equals(message.getConversationID())) {
            messageLoader.countNewMessage(message.getConversationID());
            monkeyChatFragment.smoothlyAddNewItem(newItem);
        }
        else if(messagesMap!=null && messagesMap.get(message.getConversationID())!=null){
            messagesMap.get(message.getConversationID()).add(newItem);
        }
        else{
            ArrayList<MonkeyItem> monkeyItemArrayList = new ArrayList<>();
            monkeyItemArrayList.add(newItem);
            messagesMap.put(message.getConversationID(), monkeyItemArrayList);
        }
        //Validate if conversation does not exists
        if(message.getConversationID()!=null && monkeyConversationsFragment.findConversationById(message.getConversationID())==null){
            getConversationInfo(message.getConversationID());
        }
    }

    /**
     * adds new messages to the adapter so that it can be displayed in the RecyclerView.
     * @param messages
     */
    private void processNewMessages(ArrayList<MOKMessage> messages){

        HashMap<String, Collection<MonkeyItem>> newConversationMessages = new HashMap<>();
        for(MOKMessage message: messages){
            String conversationId = message.getConversationID();
            if(messageLoader!=null)
                messageLoader.countNewMessage(conversationId);
            if(newConversationMessages.get(conversationId)==null){
                newConversationMessages.put(conversationId, new ArrayList<MonkeyItem>());
            }
            newConversationMessages.get(conversationId).
                    add(DatabaseHandler.createMessage(message, this, myMonkeyID, !message.isMyOwnMessage(myMonkeyID)));
        }

        Iterator it = newConversationMessages.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            String conversationId = (String)pair.getKey();
            ArrayList<MonkeyItem> arrayList = (ArrayList<MonkeyItem>) pair.getValue();
            if(monkeyChatFragment!=null && monkeyChatFragment.getConversationId().equals(conversationId)){
                monkeyChatFragment.smoothlyAddNewItems(arrayList);
            }
            else if(messagesMap!=null && messagesMap.get(conversationId)!=null){
                messagesMap.get(conversationId).addAll(arrayList);
            }
            else{
                messagesMap.put(conversationId, arrayList);
            }
            //Validate if conversation does not exists
            if(monkeyConversationsFragment.findConversationById(conversationId)==null){
                getConversationInfo(conversationId);
            }
            it.remove();
        }
    }

    /**
     * adds old messages to the adapter so that it can be displayed in the RecyclerView.
     * @param messages
     */
    private void processOldMessages(ArrayList<MOKMessage> messages){
        if(messageLoader==null)
            return;
        ArrayList<MessageItem> messageItems = new ArrayList<>();
        for(MOKMessage message: messages){
            messageLoader.countNewMessage(message.getConversationID());
            messageItems.add(DatabaseHandler.createMessage(message, this, myMonkeyID, !message.isMyOwnMessage(myMonkeyID)));
        }
        Collections.sort(messageItems);
        if(monkeyChatFragment != null) {
            monkeyChatFragment.addOldMessages(new ArrayList<MonkeyItem>(messageItems), messages.size() == 0);
        }
    }

    private String getSecondaryTextByMessageType(MonkeyItem monkeyItem){
        switch (MonkeyItem.MonkeyItemType.values()[monkeyItem.getMessageType()]) {
            case audio:
                return "Audio";
            case photo:
                return "Photo";
            default:
                return monkeyItem.getMessageText();
        }
    }

    private String getSecondaryTextByMOkMessage(MOKMessage message){
        if (message.getProps()!=null && message.getProps().has("file_type")) {
            if(Integer.parseInt(message.getProps().get("file_type").getAsString())==1)
                return "Audio";
            else if(Integer.parseInt(message.getProps().get("file_type").getAsString())==3)
                return "Photo";
            else
                return message.getMsg();
        }
        else
            return message.getMsg();
    }

    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK) {
            return;
        }

        initRealm();
        //Since our chat fragment uses different activities to take and edit photos, we must forward
        // the onActivityResult event to it so that it can react to the results of take photo, choose
        //photo and edit photo.
        if(monkeyChatFragment != null)
            monkeyChatFragment.onActivityResult(requestCode, resultCode, data);
    }

    /******
     * These are the methods that MonkeyKit calls to inform us about new events.
     */

    @Override
    public void storeSentMessage(final MOKMessage message) {
        /*
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        DatabaseHandler.storeSendingMessage(realm, DatabaseHandler.createMessage(message, this, prefs.getString("sessionid", ""), false),
            new Realm.Transaction.OnSuccess() {
                @Override
                public void onSuccess() {
                }
            }, new Realm.Transaction.OnError() {
                @Override
                public void onError(Throwable error) {
                    error.printStackTrace();
                }
            });
            */
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
                                       String conversationId, boolean success) {
        //TODO use better search algorithm
        super.onFileDownloadFinished(fileMessageId, fileMessageTimestamp, conversationId, success);
        updateMessage(fileMessageId,
                success ? MonkeyItem.DeliveryStatus.delivered : MonkeyItem.DeliveryStatus.error);
    }

    @Override
    public void onAcknowledgeRecieved(@NotNull String senderId, @NotNull String recipientId,
                          @NotNull String newId, @NotNull String oldId, boolean read, int messageType) {
        super.onAcknowledgeRecieved(senderId, recipientId, newId, oldId, read, messageType);
        markMessageAsDelivered(oldId, read);
        updateConversationByMessage(oldId, read);
    }

    @Override
    public void onCreateGroup(String groupMembers, String groupName, String groupID, Exception e) {
        if(e==null){
            ConversationItem conversationItem = new ConversationItem(groupID,
                    groupName, System.currentTimeMillis(), "Write to this group",
                    0, true, groupMembers, "", MonkeyConversation.ConversationStatus.empty.ordinal());
            if(monkeyConversationsFragment!=null) {
                monkeyConversationsFragment.addNewConversation(conversationItem, true);
            }
        }
    }

    @Override
    public void onAddGroupMember(@Nullable String groupID, @Nullable String members, @Nullable Exception e) {

    }

    @Override
    public void onRemoveGroupMember(@Nullable String groupID, @Nullable String members, @Nullable Exception e) {

    }

    @Override
    public void onUpdateUserData(@NotNull String monkeyId, @Nullable Exception e) {

    }

    @Override
    public void onUpdateGroupData(@NotNull String groupId, @Nullable Exception e) {

    }

    @Override
    public void onMessageRecieved(@NonNull MOKMessage message) {
        processNewMessage(message);
        boolean isMyOwnMsg = message.getSid().equals(myMonkeyID);
        updateConversation(isMyOwnMsg?message.getRid():message.getConversationID(), getSecondaryTextByMOkMessage(message),
                isMyOwnMsg? MonkeyConversation.ConversationStatus.deliveredMessage:
                        MonkeyConversation.ConversationStatus.receivedMessage, isMyOwnMsg? 0 : 1, message.getDatetimeorder());
    }

    @Override
    public void onMessageBatchReady(ArrayList<MOKMessage> messages) {
        setStatusBarState(Utils.ConnectionStatus.connected);
        if(messages.size()>0){
            processNewMessages(messages);
            MOKMessage message = messages.get(messages.size()-1);
            boolean isMyOwnMsg = message.getSid().equals(myMonkeyID);
            updateConversation(isMyOwnMsg?message.getRid():message.getConversationID(), getSecondaryTextByMOkMessage(message),
                    isMyOwnMsg? MonkeyConversation.ConversationStatus.deliveredMessage:
                            MonkeyConversation.ConversationStatus.receivedMessage, isMyOwnMsg? 0 : 1, message.getDatetimeorder());
        }
    }

    @Override
    public void onMessageFailDecrypt(MOKMessage message) {

    }

    @Override
    public void onGroupAdded(String groupid, String members, JsonObject info) {

    }

    @Override
    public void onGroupNewMember(String groupid, String new_member) {

    }

    @Override
    public void onGroupRemovedMember(String groupid, String removed_member) {

    }

    @Override
    public void onGroupsRecover(String groupids) {

    }


    @Override
    public void onDeleteConversation(@NotNull String conversationId, @Nullable Exception e) {

    }

    @Override
    public void onGetGroupInfo(@NotNull MOKConversation mokConversation, @Nullable Exception e) {
        if(e==null){
            String convName = "Unknown group";
            JsonObject userInfo = mokConversation.getInfo();
            if(userInfo!=null && userInfo.has("name"))
                convName = userInfo.get("name").getAsString();
            ConversationItem conversationItem = new ConversationItem(mokConversation.getConversationId(),
                    convName, System.currentTimeMillis(), "Write to this group",
                    1, true, mokConversation.getMembers()!=null? TextUtils.join("," ,mokConversation.getMembers()):"",
                    mokConversation.getAvatarURL(), MonkeyConversation.ConversationStatus.empty.ordinal());
            if(messagesMap!=null && messagesMap.get(mokConversation.getConversationId())!=null){
                MonkeyItem monkeyItem = new LinkedList<>(messagesMap.get(mokConversation.getConversationId())).getLast();
                boolean isMyOwnMessage = monkeyItem.getContactSessionId().equals(myMonkeyID);
                conversationItem.setSecondaryText(getSecondaryTextByMessageType(monkeyItem));
                conversationItem.setDatetime(monkeyItem.getMessageTimestampOrder());
                conversationItem.setTotalNewMessage(isMyOwnMessage? 0 : 1);
                conversationItem.setStatus(isMyOwnMessage? MonkeyConversation.ConversationStatus.receivedMessage.ordinal():
                        MonkeyConversation.ConversationStatus.deliveredMessage.ordinal());
                messagesMap.get(mokConversation.getConversationId()).clear();
            }
            if(monkeyConversationsFragment!=null) {
                monkeyConversationsFragment.addNewConversation(conversationItem, true);
            }
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
            if(messagesMap!=null && messagesMap.get(mokUser.getMonkeyId())!=null){
                MonkeyItem monkeyItem = new LinkedList<>(messagesMap.get(mokUser.getMonkeyId())).getLast();
                conversationItem.setSecondaryText(getSecondaryTextByMessageType(monkeyItem));
                conversationItem.setDatetime(monkeyItem.getMessageTimestampOrder());
                conversationItem.setTotalNewMessage(mokUser.getMonkeyId().equals(myMonkeyID)? 0 : 1);
                conversationItem.setStatus(mokUser.getMonkeyId().equals(myMonkeyID)?
                        MonkeyConversation.ConversationStatus.deliveredMessage.ordinal():
                        MonkeyConversation.ConversationStatus.receivedMessage.ordinal());
                messagesMap.get(mokUser.getMonkeyId()).clear();
            }
            if(monkeyConversationsFragment!=null) {
                monkeyConversationsFragment.addNewConversation(conversationItem, true);
            }
        }

    }

    @Override
    public void onGetUsersInfo(@NotNull ArrayList<MOKUser> mokUsers, @Nullable Exception e) {
        if(e==null && groupData!=null && monkeyChatFragment!=null) {
            groupData.setMembers(monkeyChatFragment.getConversationId(), mokUsers);
            monkeyChatFragment.reloadAllMessages();
        }
    }

    @Override
    public void onGetConversations(@NotNull ArrayList<MOKConversation> conversations, @Nullable Exception e) {

        if(e!=null) {
            e.printStackTrace();
            return;
        }

        ArrayList<MonkeyConversation> conversationItems = new ArrayList<>();
        for(MOKConversation mokConversation : conversations){
            String convName = "Unknown";
            String secondaryText = "Write to this conversation";
            if(mokConversation.isGroup())
                secondaryText = "Write to this group";
            JsonObject convInfo = mokConversation.getInfo();
            if(convInfo!=null && convInfo.has("name"))
                convName = convInfo.get("name").getAsString();
            ConversationItem conversationItem = new ConversationItem(mokConversation.getConversationId(),
                    convName, mokConversation.getLastModified(),
                    mokConversation.getLastMessage()!=null?getSecondaryTextByMOkMessage(mokConversation.getLastMessage()):secondaryText,
                    mokConversation.getUnread(), mokConversation.isGroup(), mokConversation.getMembers()!=null? TextUtils.join("," ,mokConversation.getMembers()):"",
                    mokConversation.getAvatarURL(), MonkeyConversation.ConversationStatus.receivedMessage.ordinal());

            if(mokConversation.getUnread()>0) {
                conversationItem.status = MonkeyConversation.ConversationStatus.receivedMessage.ordinal();
            }
            else if(mokConversation.getLastMessage()!=null){
//                ArrayList<MonkeyItem> monkeyItemArrayList = new ArrayList<>();
//                monkeyItemArrayList.add(DatabaseHandler.createMessage(mokConversation.getLastMessage(), this,
//                        myMonkeyID, !mokConversation.getLastMessage().isMyOwnMessage(myMonkeyID)));
//                messagesMap.put(mokConversation.getConversationId(), monkeyItemArrayList);
                if(mokConversation.getLastMessage().isMyOwnMessage(myMonkeyID)){
                    conversationItem.status = MonkeyConversation.ConversationStatus.deliveredMessage.ordinal();
                }
                else{
                    conversationItem.status = MonkeyConversation.ConversationStatus.receivedMessage.ordinal();
                }
            }

            conversationItems.add(conversationItem);
        }
        monkeyConversationsFragment.addOldConversations(conversationItems, conversationItems.size()==0);
    }

    @Override
    public void onGetConversationMessages(@NotNull ArrayList<MOKMessage> messages, @Nullable Exception e) {

        if(e!=null) {
            e.printStackTrace();
            return;
        }

        processOldMessages(messages);
    }

    @Override
    public void onFileFailsUpload(MOKMessage message) {
        super.onFileFailsUpload(message);
        updateMessage(message.getMessage_id(), MonkeyItem.DeliveryStatus.error);
    }


    @Override
    public void onConversationOpenResponse(String senderId, Boolean isOnline, String lastSeen, String lastOpenMe, String members_online) {

    }

    @Override
    public void onDeleteRecieved(String messageId, String senderId, String recipientId, String datetime) {
        //Server requested to delete a message, we could implement it here, but for now we wont
    }

    @Override
    public void onContactOpenMyConversation(String monkeyId) {
        //Update the conversation status
        updateConversation(monkeyId, null, MonkeyConversation.ConversationStatus.sentMessageRead, 0, -1);
        //Set all messages outgoing as read
        ArrayList<MonkeyItem> monkeyItemArrayList = new ArrayList<>();
        //Validate if the conversation opened is the actual conversation
        if(monkeyChatFragment!=null && monkeyChatFragment.getConversationId().equals(monkeyId)) {
            monkeyItemArrayList = new ArrayList<>(monkeyChatFragment.getAllMessages());
        }
        else if(messagesMap!=null && messagesMap.get(monkeyId)!=null){
            monkeyItemArrayList = new ArrayList<>(messagesMap.get(monkeyId));
        }
        for(MonkeyItem item: monkeyItemArrayList){
            MessageItem messageItem = (MessageItem)item;
            if(!messageItem.isIncomingMessage()){
                messageItem.setStatus(MonkeyItem.DeliveryStatus.read);
            }
        }
        if(monkeyChatFragment!=null && monkeyChatFragment.getConversationId().equals(monkeyId)) {
            monkeyChatFragment.reloadAllMessages();
        }
    }

    @Override
    public void onNotificationReceived(String messageId, String senderId, String recipientId, JsonObject params, String datetime) {   }

    @NotNull
    @Override
    public Class<?> getServiceClassName() {
        //Provide the class of the service that we subclassed so that MKActivityDelegate can automatically
        //handle the binding and unbinding for us.
        return MyServiceClass.class;
    }

    @Override
    public void onBoundToService() {

    }

    @Override
    public  void onConnectionRefused(){
        setStatusBarState(Utils.ConnectionStatus.connected);
        Log.d("MainActivity", "Connection Refused");
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
            updateMessage(item.getMessageId(), MonkeyItem.DeliveryStatus.sending);
            if(monkeyChatFragment != null)
                monkeyChatFragment.rebindMonkeyItem(item);
        }
        boolean msgIsResending = resendFile(item.getMessageId());
        if(!msgIsResending){
            MessageItem message = (MessageItem) item;
            MOKMessage resendMessage = new MOKMessage(message.getMessageId(), myMonkeyID, myFriendID,
                    message.getMessageText(), "" + message.getMessageTimestamp(), "" + message.getMessageType(),
                    message.getParams(), message.getProps());
            resendFile(resendMessage, new PushMessage("You have a new message from the sample app"), true);
        }
    }

    @Override
    public void onFileDownloadRequested(@NotNull MonkeyItem item) {

        if(item.getDeliveryStatus() == MonkeyItem.DeliveryStatus.error) {
            //If the message failed to download previously, mark it as sending and rebind.
            //Rebinding will update the UI to a loading view and call this method again to start
            //the download
            updateMessage(item.getMessageId(), MonkeyItem.DeliveryStatus.sending);
            if(monkeyChatFragment != null)
                monkeyChatFragment.rebindMonkeyItem(item);
        } else { //Not error status, download the file.
            final MessageItem messageItem = (MessageItem) item;
            downloadFile(messageItem.getMessageId(), messageItem.getFilePath(),
                    messageItem.getProps(), messageItem.getContactSessionId(),
                    messageItem.getMessageTimestampOrder(), getActiveConversation());
        }

    }

    @Override
    public void onLoadMoreData(String conversationId) {
        addOldMessagesFromServer(conversationId);
    }

    @Override
    public void onDestroyWithPendingMessages(@NotNull ArrayList<MOKMessage> errorMessages) {
        Realm newRealm = MonkeyChat.getInstance().getNewMonkeyRealm();
        DatabaseHandler.markMessagesAsError(newRealm, errorMessages);
        newRealm.close();
    }

    @Override
    public void setChatFragment(@Nullable MonkeyChatFragment chatFragment) {
        monkeyChatFragment = chatFragment;
    }

    @NotNull
    @Override
    public List<MonkeyItem> getInitialMessages(String conversationId) {
        myFriendID = conversationId;
        return messagesMap.get(conversationId);
    }

    @Override
    public GroupChat getGroupChat(@NotNull String conversationId, @NonNull String membersIds) {
        if(conversationId.contains("G:") && (groupData == null || !groupData.getConversationId().equals(conversationId))) {
            groupData = new GroupData(conversationId, membersIds, getService());
        }
        else if(groupData!=null && !groupData.getConversationId().equals(conversationId)){
            groupData = null;
        }
        return groupData;
    }

    @Override
    public void retainMessages(@NotNull String conversationId, @NotNull List<? extends MonkeyItem> messages) {
        if(messagesMap!=null)
            messagesMap.put(conversationId, (List<MonkeyItem>) messages);
    }

    @Override
    public void retainConversations(@NotNull List<? extends MonkeyConversation> conversations) {
        conversationsList = (List<MonkeyConversation>)conversations;
    }

    @Override
    public void onStartChatFragment(@NonNull String conversationId) {
        setActiveConversation(conversationId);
    }

    @Override
    public void onStopChatFragment(@NonNull String conversationId) {
        setActiveConversation(null);
    }

    /** CONVERSATION ACTIVITY METHODS **/
    @Override
    public void requestConversations() {

        ArrayList<MonkeyConversation> conversations;
        if(conversationsList!=null) {
            conversations = new ArrayList<>(conversationsList);
        }
        else {
            conversations = new ArrayList<>();
            final long datetime = System.currentTimeMillis();
            conversations.add(new ConversationItem(myMonkeyID, "Mirror Chat", datetime, "Tap to talk to yourself",
                    0, false, "", null, MonkeyConversation.ConversationStatus.empty.ordinal()));
        }
        monkeyConversationsFragment.insertConversations(conversations, false);
    }

    @Override
    public void onConversationClicked(@NotNull MonkeyConversation conversation) {

        //Initialize the messageLoader
        messageLoader = new MessageLoader(conversation.getId(), conversation.getGroupMembers(), myMonkeyID, this);
        List<MonkeyItem> messages = messagesMap.get(conversation.getId());
        if(messages!=null && !messages.isEmpty()){
            startChatWithMessages(conversation.getId(), conversation.getGroupMembers(),
                    new ArrayList<MonkeyItem>(messages), false);
        }
        else{
            messagesMap.put(conversation.getId(), new ArrayList<MonkeyItem>());
            startChatWithMessages(conversation.getId(), conversation.getGroupMembers(),
                    new ArrayList<MonkeyItem>(messagesMap.get(conversation.getId())), false);
        }

        //updateConversationBadge(conversation.getId(), 0);

        if(getSupportActionBar()!=null)
            getSupportActionBar().setTitle(conversation.getName());
    }

    @Override
    public void setConversationsFragment(@Nullable MonkeyConversationsFragment conversationsFragment) {
        monkeyConversationsFragment = conversationsFragment;

    }

    @Override
    public void onLoadMoreConversations(int loadedConversations) {

        if(monkeyConversationsFragment.getLastConversation()==null)
            getAllConversations(10, 0);
        else {
            MonkeyConversation conversation = monkeyConversationsFragment.getLastConversation();
            getAllConversations(10, conversation.getDatetime()/1000);
        }

    }

    @Override
    public void onConversationDeleted(@NotNull MonkeyConversation conversation) {

        if (conversation.isGroup()) {
            removeGroupMember(conversation.getId(), myMonkeyID);
        } else {
            deleteConversation(conversation.getId());
        }

    }
}