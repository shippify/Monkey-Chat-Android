package com.criptext.monkeychatandroid;

import com.criptext.comunication.MOKMessage;
import com.criptext.lib.MonkeyKit;

import java.util.ArrayList;

/**
 * Created by Daniel Tigse on 4/19/16.
 */

public class MyServiceClass extends MonkeyKit{

    @Override
    public void storeMessage(MOKMessage message, boolean incoming, Runnable runnable) {
        runnable.run();
    }

    @Override
    public void storeMessageBatch(ArrayList<MOKMessage> messages, Runnable runnable) {
        runnable.run();
    }
}
