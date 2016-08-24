package com.criptext.monkeychatandroid;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
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
import com.criptext.monkeychatandroid.models.DatabaseHandler;
import com.criptext.monkeychatandroid.models.MessageItem;
import com.criptext.monkeychatandroid.models.MessageLoader;
import com.criptext.monkeykitui.MonkeyChatFragment;
import com.criptext.monkeykitui.MonkeyConversationsFragment;
import com.criptext.monkeykitui.conversation.ConversationsActivity;
import com.criptext.monkeykitui.conversation.MonkeyConversation;
import com.criptext.monkeykitui.input.listeners.InputListener;
import com.criptext.monkeykitui.recycler.ChatActivity;
import com.criptext.monkeykitui.recycler.MonkeyItem;
import com.criptext.monkeykitui.recycler.audio.DefaultVoiceNotePlayer;
import com.criptext.monkeykitui.recycler.audio.VoiceNotePlayer;
import com.criptext.monkeykitui.util.MonkeyFragmentManager;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import io.realm.Realm;

public class MainActivity extends MKDelegateActivity implements ChatActivity, ConversationsActivity{

    private static String DATA_FRAGMENT = "MainActivity.chatDataFragment";
    //Since this is the Chat activity, we need a RecyclerView and an adapter. Additionally we
    //will store the messages in our own list so that they can be accessed easily.
    MessageLoader messageLoader;
    MonkeyFragmentManager monkeyFragmentManager;

    MonkeyChatFragment monkeyChatFragment;
    MonkeyConversationsFragment monkeyConversationsFragment;
    HashMap<String, Collection<MonkeyItem>> messagesMap = new HashMap<>();
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getRetainedData();
        //First, initialize the database and the constants from SharedPreferences.
        initRealm();
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        myMonkeyID = prefs.getString(MonkeyChat.MONKEY_ID, null);
        Log.d("MonkeyId", myMonkeyID);
        myFriendID = myMonkeyID;

        //Check play services. if available try to register with GCM so that we get Push notifications
        if(MonkeyRegistrationService.Companion.checkPlayServices(this))
                registerWithGCM();


        //Create a voice note player so that it can play voice note messages in the recycler view
        //when the user clicks on them
        voiceNotePlayer = new DefaultVoiceNotePlayer(this);
        //Add a sensor handler so the activity can turn off the screen when the sensors detect that
        //the user is too close to the phone and change the audio output device.
        sensorHandler = new SensorHandler(voiceNotePlayer, this);
        //finally load one page of messages.

        monkeyFragmentManager = new MonkeyFragmentManager(this);
        monkeyFragmentManager.setContentLayout(savedInstanceState);
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
            messagesMap = retainedFragment.chatMap;
            if (monkeyChatFragment != null) {
                monkeyChatFragment.setInputListener(initInputListener());
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
        dataFragment.messageLoader = this.messageLoader;
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
                        getRetainedData();
                        params = new JsonObject();
                        params.addProperty("length",""+item.getAudioDuration());

                        mokMessage = persistFileMessageAndSend(item.getFilePath(), myMonkeyID, myFriendID,
                                MessageTypes.FileTypes.Audio, params, new PushMessage("Test Push Message"), true);
                        break;
                    case photo:
                        mokMessage = persistFileMessageAndSend(item.getFilePath(), myMonkeyID, myFriendID,
                                MessageTypes.FileTypes.Photo, new JsonObject(), new PushMessage("Test Push Message"), true);
                        break;
                    default:
                        mokMessage = persistMessageAndSend(item.getMessageText(), myMonkeyID,
                                myFriendID, params, new PushMessage("Test Push Message"), true);
                        break;
                }

                //Now that the message was sent, create a MessageItem using the MOKMessage that MonkeyKit
                //created. This MessageItem will be added to MonkeyAdapter so that it can be shown in
                //the screen.
                MessageItem newItem = new MessageItem(myMonkeyID, myFriendID, mokMessage.getMessage_id(),
                        item.getMessageText(), item.getMessageTimestamp(), item.isIncomingMessage(),
                        MonkeyItem.MonkeyItemType.values()[item.getMessageType()]);

                switch (MonkeyItem.MonkeyItemType.values()[item.getMessageType()]) {
                    case audio:
                        newItem.setDuration(item.getAudioDuration());
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

    public void addOldMessages(ArrayList<MonkeyItem> oldMessages, boolean hasReachedEnd){
        if(oldMessages != null && monkeyChatFragment != null)
            monkeyChatFragment.addOldMessages(oldMessages, hasReachedEnd);
        if(monkeyChatFragment != null)
            monkeyChatFragment.addOldMessages(new ArrayList<MonkeyItem>(), hasReachedEnd);
    }

    public void setActionBarTitle(int state){

        if(getSupportActionBar()==null)
            return;

        if(state==1){//Connecting
            getSupportActionBar().setTitle("Connecting...");
        }else if(state==2){//Connected
            getSupportActionBar().setTitle(getResources().getString(R.string.chat_name));
        }else if(state==3){//Without internet
            getSupportActionBar().setTitle("Waiting for network...");
        }
    }


    private void updateMessage(String id, MonkeyItem.DeliveryStatus newStatus) {
        DatabaseHandler.updateMessageOutgoingStatus(realm, id, newStatus);
        if (monkeyChatFragment != null) {
            MessageItem monkeyItem = (MessageItem) monkeyChatFragment.findMonkeyItemById(id);
            if (monkeyItem != null) {
                monkeyItem.setStatus(newStatus);
                monkeyChatFragment.rebindMonkeyItem(monkeyItem);
            }
        }
    }

    /**
     * Creates a new MonkeyChatFragment and adds it to the activity.
     * @param conversationId unique identifier of the conversation of the fragment
     * @param initialMessages a list of the first messages to draw in the chat
     * @param hasReachedEnd true of the initial messages are the only existing messages of the chat
     */
    public void startChatWithMessages(String conversationId, ArrayList<MonkeyItem> initialMessages,
                                      boolean hasReachedEnd){
        messagesMap.put(conversationId, initialMessages);
        MonkeyChatFragment fragment = MonkeyChatFragment.Companion.newInstance(conversationId, hasReachedEnd);
        monkeyFragmentManager.setChatFragment(fragment, initInputListener(), voiceNotePlayer);
    }
    /**
     * Updates a sent message and updates de UI so that the user can see that it has been
     * successfully delivered
     * @param oldId The old Id of the message.
     */
    private void markMessageAsDelivered(String oldId){
        updateMessage(oldId, MonkeyItem.DeliveryStatus.delivered);
    }

    /**
     * adds a message to the adapter so that it can be displayed in the RecyclerView.
     * @param message
     */
    private void processIncomingMessage(MOKMessage message){
        MessageItem newItem = DatabaseHandler.createMessage(message, this, myMonkeyID, true);
        if(monkeyChatFragment != null) {
            messageLoader.countNewMessage(message.getConversationID());
            monkeyChatFragment.smoothlyAddNewItem(newItem);
        }
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
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        DatabaseHandler.storeSendingMessage(realm, DatabaseHandler.createMessage(message, this, prefs.getString("sessionid", ""), true),
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
    }

    /******
     * These are the methods that MonkeyKit calls to inform us about new events.
     */

    @Override
    public void onSocketConnected() {
        super.onSocketConnected();
        setActionBarTitle(2);
    }

    @Override
    public void onSocketDisconnected() {
        setActionBarTitle(1);
    }

    @Override
    public void onFileDownloadFinished(String fileMessageId, boolean success) {
        //TODO use better search algorithm
        super.onFileDownloadFinished(fileMessageId, success);
        updateMessage(fileMessageId,
                success ? MonkeyItem.DeliveryStatus.delivered : MonkeyItem.DeliveryStatus.error);
    }

    @Override
    public void onAcknowledgeRecieved(@NotNull String senderId, @NotNull String recipientId,
                          @NotNull String newId, @NotNull String oldId, boolean read, int messageType) {
        super.onAcknowledgeRecieved(senderId, recipientId, newId, oldId, read, messageType);
        markMessageAsDelivered(oldId);
    }

    @Override
    public void onCreateGroup(String groupMembers, String groupName, String groupID, Exception e) {
        if(e != null)
            e.printStackTrace();
        else{

        }
    }

    @Override
    public void onAddGroupMember(String members, Exception e) {

    }

    @Override
    public void onRemoveGroupMember(String members, Exception e) {

    }

    @Override
    public void onUpdateUserData(Exception e) {

    }

    @Override
    public void onUpdateGroupData(Exception e) {

    }

    @Override
    public void onMessageRecieved(MOKMessage message) {
         processIncomingMessage(message);
    }

    @Override
    public void onMessageBatchReady(ArrayList<MOKMessage> messages) {
        setActionBarTitle(2);
        for (int i=0;i<messages.size();i++) {
            if(i == messages.size()-1) {
                processIncomingMessage(messages.get(i));
            }
            else{
                processIncomingMessage(messages.get(i));
            }
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

    }

    @Override
    public void onGetUserInfo(@NotNull MOKUser mokUser, @Nullable Exception e) {

    }

    @Override
    public void onGetUsersInfo(@NotNull ArrayList<MOKUser> mokUsers, @Nullable Exception e) {

    }

    @Override
    public void onGetConversations(@NotNull ArrayList<MOKConversation> conversations, @Nullable Exception e) {

    }

    @Override
    public void onGetConversationMessages(@NotNull ArrayList<MOKMessage> messages, @Nullable Exception e) {

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
    public void onContactOpenMyConversation(String sessionID) {   }

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

        if(getService().isSocketConnected()) {
            setActionBarTitle(2);
        }
        else{
            setActionBarTitle(1);
        }
    }

    @Override
    public  void onConnectionRefused(){
        setActionBarTitle(2);
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
            resendFile(resendMessage, new PushMessage("Test Push Message"), true);
        }

    }

    @Override
    public void onFileDownloadRequested(@NotNull MonkeyItem item) {

        if(item.getDeliveryStatus() == MonkeyItem.DeliveryStatus.error) {
            updateMessage(item.getMessageId(), MonkeyItem.DeliveryStatus.sending);
            if(monkeyChatFragment != null)
                monkeyChatFragment.rebindMonkeyItem(item);
        } else {
            final MessageItem messageItem = (MessageItem) item;
            downloadFile(messageItem.getMessageId(), messageItem.getFilePath(),
                    messageItem.getProps(), messageItem.getContactSessionId());
        }

    }

    @Override
    public void onLoadMoreData(int i) {
        //If the adapter requires to load older messages, delegate this task to our messageLoader object
        messageLoader.loadNewPage(realm);
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
    public Collection<MonkeyItem> getInitialMessages(String conversationId) {
        return messagesMap.get(conversationId);
    }

    @Override
    public void retainMessages(@NotNull String conversationId, @NotNull Collection<? extends MonkeyItem> messages) {
        messagesMap.put(conversationId, (Collection<MonkeyItem>) messages);
    }

    /** CONVERSATION ACTIVITY METHODS **/
    @Override
    public void requestConversations() {
        ArrayList<MonkeyConversation> conversations = new ArrayList<>();
        final long datetime = System.currentTimeMillis();
        conversations.add(new MonkeyConversation() {

            @NotNull
            @Override
            public String getGroupMembers() {
                return "";
            }

            @NotNull
            @Override
            public String getId() {
                return myMonkeyID;
            }

            @NotNull
            @Override
            public String getName() {
                return "Mirror Chat";
            }

            @Override
            public long getDatetime() {
                return datetime;
            }

            @NotNull
            @Override
            public String getSecondaryText() {
                return "Tap to talk to yourself";
            }

            @Override
            public int getTotalNewMessages() {
                return 0;
            }

            @Override
            public boolean isGroup() {
                return false;
            }

            @Nullable
            @Override
            public String getAvatarFilePath() {
                return null;
            }

            @Override
            public int getStatus() {
                return ConversationStatus.empty.ordinal();
            }
        });
        monkeyConversationsFragment.insertConversations(conversations);
    }

    @Override
    public void onConversationClicked(@NotNull MonkeyConversation conversation) {
        //Initialize the messageLoader
        messageLoader = new MessageLoader(conversation.getId(), myMonkeyID, this);
        messageLoader.loadFirstPage(realm);
    }

    @Override
    public void setConversationsFragment(@Nullable MonkeyConversationsFragment conversationsFragment) {
        monkeyConversationsFragment = conversationsFragment;

    }

}