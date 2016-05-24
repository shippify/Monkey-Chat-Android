package com.criptext.monkeychatandroid;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuInflater;

import com.criptext.comunication.MOKMessage;
import com.criptext.comunication.MessageTypes;
import com.criptext.lib.MonkeyKit;
import com.criptext.lib.MonkeyKitDelegate;
import com.criptext.monkeychatandroid.location.MyAdapter;
import com.criptext.monkeychatandroid.models.DatabaseHandler;
import com.criptext.monkeychatandroid.models.MessageItem;
import com.criptext.monkeychatandroid.models.MessageLoader;
import com.criptext.monkeykitui.input.MediaInputView;
import com.criptext.monkeykitui.input.children.AttachmentButton;
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

public class MainActivity extends AppCompatActivity implements ChatActivity, MonkeyKitDelegate {

    MyAdapter adapter;
    RecyclerView recycler;
    MediaInputView inputView;
    ArrayList<MonkeyItem> monkeyMessages;
    MessageLoader messageLoader;
    VoiceNotePlayer voiceNotePlayer;

    private SharedPreferences prefs;
    private String mySessionID;
    private  SensorHandler sensorHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(MonkeyKit.instance()==null){
            finish();
            startActivity(new Intent(MainActivity.this, WelcomeActivity.class));
        }
        else{
            MonkeyKit.instance().addDelegate(this);
        }

        recycler = (RecyclerView) findViewById(R.id.recycler);
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mySessionID = prefs.getString("sessionid","");

        monkeyMessages = new ArrayList<MonkeyItem>();
        adapter = new MyAdapter(this, monkeyMessages);
        messageLoader = new MessageLoader(mySessionID, mySessionID);
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

    @Override
    protected void onResume() {
        super.onResume();

        if(MonkeyKit.instance()!=null && MonkeyKit.instance().isInialized() && !MonkeyKit.instance().monkeyIsConnected()) {
            MonkeyKit.instance().onResume();
            setActionBarTitle(1);
        }
        else{
            setActionBarTitle(2);
        }

        if(MonkeyKit.instance()!=null)
            MonkeyKit.instance().sendSync(MonkeyKit.instance().getLastTimeSynced());
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

                    JsonObject params = new JsonObject();
                    MOKMessage mokMessage;
                    switch (MonkeyItem.MonkeyItemType.values()[item.getMessageType()]) {
                        case audio:
                            params = new JsonObject();
                            params.addProperty("length",""+item.getAudioDuration());
                            mokMessage = MonkeyKit.instance().persistFileMessageAndSend(item.getFilePath(), mySessionID,
                                    MessageTypes.FileTypes.Audio, params, "Test Push Message");
                            break;
                        case photo:
                            mokMessage = MonkeyKit.instance().persistFileMessageAndSend(item.getFilePath(), mySessionID,
                                    MessageTypes.FileTypes.Photo, new JsonObject(), "Test Push Message");
                            break;
                        default:
                            mokMessage = MonkeyKit.instance().persistMessageAndSend(item.getMessageText(), mySessionID, "Test Push Message", params);
                            break;
                    }

                    MessageItem newItem = new MessageItem(mySessionID, mySessionID, mokMessage.getMessage_id(),
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
            inputView.getAttachmentHandler().addNewAttachmentButton(new AttachmentButton() {
                @NonNull
                @Override
                public String getTitle() {
                    return "Send Location";
                }

                @Override
                public void clickButton() {
                    MessageItem newItem = new MessageItem(mySessionID, mySessionID, "-"+System.currentTimeMillis(),
                            "Location", System.currentTimeMillis(), false, 8);
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

        monkeyMessages.add(DatabaseHandler.createMessage(message, this, mySessionID, true));

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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
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
        if(MonkeyKit.instance()!=null && messageItem!=null && !messageItem.isDownloading()) {
            messageItem.setDownloading(true);
            DatabaseHandler.updateMessageDownloadingStatus(messageItem.model, true);
            MonkeyKit.instance().downloadFile(messageItem.getMessageText(), messageItem.getProps(),
                    mySessionID, new Runnable() {
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

        if(message.getSid().equals(mySessionID)){
            processIncomingMessage(message,true);
            MonkeyKit.instance().setLastTimeSynced(Long.parseLong(message.getDatetime()));
        }

    }

    @Override
    public void onMessageBatchReady(ArrayList<MOKMessage> messages) {
        setActionBarTitle(2);
        for (int i=0;i<messages.size();i++) {
            if(i == messages.size()-1) {
                processIncomingMessage(messages.get(i), true);
                MonkeyKit.instance().setLastTimeSynced(Long.parseLong(messages.get(messages.size() - 1).getDatetime()));
            }
            else{
                processIncomingMessage(messages.get(i), false);
            }
        }
    }

    @Override
    public void onAcknowledgeRecieved(MOKMessage message) {

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
        if(message.getMessage_id()!=null && message.getMessage_id().length()>0 && !message.getMessage_id().equals("0")){
            MonkeyKit.instance().setLastTimeSynced(Long.parseLong(message.getDatetime()));
        }
    }

    @Override
    public void onContactOpenMyConversation(String sessionID) {

    }

    @Override
    public void onNotificationReceived(MOKMessage notification) {
        if(notification.getMessage_id()!=null && notification.getMessage_id().length()>0 && !notification.getMessage_id().equals("0")){
            MonkeyKit.instance().setLastTimeSynced(Long.parseLong(notification.getDatetime()));
        }
    }

}