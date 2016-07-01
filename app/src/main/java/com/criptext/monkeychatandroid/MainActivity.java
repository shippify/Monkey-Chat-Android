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

import com.criptext.ClientData;
import com.criptext.MonkeyKitSocketService;
import com.criptext.comunication.MOKMessage;
import com.criptext.comunication.MessageTypes;
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
        myFriendID = "iq2halrqq519fgko9di8jjor";

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
        recycler.setAdapter(adapter);

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
    protected void onStart(){
        super.onStart();
        initRealm();
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

                    final MonkeyKitSocketService socketService = getService();
                    if (socketService == null) {
                        Log.e("MainActivity", "Can't send message. service is null");
                        return;
                    }

                    JsonObject params = new JsonObject();
                    MOKMessage mokMessage;

                    //Store the message in the DB and send it via MonkeyKit
                    switch (MonkeyItem.MonkeyItemType.values()[item.getMessageType()]) {
                        case audio:
                            params = new JsonObject();
                            params.addProperty("length",""+item.getAudioDuration());

                            mokMessage = socketService.persistFileMessageAndSend(item.getFilePath(), myFriendID,
                                    MessageTypes.FileTypes.Audio, new PushMessage("Test Push Message"), params);
                            break;
                        case photo:
                            mokMessage = socketService.persistFileMessageAndSend(item.getFilePath(), myFriendID,
                                    MessageTypes.FileTypes.Photo, new PushMessage("Test Push Message"), new JsonObject());
                            break;
                        default:
                            mokMessage = socketService.persistMessageAndSend(item.getMessageText(),
                                    myFriendID, new PushMessage("Test Push Message"), params);
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

    /**
     * Updates a sent message and updates de UI so that the user can see that it has been
     * successfully delivered
     * @param message The sent message.
     */
    private void markMessageAsDelivered(MOKMessage message){
        MessageItem monkeyItem = (MessageItem) searchMessage(message.getOldId());
        if(monkeyItem != null) {
            monkeyItem.setStatus(MonkeyItem.OutgoingMessageStatus.delivered);
            DatabaseHandler.updateMessageOutgoingStatus(realm, monkeyItem.model, MonkeyItem.OutgoingMessageStatus.delivered);
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * adds a message to the adapter so that it can be displayed in the RecyclerView.
     * @param message
     */
    private void processIncomingMessage(MOKMessage message){
        Log.d("MainActivity", "Received " + message.getMessage_id());
        MessageItem newItem = DatabaseHandler.createMessage(message, this, myMonkeyID, true);
        adapter.smoothlyAddNewItem(newItem, recycler);
    }

    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK) {
            return;
        }
        //Since our InputView uses different activities to take and edit photos, we must forward
        // the onActivityResult event to it so that it can react to the results of take photo, choose
        //photo and edit photo.
        if(inputView!=null)
            inputView.getCameraHandler().onActivityResult(requestCode,resultCode, data);

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
    public void onFileDownloadRequested(int position, @NotNull MonkeyItem item) {

        MessageItem messageItem = (MessageItem) item;
        final MonkeyKitSocketService socketService = getService();
        if(socketService !=null && !messageItem.isDownloading()) {
            messageItem.setDownloading(true); //Make the bubble show a downloading animation
            DatabaseHandler.updateMessageDownloadingStatus(realm, messageItem.model, true);
            //The socket service will download the file for us and let us add callback to execute when it finishes
            socketService.downloadFile(messageItem.getFilePath(), messageItem.getProps().toString(),
                    myMonkeyID, new Runnable() {
                        @Override
                        public void run() {
                            //When then Download is done, update the RecyclerView.
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

    /******
     * These are the methods that MonkeyKit calls to inform us about new events.
     */

    @Override
    public void onSocketConnected() {  }

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
    public void onAcknowledgeRecieved(MOKMessage message) {

        Log.d("MainActivity", "Ack received");
        int tipo = Integer.parseInt(message.getType());
        switch (tipo) {
            case 1:
            case 2:
                markMessageAsDelivered(message);
                break;
        }

    }

    @Override
    public void onDeleteRecieved(MOKMessage message) {
        //Server requested to delete a message, we could implement it here, but for now we wont
    }

    @Override
    public void onContactOpenMyConversation(String sessionID) {   }

    @Override
    public void onNotificationReceived(MOKMessage notification) {   }

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
            setActionBarTitle(1);
        }
        else{
            setActionBarTitle(2);
        }
    }
}