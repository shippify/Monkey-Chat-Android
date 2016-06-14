package com.criptext.monkeychatandroid;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuInflater;

import com.criptext.ClientData;
import com.criptext.MonkeyKitSocketService;
import com.criptext.comunication.MOKMessage;
import com.criptext.comunication.MessageTypes;
import com.criptext.gcm.MonkeyRegistrationService;
import com.criptext.lib.MKDelegateActivity;
import com.criptext.lib.MonkeyKit;
import com.criptext.lib.MonkeyKitDelegate;
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

public class MainActivity extends MKDelegateActivity implements ChatActivity{

    MonkeyAdapter adapter;
    RecyclerView recycler;
    MediaInputView inputView;
    ArrayList<MonkeyItem> monkeyMessages;
    MessageLoader messageLoader;
    VoiceNotePlayer voiceNotePlayer;

    private SharedPreferences prefs;
    private String myMonkeyID;
    private  SensorHandler sensorHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        myMonkeyID = prefs.getString(MonkeyChat.MONKEY_ID, null);
        if(MonkeyRegistrationService.Companion.checkPlayServices(this))
                registerWithGCM();

        recycler = (RecyclerView) findViewById(R.id.recycler);
        monkeyMessages = new ArrayList<MonkeyItem>();
        adapter = new MonkeyAdapter(this, monkeyMessages);
        messageLoader = new MessageLoader(myMonkeyID, myMonkeyID);
        messageLoader.setAdapter(adapter);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        linearLayoutManager.setStackFromEnd(true);
        recycler.setLayoutManager(linearLayoutManager);
        recycler.setAdapter(adapter);

        initInputView();
        voiceNotePlayer = new DefaultVoiceNotePlayer(adapter, recycler);
        sensorHandler = new SensorHandler(voiceNotePlayer, this);
        messageLoader.loadNewPage();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorHandler.onDestroy();
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
                DatabaseHandler.deleteAll();
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
                    switch (MonkeyItem.MonkeyItemType.values()[item.getMessageType()]) {
                        case audio:
                            params = new JsonObject();
                            params.addProperty("length",""+item.getAudioDuration());
                            mokMessage = socketService.persistFileMessageAndSend(item.getFilePath(), myMonkeyID,
                                    MessageTypes.FileTypes.Audio, "Test Push Message", params);
                            break;
                        case photo:
                            mokMessage = socketService.persistFileMessageAndSend(item.getFilePath(), myMonkeyID,
                                    MessageTypes.FileTypes.Photo, "Test Push Message", new JsonObject());
                            break;
                        default:
                            mokMessage = socketService.persistMessageAndSend(item.getMessageText(), myMonkeyID, "Test Push Message", params);
                            break;
                    }

                    MessageItem newItem = new MessageItem(myMonkeyID, myMonkeyID, mokMessage.getMessage_id(),
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

    private void markMessageAsDelivered(MOKMessage message){
        MessageItem monkeyItem = (MessageItem) searchMessage(message.getOldId());
        if(monkeyItem != null) {
            monkeyItem.setStatus(MonkeyItem.OutgoingMessageStatus.delivered);
            DatabaseHandler.updateMessageOutgoingStatus(monkeyItem.model, MonkeyItem.OutgoingMessageStatus.delivered);
            adapter.notifyDataSetChanged();
        }
    }

    private void processIncomingMessage(MOKMessage message, boolean refresh){

        monkeyMessages.add(DatabaseHandler.createMessage(message, this, myMonkeyID, true));

        if(refresh) {
            adapter.notifyDataSetChanged();
            recycler.scrollToPosition(monkeyMessages.size() - 1);
        }
    }

    public MediaPlayer.OnCompletionListener localCompletionForProximity=new MediaPlayer.OnCompletionListener() {

        @Override
        public void onCompletion(MediaPlayer mp) {

        }
    };

    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK) {
            return;
        }

        if(inputView!=null && inputView.getCameraHandler()!=null)
            inputView.getCameraHandler().onActivityResult(requestCode,resultCode, data);

    }

    /***
     * OVERRIDE METHODS
     ****/

    @Override
    public boolean isOnline() {
        return true;
    }

    @Override
    public void onFileDownloadRequested(int position, @NotNull MonkeyItem item) {

        MessageItem messageItem = (MessageItem)searchMessage(item.getMessageId());
        final MonkeyKitSocketService socketService = getService();
        if(socketService !=null && messageItem != null && !messageItem.isDownloading()) {
            messageItem.setDownloading(true);
            DatabaseHandler.updateMessageDownloadingStatus(messageItem.model, true);
            socketService.downloadFile(messageItem.getMessageText(), messageItem.getProps().toString(),
                    myMonkeyID, new Runnable() {
                        @Override
                        public void run() {
                            adapter.notifyDataSetChanged();
                        }
                    });
        }
    }

    @Override
    public void onLoadMoreData(int i) {
        messageLoader.loadNewPage();
    }

    /******
     * MONKEY METHODS
     */

    @Override
    public void onNetworkError(Exception exception) {

    }

    @Override
    public void onSocketConnected() {

    }

    @Override
    public void onSocketDisconnected() {
        setActionBarTitle(1);
    }

    @Override
    public void onCreateGroupOK(String grupoID) {

    }

    @Override
    public void onCreateGroupError(String errmsg) {

    }

    @Override
    public void onDeleteGroupOK(String grupoID) {

    }

    @Override
    public void onDeleteGroupError(String errmsg) {

    }

    @Override
    public void onGetGroupInfoOK(JsonObject json) {

    }

    @Override
    public void onGetGroupInfoError(String errmsg) {

    }

    @Override
    public void onMessageRecieved(MOKMessage message) {

        final MonkeyKitSocketService socketService = getService();
        if(message.getSid().equals(myMonkeyID)){
            processIncomingMessage(message,true);
            socketService.setLastTimeSynced(Long.parseLong(message.getDatetime()));
        }

    }

    @Override
    public void onMessageBatchReady(ArrayList<MOKMessage> messages) {
        setActionBarTitle(2);
        final MonkeyKitSocketService socketService = getService();
        for (int i=0;i<messages.size();i++) {
            if(i == messages.size()-1) {
                processIncomingMessage(messages.get(i), true);
                socketService.setLastTimeSynced(Long.parseLong(messages.get(messages.size() - 1).getDatetime()));
            }
            else{
                processIncomingMessage(messages.get(i), false);
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
        final MonkeyKitSocketService socketService = getService();
        if(message.getMessage_id()!=null && message.getMessage_id().length()>0 && !message.getMessage_id().equals("0")){
            socketService.setLastTimeSynced(Long.parseLong(message.getDatetime()));
        }
    }

    @Override
    public void onContactOpenMyConversation(String sessionID) {

    }

    @Override
    public void onNotificationReceived(MOKMessage notification) {
        final MonkeyKitSocketService socketService = getService();
        if(notification.getMessage_id()!=null && notification.getMessage_id().length()>0 && !notification.getMessage_id().equals("0")){
            socketService.setLastTimeSynced(Long.parseLong(notification.getDatetime()));
        }
    }

    @NotNull
    @Override
    public ClientData getClientData() {
        return new ClientData(getIntent());
    }

    @NotNull
    @Override
    public Class<?> getServiceClassName() {
        return MyServiceClass.class;
    }

    @Override
    public void onBoundToService() {
        if(getService().isSocketConnected()) {
            //MonkeyKit.instance().onResume();
            setActionBarTitle(1);
        }
        else{
            setActionBarTitle(2);
        }


    }
}