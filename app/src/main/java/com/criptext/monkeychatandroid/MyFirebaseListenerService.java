package com.criptext.monkeychatandroid;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import com.criptext.firebase.MonkeyFirebaseListenerService;

import org.jetbrains.annotations.NotNull;

import static android.content.Context.NOTIFICATION_SERVICE;


public class MyFirebaseListenerService extends MonkeyFirebaseListenerService {
    final int myNotificationID = 65489;
    static long lastNotificationTime = 0L;
    final static long timeBetweenNotifications = 10000L;

    @Override
    public Class<?> getSocketServiceClass() {
        return MyServiceClass.class;
    }

    @Override
    public void createLocalizedNotification(@NotNull String key, @NotNull String[] args) {

    }

    @Override
    public void createSimpleNotification(@NotNull String message) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        PendingIntent pIntent = PendingIntent.getActivity(this, 89, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
        String title = "MonkeyChat Sample";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder .setContentTitle(title)
                .setContentText(message)
                .setTicker(message)
                .setSmallIcon(R.drawable.ic_send_black_24dp)
                .setContentIntent(pIntent)
                .setAutoCancel(false)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message)
                        .setBigContentTitle(title));

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            builder.setColor(Color.parseColor("#0276a9"));

        Notification n = builder.build();

        long newNotificationTime = System.currentTimeMillis();
        if(newNotificationTime - lastNotificationTime > timeBetweenNotifications) {
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            n.sound = alarmSound;
            long[] vibrate = {0, 200, 0, 200};
            n.vibrate = vibrate;
            lastNotificationTime = newNotificationTime;
        }

        n.ledARGB = Color.BLUE;
        n.flags = Notification.FLAG_SHOW_LIGHTS | Notification.FLAG_AUTO_CANCEL;
        n.ledOnMS = 1000;
        n.ledOffMS = 1000;

        NotificationManager notificationManager = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(myNotificationID, n);
    }
}
