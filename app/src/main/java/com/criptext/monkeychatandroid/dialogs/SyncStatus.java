package com.criptext.monkeychatandroid.dialogs;

import android.os.Handler;

import com.criptext.monkeykitui.util.MonkeyFragmentManager;
import com.criptext.monkeykitui.util.Utils;

import java.lang.ref.WeakReference;

/**
 * Created by gesuwall on 12/1/16.
 */
public class SyncStatus {
    WeakReference<MonkeyFragmentManager> managerRef;
    Handler handler;
    private boolean cancelled;
    private long startTime;
    private final long WAIT_TIME = 3000L;

    public SyncStatus(MonkeyFragmentManager fragmentManager) {
        managerRef = new WeakReference<>(fragmentManager);
        handler = new Handler();
    }

    public void delayConnectingMessage() {
        cancelled = false;
        startTime = System.currentTimeMillis();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!cancelled) {
                    MonkeyFragmentManager manager = managerRef.get();
                    if (manager != null) {
                        manager.showStatusNotification(Utils.ConnectionStatus.connecting);
                    }
                }
            }
        }, WAIT_TIME);
    }

    public void cancelMessage() {
        cancelled = true;
        if(System.currentTimeMillis() - startTime > WAIT_TIME) {
            MonkeyFragmentManager manager = managerRef.get();
            if (manager != null) {
                manager.showStatusNotification(Utils.ConnectionStatus.connected);
            }
        }
    }


}
