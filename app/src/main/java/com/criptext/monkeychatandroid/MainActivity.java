package com.criptext.monkeychatandroid;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuInflater;
import android.widget.Toast;

import com.criptext.ClientData;
import com.criptext.MonkeyKitSocketService;
import com.criptext.comunication.MOKMessage;
import com.criptext.comunication.MessageTypes;
import com.criptext.comunication.MonkeyHttpResponse;
import com.criptext.comunication.PushMessage;
import com.criptext.gcm.MonkeyRegistrationService;
import com.criptext.lib.MKDelegateActivity;
import com.criptext.monkeychatandroid.gcm.SampleRegistrationService;
import com.criptext.monkeychatandroid.models.DatabaseHandler;
import com.criptext.monkeychatandroid.models.MessageItem;
import com.criptext.monkeychatandroid.models.MessageLoader;
import com.criptext.monkeykitui.input.MediaInputView;
import com.criptext.monkeykitui.input.listeners.InputListener;
import com.criptext.monkeykitui.recycler.ChatActivity;
import com.criptext.monkeykitui.recycler.MonkeyAdapter;
import com.criptext.monkeykitui.recycler.MonkeyItem;
import com.criptext.monkeykitui.recycler.audio.DefaultVoiceNotePlayer;
import com.criptext.monkeykitui.recycler.audio.VoiceNotePlayer;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;

import io.realm.Realm;

public class MainActivity extends MKDelegateActivity implements ChatActivity{

    //Since this is the Chat activity, we need a RecyclerView and an adapter. Additionally we
    //will store the messages in our own list so that they can be accessed easily.
    MonkeyAdapter adapter;
    RecyclerView recycler;
    ArrayList<MonkeyItem> monkeyMessages;

    MediaInputView inputView;
    MessageLoader messageLoader;
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
        setContentView(R.layout.activity_main);


        //First, initialize the database and the constants from SharedPreferences.
        initRealm();
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        myMonkeyID = prefs.getString(MonkeyChat.MONKEY_ID, null);
        myFriendID = myMonkeyID;

        //Check play services. if available try to register with GCM so that we get Push notifications
        if(MonkeyRegistrationService.Companion.checkPlayServices(this))
                registerWithGCM();

        //Initialize the recycler view with a MonkeyAdapter.
        recycler = (RecyclerView) findViewById(R.id.recycler);
        monkeyMessages = new ArrayList<MonkeyItem>();
        adapter = new MonkeyAdapter(this, monkeyMessages);
        messageLoader = new MessageLoader(myMonkeyID, myMonkeyID);
        messageLoader.setAdapter(adapter);

        //Since our recyclerView is a list, use a LinearLayoutManager.
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        linearLayoutManager.setStackFromEnd(true);
        recycler.setLayoutManager(linearLayoutManager);

        //Initialize the inputview so that the user can send messages,
        initInputView();

        //Create a voice note player so that it can play voice note messages in the recycler view
        //when the user clicks on them
        voiceNotePlayer = new DefaultVoiceNotePlayer(adapter, recycler);
        //Add a sensor handler so the activity can turn off the screen when the sensors detect that
        //the user is too close to the phone and change the audio output device.
        sensorHandler = new SensorHandler(voiceNotePlayer, this);
        //finally load one page of messages.
        messageLoader.loadNewPage(realm);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorHandler.onDestroy();
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
        inflater.inflate(R.menu.menu_chat, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_deleteall:
                DatabaseHandler.deleteAll(realm);
                monkeyMessages.clear();
                adapter.notifyDataSetChanged();
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
    public void initInputView(){
        inputView = (MediaInputView) findViewById(R.id.inputView);
        if(inputView!=null) {
            inputView.setInputListener(new InputListener() {
                @Override
                public void onNewItem(@NotNull MonkeyItem item) {

                    JsonObject params = new JsonObject();
                    MOKMessage mokMessage;

                    //Store the message in the DB and send it via MonkeyKit
                    switch (MonkeyItem.MonkeyItemType.values()[item.getMessageType()]) {
                        case audio:
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
                    adapter.smoothlyAddNewItem(newItem, recycler); // Add to recyclerView
                }
            });
        }
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

    /**
     * Searches a message with a particular Id. The search is lineal.
     * @return the requested message. if it does not exist, returns null.
     */
    private MonkeyItem searchMessage(String messageId){
        Iterator<MonkeyItem> iterator = monkeyMessages.iterator();
        while(iterator.hasNext()) {
            MonkeyItem monkeyItem = iterator.next();
            if(monkeyItem.getMessageId().equals(messageId)) {
                return monkeyItem;
            }
        }
        return null;
    }

    private void updateMessage(String id, MonkeyItem.DeliveryStatus newStatus){
        MessageItem monkeyItem = (MessageItem) searchMessage(id);
        if(monkeyItem != null) {
            monkeyItem.setStatus(newStatus);
            DatabaseHandler.updateMessageOutgoingStatus(realm, monkeyItem.model, newStatus);
            adapter.notifyDataSetChanged();
        }
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
        adapter.smoothlyAddNewItem(newItem, recycler);
    }

    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK) {
            return;
        }

        initRealm();
        //Since our InputView uses different activities to take and edit photos, we must forward
        // the onActivityResult event to it so that it can react to the results of take photo, choose
        //photo and edit photo.
        if(inputView!=null)
            inputView.getCameraHandler().onActivityResult(requestCode,resultCode, data);

    }

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

    /***
     * Chat activity methods. this are the methods that MonkeyAdapter calls depending on user
     * interaction with the recyclerView that displays the messages.
     ****/

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
            adapter.rebindMonkeyItem(item, recycler);
        }
        resendFile(item.getMessageId());
    }

    @Override
    public void onFileDownloadRequested(@NotNull MonkeyItem item) {

        final MessageItem messageItem = (MessageItem) item;
        final MonkeyKitSocketService socketService = getService();
        if(socketService !=null && !messageItem.isDownloading()) {
            messageItem.setDownloading(true); //Make the bubble show a downloading animation
            DatabaseHandler.updateMessageDownloadingStatus(realm, messageItem.model, true);
            //The socket service will download the file for us and let us add callback to execute when it finishes
            socketService.downloadFile(messageItem.getFilePath(), messageItem.getProps().toString(),
                    myMonkeyID, new MonkeyHttpResponse() {
                        @Override
                        public void OnSuccess() {
                            //When then Download is done, update the RecyclerView.
                            updateMessage(messageItem.getMessageId(), MonkeyItem.DeliveryStatus.delivered);
                            adapter.notifyDataSetChanged();
                        }

                        @Override
                        public void OnError() {
                            adapter.notifyDataSetChanged();
                        }
                    });
        }
    }

    @Override
    public void onLoadMoreData(int i) {
        //If the adapter requires to load older messages, delegate this task to our messageLoader object
        messageLoader.loadNewPage(realm);
    }

    @Override
    public void onNetworkError(Exception exception) {

    }

    /******
     * These are the methods that MonkeyKit calls to inform us about new events.
     */

    @Override
    public void onSocketConnected() {
        super.onSocketConnected();
        setActionBarTitle(2); }

    @Override
    public void onSocketDisconnected() {
        setActionBarTitle(1);
    }

    @Override
    public void onCreateGroupOK(String grupoID) {    }

    @Override
    public void onCreateGroupError(String errmsg) {    }

    @Override
    public void onDeleteGroupOK(String grupoID) {    }

    @Override
    public void onDeleteGroupError(String errmsg) {    }

    @Override
    public void onGetGroupInfoOK(JsonObject json) {     }

    @Override
    public void onGetGroupInfoError(String errmsg) {     }

    @Override
    public void onMessageRecieved(MOKMessage message) {
         processIncomingMessage(message);
    }

    @Override
    public void onMessageBatchReady(ArrayList<MOKMessage> messages) {
        setActionBarTitle(2);
        final MonkeyKitSocketService socketService = getService();
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
    public void onFileFailsUpload(MOKMessage message) {
        updateMessage(message.getMessage_id(), MonkeyItem.DeliveryStatus.error);
    }

    @Override
    public void onAcknowledgeRecieved(String senderId, String recipientId, String newId, String oldId, Boolean read, int messageType) {

        markMessageAsDelivered(oldId);

    }

    @Override
    public void onConversationOpenResponse(String senderId, Boolean isOnline, String lastSeen, String lastOpenMe) {

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

        recycler.setAdapter(adapter);
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
}