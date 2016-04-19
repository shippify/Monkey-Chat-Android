package com.criptext.monkeychatandroid;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.criptext.comunication.MOKMessage;
import com.criptext.lib.MonkeyKit;
import com.criptext.lib.MonkeyKitDelegate;
import com.criptext.monkeykitui.input.ButtonsListeners;
import com.criptext.monkeykitui.input.InputView;
import com.criptext.monkeykitui.input.RecordingListeners;
import com.criptext.monkeykitui.recycler.ChatActivity;
import com.criptext.monkeykitui.recycler.MonkeyAdapter;
import com.criptext.monkeykitui.recycler.MonkeyItem;
import com.criptext.monkeykitui.recycler.audio.AudioPlaybackHandler;
import com.google.gson.JsonObject;
import com.soundcloud.android.crop.Crop;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements ChatActivity, MonkeyKitDelegate {

    MonkeyAdapter adapter;
    RecyclerView recycler;
    InputView inputView;
    ArrayList<MonkeyItem> monkeyMessages;
    AudioPlaybackHandler audioHandler;

    String mAudioFileName = null;
    String mPhotoFileName = null;
    File mPhotoFile = null;
    MediaRecorder mRecorder = null;

    public static final Uri CONTENT_URI = Uri.parse("content://com.criptext.monkeychatandroid/");
    public static final String TEMP_PHOTO_FILE_NAME = "temp_photo.jpg";
    public static final String TEMP_AUDIO_FILE_NAME = "temp_audio.3gp";

    public enum RequestType {openGallery, takePicture, editPhoto, cropPhoto}

    private int orientationImage;

    int playingItemPosition = -1;
    boolean playingAudio = false;
    Runnable playerRunnable = new Runnable() {
        @Override
        public void run() {
            if (playingAudio) {
                adapter.notifyItemChanged(playingItemPosition);
                recycler.postDelayed(this, 500);
            }
        }
    };

    private SharedPreferences prefs;
    private String mySessionID;

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

        inputView = (InputView) findViewById(R.id.inputView);
        recycler = (RecyclerView) findViewById(R.id.recycler);
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mySessionID = prefs.getString("sessionid","");

        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            mPhotoFile = new File(Environment.getExternalStorageDirectory(), TEMP_PHOTO_FILE_NAME);
        } else {
            mPhotoFile = new File(getFilesDir(), TEMP_PHOTO_FILE_NAME);
        }

        monkeyMessages = new ArrayList<MonkeyItem>();
        adapter = new MonkeyAdapter(this, monkeyMessages);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        linearLayoutManager.setStackFromEnd(true);
        recycler.setLayoutManager(linearLayoutManager);
        recycler.setAdapter(adapter);

        inputView.setOnRecordListener(new RecordingListeners() {
            @Override
            public void onStartRecording() {
                super.onStartRecording();
                startRecording();
            }

            @Override
            public void onStopRecording() {
                super.onStopRecording();
                stopRecording();
                sendAudioFile();
            }

            @Override
            public void onCancelRecording() {
                super.onCancelRecording();
                cancelRecording();
            }
        });

        inputView.setOnButtonsClickedListener(new ButtonsListeners() {

            @Override
            public void onAttachmentButtonClicked() {
                super.onAttachmentButtonClicked();
                selectImage();
            }

            @Override
            public void onSendButtonClicked(String text) {
                super.onSendButtonClicked(text);
                sendMessage(text);
            }
        });

        audioHandler = new AudioPlaybackHandler(adapter, recycler);

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
    protected void onStop() {
        audioHandler.releasePlayer();
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
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

    /***
     * MY OWN METHODS
     */

    private void sendMessage(String text){
        MOKMessage mokMessage= MonkeyKit.instance().sendMessage(text, mySessionID, "Test Push Message", new JsonObject());
        long timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 48;
        MessageItem item = new MessageItem(mySessionID, mokMessage.getMessage_id(), text, timestamp, false,
                MonkeyItem.MonkeyItemType.text);
        monkeyMessages.add(item);
        adapter.notifyDataSetChanged();
        recycler.scrollToPosition(monkeyMessages.size() - 1);
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
            //monkeyItem.setStatus ??
            monkeyItem.setStatus(MonkeyItem.OutgoingMessageStatus.delivered);
            adapter.notifyDataSetChanged();
        }
    }

    /***
     * AUDIO RECORD STUFFS
     ****/

    private void startRecording() {

        try {
            mAudioFileName = getCacheDir().toString() + "/" + (System.currentTimeMillis() / 1000) + TEMP_AUDIO_FILE_NAME;
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setOutputFile(mAudioFileName);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            //TO MAKE AUDIO LOW QUALITY
            mRecorder.setAudioSamplingRate(22050);//8khz-92khz
            mRecorder.setAudioEncodingBitRate(22050);//8000
            mRecorder.prepare();
            mRecorder.start();
            mRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {

                @Override
                public void onError(MediaRecorder mr, int what, int extra) {
                    if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
                        mr.release();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void stopRecording() {
        try {
            if (mRecorder != null) {
                mRecorder.stop();
                mRecorder.release();
                mRecorder = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cancelRecording() {
        stopRecording();
        File file = new File(mAudioFileName);
        if (file.exists())
            file.delete();
    }

    private void sendAudioFile() {

        File file = new File(mAudioFileName);
        if (file.exists()) {
            long timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 48;
            MessageItem item = new MessageItem("0", "" + timestamp,
                    mAudioFileName, timestamp, false,
                    MonkeyItem.MonkeyItemType.audio);
            item.setDuration("00:10");
            monkeyMessages.add(item);
            adapter.notifyDataSetChanged();
            recycler.scrollToPosition(monkeyMessages.size() - 1);
        }
    }

    /***
     * IMAGE RECORD STUFFS
     ****/

    private void selectImage() {

        mPhotoFileName = (System.currentTimeMillis() / 1000) + TEMP_PHOTO_FILE_NAME;

        final String[] items = new String[]{"Take a Photo", "Choose Photo"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.select_dialog_item, items);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if (item == 0) {
                    takePicture();
                } else if (item == 1) {
                    Crop.pickImage(MainActivity.this);
                }
                dialog.dismiss();
            }
        }).show();

    }

    public void takePicture() {

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            Uri mImageCaptureUri;
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                mImageCaptureUri = Uri.fromFile(getTempFile());
            } else {
				/*
				 * The solution is taken from here: http://stackoverflow.com/questions/10042695/how-to-get-camera-result-as-a-uri-in-data-folder
				 */
                mImageCaptureUri = CONTENT_URI;
            }
            intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, mImageCaptureUri);
            intent.putExtra("return-data", true);
            startActivityForResult(intent, RequestType.takePicture.ordinal());
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    public File getTempFile() {

        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return new File(Environment.getExternalStorageDirectory(), mPhotoFileName);
        } else {
            return new File(getFilesDir(), mPhotoFileName);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK) {
            return;
        }

        if (requestCode == Crop.REQUEST_PICK)
            requestCode = RequestType.openGallery.ordinal();
        if (requestCode == Crop.REQUEST_CROP)
            requestCode = RequestType.cropPhoto.ordinal();

        Uri destination = Uri.fromFile(getTempFile());

        switch (RequestType.values()[requestCode]) {
            case openGallery: {
                try {
                    ExifInterface ei = new ExifInterface(getTempFile().getAbsolutePath());
                    orientationImage = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                } catch (IOException e) {
                    Log.e("error", "Exif error");
                }
                Crop.of(data.getData(), destination).start(this);
                break;
            }
            case takePicture: {
                try {
                    ExifInterface ei = new ExifInterface(getTempFile().getAbsolutePath());
                    orientationImage = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                } catch (IOException e) {
                    Log.e("error", "Exif error");
                }
                Crop.of(destination, destination).start(this);
                break;
            }
            case cropPhoto: {

                int rotation = 0;
                if (orientationImage == 0) {
                    try {
                        ExifInterface ei = new ExifInterface(getTempFile().getAbsolutePath());
                        orientationImage = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                    } catch (IOException e) {
                        Log.e("error", "Exif error");
                    }
                }
                if (orientationImage != 0) {
                    switch (orientationImage) {
                        case 3: { // ORIENTATION_ROTATE_180
                            rotation = 180;
                        }
                        break;
                        case 6: { // ORIENTATION_ROTATE_90
                            rotation = 90;
                        }
                        break;
                        case 8: { // ORIENTATION_ROTATE_270
                            rotation = 270;
                        }
                        break;
                    }
                }
                if (rotation != 0) {
                    Bitmap bmp = BitmapFactory.decodeFile(getTempFile().getAbsolutePath());
                    Matrix matrix = new Matrix();
                    matrix.postRotate(rotation);
                    Bitmap rotatedImg = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
                    bmp.recycle();

                    try {
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        rotatedImg.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                        byte[] bitmapdata = bos.toByteArray();
                        FileOutputStream fos = new FileOutputStream(getTempFile());
                        fos.write(bitmapdata);
                        fos.flush();
                        fos.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                long timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 48;
                MessageItem item = new MessageItem("0", "" + timestamp,
                        getTempFile().getAbsolutePath(), timestamp, false,
                        MonkeyItem.MonkeyItemType.photo);
                monkeyMessages.add(item);
                adapter.notifyDataSetChanged();
                recycler.scrollToPosition(monkeyMessages.size() - 1);

                break;
            }
        }
    }

    /***
     * OVERRIDE METHODS
     ****/

    @Override
    public int getMemberColor(@NotNull String sessionId) {
        return Color.WHITE;
    }

    @NotNull
    @Override
    public String getMenberName(@NotNull String sessionId) {
        return "Unknown";
    }

    @Override
    public boolean isGroupChat() {
        return false;
    }

    @Override
    public boolean isOnline() {
        return true;
    }

    @Override
    public void onFileDownloadRequested(int position, @NotNull MonkeyItem item) {

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

            MonkeyKit.instance().setLastTimeSynced(Long.parseLong(message.getDatetime()));

            long timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 48;
            MessageItem item = new MessageItem(mySessionID, message.getMessage_id(), message.getMsg(), timestamp, true,
                    MonkeyItem.MonkeyItemType.text);
            monkeyMessages.add(item);
            adapter.notifyDataSetChanged();
            recycler.scrollToPosition(monkeyMessages.size() - 1);
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

    @Override
    public void onMessageBatchReady(ArrayList<MOKMessage> messages) {
        setActionBarTitle(2);
        if(messages.size() > 0)
            MonkeyKit.instance().setLastTimeSynced(Long.parseLong(messages.get(messages.size() - 1).getDatetime()));
    }
}