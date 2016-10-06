package com.criptext.monkeychatandroid;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuInflater;
import android.widget.Toast;

import com.criptext.ClientData;
import com.criptext.comunication.MOKConversation;
import com.criptext.comunication.MOKDelete;
import com.criptext.comunication.MOKMessage;
import com.criptext.comunication.MOKNotification;
import com.criptext.comunication.MOKUser;
import com.criptext.comunication.MessageTypes;
import com.criptext.comunication.PushMessage;
import com.criptext.gcm.MonkeyRegistrationService;
import com.criptext.lib.MKDelegateActivity;
import com.criptext.monkeychatandroid.dialogs.NewGroupDialog;
import com.criptext.monkeychatandroid.gcm.SampleRegistrationService;
import com.criptext.monkeychatandroid.models.AsyncDBHandler;
import com.criptext.monkeychatandroid.models.ConversationItem;
import com.criptext.monkeychatandroid.models.DatabaseHandler;
import com.criptext.monkeychatandroid.models.FindConversationTask;
import com.criptext.monkeychatandroid.models.FindMessageTask;
import com.criptext.monkeychatandroid.models.GetMessagePageTask;
import com.criptext.monkeychatandroid.models.MessageItem;
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
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends MKDelegateActivity implements ChatActivity, ConversationsActivity{

    private static String DATA_FRAGMENT = "MainActivity.chatDataFragment";
    //Since this is the Chat activity, we need a RecyclerView and an adapter. Additionally we
    //will store the messages in our own list so that they can be accessed easily.
    MonkeyFragmentManager monkeyFragmentManager;
    MonkeyChatFragment monkeyChatFragment;

    MonkeyConversationsFragment monkeyConversationsFragment;
    HashMap<String, List<MonkeyItem>> messagesMap = new HashMap<>();
    Collection<MonkeyConversation> conversationsList = null;
    ChatDataFragment dataFragment;

    int actualConversationsPage = 0;
    int actualMessagesPage = 0;

    static int CONV_PERPAGE = 20;
    static int MESS_PERPAGE = 30;
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
     * This class is used to handle group methods.
     */
    private GroupData groupData;

    private AsyncDBHandler asyncDBHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getRetainedData();
        //First, initialize the constants from SharedPreferences.
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
        monkeyFragmentManager.setConversationsTitle(getResources().getString(R.string.app_name));
        monkeyFragmentManager.setContentLayout(savedInstanceState);

        asyncDBHandler = new AsyncDBHandler();
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
        voiceNotePlayer.releasePlayer();
        sensorHandler.onStop();
    }

    private void getRetainedData(){
        final ChatDataFragment retainedFragment =(ChatDataFragment) getSupportFragmentManager().findFragmentByTag(DATA_FRAGMENT);
        if(retainedFragment != null) {
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
        dataFragment.groupData = this.groupData;
        dataFragment.actualConversationsPage = this.actualConversationsPage;
        dataFragment.actualMessagesPage = this.actualMessagesPage;
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorHandler.onDestroy();
        retainDataInFragment();
        if(monkeyChatFragment != null)
            monkeyChatFragment.setInputListener(null);
        asyncDBHandler.cancelAll();
    }

    @Override
    protected void onRestart(){
        super.onRestart();
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
            case R.id.action_deleteall:
                DatabaseHandler.deleteAll();
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
            public void onNewItemFileError(int type) {
                Toast.makeText(MainActivity.this, "Error writing file of type " +
                        MonkeyItem.MonkeyItemType.values()[type], Toast.LENGTH_LONG).show();
            }

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
                newItem.setParams(params.toString());
                newItem.setProps(mokMessage.getProps().toString());

                switch (MonkeyItem.MonkeyItemType.values()[item.getMessageType()]) {
                    case audio:
                        newItem.setAudioDuration(item.getAudioDuration()/1000);
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
        if (monkeyChatFragment != null) {
            MessageItem message = (MessageItem) monkeyChatFragment.findMonkeyItemById(oldId != null ? oldId : id);
            if (message != null) {
                message.setStatus(newStatus.ordinal());
                if(oldId != null){
                    message.setOldMessageId(oldId);
                    message.setMessageId(id);
                    DatabaseHandler.updateMessageStatus(id, oldId, newStatus);

                }else{
                    DatabaseHandler.updateMessageStatus(id, null, newStatus);
                }
                monkeyChatFragment.updateMessageDeliveryStatus(message);
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
    private ConversationTransaction newConversationTransaction(final String conversationId,
                                    final String secondaryText,
                                    final MonkeyConversation.ConversationStatus status, final int unread,
                                    final long dateTime, final long lastRead) {
        return new ConversationTransaction() {
            @Override
            public void updateConversation(@NotNull MonkeyConversation conversation) {
                ConversationItem newConversationItem = (ConversationItem)conversation;
                newConversationItem.setSecondaryText(secondaryText!=null?secondaryText:conversation.getSecondaryText());
                if(status != MonkeyConversation.ConversationStatus.empty){
                    newConversationItem.setStatus(status.ordinal());
                    newConversationItem.setTotalNewMessage(unread == 0 ? 0 : conversation.getTotalNewMessages()+unread);
                }
                if(status == MonkeyConversation.ConversationStatus.empty ||
                        (monkeyChatFragment!=null && monkeyChatFragment.getConversationId().equals(conversationId))){
                    newConversationItem.setTotalNewMessage(0);
                }
                newConversationItem.setDatetime(dateTime>-1?dateTime:conversation.getDatetime());
                newConversationItem.lastRead = Math.max(lastRead, newConversationItem.lastRead);

                if(conversation.isGroup() && conversation.getStatus() ==
                        MonkeyConversation.ConversationStatus.sentMessageRead.ordinal())
                    newConversationItem.setStatus(MonkeyConversation.ConversationStatus.
                            deliveredMessage.ordinal());
            }
        };
    }

    /**
     * Update a conversation according to the params received.
     * @param conversationId conversation id to change
     * @param secondaryText secondary text
     * @param status new status
     * @param unread number of messages without read
     * @param dateTime new date time of the conversation
     */
    private void updateConversation(final String conversationId, final String secondaryText,
                                    final MonkeyConversation.ConversationStatus status, final int unread,
                                    final long dateTime, final long lastRead){
        final ConversationTransaction transaction = newConversationTransaction(conversationId,
                secondaryText, status, unread, dateTime, lastRead);
        if(monkeyConversationsFragment!=null) {
            final ConversationItem conversationItem = (ConversationItem) monkeyConversationsFragment.findConversationById(conversationId);
            if(conversationItem!=null) {
                monkeyConversationsFragment.updateConversation(conversationItem, transaction);
                DatabaseHandler.updateConversation(conversationItem);
            }
        } else { //Conversation not in memory, look in database and update.
             asyncDBHandler.getConversationById(new FindConversationTask.OnQueryReturnedListener() {
                @Override
                public void onQueryReturned(ConversationItem result) {
                    transaction.updateConversation(result);
                    DatabaseHandler.updateConversation(result);

                }
            }, conversationId);
        }
    }


    private void updateConversationBadge(String conversationId, int unread){
        if(monkeyConversationsFragment!=null) {
            final ConversationItem conversationItem = (ConversationItem) monkeyConversationsFragment.findConversationById(conversationId);
            if(conversationItem!=null) {
                DatabaseHandler.updateConversationNewMessagesCount(conversationItem, unread);
                monkeyConversationsFragment.updateConversation(conversationItem);
            }
        }
    }

    /**
     * Search a conversation by its message id and update its status.
       unsend working with messages
     */
    private void updateConversationLastRead(final String convId, final long lastRead){
        asyncDBHandler.getConversationById(new FindConversationTask.OnQueryReturnedListener() {
            @Override
            public void onQueryReturned(ConversationItem result) {
                if(result != null){

                    ConversationTransaction transaction = new ConversationTransaction() {
                        @Override
                        public void updateConversation(MonkeyConversation monkeyConversation) {
                            ConversationItem conversation = (ConversationItem) monkeyConversation;
                            conversation.lastRead = lastRead;
                        }
                    };

                    if(monkeyConversationsFragment != null)
                        monkeyConversationsFragment.updateConversation(result, transaction);

                    transaction.updateConversation(result);

                    DatabaseHandler.updateConversation(result);
                }
            }
        }, convId);
    }

    /**
     * Search a conversation by its message id and update its status.
     * @param message message that updates conversation's last text
     * @param read boolean if the message is read or not
     */
    private void updateConversationByMessage(final MessageItem message, final boolean read){
        asyncDBHandler.getConversationById(new FindConversationTask.OnQueryReturnedListener() {
            @Override
            public void onQueryReturned(ConversationItem result) {
                if(result != null){

                    ConversationTransaction transaction = new ConversationTransaction() {
                        @Override
                        public void updateConversation(MonkeyConversation monkeyConversation) {
                            long dateTime = message.getMessageTimestampOrder();
                            ConversationItem conversation = (ConversationItem) monkeyConversation;
                            conversation.setDatetime(dateTime>-1?dateTime:conversation.getDatetime());
                            conversation.lastRead = message.getMessageTimestampOrder();
                        }
                    };

                    if(monkeyConversationsFragment != null)
                        monkeyConversationsFragment.updateConversation(result, transaction);
                    else
                        transaction.updateConversation(result);

                    DatabaseHandler.updateConversationWithSentMessage(result,
                            getSecondaryTextByMessageType(message),
                            read ? MonkeyConversation.ConversationStatus.sentMessageRead
                                    : MonkeyConversation.ConversationStatus.deliveredMessage, 0);
                }
            }
        }, message.getConversationId());
    }

    /**
     * Creates a new MonkeyChatFragment and adds it to the activity.
     * @param chat conversation to display
     * @param initialMessages a list of the first messages to draw in the chat
     * @param hasReachedEnd true of the initial messages are the only existing messages of the chat
     */
    public void startChatWithMessages(ConversationItem chat, ArrayList<MonkeyItem> initialMessages,
                                      boolean hasReachedEnd){
        messagesMap.put(chat.getConvId(), initialMessages);
        MonkeyChatFragment fragment = chat.isGroup() ?
                MonkeyChatFragment.Companion.newGroupInstance(chat.getConvId(), chat.getName(),
                        chat.getAvatarFilePath(), hasReachedEnd, chat.lastRead, chat.getGroupMembers()) :
                MonkeyChatFragment.Companion.newInstance(chat.getConvId(), chat.getName(),
                        chat.getAvatarFilePath(), hasReachedEnd, chat.lastRead);

        conversationsList = monkeyFragmentManager.setChatFragment(fragment, initInputListener(), voiceNotePlayer);
    }
    /**
     * Updates a sent message and updates de UI so that the user can see that it has been
     * successfully delivered
     * @param oldId The old Id of the message.
     */
    private void markMessageAsDelivered(String oldId, String newId){
        updateMessage(newId, oldId, MonkeyItem.DeliveryStatus.delivered);
    }

    /**
     * adds a message to the adapter so that it can be displayed in the RecyclerView.
     * @param message
     */
    private void processNewMessage(MOKMessage message){
        String conversationID = message.getConversationID(myMonkeyID);
        MessageItem newItem = DatabaseHandler.createMessage(message, this, myMonkeyID, !message.isMyOwnMessage(myMonkeyID));
        if(monkeyChatFragment != null && monkeyChatFragment.getConversationId().equals(conversationID)) {
            monkeyChatFragment.smoothlyAddNewItem(newItem);
        }
        else if(messagesMap!=null && messagesMap.get(conversationID)!=null){
            messagesMap.get(conversationID).add(newItem);
        }
        else{
            ArrayList<MonkeyItem> monkeyItemArrayList = new ArrayList<>();
            monkeyItemArrayList.add(newItem);
            messagesMap.put(conversationID, monkeyItemArrayList);
        }
        //Validate if conversation does not exists
        if(monkeyConversationsFragment != null &&
                monkeyConversationsFragment.findConversationById(conversationID)==null){
            getConversationInfo(conversationID);
        }
    }

    /**
     * adds new messages to the adapter so that it can be displayed in the RecyclerView.
     * @param messages
     */
    private void processNewMessages(String conversationId, List<MonkeyItem> messages){

            if(monkeyChatFragment!=null && monkeyChatFragment.getConversationId().
                    equals(conversationId)){
                monkeyChatFragment.smoothlyAddNewItems(messages);
            }
            else if(messagesMap!=null && messagesMap.get(conversationId)!=null){
                messagesMap.get(conversationId).addAll(messages);
            }
            else if(messagesMap!=null){
                messagesMap.put(conversationId, messages);
            }
            //Validate if conversation does not exists
            if(monkeyConversationsFragment!=null && monkeyConversationsFragment.
                    findConversationById(conversationId)==null){
                getConversationInfo(conversationId);
            }
    }

    /**
     * adds old messages to the adapter so that it can be displayed in the RecyclerView.
     * @param messages
     */
    private void processOldMessages(ArrayList<MOKMessage> messages){

        ArrayList<MessageItem> messageItems = new ArrayList<>();
        for(MOKMessage message: messages){
            messageItems.add(DatabaseHandler.createMessage(message, this, myMonkeyID, !message.isMyOwnMessage(myMonkeyID)));
        }
        Collections.sort(messageItems);
        DatabaseHandler.saveMessages(messageItems);
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
    public void storeSendingMessage(final MOKMessage message) {
        //TODO update conversation
        DatabaseHandler.storeNewMessage(DatabaseHandler.createMessage(message, this, myMonkeyID, false));
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
        updateMessage(fileMessageId, null,
                success ? MonkeyItem.DeliveryStatus.delivered : MonkeyItem.DeliveryStatus.error);
    }

    @Override
    public void onAcknowledgeRecieved(@NotNull String senderId, @NotNull String recipientId,
                          final @NotNull String newId, final @NotNull String oldId, final boolean read,
                                      final int messageType) {

        super.onAcknowledgeRecieved(senderId, recipientId, newId, oldId, read, messageType);
        Log.d("MainActivity", "On ACK Received read? " + read);

        asyncDBHandler.getMessageById(new FindMessageTask.OnQueryReturnedListener() {
            @Override
            public void onQueryReturned(MessageItem result) {
                if(result != null){
                    markMessageAsDelivered(oldId, newId);
                    updateConversationByMessage(result, read);

                } else if((messageType == Integer.parseInt(MessageTypes.MOKText)
                        || messageType == Integer.parseInt(MessageTypes.MOKFile))){
                    //If we use the same monkeyId for several devices (multisession) we receive an
                    // acknowledge for each message sent. So to validate if we have the message
                    // sent, we can send a sync message.
                    //sendSync();
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
    public void onMessageReceived(@NonNull MOKMessage message) {
        processNewMessage(message);
        boolean isMyOwnMsg = message.getSid().equals(myMonkeyID);
        updateConversation(message.getConversationID(myMonkeyID), getSecondaryTextByMOkMessage(message),
                isMyOwnMsg? MonkeyConversation.ConversationStatus.deliveredMessage:
                        MonkeyConversation.ConversationStatus.receivedMessage, isMyOwnMsg? 0 : 1, message.getDatetimeorder(), 0L);

    }

    @Override
    public void onSyncComplete(HashMap<String, List<MOKMessage>> conversationMessages,
                               List<MOKNotification> notifications, List<MOKDelete> deletes) {
        setStatusBarState(Utils.ConnectionStatus.connected);
        Iterator<Map.Entry<String, List<MOKMessage>>> iterator = conversationMessages.entrySet().iterator();
        HashMap<MonkeyConversation, ConversationTransaction> updates = new HashMap<>();
        HashMap<String, ConversationTransaction> dbUpdates = new HashMap<>();
        while(iterator.hasNext()) {
            Map.Entry<String, List<MOKMessage>> entry = iterator.next();
            String id = entry.getKey();
            List<MOKMessage> value = entry.getValue();
            List<MonkeyItem> messages = new LinkedList<>();
            int newMessages = 0;
            for(MOKMessage mokMessage : value) {
                messages.add(DatabaseHandler.createMessage(mokMessage, this, myMonkeyID,
                        !mokMessage.getSid().equals(myMonkeyID)));
                if(!mokMessage.isMyOwnMessage(myMonkeyID))
                    newMessages++;
            }

            processNewMessages(id, messages);
            MOKMessage message = value.get(value.size() - 1);
            boolean isMyOwnMsg = message.getSid().equals(myMonkeyID);
            ConversationTransaction transaction = newConversationTransaction(
                    message.getConversationID(myMonkeyID), getSecondaryTextByMOkMessage(message),
                    isMyOwnMsg? MonkeyConversation.ConversationStatus.deliveredMessage:
                            MonkeyConversation.ConversationStatus.receivedMessage,
                    newMessages, message.getDatetimeorder(), 0L);
            MonkeyConversation conversation = null;
            dbUpdates.put(id, transaction);
            if(monkeyConversationsFragment != null)
                conversation = monkeyConversationsFragment.findConversationById(id);
            if(conversation != null) {
                updates.put(conversation, transaction);
            }
        }

        if(monkeyConversationsFragment != null)
            monkeyConversationsFragment.updateConversations(updates.entrySet());

        DatabaseHandler.updateConversations(dbUpdates);

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
                boolean isMyOwnMessage = monkeyItem.getSenderId().equals(myMonkeyID);
                conversationItem.setSecondaryText(getSecondaryTextByMessageType(monkeyItem));
                conversationItem.setDatetime(monkeyItem.getMessageTimestampOrder());
                conversationItem.setTotalNewMessage(isMyOwnMessage? 0 : 1);
                conversationItem.setStatus(isMyOwnMessage? MonkeyConversation.ConversationStatus.receivedMessage.ordinal():
                        MonkeyConversation.ConversationStatus.deliveredMessage.ordinal());
                messagesMap.get(mokConversation.getConversationId()).clear();
                conversationItem.save();
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
                conversationItem.save();
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

        ArrayList<MonkeyConversation> monkeyConversations = new ArrayList<>();
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
                if(mokConversation.getLastMessage().isMyOwnMessage(myMonkeyID)){
                    conversationItem.status = MonkeyConversation.ConversationStatus.deliveredMessage.ordinal();
                }
                else{
                    conversationItem.status = MonkeyConversation.ConversationStatus.receivedMessage.ordinal();
                }
            }
            monkeyConversations.add(conversationItem);
        }
        ConversationItem[] conversationItems = new ConversationItem[monkeyConversations.size()];
        for(int i = 0; i < monkeyConversations.size(); i++){
            conversationItems[i] = new ConversationItem(monkeyConversations.get(i));
        }
        DatabaseHandler.saveConversations(conversationItems);
        monkeyConversationsFragment.addOldConversations(monkeyConversations, monkeyConversations.size()==0);
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
        updateMessage(message.getMessage_id(), null, MonkeyItem.DeliveryStatus.error);
    }


    @Override
    public void onConversationOpenResponse(String senderId, Boolean isOnline, String lastSeen, String lastOpenMe, String members_online) {
        if(monkeyFragmentManager!=null) {
            String subtitle = isOnline? "Online":"";
            long lastSeenValue = System.currentTimeMillis();
            boolean isGroupConversation = senderId.contains("G:");
            if(!isOnline){
                if(lastSeen.isEmpty())
                    lastSeenValue = 0L;
                else
                    lastSeenValue = Long.valueOf(lastSeen);
                subtitle = "Last seen: "+Utils.Companion.getFormattedDay(lastSeenValue * 1000, this);
            }
            else if(isGroupConversation){
                int membersOnline = members_online.split(",").length;
                subtitle = membersOnline + " " + (membersOnline>1?"members online":"member online");
            }
            monkeyFragmentManager.setSubtitle(subtitle);

            if(!isGroupConversation && monkeyChatFragment!=null &&
                    monkeyChatFragment.getConversationId().equals(senderId)) {
                monkeyChatFragment.setLastRead(lastSeenValue);
                updateConversationLastRead(senderId, lastSeenValue);
            }
        }
    }

    @Override
    public void onDeleteReceived(String messageId, String senderId, String recipientId) {

        String conversationId = senderId.equals(myMonkeyID) ? recipientId : senderId;
        MessageItem message = DatabaseHandler.lastConversationMessage(conversationId);
        int unreadCounter = 0;
        ConversationItem conversationItem = (ConversationItem) monkeyConversationsFragment.findConversationById(conversationId);
        MonkeyConversation.ConversationStatus status;

        if(monkeyChatFragment != null && monkeyChatFragment.getConversationId().equals(conversationId)) {
            monkeyChatFragment.removeMonkeyItem(messageId);
        }else{
            List<MonkeyItem> conversationMessages = messagesMap.get(conversationId);
            if(conversationMessages != null){
                if(conversationItem!=null && conversationItem.getTotalNewMessages() > 0) {
                    if(conversationMessages.size() - MonkeyItem.Companion.findLastPositionById(messageId, conversationMessages) <= conversationItem.getTotalNewMessages()){
                        unreadCounter = conversationItem.getTotalNewMessages() - 1;
                    }
                }

                int position = MonkeyItem.Companion.findLastPositionById(messageId, conversationMessages);
                if(position > -1){
                    conversationMessages.remove(position);
                }
            }
        }

        if(message != null && message.getMessageId().equals(messageId)){
            MessageItem lastMessage = DatabaseHandler.unsendMessage(messageId, conversationId);

            status = lastMessage.getSenderId().equals(myMonkeyID) ? MonkeyConversation.ConversationStatus.deliveredMessage : MonkeyConversation.ConversationStatus.receivedMessage;
            if(status.equals(MonkeyConversation.ConversationStatus.deliveredMessage) && lastMessage.getMessageTimestamp() <= conversationItem.lastRead){
                status = MonkeyConversation.ConversationStatus.sentMessageRead;
            }
            updateConversation(lastMessage.getConversationId(), getSecondaryTextByMessageType(lastMessage), status, unreadCounter > 0 ? -1 : 0, lastMessage.getMessageTimestampOrder(), 0);
            return;
        }else if(unreadCounter != 0){
            updateConversationBadge(conversationId, unreadCounter);
        }
        DatabaseHandler.deleteMessage(messageId);
    }

    @Override
    public void onContactOpenMyConversation(String monkeyId) {
        //Update the conversation status
        long newLastReadValue = System.currentTimeMillis();
        if(monkeyChatFragment!=null && monkeyChatFragment.getConversationId().equals(monkeyId)) {
            monkeyChatFragment.setLastRead(newLastReadValue);
            MonkeyItem newestMessage = monkeyChatFragment.getLastMessage();
            if(newestMessage != null) newLastReadValue = newestMessage.getMessageTimestamp();
        }
        //updateConversation(monkeyId, null, MonkeyConversation.ConversationStatus.sentMessageRead,
         //       0, -1, newLastReadValue);
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
            updateMessage(item.getMessageId(), null, MonkeyItem.DeliveryStatus.sending);
            if(monkeyChatFragment != null)
                monkeyChatFragment.rebindMonkeyItem(item);
        }
        boolean msgIsResending = resendFile(item.getMessageId());
        if(!msgIsResending){
            MessageItem message = (MessageItem) item;
            MOKMessage resendMessage = new MOKMessage(message.getMessageId(), myMonkeyID, myFriendID,
                    message.getMessageText(), "" + message.getMessageTimestamp(), "" + message.getMessageType(),
                    message.getJsonParams(), message.getJsonProps());
            resendFile(resendMessage, new PushMessage("You have a new message from the sample app"), true);
        }
    }

    @Override
    public void onFileDownloadRequested(@NotNull MonkeyItem item) {

        if(item.getDeliveryStatus() == MonkeyItem.DeliveryStatus.error) {
            //If the message failed to download previously, mark it as sending and rebind.
            //Rebinding will update the UI to a loading view and call this method again to start
            //the download
            updateMessage(item.getMessageId(), null, MonkeyItem.DeliveryStatus.sending);
            if(monkeyChatFragment != null)
                monkeyChatFragment.rebindMonkeyItem(item);
        } else { //Not error status, download the file.
            final MessageItem messageItem = (MessageItem) item;
            downloadFile(messageItem.getMessageId(), messageItem.getFilePath(),
                    messageItem.getJsonProps(), messageItem.getSenderId(),
                    messageItem.getMessageTimestampOrder(), getActiveConversation());
        }

    }

    @Override
    public void onLoadMoreMessages(final String conversationId) {
        asyncDBHandler.getMessagePage(new GetMessagePageTask.OnQueryReturnedListener() {
            @Override
            public void onQueryReturned(List<MessageItem> messageItems) {
                actualMessagesPage++;
                if(messageItems.size() > 0){
                    if(monkeyChatFragment != null) {
                        monkeyChatFragment.addOldMessages(new ArrayList<MonkeyItem>(messageItems), messageItems.size() == 0);
                    }
                }
                else {
                    addOldMessagesFromServer(conversationId);
                }
            }
        }, myMonkeyID, conversationId, MESS_PERPAGE, actualMessagesPage);
    }

    @Override
    public void onDestroyWithPendingMessages(@NotNull ArrayList<MOKMessage> errorMessages) {
        DatabaseHandler.markMessagesAsError(errorMessages);
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
            groupData = new GroupData(conversationId, membersIds, this);
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
            //Get conversations from DB
            conversations.addAll(DatabaseHandler.getConversations(CONV_PERPAGE, actualConversationsPage));
            //Add a mirror chat
            //final long datetime = System.currentTimeMillis();
            //conversations.add(new ConversationItem(myMonkeyID, "Mirror Chat", datetime, "Tap to talk to yourself",
            //        0, false, "", null, MonkeyConversation.ConversationStatus.empty.ordinal()));
        }
        monkeyConversationsFragment.insertConversations(conversations, false);
    }

    @Override
    public void onConversationClicked(final @NotNull MonkeyConversation conversation) {

        actualMessagesPage = 0;
        List<MonkeyItem> messages = messagesMap.get(conversation.getConvId());
        if(messages!=null && !messages.isEmpty()){
            //Get initial messages from memory
            startChatWithMessages((ConversationItem) conversation, new ArrayList<MonkeyItem>(messages), false);
        }
        else{
            //Get initial messages from DB
            asyncDBHandler.getMessagePage(new GetMessagePageTask.OnQueryReturnedListener() {
                @Override
                public void onQueryReturned(List<MessageItem> messageItems) {
                    messagesMap.put(conversation.getConvId(), new ArrayList<MonkeyItem>(messageItems));
                    startChatWithMessages((ConversationItem) conversation, new ArrayList<>(messagesMap.get(conversation.getConvId())),
                             false);
                }
            }, myMonkeyID, conversation.getConvId(), MESS_PERPAGE, actualMessagesPage);
        }

        updateConversationBadge(conversation.getConvId(), 0);
    }

    @Override
    public void setConversationsFragment(@Nullable MonkeyConversationsFragment conversationsFragment) {
        monkeyConversationsFragment = conversationsFragment;

    }

    @Override
    public void onLoadMoreConversations(int loadedConversations) {

        if (monkeyConversationsFragment == null) {
            return;
        }

        actualConversationsPage++;
        List<ConversationItem> conversationItems = DatabaseHandler.getConversations(CONV_PERPAGE, actualConversationsPage);
        if(conversationItems.size() > 0) {
            monkeyConversationsFragment.addOldConversations(conversationItems, conversationItems.size()==0);
        }
        else{
            //If there is no more conversations we get conversations from server
            if (monkeyConversationsFragment.getLastConversation() == null)
                getConversationsFromServer(CONV_PERPAGE, 0);
            else {
                MonkeyConversation conversation = monkeyConversationsFragment.getLastConversation();
                getConversationsFromServer(CONV_PERPAGE, conversation.getDatetime() / 1000);
            }
        }
    }

    @Override
    public void onConversationDeleted(@NotNull MonkeyConversation conversation) {

        if (conversation.isGroup()) {
            removeGroupMember(conversation.getConvId(), myMonkeyID);
        } else {
            deleteConversation(conversation.getConvId());
        }
        DatabaseHandler.deleteConversation(conversation.getConvId());

    }

    @Override
    public void onMessageRemoved(@NotNull MonkeyItem item, boolean unsent) {

        if(unsent){
            unsendMessage(item.getSenderId(), ((MessageItem)item).getConversationId(), item.getMessageId());
        }else{
            DatabaseHandler.deleteMessage(item.getMessageId());
        }
    }
}