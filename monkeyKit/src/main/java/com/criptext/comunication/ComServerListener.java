package com.criptext.comunication;


import com.criptext.socket.DarkStarListener;

//handler of events

public class ComServerListener implements DarkStarListener
{

	private ComServerDelegate proxy;//is an screenProxy with the user reference and the screen game logic reference to control and communicate each other
	
	  /** The message encoding. **/
    public static final String MESSAGE_CHARSET = "UTF-8";
	
	public ComServerListener(ComServerDelegate vproxyscreen)
	{
		setProxy(vproxyscreen);
	}
	public void setProxy(ComServerDelegate proxy) {
		//Log.d("ComServerListener - setProxy", "seteando proxy: "+ proxy);
		this.proxy = proxy;
	}
	public void disconnected(boolean arg0, String arg1) {
		//Log.d("ComServerListener - disconnected", "arg0:"+arg0+" arg1: "+ arg1+"ComMessageProtocol.FAILLOGGINMSGDISCONECT: "+ComMessageProtocol.FAILLOGGINMSG);
		this.proxy.handleEvent(ComMessageProtocol.FAILLOGGINMSGDISCONECT);	
	}
	public void disconnected() {
		//Log.d("ComServerListener - disconnected", "arg0:"+" arg1: "+"ComMessageProtocol.FAILLOGGINMSGDISCONECT: "+ComMessageProtocol.FAILLOGGINMSG);
		this.proxy.handleEvent(ComMessageProtocol.FAILLOGGINMSGDISCONECT);	
	}

	public void joinedChannel(String arg0) {
		//Log.d("ComServerListener - joinedChannel", "arg0: "+ arg0);
	}

	public void leftChannel(String arg0) {
		//Log.d("ComServerListener - leftChannel", "arg0: "+ arg0);
	}

	public void loggedIn() {
		//Log.d("ComServerListener - loggedIn", "ComMessageProtocol.LOGGINMSG_ID: "+ ComMessageProtocol.LOGGINMSG_ID);
		this.proxy.handleEvent(ComMessageProtocol.LOGGINMSG_ID);
	}

	public void loginFailed(String arg0) {
		//Log.d("ComServerListener - loginFailed", "arg0:"+arg0+"ComMessageProtocol.FAILLOGGINMSG: "+ComMessageProtocol.FAILLOGGINMSG);
		this.proxy.handleEvent(ComMessageProtocol.FAILLOGGINMSG);		
	}

	public void receivedChannelMessage(String arg0, byte[] arg1) {
		//this.proxy.promptAction(MessageManager.decodeToString(arg1));
		//Log.d("ComServerListener - receivedChannelMessage", "arg0:"+arg0+" arg1: "+ arg1+"decodeMessage: "+MessageManager.decodeMessage(arg1));
		this.proxy.handleMessage(MessageManager.decodeMessage(arg1));
	}

	public void receivedSessionMessage(byte[] arg0) {
		//this.proxy.promptAction(MessageManager.decodeToString(arg0));
		//System.out.println("Mi listener:"+new String(arg0));
		//Log.d("ComServerListener - receivedSessionMessage", "arg0:"+arg0+"decodeMessage: "+MessageManager.decodeMessage(arg0));
		this.proxy.handleMessage(MessageManager.decodeMessage(arg0));
	}
	 public void showAlert(){
		 //Log.d("ComServerListener - showAlert", "ComMessageProtocol.SHOWALERT:"+ComMessageProtocol.SHOWALERT);
	 	this.proxy.handleEvent(ComMessageProtocol.SHOWALERT);
	 			
	 }
	
	public void reconnected() {
		//Log.d("ComServerListener - reconnected", "reconnected");
	}

	public void reconnecting() { 
		//Log.d("ComServerListener - reconnecting", "reconnecting");
	}
	
}
