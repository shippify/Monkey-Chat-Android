package com.criptext.monkeychatandroid;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
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
import com.criptext.http.HttpSync;
import com.criptext.lib.MKDelegateActivity;
import com.criptext.monkeychatandroid.gcm.SampleRegistrationService;
import com.criptext.monkeychatandroid.models.AsyncDBHandler;
import com.criptext.monkeychatandroid.models.ConversationItem;
import com.criptext.monkeychatandroid.models.DatabaseHandler;
import com.criptext.monkeychatandroid.models.FindConversationsTask;
import com.criptext.monkeychatandroid.models.FindConversationTask;
import com.criptext.monkeychatandroid.models.FindMessageTask;
import com.criptext.monkeychatandroid.models.GetMessagePageTask;
import com.criptext.monkeychatandroid.models.MessageItem;
import com.criptext.monkeychatandroid.models.StoreNewConversationTask;
import com.criptext.monkeykitui.MonkeyChatFragment;
import com.criptext.monkeykitui.MonkeyConversationsFragment;
import com.criptext.monkeykitui.MonkeyInfoFragment;
import com.criptext.monkeykitui.cav.EmojiHandler;
import com.criptext.monkeykitui.conversation.ConversationsActivity;
import com.criptext.monkeykitui.conversation.MonkeyConversation;
import com.criptext.monkeykitui.conversation.holder.ConversationTransaction;
import com.criptext.monkeykitui.info.InfoActivity;
import com.criptext.monkeykitui.input.listeners.InputListener;
import com.criptext.monkeykitui.recycler.ChatActivity;
import com.criptext.monkeykitui.recycler.GroupChat;
import com.criptext.monkeykitui.recycler.MonkeyInfo;
import com.criptext.monkeykitui.recycler.MonkeyItem;
import com.criptext.monkeykitui.recycler.audio.PlaybackNotification;
import com.criptext.monkeykitui.recycler.audio.PlaybackService;
import com.criptext.monkeykitui.recycler.audio.VoiceNotePlayer;
import com.criptext.monkeykitui.toolbar.ToolbarDelegate;
import com.criptext.monkeykitui.util.MonkeyFragmentManager;
import com.criptext.monkeykitui.util.Utils;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends MKDelegateActivity implements ChatActivity, ConversationsActivity, InfoActivity, ToolbarDelegate{

    private static String DATA_FRAGMENT = "MainActivity.chatDataFragment";
    //Since this is the Chat activity, we need a RecyclerView and an adapter. Additionally we
    //will store the messages in our own list so that they can be accessed easily.
    MonkeyFragmentManager monkeyFragmentManager;
    MonkeyChatFragment monkeyChatFragment;
    MonkeyConversationsFragment monkeyConversationsFragment;
    ConversationItem activeConversationItem = null;
    MonkeyInfoFragment monkeyInfoFragment;

    HashMap<String, List<MonkeyItem>> messagesMap = new HashMap<>();
    Collection<MonkeyConversation> conversationsList = null;
    Collection<MonkeyInfo> usersList = null;
    ChatDataFragment dataFragment;

    static int CONV_PERPAGE = 20;
    static int MESS_PERPAGE = 30;
    int actualConversationsPage;

    /**
     * This class is basically a media player for our voice notes. we pass this to MonkeyAdapter
     * so that it can handle all the media playback for us. However, we must initialize it in "onStart".
     * and release it in "onStop" method.
     */
    PlaybackService.VoiceNotePlayerBinder voiceNotePlayer;

    private SharedPreferences prefs;
    /**
     * Monkey ID of the current user. This is stored in Shared Preferences, so we use this
     * property to cache it so that we don't have to read from disk every time we need it.
     */
    private String myMonkeyID;
    /**
     * Name of the current user. This is stored in Shared Preferences, so we use this
     * property to cache it so that we don't have to read from disk every time we need it.
     */
    private String myName;
    /**
     * Monkey ID of the user that we are going to talk with.
     */
    private String myFriendID;
    /**
     * This class is used to handle group methods.
     */
    private GroupData groupData;

    private AsyncDBHandler asyncDBHandler;

    private File downloadDir;

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
        getRetainedData();
        //First, initialize the constants from SharedPreferences.
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        myMonkeyID = prefs.getString(MonkeyChat.MONKEY_ID, null);
        myName = prefs.getString(MonkeyChat.FULLNAME, null);
        //Log.d("MonkeyId", myMonkeyID);
        downloadDir = MonkeyChat.getDownloadDir(this);

        //Check play services. if available try to register with GCM so that we get Push notifications
        if(MonkeyRegistrationService.Companion.checkPlayServices(this))
                registerWithGCM();

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
        //sensorHandler.onPause();
    }

    @Override
    protected void onStart(){
        super.onStart();
        //bind to the service that plays voice notes.
        startPlaybackService();
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

    private void getRetainedData(){
        final ChatDataFragment retainedFragment =(ChatDataFragment) getSupportFragmentManager().findFragmentByTag(DATA_FRAGMENT);
        if(retainedFragment != null) {
            messagesMap = retainedFragment.chatMap!=null?retainedFragment.chatMap:new HashMap<String, List<MonkeyItem>>();
            conversationsList = retainedFragment.conversationsList;
            groupData = retainedFragment.groupData;
            activeConversationItem = retainedFragment.activeConversationItem;
            if (monkeyChatFragment != null) {
                monkeyChatFragment.setInputListener(initInputListener());
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
        dataFragment.activeConversationItem = activeConversationItem;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //sensorHandler.onDestroy();
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
            public void onNewItemFileError(int type) {
                Toast.makeText(MainActivity.this, "Error writing file of type " +
                        MonkeyItem.MonkeyItemType.values()[type], Toast.LENGTH_LONG).show();
            }

            @Override
            public void onNewItem(@NotNull MonkeyItem item) {

                String textTalk = null;
                JsonObject params = new JsonObject();
                MOKMessage mokMessage;

                if(activeConversationItem != null && activeConversationItem.isGroup()){
                    textTalk = activeConversationItem.getName();
                }

                //Store the message in the DB and send it via MonkeyKit
                switch (MonkeyItem.MonkeyItemType.values()[item.getMessageType()]) {
                    case audio:
                        params = new JsonObject();
                        params.addProperty("length",""+item.getAudioDuration()/1000);

                        mokMessage = persistFileMessageAndSend(item.getFilePath(), myMonkeyID, myFriendID,
                                MessageTypes.FileTypes.Audio, params, new PushMessage(EmojiHandler.encodeJavaForPush(myName) +  (textTalk==null ? " sent you an audio" : "sent an audio to " + textTalk) ), true);
                        break;
                    case photo:
                        mokMessage = persistFileMessageAndSend(item.getFilePath(), myMonkeyID, myFriendID,
                                MessageTypes.FileTypes.Photo, new JsonObject(), new PushMessage(EmojiHandler.encodeJavaForPush(myName) + (textTalk==null ? " sent you a photo" : "sent a photo to " + textTalk) ), true);
                        break;
                    default:
                        mokMessage = persistMessageAndSend(item.getMessageText(), myMonkeyID,
                                myFriendID, params, new PushMessage(EmojiHandler.encodeJavaForPush(myName) + (textTalk==null ? " sent you a message" : " sent a message to " + textTalk) ), true);
                        break;
                }

                //Now that the message was sent, create a MessageItem using the MOKMessage that MonkeyKit
                //created. This MessageItem will be added to MonkeyAdapter so that it can be shown in
                //the screen.
                //USE THE DATETIMEORDER FROM MOKMESSAGE, NOT THE ONE FROM MONKEYITEM
                MessageItem newItem = new MessageItem(myMonkeyID, myFriendID, mokMessage.getMessage_id(),
                        item.getMessageText(), item.getMessageTimestamp(), mokMessage.getDatetimeorder(), item.isIncomingMessage(),
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
                if(monkeyConversationsFragment != null){
                    updateConversationByMessage(newItem, false);
                }
            }
        };
    }

    /**
     * Add messages retrieved from DB to the messages list
     * @param oldMessages list of messages
     * @param hasReachedEnd boolean if messages has reached end
     */
    public void addOldMessages(ArrayList<MonkeyItem> oldMessages, boolean hasReachedEnd){
        if(oldMessages != null && oldMessages.size()>0 && monkeyChatFragment != null) {
            if(monkeyChatFragment.getConversationId().equals( ((MessageItem)oldMessages.get(0)).getConversationId())){
                monkeyChatFragment.addOldMessages(oldMessages, hasReachedEnd);
            }
        }else if(monkeyChatFragment != null) {
            monkeyChatFragment.addOldMessages(new ArrayList<MonkeyItem>(), hasReachedEnd);
        }
    }

    /**
     * Ask old messages from server
     * @param conversationId id of the conversation messages required
     */
    public void addOldMessagesFromServer(String conversationId){
        if(monkeyChatFragment!=null && monkeyChatFragment.getConversationId().equals(conversationId)) {
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
            if (monkeyChatFragment != null) {
                MessageItem message = (MessageItem) monkeyChatFragment.findMonkeyItemById(oldId != null ? oldId : id);
                if (message != null) {
                    message.setStatus(newStatus.ordinal());
                    if(oldId != null){
                        message.setOldMessageId(oldId);
                        message.setMessageId(id);
                    }
                    monkeyChatFragment.updateMessageDeliveryStatus(message);
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
        if(monkeyConversationsFragment != null) {
            final ConversationItem conversationItem = (ConversationItem) monkeyConversationsFragment.
                    findConversationById(conversationId);

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
        if(monkeyConversationsFragment != null) {
            final ConversationItem conversationItem = (ConversationItem) monkeyConversationsFragment.
                    findConversationById(conversationId);
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
                            if(read){
                                conversation.lastRead = message.getMessageTimestampOrder();
                            }
                            if(message.senderMonkeyId == myMonkeyID){
                                int newStatus = conversation.getStatus();
                                if(message.getDeliveryStatus().ordinal() == 0){
                                    newStatus = 3;
                                }
                                conversation.setStatus(newStatus);
                            }
                        }
                    };

                    int newStatus = result.getStatus();
                    if (message.senderMonkeyId.equals(myMonkeyID)) {
                        if (message.getDeliveryStatus().ordinal() == 0) {
                            newStatus = 3;
                        }else if(read){
                            newStatus = MonkeyConversation.ConversationStatus.sentMessageRead.ordinal();
                        }else{
                            newStatus = MonkeyConversation.ConversationStatus.deliveredMessage.ordinal();
                        }
                        result.setStatus(newStatus);
                    }
                    Log.d("TEST", MonkeyConversation.ConversationStatus.values()[newStatus].toString());
                    if(monkeyConversationsFragment != null) {
                        monkeyConversationsFragment.updateConversation(result, transaction);
                    }else
                        transaction.updateConversation(result);

                    DatabaseHandler.updateConversationWithSentMessage(result,
                            DatabaseHandler.getSecondaryTextByMessageType(message, result.isGroup()),
                            MonkeyConversation.ConversationStatus.values()[newStatus], 0);
                }
            }
        }, message.getConversationId());
    }

    /**
     * Creates a new MonkeyChatFragment and adds it to the activity.
     * @param chat conversation to display
     * @param hasReachedEnd true of the initial messages are the only existing messages of the chat
     */
    public void startChatWithMessages(ConversationItem chat, boolean hasReachedEnd){
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
        String conversationID = message.getConversationID(myMonkeyID);
        MessageItem newItem = DatabaseHandler.createMessage(message, downloadDir.getAbsolutePath(), myMonkeyID);
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
        return newItem;
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
        if(monkeyConversationsFragment !=null && monkeyConversationsFragment.findConversationById(conversationId)==null){
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
            messageItems.add(DatabaseHandler.createMessage(message, downloadDir.getAbsolutePath(), myMonkeyID));
        }
        Collections.sort(messageItems);
        DatabaseHandler.saveMessages(messageItems);
        if(monkeyChatFragment != null) {
            monkeyChatFragment.addOldMessages(new ArrayList<MonkeyItem>(messageItems), messages.size() == 0);
        }
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
        DatabaseHandler.storeNewMessage(DatabaseHandler.createMessage(message,
                downloadDir.getAbsolutePath(), myMonkeyID));
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
    public void onAcknowledgeRecieved(@NotNull final String senderId, @NotNull final String recipientId,
                                      final @NotNull String newId, final @NotNull String oldId, final boolean read,
                                      final int messageType) {
        //Always call super so that MKDelegate knows that it should not attempt to retry this message anymore
        super.onAcknowledgeRecieved(senderId, recipientId, newId, oldId, read, messageType);

        asyncDBHandler.getMessageById(new FindMessageTask.OnQueryReturnedListener() {
            @Override
            public void onQueryReturned(MessageItem result) {
                if(result != null){
                    if(!read)
                        markMessageAsDelivered(oldId, newId, read);
                    else if(getActiveConversation() != null && senderId.equals(getActiveConversation()))
                        monkeyChatFragment.setLastRead(result.getMessageTimestampOrder());
                    updateConversationByMessage(result, read);

                } else if((messageType == Integer.parseInt(MessageTypes.MOKText)
                        || messageType == Integer.parseInt(MessageTypes.MOKFile))){
                    //If we use the same monkeyId for several devices (multisession) we receive an
                    // acknowledge for each message sent. So to validate if we have the message
                    // sent, we can send a sync message.
                    sendSync();
                }

                List<MonkeyItem> conversationMessageList = messagesMap.get(senderId);
                if(conversationMessageList != null) {
                    Iterator<MonkeyItem> iter = conversationMessageList.iterator();
                    while (iter.hasNext()) {
                        MessageItem updateMessage = (MessageItem) iter.next();
                        if (updateMessage.getMessageId().equals(oldId) || updateMessage.getMessageId().equals(newId)) {
                            updateMessage.setStatus(MonkeyItem.DeliveryStatus.delivered.ordinal());
                            break;
                        }
                    }
                }
            }
        }, oldId, newId);
    }

    @Override
    public void onCreateGroup(String groupMembers, String groupName, String groupID, Exception e) {
        Log.d("TEST", "group create");
        if(e==null){
            ConversationItem conversationItem = new ConversationItem(groupID,
                    groupName, System.currentTimeMillis(), "Write to this group",
                    0, true, groupMembers, "", MonkeyConversation.ConversationStatus.empty.ordinal());
            if(monkeyConversationsFragment != null) {
                monkeyConversationsFragment.addNewConversation(conversationItem, true);
            }
        }
    }

    @Override
    public void onAddGroupMember(@Nullable String groupID, @Nullable String members, @Nullable Exception e) {
        Log.d("TEST", "group add");
    }

    @Override
    public void onRemoveGroupMember(@Nullable String groupID, @Nullable String members, @Nullable Exception e) {
        Log.d("TEST", "remove member");
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
        boolean isMyOwnMsg = !newItem.isIncomingMessage();
        updateConversation(newItem.getConversationId(),
                DatabaseHandler.getSecondaryTextByMessageType(newItem, false),
                isMyOwnMsg? MonkeyConversation.ConversationStatus.deliveredMessage:
                        MonkeyConversation.ConversationStatus.receivedMessage,
                isMyOwnMsg? 0 : 1,
                message.getDatetimeorder(), 0L);
    }

    private void syncConversationsFragment(final LinkedHashSet<String> conversationsToUpdate) {
        if(monkeyConversationsFragment != null) {
            String[] ids = new String[conversationsToUpdate.size()];
            conversationsToUpdate.toArray(ids);
            asyncDBHandler.getConversationsById(new FindConversationsTask.OnQueryReturnedListener() {
                @Override
                public void onQueryReturned(HashMap<String, ConversationItem> conversationMap) {
                    if(monkeyConversationsFragment != null) {
                        LinkedList<String> missingConversations = new LinkedList<>();
                        HashMap<String, ConversationTransaction> updates = new HashMap<>();
                        for (String id: conversationsToUpdate) {
                            ConversationItem conv = conversationMap.get(id);
                            if(conv != null && conv.getStatus() != MonkeyConversation.
                                    ConversationStatus.empty.ordinal()) {
                                updates.put(id, DatabaseHandler.newCopyTransaction(conv));
                            } else {
                                missingConversations.add(id);
                            }
                        }
                        monkeyConversationsFragment.updateConversations(updates.entrySet());
                        for(String id : missingConversations) {
                            getConversationInfo(id);
                        }
                    }
                }
            }, ids);
        } else conversationsList.clear();

    }

    private void syncChatFragment(boolean deletedMessages, int totalNewMessages) {
        final String activeConversationId = getActiveConversation();
        if(monkeyChatFragment != null) {
            //update fragment
            if(deletedMessages) {
                asyncDBHandler.getMessagePage(new GetMessagePageTask.OnQueryReturnedListener() {
                    @Override
                    public void onQueryReturned(List<MessageItem> messagePage) {
                        if(activeConversationId.equals(getActiveConversation()) && monkeyChatFragment != null) {
                            monkeyChatFragment.smoothlyAddNewItems(messagePage);
                        }
                    }
                }, activeConversationId, totalNewMessages, 0);
            } else {
                List<MonkeyItem> currentMessages = monkeyChatFragment.takeAllMessages();
                asyncDBHandler.getMessagePage(new GetMessagePageTask.OnQueryReturnedListener() {
                    @Override
                    public void onQueryReturned(List<MessageItem> messagePage) {
                        if(activeConversationId.equals(getActiveConversation()) && monkeyChatFragment != null) {
                            monkeyChatFragment.insertMessages(messagePage);
                        }
                    }
                }, activeConversationId, currentMessages.size(), 0);
            }
        }
    }

    /**
     * Updates the conversation that user may have closed. This updates the lastOpen, secondaryText
     * and totalNewMessages, so that when the user goes back to the conversation list, he/she sees
     * up-to-date data.
     * @param conversationId ID of the conversation to update
     * @param lastOpen timestamp with the last time conversation was open. should be the datetime of last message
     * @param lastMessageText the secondary text to put in the conversation
     */
    public void updateClosedConversation(String conversationId, final long lastOpen, final String lastMessageText) {
        if(activeConversationItem != null && activeConversationItem.getConvId().equals((conversationId))) {
            ConversationTransaction t = new ConversationTransaction() {
                @Override
                public void updateConversation(@NotNull MonkeyConversation conversation) {
                    ConversationItem conversationItem = (ConversationItem) conversation;
                    conversationItem.lastOpen = lastOpen;
                    conversationItem.setTotalNewMessage(0);
                    conversationItem.setSecondaryText(lastMessageText);
                }
            };
            //Apply transaction on the DB
            t.updateConversation(activeConversationItem);
            DatabaseHandler.updateConversation(activeConversationItem);
            //Apply the same transaction on the UI
            if(monkeyConversationsFragment != null)
                monkeyConversationsFragment.updateConversation(activeConversationItem, t);
        } else {
            //throw new IllegalStateException("Tried to update the lastOpen of a non-active conversation");
            IllegalStateException exception = new IllegalStateException("Tried to update the lastOpen of a non-active conversation");
            exception.printStackTrace();
        }
    }

    @Override
    public void onSyncComplete(@NonNull HttpSync.SyncData syncData) {
        Log.d("MainActivity", "Sync complete");
        setStatusBarState(Utils.ConnectionStatus.connected);

        final String activeConversationId = getActiveConversation();
        boolean activeConversationNeedsUpdate = false;
        final HashMap<String, List<MOKMessage>> newMessagesMap = syncData.getNewMessages();
        HashMap<String, List<MOKDelete>> deletesMap = syncData.getDeletes();

        final LinkedHashSet<String> conversationsToUpdate = syncData.getConversationsToUpdate();
        Iterator<String> iterator = syncData.getConversationsToUpdate().iterator();
        while (iterator.hasNext()) {
            String convId = iterator.next();

            if((activeConversationId != null) && activeConversationId.equals(convId)) {
                activeConversationNeedsUpdate = true;
            }
            messagesMap.remove(convId);
        }

        final List<MOKMessage> activeConversationMessages = newMessagesMap.get(activeConversationId);
        syncConversationsFragment(conversationsToUpdate);
        if(activeConversationNeedsUpdate && activeConversationMessages != null)
            syncChatFragment(!deletesMap.containsKey(activeConversationId),
                    activeConversationMessages.size());

        final List<MOKNotification> notifications = syncData.getNotifications();
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
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onMessageFailDecrypt(MOKMessage message) {

    }

    @Override
    public void onGroupAdded(String groupid, String members, JsonObject info) {
        Log.d("TEST", "ADDING NEW GROUP");
        ConversationItem conversation = DatabaseHandler.getConversationById(groupid);
        if(conversation == null) {
            conversation = new ConversationItem(groupid, info.has("name") ? info.get("name").getAsString() : "Uknown Group", System.currentTimeMillis(),
                    "Write to this group", 0, true, members, info.has("avatar") ? info.get("avatar").getAsString() : "", MonkeyConversation.ConversationStatus.empty.ordinal());
            DatabaseHandler.saveConversations(new ConversationItem[]{conversation});
            if (monkeyConversationsFragment != null) {
                monkeyConversationsFragment.addNewConversation(conversation, true);
            }
            if (conversationsList != null) {
                conversationsList.add(conversation);
            }
        }
    }

    @Override
    public void onGroupNewMember(String groupid, String new_member) {

        if(groupData!=null && groupData.getConversationId().equals(groupid)){
            groupData.addMember(new_member);
            getUsersInfo(groupData.getMembersIds());
        }
        ConversationItem conversation = DatabaseHandler.getConversationById(groupid);
        if(conversation != null){
            conversation.addMember(new_member);
            DatabaseHandler.updateConversation(conversation);
            if(monkeyConversationsFragment != null){
                monkeyConversationsFragment.updateConversation(conversation);
            }
        }
        int convPosition = getConversationFromList(groupid, (ArrayList<MonkeyConversation>) conversationsList);
        if(convPosition > -1){
            ((ArrayList<MonkeyConversation>) conversationsList).set(convPosition, conversation);
        }
    }

    @Override
    public void onGroupRemovedMember(String groupid, String removed_member) {

        if(groupData!=null && groupData.getConversationId().equals(groupid)){
            groupData.removeMember(removed_member);
            groupData.setInfoList(myMonkeyID, myName);
        }
        if(monkeyInfoFragment!=null && monkeyChatFragment!=null){
            if(monkeyChatFragment.getConversationId().equals(groupid)){
                monkeyInfoFragment.removeMember(removed_member);
            }
        }
        ConversationItem conversation = DatabaseHandler.getConversationById(groupid);
        if(conversation != null){
            conversation.removeMember(removed_member);
            DatabaseHandler.updateConversation(conversation);
            if(monkeyConversationsFragment != null){
                monkeyConversationsFragment.updateConversation(conversation);
            }
        }
        int convPosition = getConversationFromList(groupid, (ArrayList<MonkeyConversation>) conversationsList);
        if(convPosition > -1){
            ((ArrayList<MonkeyConversation>) conversationsList).set(convPosition, conversation);
        }
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
                    if(monkeyConversationsFragment != null) {
                        monkeyConversationsFragment.addNewConversation(result, true);
                    }
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
                    if(monkeyConversationsFragment != null) {
                        monkeyConversationsFragment.addNewConversation(result, true);
                    }
                }
            }, conversationItem);
        }

    }

    @Override
    public void onGetUsersInfo(@NotNull ArrayList<MOKUser> mokUsers, @Nullable Exception e) {
        if(e==null && groupData!=null && monkeyChatFragment!=null) {
            groupData.setMembers(monkeyChatFragment.getConversationId(), mokUsers);
            groupData.setAdmins(DatabaseHandler.getConversationById(monkeyChatFragment.getConversationId()).getAdmins());
            groupData.setInfoList(myMonkeyID, myName);
            monkeyChatFragment.reloadAllMessages();
            if(monkeyInfoFragment != null){
                monkeyInfoFragment.setInfo(groupData.getInfoList());
            }
        }
    }

    @Override
    public void onGetConversations(@NotNull ArrayList<MOKConversation> conversations, @Nullable Exception e) {
        //ALWAYS CALL SUPER FOR THIS CALLBACK!!
        super.onGetConversations(conversations, e);
        if(e!=null) {
            e.printStackTrace();
            return;
        }

        if(conversations.isEmpty())
            Log.d("MainActivity", "getconversations empty");
        else
            Log.d("MainActvity", "getconversations. first is " + conversations.get(0).getConversationId());

        ArrayList<MonkeyConversation> monkeyConversations = new ArrayList<>();
        for(MOKConversation mokConversation : conversations){
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
                    downloadDir.getAbsolutePath(), myMonkeyID);
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
        if(monkeyConversationsFragment != null)
            monkeyConversationsFragment.addOldConversations(monkeyConversations, monkeyConversations.size() == 0);
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
        if(monkeyFragmentManager!=null && monkeyChatFragment!=null) {
            if(!monkeyChatFragment.getConversationId().equals(senderId)){
                return;
            }

            String subtitle = isOnline? "Online":"";

            long lastSeenValue = -1L;
            boolean isGroupConversation = senderId.contains("G:");
            if(isGroupConversation){
                groupData.setMembersOnline(members_online);
                int membersOnline = members_online != null ? members_online.split(",").length : 0;
                if(membersOnline > 0) {
                    subtitle = membersOnline + " " + (membersOnline > 1 ? "members online" : "member online");
                }
                groupData.setInfoList(myMonkeyID, myName);
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

            if(!isGroupConversation && monkeyChatFragment!=null &&
                    monkeyChatFragment.getConversationId().equals(senderId)) {
                lastSeenValue = (activeConversationItem.lastRead > lastSeenValue ? activeConversationItem.lastRead : lastSeenValue);
                monkeyChatFragment.setLastRead(lastSeenValue);
                updateConversationLastRead(senderId, lastSeenValue);
            }
        }
    }

    @Override
    public void onDeleteReceived(String messageId, String senderId, String recipientId) {

        String conversationId = senderId.equals(myMonkeyID) ? recipientId : senderId;
        MessageItem message = DatabaseHandler.lastConversationMessage(conversationId);
        MonkeyConversation.ConversationStatus status;
        int unreadCounter = 0;

        ConversationItem conversationItem = (ConversationItem) monkeyConversationsFragment.findConversationById(conversationId);

        if(conversationItem == null){
            return;
        }


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

            if(lastMessage == null){
                updateConversation(conversationItem.getConvId(), conversationItem.getConvId().contains("G:") ? "Write to this Group" : "Write to this Conversation",
                        MonkeyConversation.ConversationStatus.receivedMessage, 0, conversationItem.getDatetime(), 0);
                DatabaseHandler.deleteMessage(messageId);
                return;
            }

            status = lastMessage.isIncomingMessage() ?
                    MonkeyConversation.ConversationStatus.receivedMessage:
                MonkeyConversation.ConversationStatus.deliveredMessage;

            if(status.equals(MonkeyConversation.ConversationStatus.deliveredMessage) &&
                    lastMessage.getMessageTimestamp() <= conversationItem.lastRead){
                status = MonkeyConversation.ConversationStatus.sentMessageRead;
            }
            updateConversation(lastMessage.getConversationId(), DatabaseHandler.getSecondaryTextByMessageType(lastMessage, conversationItem.isGroup()),
                    status, unreadCounter > 0 ? -1 : 0, lastMessage.getMessageTimestampOrder(), 0);

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
            if(newestMessage != null){
                newLastReadValue = newLastReadValue > newestMessage.getMessageTimestamp() ? newLastReadValue : newestMessage.getMessageTimestamp();
            }
        }
        MessageItem lastMessage = DatabaseHandler.getLastMessage(monkeyId);
        updateConversation(monkeyId, null, (lastMessage != null && !lastMessage.isIncomingMessage()) ? MonkeyConversation.ConversationStatus.sentMessageRead : MonkeyConversation.ConversationStatus.receivedMessage,
                0, -1, newLastReadValue);
    }

    @Override
    public void onNotificationReceived(String messageId, String senderId, String recipientId, JsonObject params, String datetime) {

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
    public void onLoadMoreMessages(final String conversationId, int currentMessageCount) {
        asyncDBHandler.getMessagePage(new GetMessagePageTask.OnQueryReturnedListener() {
            @Override
            public void onQueryReturned(List<MessageItem> messageItems) {
                if(messageItems.size() > 0){
                    if(monkeyChatFragment != null) {
                        monkeyChatFragment.addOldMessages(new ArrayList<MonkeyItem>(messageItems), messageItems.size() == 0);
                    }
                }
                else {
                    addOldMessagesFromServer(conversationId);
                }
            }
        }, conversationId, MESS_PERPAGE, currentMessageCount);
    }

    @Override
    public void onDestroyWithPendingMessages(@NotNull ArrayList<MOKMessage> errorMessages) {
        DatabaseHandler.markMessagesAsError(errorMessages);
    }

    @Override
    public void setChatFragment(@Nullable MonkeyChatFragment chatFragment) {
        Log.d("MainActivity", "set chat fragment");
        monkeyChatFragment = chatFragment;
    }

    @Override
    public void deleteChatFragment(@Nullable MonkeyChatFragment chatFragment) {
        if(monkeyChatFragment != null && monkeyChatFragment == chatFragment ){
            monkeyChatFragment = null;
        }
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
        if(!messages.isEmpty()) {
            long lastOpenValue = 0L;
            MonkeyItem lastItem = messages.get(messages.size() - 1);
            lastOpenValue = lastItem.getMessageTimestampOrder();
            //We don;t actually know if the conversation is a group but it's not important here.
            updateClosedConversation(conversationId, lastOpenValue,
                    DatabaseHandler.getSecondaryTextByMessageType(lastItem, false));
        }
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
        if(voiceNotePlayer != null)
            voiceNotePlayer.setupNotificationControl(new PlaybackNotification(R.mipmap.ic_launcher,
                    " Playing voice note"));
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
        }
        monkeyConversationsFragment.insertConversations(conversations, false);
    }

    @Override
    public void onConversationClicked(final @NotNull MonkeyConversation conversation) {

        activeConversationItem = (ConversationItem) conversation;
        List<MonkeyItem> messages = messagesMap.get(conversation.getConvId());
        if(messages!=null && !messages.isEmpty()){
            //Get initial messages from memory
            startChatWithMessages((ConversationItem) conversation, false);
        }
        else{
            //Get initial messages from DB
            asyncDBHandler.getMessagePage(new GetMessagePageTask.OnQueryReturnedListener() {
                @Override
                public void onQueryReturned(List<MessageItem> messageItems) {
                    messagesMap.put(conversation.getConvId(), new ArrayList<MonkeyItem>(messageItems));
                    startChatWithMessages((ConversationItem) conversation, false);
                }
            }, conversation.getConvId(), MESS_PERPAGE, 0);
        }
    }

    @Override
    public void setConversationsFragment(@Nullable MonkeyConversationsFragment monkeyConversationsFragment) {
        this.monkeyConversationsFragment = monkeyConversationsFragment;

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

    @Override
    public void onClickToolbar(@NotNull String monkeyID, @NotNull String name, @NotNull String lastSeen, @NotNull String avatarURL){
        if(monkeyInfoFragment == null){
            MonkeyInfoFragment infoFragment = MonkeyInfoFragment.Companion.newInfoInstance(monkeyChatFragment.getConversationId(), monkeyChatFragment.getConversationId().contains("G:"));
            monkeyFragmentManager.setInfoFragment(infoFragment);
            if(getCurrentFocus() != null){
                getCurrentFocus().clearFocus();
            }
        }
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
        if(monkeyChatFragment.getConversationId().contains("G:")){
            return groupData.getInfoList();
        }
        Iterator it = conversationsList.iterator();
        ArrayList<MonkeyInfo> infoList = new ArrayList<>();

        while(it.hasNext()){
            ConversationItem conversation = (ConversationItem)it.next();
            if(conversation.getConvId().contains("G:") && conversation.getGroupMembers().contains(myMonkeyID) && conversation.getGroupMembers().contains(conversationId)){
                infoList.add(conversation);
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
            activeConversationItem = (ConversationItem) infoItem;
            final ConversationItem conversation = (ConversationItem) infoItem;
            List<MonkeyItem> messages = messagesMap.get(conversation.getConvId());
            if(messages!=null && !messages.isEmpty()){
                startChatFromInfo(conversation, false);
            }else{
                //Get initial messages from DB
                asyncDBHandler.getMessagePage(new GetMessagePageTask.OnQueryReturnedListener() {
                    @Override
                    public void onQueryReturned(List<MessageItem> messageItems) {
                        messagesMap.put(conversation.getConvId(), new ArrayList<MonkeyItem>(messageItems));
                        startChatFromInfo(conversation, false);
                    }
                }, conversation.getConvId(), MESS_PERPAGE, 0);
            }
        }else{
            ArrayList<MonkeyConversation> conversations = new ArrayList<>(conversationsList);
            ConversationItem conversationUser = null;
            for(MonkeyConversation conv : conversations) {
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
                startChatFromInfo(conversationItem, true);
                DatabaseHandler.saveConversations(new ConversationItem[]{conversationItem});
                return;
            }
            final ConversationItem conversationUserCopy = conversationUser;
            List<MonkeyItem> messages = messagesMap.get(conversationUser.getConvId());
            if(messages!=null && !messages.isEmpty()){
                startChatFromInfo(conversationUser, false);
            }else{
                //Get initial messages from DB
                asyncDBHandler.getMessagePage(new GetMessagePageTask.OnQueryReturnedListener() {
                    @Override
                    public void onQueryReturned(List<MessageItem> messageItems) {
                        messagesMap.put(conversationUserCopy.getConvId(), new ArrayList<MonkeyItem>(messageItems));
                        startChatFromInfo(conversationUserCopy, false);
                    }
                }, conversationUser.getConvId(), MESS_PERPAGE, 0);
            }


        }
    }

    public void startChatFromInfo(ConversationItem chat, boolean hasReachedEnd){
        MonkeyChatFragment fragment = chat.isGroup() ?
                MonkeyChatFragment.Companion.newGroupInstance(chat.getConvId(), chat.getName(),
                        chat.getAvatarFilePath(), hasReachedEnd, chat.lastRead, chat.getGroupMembers()) :
                MonkeyChatFragment.Companion.newInstance(chat.getConvId(), chat.getName(),
                        chat.getAvatarFilePath(), hasReachedEnd, chat.lastRead);

        activeConversationItem = chat;
        monkeyFragmentManager.setChatFragmentFromInfo(fragment, initInputListener(), voiceNotePlayer);
        monkeyInfoFragment = null;
    }

    @Override
    public void onExitGroup(@NotNull String conversationId) {

        removeGroupMember(conversationId, myMonkeyID);
        DatabaseHandler.deleteConversation(conversationId);
        Iterator<MonkeyConversation> it = conversationsList.iterator();
        while (it.hasNext()) {
            if (it.next().getConvId().equals(conversationId)) {
                it.remove();
                break;
            }
        }
        monkeyFragmentManager.popStack(2);
    }

    public int getConversationFromList(String conversationId, ArrayList<MonkeyConversation> conversations){
        if(conversations == null){
            return -1;
        }

        Iterator<MonkeyConversation> it = conversations.iterator();
        int i = 0;
        while(it.hasNext()){
            if(it.next().getConvId().equals(conversationId)){
                return i;
            }
            i++;
        }

        return -1;
    }

    @Override
    public void removeMember(@NotNull String monkeyId) {
        if(monkeyChatFragment != null) {
            removeGroupMember(monkeyChatFragment.getConversationId(), monkeyId);
        }
    }

    @Override
    public void deleteAllMessages(@NotNull String conversationId) {
        DatabaseHandler.deleteAll(conversationId);
        if(monkeyChatFragment != null) monkeyChatFragment.clearMessages();
    }
}