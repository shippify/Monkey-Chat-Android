package com.criptext.comunication;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.criptext.MsgSenderService;
import com.criptext.lib.KeyStoreCriptext;
import com.criptext.socket.SecureSocketService;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * A handler for MonkeyKitSocketService to recieve MOKMessages from a background thread
 * Created by gesuwall on 6/2/16.
 */
public class MOKMessageHandler extends Handler {
    private WeakReference<MsgSenderService> serviceRef;

        public MOKMessageHandler(MsgSenderService service){
            serviceRef = new WeakReference<>(service);
        }

        public void handleMessage(Message msg) {

            MOKMessage message = null;
            if(msg.obj instanceof MOKMessage){
                message=(MOKMessage)msg.obj;
            }

            //if(message != null && message.getMsg() != null)
            Log.d("MOKMonkeyHandler", "message: " + " tipo: " + msg.what);
            final MsgSenderService service = serviceRef.get();
            if(service != null)
                switch (msg.what) {
                    case MessageTypes.MOKProtocolMessage:
                        int type = 0;
                        if(message.getProps() != null) {
                            if (message.getProps().has("file_type")) {
                                type = message.getProps().get("file_type").getAsInt();
                                if (type <= 4 && type >= 0)
                                    service.executeInDelegate(CBTypes.onMessageReceived, new Object[]{message});
                                else
                                    System.out.println("MONKEY - archivo no soportado");
                            } else if (message.getProps().has("type")) {
                                type = message.getProps().get("type").getAsInt();
                                if (type == 2 || type == 1)
                                    service.executeInDelegate(CBTypes.onMessageReceived, new Object[]{message});
                            } else if (message.getProps().has("monkey_action")) {
                                type = message.getProps().get("monkey_action").getAsInt();
                                message.setMonkeyAction(type);
                                service.executeInDelegate(CBTypes.onNotificationReceived, new Object[]{message});
                            } else
                                service.executeInDelegate(CBTypes.onNotificationReceived, new Object[]{message});
                        }
                        break;
                    case MessageTypes.MOKProtocolMessageBatch:
                        service.executeInDelegate(CBTypes.onMessageBatchReady, new Object[]{(ArrayList<MOKMessage>)msg.obj});
                        break;
                    case MessageTypes.MOKProtocolMessageHasKeys:
                        service.executeInDelegate(CBTypes.onMessageReceived, new Object[]{message});
                        break;
                    case MessageTypes.MOKProtocolMessageNoKeys:
                    case MessageTypes.MOKProtocolMessageWrongKeys:
                        service.requestKeysForMessage(message);
                        break;
                    case MessageTypes.MOKProtocolAck:
                        try {
                            System.out.println("MOK ack 205");
                            //The acknowledge message has the id of the successfully sent message in
                            //the Msg field. We'll use that to update our pending messages list.
                            service.removePendingMessage(message.getMsg());
                            service.executeInDelegate(CBTypes.onAcknowledgeReceived, new Object[]{message});
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case MessageTypes.MOKProtocolOpen:{
                        if(KeyStoreCriptext.getString(service.getContext(), message.getRid()).compareTo("")==0)
                            service.sendOpenConversation(message.getRid());
                        else
                            System.out.println("MONKEY - llego open pero ya tengo las claves");
                        //MANDAR AL APP QUE PONGA LEIDO TODOS LOS MENSAJES
                        service.executeInDelegate(CBTypes.onContactOpenMyConversation, new Object[]{message.getSid()});
                        break;
                    }
                    case MessageTypes.MOKProtocolDelete:{
                        service.executeInDelegate(CBTypes.onDeleteReceived, new Object[]{message});
                        break;
                    }
                    case MessageTypes.MessageSocketConnected:{
                        service.executeInDelegate(CBTypes.onSocketConnected, new Object[]{""});
                        break;
                    }
                    case MessageTypes.MessageSocketDisconnected:{
                        service.executeInDelegate(CBTypes.onSocketDisconnected, new Object[]{""});//new Object[]{""}
                        //If socket disconnected and this handler is still alive we should reconnect
                        //immediately.
                        service.startSocketConnection();
                        break;
                    }
                    case MessageTypes.MOKProtocolGet: {
                        service.executeInDelegate(CBTypes.onNotificationReceived, new Object[]{message});
                        break;
                    }
                    case MessageTypes.MOKProtocolSync: {
                        service.executeInDelegate(CBTypes.onNotificationReceived, new Object[]{message});
                        break;
                    }
                    default:
                        break;
                }

        }
}