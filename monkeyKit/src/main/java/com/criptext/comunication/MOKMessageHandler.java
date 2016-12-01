package com.criptext.comunication;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.criptext.MonkeyKitSocketService;
import com.criptext.http.OpenConversationTask;
import com.criptext.lib.KeyStoreCriptext;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * A handler for MonkeyKitSocketService to recieve MOKMessages from a background thread
 * Created by gesuwall on 6/2/16.
 */
public class MOKMessageHandler extends Handler {
    private WeakReference<MonkeyKitSocketService> serviceRef;

        public MOKMessageHandler(MonkeyKitSocketService service){
            serviceRef = new WeakReference<>(service);
        }

        public void clearServiceReference(){
            serviceRef.clear();
        }
        public void handleMessage(Message msg) {

            MOKMessage message = null;
            if(msg.obj instanceof MOKMessage){
                message=(MOKMessage)msg.obj;
            }

            //if(message != null && message.getMsg() != null)
            Log.d("MOKMonkeyHandler", "message: " + " tipo: " + msg.what);
            final MonkeyKitSocketService service = serviceRef.get();
            if(service != null)
                switch (msg.what) {
                    case MessageTypes.MOKProtocolMessage:
                        int type = 0;
                        if(message.getProps() != null) {
                            if (message.getProps().has("file_type")) {
                                type = message.getProps().get("file_type").getAsInt();
                                if (type <= 4 && type >= 0)
                                    service.processMessageFromHandler(CBTypes.onMessageReceived, new Object[]{message});
                                else
                                    System.out.println("MONKEY - archivo no soportado");
                            } else if (message.getProps().has("type")) {
                                type = message.getProps().get("type").getAsInt();
                                if (type == 2 || type == 1)
                                    service.processMessageFromHandler(CBTypes.onMessageReceived, new Object[]{message});
                            } else if (message.getProps().has("monkey_action")) {
                                type = message.getProps().get("monkey_action").getAsInt();
                                message.setMonkeyAction(type);
                                if(message.getMessage_id()!=null && message.getMessage_id().length()>0 && !message.getMessage_id().equals("0")){
                                    service.setLastTimeSynced(Long.parseLong(message.getDatetime()));
                                }
                                try {
                                    switch (type) {
                                        case com.criptext.comunication.MessageTypes.MOKGroupCreate:
                                            service.processMessageFromHandler(CBTypes.onGroupAdded, new Object[]{message.getProps().get("group_id").getAsString(), message.getProps().get("members").getAsString(), message.getProps().get("info")});
                                            break;
                                        case com.criptext.comunication.MessageTypes.MOKGroupNewMember:
                                            service.processMessageFromHandler(CBTypes.onGroupNewMember, new Object[]{message.getRid(), message.getProps().get("new_member").getAsString()});
                                            break;
                                        case com.criptext.comunication.MessageTypes.MOKGroupRemoveMember:
                                            service.processMessageFromHandler(CBTypes.onGroupRemovedMember, new Object[]{message.getRid(), message.getSid()});
                                            break;
                                    }
                                }
                                catch (Exception e){
                                    e.printStackTrace();
                                }
                            } else
                                service.processMessageFromHandler(CBTypes.onNotificationReceived, new Object[]{message.getMessage_id(), message.getSid(), message.getRid(), message.getParams(), message.getDatetime()});
                        }
                        break;
                    case MessageTypes.MOKProtocolMessageHasKeys:
                        service.processMessageFromHandler(CBTypes.onMessageReceived, new Object[]{message});
                        break;
                    case MessageTypes.MOKProtocolMessageNoKeys:
                    case MessageTypes.MOKProtocolMessageWrongKeys:
                        service.requestKeysForMessage(message);
                        break;
                    case MessageTypes.MOKProtocolAck:
                        try {
                            if(message.getType().compareTo(MessageTypes.MOKOpen)==0){
                                service.processMessageFromHandler(CBTypes.onConversationOpenResponse
                                                , new Object[]{message.getSid()
                                                        ,message.getProps().get("online").getAsString().compareTo("1")==0
                                                        ,message.getProps().has("last_seen")?message.getProps().get("last_seen").getAsString():null
                                                        ,message.getProps().has("last_open_me")?message.getProps().get("last_open_me").getAsString():null
                                                        ,message.getProps().has("members_online")?message.getProps().get("members_online").getAsString(): ""});
                            }
                            else {
                                //The acknowledge message has the id of the successfully sent message in
                                //the Msg field. We'll use that to update our pending messages list.
                                service.removePendingMessage(message.getMsg());
                                service.processMessageFromHandler(CBTypes.onAcknowledgeReceived
                                        , new Object[]{message.getSid()
                                                ,message.getRid(),message.getMessage_id()
                                                ,message.getOldId(),message.getStatus() == 52
                                                ,Integer.parseInt(message.getType())});
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case MessageTypes.MOKProtocolOpen:{
                        if(message!=null && !message.isGroupConversation()) {
                            if (KeyStoreCriptext.getString(service, message.getRid()).compareTo("") == 0) {
                                OpenConversationTask task = new OpenConversationTask(serviceRef.get(), null);
                                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, message.getRid());
                            }
                            else
                                System.out.println("MONKEY - llego open pero ya tengo las claves");
                            //MANDAR AL APP QUE PONGA LEIDO TODOS LOS MENSAJES
                            service.processMessageFromHandler(CBTypes.onContactOpenMyConversation, new Object[]{message.getSid()});
                        }
                        break;
                    }
                    case MessageTypes.MOKProtocolDelete:{
                        service.processMessageFromHandler(CBTypes.onDeleteReceived, new Object[]{message.getMessage_id(), message.getSid(), message.getRid(), message.getDatetime()});
                        break;
                    }
                    case MessageTypes.MessageSocketConnected:{
                        service.processMessageFromHandler(CBTypes.onSocketConnected, new Object[]{""});
                        break;
                    }
                    case MessageTypes.MessageSocketUnauthorized:{
                        service.processMessageFromHandler(CBTypes.onConnectionRefused, new Object[]{""});
                        break;
                    }
                    case MessageTypes.MessageSocketDisconnected:{
                        service.processMessageFromHandler(CBTypes.onSocketDisconnected, new Object[]{""});//new Object[]{""}
                        break;
                    }
                    case MessageTypes.MOKProtocolSync: {
                        service.processMessageFromHandler(CBTypes.onNotificationReceived,
                            new Object[]{message.getMessage_id(), message.getSid(), message.getRid(),
                                    message.getParams(), message.getDatetime()});
                        break;
                    }
                    case MessageTypes.MOKProtocolMessageSync: {
                        long lastTimeSynced = (Long)msg.obj;
                        //service.sendSync(lastTimeSynced);
                        break;
                    }
                    default:
                        break;
                }

        }



}
