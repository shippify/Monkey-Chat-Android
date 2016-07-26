package com.criptext.comunication;

import org.json.JSONObject;

import android.accounts.NetworkErrorException;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Base64;
import android.util.Log;

import com.criptext.ClientData;
import com.criptext.MonkeyKitSocketService;
import com.criptext.MsgSenderService;
import com.criptext.security.AESUtil;
import com.criptext.lib.KeyStoreCriptext;
import com.criptext.socket.DarkStarClient;
import com.criptext.socket.DarkStarListener;
import com.criptext.socket.DarkStarSocketClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AsyncConnSocket implements ComServerDelegate{


	private String sessionId;
	private String urlPassword;
	public DarkStarClient socketClient;
	protected ComServerListener userServerListener;
	private HandlerThread handlerThread;
	Thread connThread;
	private Void response;

	public static int isdisconectfinal=-1;

	public enum Status {unauthorized, sinIniciar, conectado, reconectando, desconectado};
	private Status socketStatus;
	private Handler mainMessageHandler;
	private Handler socketMessageHandler;
	private Runnable lastAction = null;
	private MonkeyKitSocketService service;
	//TIMEOUT

	public AsyncConnSocket(String sessionId, String urlPassword, Handler mainMessageHandler) {
		this.sessionId=sessionId;
		this.urlPassword=urlPassword;
		this.mainMessageHandler=mainMessageHandler;
		socketStatus = Status.sinIniciar;
	}

	public AsyncConnSocket(String sessionId, String urlPassword, Handler mainMessageHandler, Runnable r) {
		this.sessionId=sessionId;
		this.urlPassword=urlPassword;
		this.mainMessageHandler=mainMessageHandler;
		this.lastAction = r;
		socketStatus = Status.sinIniciar;
	}

	public AsyncConnSocket(ClientData cdata, Handler messageHandler, MonkeyKitSocketService service){
		this.sessionId = cdata.getMonkeyId();
		this.urlPassword = cdata.getPassword();
		this.mainMessageHandler = messageHandler;
		this.socketStatus = Status.sinIniciar;
		this.service = service;
		Log.d("AsyncConnSocket", "new socket");
	}


	@Override
	public void promptAction(String action) {}


	public void fireInTheHole(){
		if(shouldTryToConnect())
			initConnection();
		if(handlerThread != null){
			handlerThread.quit();
		}
		handlerThread = new HandlerThread("AsyncConnSocketThread");
		handlerThread.start();
		socketMessageHandler = new Handler(handlerThread.getLooper()) {

			public void handleMessage(Message msg) {

				try {
					//Thread.sleep(100);
					if(msg.obj.toString().compareTo("desconectar")==0){
						if(isConnected()){
							socketStatus = Status.desconectado;
							socketClient.logout(true);
							stop();
							this.getLooper().quit();
							Log.d("AsyncConnSocket", "Logged Out.");
						}
					}
					if(msg.obj.toString().compareTo("logout")==0){
						if(isConnected()){
							socketStatus = Status.desconectado;
							socketClient.sendToSession(MessageManager.encodeString("{\"cmd\":\"80\",\"args\":{}}"));
							socketClient.logout(false);
						}
					}
					else if(msg.obj.toString().compareTo("desconectarpull")==0){
						if(isConnected()){
							socketStatus = Status.desconectado;
							socketClient.logout(true);
						}
					}
					else if(msg.obj.toString().compareTo("conectar")==0){

						if(shouldTryToConnect()){
							initConnection();
						}

					}
					else{

						if(isConnected()){
							System.out.println("Socket - Sending Message: "+msg.obj.toString());
							socketClient.sendToSession(MessageManager.encodeString(msg.obj.toString()));
						}
						else{
							System.out.println("NO se pudo enviar mensaje Socket desconectado");
						}

					}

				}
				catch(Exception ex){
					ex.printStackTrace();
				}
			}
		};
	}

	public void initConnection(){
    	System.out.println("INICIANDO CONEXION SOCKET");
		conexionRecursiva();

	}



	public void conexionRecursiva(){

		socketStatus = Status.reconectando; userServerListener=new ComServerListener((ComServerDelegate) this);
		socketClient = new DarkStarSocketClient(MonkeyKitSocketService.Companion.getBaseURL(),
				1139,(DarkStarListener)userServerListener);
		connThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					final int STRIKES = 5;

					for(int i = 1; i <= STRIKES; i++)
                        if (AsyncConnSocket.this.getSocketStatus() != Status.conectado) {
                            System.out.println("RECONNECTING - "+sessionId + " " + (System.currentTimeMillis()/1000));
                            socketClient.connect();
                            AsyncConnSocket.this.socketClient.login(AsyncConnSocket.this.sessionId, urlPassword);

                            Thread.sleep((int) (1000L * Math.pow(2, i)));
                        }

					if(AsyncConnSocket.this.getSocketStatus() != Status.conectado)
						throw(new NetworkErrorException("Cannot establish connection with service. Please contact support"));

					Handler handler = new Handler(Looper.getMainLooper());
					handler.post(new Runnable() {
						public void run() {
							fireInTheHole();
							//connection is successful, let's resend all pending messages
							service.resendPendingMessages();

						}
					});


				} catch (InterruptedException e){
					e.printStackTrace();
				}
				catch (Exception e) {
                    socketStatus=Status.desconectado;
					e.printStackTrace();
					//TODO tell a delegate that we cant establish connection
				}
			}
		});
		//Log.d("AsyncConnSocket", "start connection Thread");
		connThread.start();


	}

	/**
	 * Procesa el JSONArray que llega despues de un GET O SYNC. Decripta los mensajes que necesitan
	 * ser decriptados y arma un ArrayList de MOKMessages para pasarselo al Thread principal
	 * @param protocol GET o SYNC de MessageTypes
	 * @param args JsonObject "args" del GET o SYNC
	 * @param parser
	 */
	private void processBatch(int protocol, JsonObject args, JsonParser parser){
		System.out.println("MOK PROTOCOL SYNC");
		JsonObject props = new JsonObject(), params = new JsonObject();
        //service.notifySyncSuccess();
		MOKMessage remote;
        if(args.get("type").getAsInt() == 1) {
            JsonArray array = args.get("messages").getAsJsonArray();
            long lastTimeSynced = 0;
			String lastMessageId = "";
            ArrayList<MOKMessage> batch = new ArrayList<>();

            for (int i = 0; i < array.size(); i++) {
				JsonObject currentMessage =  null;
				try {
					JsonElement jsonMessage = array.get(i);
					currentMessage = jsonMessage.getAsJsonObject();
					//init params props
					if (currentMessage.has("params") && !currentMessage.get("params").isJsonNull() && !parser.parse(currentMessage.get("params").getAsString()).isJsonNull())
						if (parser.parse(currentMessage.get("params").getAsString()) instanceof JsonObject)
							params = (JsonObject) parser.parse(currentMessage.get("params").getAsString());
					if (currentMessage.has("props") && !currentMessage.get("props").isJsonNull() && !parser.parse(currentMessage.get("props").getAsString()).isJsonNull())
						props = (JsonObject) parser.parse(currentMessage.get("props").getAsString());

					lastTimeSynced = Long.parseLong(currentMessage.get("datetime").getAsString());
					lastMessageId = currentMessage.get("id").getAsString();

					if (currentMessage.get("type").getAsString().compareTo(MessageTypes.MOKText) == 0
							|| currentMessage.get("type").getAsString().compareTo(MessageTypes.MOKFile) == 0) {

                        remote = createMOKMessageFromJSON(currentMessage, params, props);
                        if (remote.getProps().get("encr").getAsString().compareTo("1") == 0)
                            remote = getKeysAndDecryptMOKMessage(remote, false);
                        else if (remote.getProps().has("encoding") && !remote.getType().equals(MessageTypes.MOKFile)) {
                            if(remote.getProps().get("encoding").getAsString().equals("base64"))
                                remote.setMsg(new String(Base64.decode(remote.getMsg().getBytes(), Base64.NO_WRAP)));
                        }
                        if (remote != null)
                            batch.add(remote);
					}
                    else if(currentMessage.get("type").getAsString().compareTo(MessageTypes.MOKNotif)==0){
                        remote = createMOKMessageFromJSON(currentMessage, params, props);
                        Message msg = mainMessageHandler.obtainMessage();
                        msg.what=MessageTypes.MOKProtocolMessage;
                        msg.obj =remote;
                        mainMessageHandler.sendMessage(msg);
                    }
                    else if(currentMessage.get("type").getAsString().compareTo(""+MessageTypes.MOKProtocolDelete)==0){
                        remote=new MOKMessage(props.get("message_id").getAsString(),
                                currentMessage.get("sid").getAsString(),currentMessage.get("rid").getAsString(),
                                "",currentMessage.get("datetime").getAsString(),
                                currentMessage.get("type").getAsString(), params, props);
                        Message msg = mainMessageHandler.obtainMessage();
                        msg.what = MessageTypes.MOKProtocolDelete;
                        msg.obj = remote;
                        mainMessageHandler.sendMessage(msg);
                    }
				}
                catch( Exception ex){
					if(currentMessage != null)
						Log.d("MissingDateTime", currentMessage.toString());
					ex.printStackTrace();
				}
            }

            Message msg = mainMessageHandler.obtainMessage();
            msg.what = MessageTypes.MOKProtocolMessageBatch;
            Log.d("AsyncConnSocket", "Batch Ready with " + batch.size() + '/' + array.size());
            msg.obj = batch;
            mainMessageHandler.sendMessage(msg);

            if (args.get("remaining_messages").getAsInt() > 0) {
				if(protocol == MessageTypes.MOKProtocolSync)
                	service.sendSync(lastTimeSynced);
				else if(protocol == MessageTypes.MOKProtocolGet)
					service.sendGet(lastMessageId);
            }
            else{
                service.setPortionsMessages(15);
            }
        } else {
            //PARSE GROUPS UPDATES
            parseGroupUpdates(MessageTypes.MOKProtocolSync, args, params, props);
        }
	}
	public boolean isConnected(){
		return socketStatus == Status.conectado;
	}

	public boolean shouldTryToConnect(){
		return socketStatus != Status.conectado
				&& socketStatus != Status.reconectando && socketStatus != Status.unauthorized;
	}

	@Override
	public void handleMessage(ComMessage message) {
		this.parseMessage(message);
	}

	public void parseMessage(ComMessage socketMessage) {
		if(socketMessage!=null){
			int cmd = socketMessage.getMessageCmd();
			if(socketStatus != Status.desconectado)
				socketStatus = Status.conectado;
			JsonObject args = socketMessage.getArgs().getAsJsonObject();
            System.out.println("parsing message: " + args.toString() + " cmd: " + cmd);
            buildMessage(cmd, args);
		}
		else{
			//LLEGARON MUCHOS MENSAJES POR ENDE LLAMO UPDATES CON VALORES MENORES
			int msgQuantity = Math.max(service.getPortionsMessages() - 1, 1);
			service.setPortionsMessages(msgQuantity);
			service.sendSync(service.getLastTimeSynced());
		}
	}

	/**
	 * Crea un MOKMessage a partir de un JSON
	 * @param args
	 * @param params
	 * @param props
	 * @return
	 */
	public MOKMessage createMOKMessageFromJSON(JsonObject args, JsonObject params, JsonObject props){
		MOKMessage remote = new MOKMessage(args.get("id").getAsString(),
						args.get("sid").getAsString(),
						args.get("rid").getAsString(),
						args.get("msg").getAsString(),
						args.get("datetime").getAsString(),
						args.get("type").getAsString(),params,props);
		remote.setDatetimeorder(System.currentTimeMillis());
		return remote;
	}

	/**
	 * Consigues las llaves necesarias y decripta el contenido del MOKMessage.
	 * @param remote El remote message a decriptar.
	 * @return El MessageType de acuerdo a si se pudo o no decriptar el mensaje con las llaves
	 * existentes. Puede ser :
	 * 	MOKProtocolMessageNoKeys
	 * 	MOKProtocolMessageWrongKeys
	 * 	MOKProtocolMessageHasKeys
	 */
	public int decryptMOKMessage(MOKMessage remote){
        String claves= KeyStoreCriptext.getString(service.getApplicationContext(), remote.getSid());
        if(claves.compareTo("")==0 && !remote.getSid().startsWith("legacy:")){
            if(remote.getProps()!=null && remote.getProps().has("encr") && remote.getProps().get("encr").getAsString().compareTo("1")==0) {
                System.out.println("MONKEY - NO TENGO CLAVES DE AMIGO LAS MANDO A PEDIR");
                return MessageTypes.MOKProtocolMessageNoKeys;
            }
            else if(remote.getProps()!=null && remote.getProps().has("encoding") && !remote.getType().equals(MessageTypes.MOKFile)){
                if(remote.getProps().get("encoding").getAsString().equals("base64"))
                    remote.setMsg(new String(Base64.decode(remote.getMsg().getBytes(), Base64.NO_WRAP)));
                return MessageTypes.MOKProtocolMessageHasKeys;
            }
        }
        else{
            try {
                if (remote.getProps().get("encr").getAsString().compareTo("1") == 0 && !remote.getType().equals(MessageTypes.MOKFile)) {
                    remote.setMsg(AESUtil.decryptWithCustomKeyAndIV(remote.getMsg(),claves.split(":")[0], claves.split(":")[1]));
                }
                else if (remote.getProps().has("encoding") && !remote.getType().equals(MessageTypes.MOKFile)) {
                    if(remote.getProps().get("encoding").getAsString().equals("base64"))
                        remote.setMsg(new String(Base64.decode(remote.getMsg().getBytes(), Base64.NO_WRAP)));
                }
            } catch (IOException ex){
                Log.d("MonkeyKit", "Message with content no encrypted");
            }
            catch (Exception e){
                Log.d("MonkeyKit", "BadPaddingException Wrong Keys");
                return MessageTypes.MOKProtocolMessageWrongKeys;
            }
        }
		return MessageTypes.MOKProtocolMessageHasKeys;
	}

	/**
	 * Intenta decriptar un MOKMessage. Si no tiene llaves o estan mal, hace los requerimientos
	 * pertinentes al servidor y recursivamente vuelve a intentar. Si no tiene llaves, las pide al server.
	 * Si estan mal las llaves, pide las ultimas llaves al server, si son iguales a las que ya tiene,
	 * pide al server el texto del mensaje encriptado con las ultimas llaves. Si nada de eso funciona
	 * retorna null
	 * @param remote El mensaje a decriptar
	 * @param lastChance Esta variable se usa internamente para controlar la recursion. Al llamar
	 *                   esta funcion debe de hacerse con false.
	 * @return El mensaje con su texto decriptado. Si no se pudo decriptar retorna null
	 */
	public MOKMessage getKeysAndDecryptMOKMessage(MOKMessage remote, boolean lastChance){
		int what = decryptMOKMessage(remote);

		if(lastChance && what != MessageTypes.MOKProtocolMessageHasKeys)
			return null;

        if(what == MessageTypes.MOKProtocolMessageHasKeys) {
            return remote;
        } else if(what == MessageTypes.MOKProtocolMessageNoKeys) {
            service.requestKeyBySession(remote.getSid());
            Log.d("BatchGET", "Got Keys for " + remote.getSid() + ". Apply recursion");
			return getKeysAndDecryptMOKMessage(remote, true);
        } else if(what == MessageTypes.MOKProtocolMessageWrongKeys){
            String claves= KeyStoreCriptext.getString(service.getApplicationContext()
                  , remote.getSid());
            String newClaves = service.requestKeyBySession(remote.getSid());
            if(newClaves != null && !newClaves.equals(claves))
                return getKeysAndDecryptMOKMessage(remote, false);
            else if (newClaves != null){
				String newMsg = service.requestTextWithLatestKeys(remote.getMessage_id());
				if(newMsg != null) {
					remote.setMsg(newMsg);
					return getKeysAndDecryptMOKMessage(remote, true);
				} else {
					Log.d("BatchGET", "Message discarded due to wrong keys");
                    service.processMessageFromHandler(CBTypes.onMessageFailDecrypt, new Object[]{remote});
					return null;
				}
			}
        }

		Log.d("BatchGET", "wrong Protocol");
		return null;
	}
	public MOKMessage buildMessage(int cmd, JsonObject args){

		MOKMessage remote = null;
		JsonObject props = new JsonObject();
		JsonObject params = new JsonObject();
		JsonParser parser = new JsonParser();
		if(args.has("params") && !args.get("params").isJsonNull() && !parser.parse(args.get("params").getAsString()).isJsonNull())
			if(parser.parse(args.get("params").getAsString()) instanceof JsonObject)
                params=(JsonObject)parser.parse(args.get("params").getAsString());
		if(args.has("props") && !args.get("props").isJsonNull() && !parser.parse(args.get("props").getAsString()).isJsonNull())
			props=(JsonObject)parser.parse(args.get("props").getAsString());
		switch (cmd) {
		case MessageTypes.MOKProtocolMessage:{

			if(args.get("type").getAsString().compareTo(MessageTypes.MOKText)==0
				|| args.get("type").getAsString().compareTo(MessageTypes.MOKFile)==0){
				Message msg = mainMessageHandler.obtainMessage();
				remote = createMOKMessageFromJSON(args, params, props);
				msg.what = decryptMOKMessage(remote);
				msg.obj = remote;
				mainMessageHandler.sendMessage(msg);
			}
			else if(args.get("type").getAsString().compareTo(MessageTypes.MOKTempNote)==0){
				remote=new MOKMessage("",args.get("sid").getAsString(), 
						args.get("rid").getAsString(),"",
						args.get("datetime").getAsString(), 
						args.get("type").getAsString(), params, props);
				Message msg = mainMessageHandler.obtainMessage();			      
				msg.what=MessageTypes.MOKProtocolMessage;
				msg.obj =remote;
				mainMessageHandler.sendMessage(msg);
			}
			else if(args.get("type").getAsString().compareTo(MessageTypes.MOKNotif)==0){
				remote=new MOKMessage(args.get("id").getAsString(), 
						args.get("sid").getAsString(), 
						args.get("rid").getAsString(),
						args.get("msg").getAsString(),
						args.get("datetime").getAsString(), 
						args.get("type").getAsString(), params, props);
				Message msg = mainMessageHandler.obtainMessage();
				msg.what=MessageTypes.MOKProtocolMessage;
				msg.obj =remote;
				mainMessageHandler.sendMessage(msg);
			}
            else if(args.get("type").getAsInt()==MessageTypes.MOKProtocolDelete){
                remote=new MOKMessage(props.get("message_id").getAsString(),
                        args.get("sid").getAsString(),args.get("rid").getAsString(),
                        "",args.get("datetime").getAsString(),
                        args.get("type").getAsString(), params, props);
                Message msg = mainMessageHandler.obtainMessage();
                msg.what=MessageTypes.MOKProtocolDelete;
                msg.obj =remote;
                mainMessageHandler.sendMessage(msg);
                break;
            }
			else{
				System.out.println("MONKEY - Tipo "+args.get("type").getAsString()+" no soportado");
			}
			
			break;
		}		
		case MessageTypes.MOKProtocolOpen:{
			if(args.get("type").getAsString().compareTo(MessageTypes.MOKOpen)==0){
				remote=new MOKMessage(args.get("id").getAsString(),
						args.get("sid").getAsString(),args.get("rid").getAsString(),"",
						args.get("datetime").getAsString(), 
						"", params, props);
				Message msg = mainMessageHandler.obtainMessage();			      
				msg.what=MessageTypes.MOKProtocolOpen;
				msg.obj =remote;
				mainMessageHandler.sendMessage(msg);
			}
			break;
		}
		case MessageTypes.MOKProtocolAck:{			
			if(args.get("type").getAsString().equals(MessageTypes.MOKText)
				|| args.get("type").getAsString().equals(MessageTypes.MOKNotif)
                    || args.get("type").getAsString().equals(MessageTypes.MOKFile)){
				remote=new MOKMessage(props.get("new_id").getAsString(), //En el atributo message_id va el nuevo id
						args.get("sid").getAsString(),args.get("rid").getAsString(),
						props.get("old_id").getAsString(), //En el atributo msg mando en el old_id
						args.get("datetime").getAsString(), 
						args.get("type").getAsString(), params, props);
				Message msg = mainMessageHandler.obtainMessage();			      
				msg.what=MessageTypes.MOKProtocolAck;
				msg.obj =remote;
				mainMessageHandler.sendMessage(msg);
			}
			else if(args.get("type").getAsString().compareTo(MessageTypes.MOKOpen)==0){
				remote=new MOKMessage(args.get("id").getAsString(),
						args.get("sid").getAsString(),args.get("rid").getAsString(),"",
						args.get("datetime").getAsString(), 
						args.get("type").getAsString(), params, props);
				Message msg = mainMessageHandler.obtainMessage();			      
				msg.what=MessageTypes.MOKProtocolAck;
				msg.obj =remote;
				mainMessageHandler.sendMessage(msg);
			}
            else if(args.get("type").getAsString().compareTo(""+MessageTypes.MOKProtocolDelete)==0){
                remote=new MOKMessage(props.get("message_id").getAsString(),
                        args.get("sid").getAsString(),args.get("rid").getAsString(),
                        "",args.get("datetime").getAsString(),
                        args.get("type").getAsString(), params, props);
                Message msg = mainMessageHandler.obtainMessage();
                msg.what=MessageTypes.MOKProtocolDelete;
                msg.obj =remote;
                mainMessageHandler.sendMessage(msg);
                break;
            }
			break;
		}
		case MessageTypes.MOKProtocolDelete:{
			remote=new MOKMessage(props.get("message_id").getAsString(),
					args.get("sid").getAsString(),args.get("rid").getAsString(),
					"",args.get("datetime").getAsString(), 
					args.get("type").getAsString(), params, props);
			Message msg = mainMessageHandler.obtainMessage();			      
			msg.what=MessageTypes.MOKProtocolDelete;
			msg.obj =remote;
			mainMessageHandler.sendMessage(msg);
			break;
		}
		case MessageTypes.MOKProtocolGet:
		case MessageTypes.MOKProtocolSync:{
			processBatch(cmd, args, parser);
			break;
		}
		default:
			break;
		}

		return remote;
	}

	@Override
	public void handleEvent(short evid) {

		if(evid==ComMessageProtocol.LOGGINMSG_ID){
			//Este es el mensaje que indica que ya estoy conectado
			socketStatus = Status.conectado;
			System.out.println("Conectado al Socket!");
			Message msg = mainMessageHandler.obtainMessage();			      
			msg.what=MessageTypes.MessageSocketConnected;
			mainMessageHandler.sendMessage(msg);
		}
		else if(evid==ComMessageProtocol.FAILLOGGINMSG){
			//REMOTE LOGOUT UNAUTHORIZED
			socketStatus = Status.unauthorized;
			if(connThread != null) {
				connThread.interrupt();
				connThread = null;
			}
			System.out.println("UNAUTHORIZED");
            socketStatus = Status.unauthorized;
            Message msg = mainMessageHandler.obtainMessage();
            msg.what = MessageTypes.MessageSocketUnauthorized;
            mainMessageHandler.sendMessage(msg);

		}else if(evid==ComMessageProtocol.FAILLOGGINMSGDISCONECT){
			//REMOTE LOGOUT DESDE EL SOCKET
			socketStatus = Status.desconectado;
			//This logout should only occur when the watchdo
			System.out.println("Desconectado del Socket!");
			if(mainMessageHandler != null && isConnected()) {
				Message msg = mainMessageHandler.obtainMessage();
				msg.what = MessageTypes.MessageSocketDisconnected;
				mainMessageHandler.sendMessage(msg);
			}
		}

	}


	private void parseGroupUpdates(int protocol, JsonObject args, JsonObject  params, JsonObject props){
		Log.d("MissingGroups", "ParseGroupUpdates");
         MOKMessage remote=new MOKMessage("","","",args.get("messages").getAsString(), "",
                args.get("type").getAsString(), params, props);
        remote.setMonkeyAction(MessageTypes.MOKGroupJoined);
        Message msg = mainMessageHandler.obtainMessage();
        msg.what=protocol;
        msg.obj = remote;
        mainMessageHandler.sendMessage(msg);
	}
	@Override
	public void disconnected(){
		if(socketStatus != Status.desconectado && socketStatus != Status.reconectando)
			initConnection();

	}

	/*****************************/
	/****FUNCIONES DEL SOCKET*****/
	/*****************************/

	//TODO delete this
	public void sendMessage(JSONObject params){
		try{
			if(socketMessageHandler!=null){
				Message msg = socketMessageHandler.obtainMessage();
				msg.obj =params.toString();
				System.out.println("MONKEY - AsyncConnSocket - enviando mensaje");
				socketMessageHandler.sendMessage(msg);
			}
			else {
				System.out.println("MONKEY - AsyncConnSocket - socketMessageHandler es null");
				fireInTheHole();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public void sendMessage(JsonObject params){
		try{
			if(socketMessageHandler!=null){
				Message msg = socketMessageHandler.obtainMessage();
				msg.obj =params.toString();
				System.out.println("MONKEY - AsyncConnSocket - enviando mensaje");
				socketMessageHandler.sendMessage(msg);
			}
			else {
				System.out.println("MONKEY - AsyncConnSocket - socketMessageHandler es null");
				fireInTheHole();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public void disconectSocket(){
		try {
			if(socketStatus != Status.desconectado){
				if(socketMessageHandler!=null){
					Message msg = socketMessageHandler.obtainMessage();			      
					msg.obj ="desconectar";
					socketMessageHandler.sendMessage(msg);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void conectSocket(){
		try {
			if(shouldTryToConnect()){
				if(socketMessageHandler==null || handlerThread == null || !handlerThread.isAlive()){
					System.out.println("MONKEY - mandaron a conectar pero no esta inicializado el socketMessageHandler");
					fireInTheHole();
				}
				else{
					System.out.println("MONKEY - mandaron a conectar y SI esta inicializado el socketMessageHandler");
					Message msg = socketMessageHandler.obtainMessage();
					msg.obj ="conectar";
					socketMessageHandler.sendMessage(msg);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void sendLogout(){
		try {
			if(socketStatus != Status.desconectado){
				socketStatus = Status.desconectado;
				Message msg = socketMessageHandler.obtainMessage();			      
				msg.obj ="logout";
				socketMessageHandler.sendMessage(msg);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void sendDisconectFromPull(){
		try {
			if(socketStatus != Status.desconectado){
				Message msg = socketMessageHandler.obtainMessage();			      
				msg.obj ="desconectarpull";
				socketMessageHandler.sendMessage(msg);
			}
		} catch (Exception e) {

			e.printStackTrace();
		}
	}

	public Status getSocketStatus(){
		return socketStatus;
	}

	public void stop(){
		if(handlerThread != null)
			handlerThread.quit();
		mainMessageHandler = null;
		socketMessageHandler = null;

	}

}

