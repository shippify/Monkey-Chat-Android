package com.criptext.comunication;

import java.util.Hashtable;

/*import com.blip.android.application.Settings;
import com.blip.android.application.domain.Invite;
import com.blip.android.application.exchange.remote.OutgoingFbInvite;
import com.blip.android.application.exchange.remote.OutgoingFbJiglInvite;*/

public class ComMessageProtocol {


	//commands from events
	public static final short LOGGINMSG_ID=26;
	public static final short FAILLOGGINMSG = 27;	
	public static final short FAILLOGGINMSGDISCONECT = 28;
	
	public static final short SHOWALERT = 28;	
	
	public static final int MESSAGE_TYPE_DEFAULT = 0;
	public static final int MESSAGE_TYPE_INVITE_GOT = 1;
	public static final int MESSAGE_TYPE_TYPING = 2;
	public static final int MESSAGE_TYPE_PROFILE_UPDATED = 3;
	public static final int MESSAGE_TYPE_AVATAR_UPDATED = 4;
	public static final int MESSAGE_TYPE_GROUP_INVITE_GOT = 5;
	public static final int MESSAGE_TYPE_GROUP_REMOVED_FROM = 6;
	public static final int MESSAGE_TYPE_GROUP_MEMBERS_CHANGED = 7;
	public static final int MESSAGE_TYPE_GROUP_DELETED = 8;
	public static final int MESSAGE_TYPE_GROUP_MESSAGE_GOT = 9;
	public static final int MESSAGE_TYPE_INVITE_ACCEPTED = 10;
	public static final int MESSAGE_TYPE_INVITE_DENIED = 11;
	public static final int MESSAGE_TYPE_INVITE_CANCELLED = 12;
	public static final int MESSAGE_TYPE_FRIEND_REMOVED = 13;
	public static final int MESSAGE_TYPE_SHARE_A_FRIEND = 14;
	public static final int MESSAGE_TYPE_CONVERSATION_OPENNED = 15;
	public static final int MESSAGE_TYPE_CONVERSATION_CLOSED = 16;
	public static final int MESSAGE_TYPE_ATTACH_AUDIO = 17;
	public static final int MESSAGE_TYPE_ATTACH_PHOTO = 18;
	public static final int MESSAGE_TYPE_FILE = 19;
	public static final int MESSAGE_TYPE_UNTYPING = 20;
	public static final int SUSCRIBE_TO_CHANNELS = 25;
	public static final int MESSAGE_NOT_DELIVERED=50;
	public static final int MESSAGE_DELIVERED=51;
	public static final int MESSAGE_NOT_VIEW=52;
	public static final int MESSAGE_OPEN_CLOSE_NOTIFICATION = 53;
	
	public static final int MESSAGE_LIST = 201;
	
	//+(ComMessage *) createChatMsg:(NSString *)msg forUser:(int)idUser andId:(long long int)mid
	public static ComMessage createChatMsg(String msg,long idUser,long mid){
		
		Hashtable<String, Object> args=new Hashtable<String, Object>();
		if(mid>0){
			
			args.put("id", Long.toString(mid));
		}
		/*if(msg==null){
			msg="";
		}*/
		args.put("msg", msg);
		args.put("rid", Long.toString(idUser));
		
		return ComMessage.createMessageWithCommand(MESSAGE_TYPE_DEFAULT,args);
	}
	public static ComMessage createChatMsgJSON(String msg,long idUser,long mid){
		
		Hashtable<String, Object> args=new Hashtable<String, Object>();
		if(mid>0){
			
			args.put("id", Long.toString(mid));
		}
		/*if(msg==null){
			msg="";
		}*/
		args.put("msg", msg);
		args.put("rid", Long.toString(idUser));
		
		return ComMessage.createMessageWithCommand(MESSAGE_TYPE_DEFAULT,args);
	}
	//+(ComMessage *) createGroupMsg:(NSString *)msg forGroup:(int)idGroup andId:(long long int)mid	
	public static ComMessage createGroupMsg(String msg,int idGroup, long mid){
		
		Hashtable<String, Object> args=new Hashtable<String, Object>();
		if(mid>0){
			args.put("id", Long.toString(mid));
			
		}
		args.put("msg", msg);
		args.put("gid", Integer.toString(idGroup));
		
		return ComMessage.createMessageWithCommand(MESSAGE_TYPE_GROUP_MESSAGE_GOT, args);
	}
	
	//+(ComMessage *) createBasicMsg:(int)idUser ofType:(int)messageType andMessage:(NSString *)msg toGroup:(int)idGroup andId:(long long int)mid
	public static ComMessage createBasicMsg(long idUser, int messageType, String msg, long idGroup, long mid, String time){
		
		Hashtable<String, Object> args=new Hashtable<String, Object>();
		if(mid>0 || mid<0){
			args.put("id", Long.toString(mid));
			
		}
		if(idUser>0){
			args.put("rid", Long.toString(idUser));
		}
		if(msg!=null){
			args.put("msg", msg);
		}
		if(idGroup>0||messageType==11){
			args.put("gid", Long.toString(idGroup));
		}
		if(time!=""){
			args.put("time", time);
		}
		/*if(messageType==27)
			args.put("last_id", Settings.instance().lastMessageId());*/
		
		ComMessage finaleMessage=ComMessage.createMessageWithCommand(messageType, args);
		System.out.println("argumentos: "+finaleMessage.getArgs());
		return finaleMessage;
	}
	
	//+(ComMessage *) createShareFriendToUserMsg:(int)idUser withName:(NSString *)fname lastName:(NSString *)lname toUser:(int)idDestin andId:(long long int)mid
	/*public static ComMessage createShareFriendToUserMsg(int idUser,String fname,String lname,int idDestin,long mid){
		
		
		Hashtable map=new Hashtable();
		map.put("uid", Integer.toString(idUser));
		map.put("first_name", fname);
		map.put("last_name", lname);
		
		String messageText=new JSONObject(map).toString();
		
		return createBasicMsg(idDestin,MESSAGE_TYPE_SHARE_A_FRIEND ,messageText,-1,mid); 
	}*/
	//+(ComMessage *) createShareFriendToGroupMsg:(int)idUser withName:(NSString *)fname lastName:(NSString *)lname toGroup:(int)idGroup andId:(long long int)mid
	/*public static ComMessage createShareFriendToGroupMsg(int idUser,String fname,String lname,int idGroup,long mid){
		//String messageText="{\"uid\":"+idUser+",\"first_name\":"+fname+",\"last_name\":"+lname+"}";
		Hashtable map=new Hashtable();
		map.put("uid", Integer.toString(idUser));
		map.put("first_name", fname);
		map.put("last_name", lname);
		
		String messageText=new JSONObject(map).toString();
		
		return createBasicMsg(-1,MESSAGE_TYPE_SHARE_A_FRIEND,messageText,idGroup,mid);
	}*/
	
	//+(ComMessage *) createNotificationMsg:(int) idUser ofStringType:(NSString *) messageType
	public static ComMessage createNotificationMsg(int idUser,int messageType){
		//int type=Integer.parseInt(messageType);
		Hashtable<String, Object> args=new Hashtable<String, Object>();
		args.put("rid", Integer.toString(idUser));
		args.put("r", Integer.toString(messageType));
		
		return ComMessage.createMessageWithCommand(messageType, args);
	}
	
	public static ComMessage createNotificationDeleteFriend(int idUser,int messageType){
		//int type=Integer.parseInt(messageType);
		Hashtable<String, Object> args=new Hashtable<String, Object>();
		args.put("rid", Integer.toString(idUser));
		
		return ComMessage.createMessageWithCommand(messageType, args);
	}
	
	//+(ComMessage *) createAddRemoveGroupMsg:(BLGroupId)idGroup add:(NSArray*)arrayAdd andRemove:(NSArray*)arrayRemove{
	/*public static ComMessage createAddRemoveGroupMsg(long idGroup,Vector arrayAdd,Vector arrayRemove){
		
		Hashtable args=new Hashtable();
		if(arrayAdd.size()>0){
			
			Vector invites=new Vector(arrayAdd.size());
			for (Enumeration e = arrayAdd.elements(); e.hasMoreElements();) {
				User user = (User)e.nextElement();
				Hashtable dict=new Hashtable();
				if (user.getServerId() != 0) {
					//dict.put("jiglid",Long.toString(user.getServerId()));
					dict.put("jiglid",Integer.valueOf(user.getServerId()+""));
				}
				if (user.getfb_Id() != null) {
					dict.put("fb_id", user.getfb_Id());
				}
				//invites.addElement(new JSONObject(dict).toString());
				invites.addElement(new JSONObject(dict));
			}
			args.put("invites", new JSONArray(invites));
		}
		
		if(arrayRemove.size()>0){
			Vector removes=new Vector(arrayRemove.size());
			for (Enumeration e = arrayRemove.elements(); e.hasMoreElements();) {
				User user = (User)e.nextElement();
				Hashtable dict=new Hashtable();
				if (user.getServerId() != 0) {
					//dict.put("jiglid",Long.toString(user.getServerId()));
					dict.put("jiglid",Integer.valueOf(user.getServerId()+""));
				}
				if (user.getfb_Id() != null) {
					dict.put("fb_id", user.getfb_Id());
				}
				//removes.addElement(new JSONObject(dict).toString());
				removes.addElement(new JSONObject(dict));
			}
			args.put("removes", new JSONArray(removes));
		}
		
		return ComMessage.createMessageWithCommand(MESSAGE_TYPE_GROUP_INVITE_GOT , args);
	}*/
	
	//+(ComMessage *) createInvitesNotificationMsg:(int)idUser action:(int)typeNotification{
	public static ComMessage createInvitesNotificationMsg(long idUser,int typeNotification,long gid){
		
		int type=0;
		
		switch (typeNotification) {
			case 0:
				type=MESSAGE_TYPE_INVITE_DENIED;
				break;
			case 1:
				type=MESSAGE_TYPE_INVITE_ACCEPTED;
				break;
			case 2:
				type=MESSAGE_TYPE_INVITE_CANCELLED;
				break;
			default:
				break;
		}
		Hashtable<String, Object> args=new Hashtable<String, Object>();
		args.put("rid", Long.toString(idUser));
		try{
			
			args.put("gid", Long.toString(gid));
			
		}
		catch(Exception e){
			
		}
		
		
		
		return ComMessage.createMessageWithCommand(type, args);
	}
	
	//+(ComMessage *) createInvitesMsg:(NSArray*)array{
	
	/*public static ComMessage createInvitesMsg(Vector<OutgoingFbJiglInvite> array){
		
		Hashtable<String, Object> args=new Hashtable<String, Object>();
		Vector<String> invites=new Vector<String>(array.size());
		
		for (Enumeration<?> e = array.elements(); e.hasMoreElements();) {
			OutgoingFbJiglInvite invite=(OutgoingFbJiglInvite) e.nextElement();
			Hashtable<String, String> dict=new Hashtable<String, String>();
			if (invite.jiglid() != 0) {
				dict.put("jiglid", Long.toString(invite.jiglid()));
			}else if (invite.fbid() != 0) {
				dict.put("fb_id",Long.toString( invite.fbid()));
			}
			invites.addElement(new JSONObject(dict).toString());
			
			
		}
		args.put("invites", new JSONArray(invites));
		
		return ComMessage.createMessageWithCommand(MESSAGE_TYPE_INVITE_GOT, args);
	}
	
	public static ComMessage createResponseInvitesMsg(Vector<OutgoingFbJiglInvite> array){
		
		Hashtable<String, Object> args=new Hashtable<String, Object>();
		Vector<String> invites=new Vector<String>(array.size());
		
		for (Enumeration<?> e = array.elements(); e.hasMoreElements();) {
			OutgoingFbJiglInvite invite=(OutgoingFbJiglInvite) e.nextElement();
			Hashtable<String, String> dict=new Hashtable<String, String>();
			if (invite.jiglid() != 0) {
				dict.put("jiglid", Long.toString(invite.jiglid()));
				//dict.put("jiglid",Integer.valueOf(invite.uid_to()+""));
			}else if (invite.fbid() != 0) {
				dict.put("fb_id",Long.toString( invite.fbid()));
			}
			invites.addElement(new JSONObject(dict).toString());
			
			
		}
		args.put("invites", new JSONArray(invites));
		
		return ComMessage.createMessageWithCommand(MESSAGE_TYPE_INVITE_GOT, args);
	}*/
	/**
	 * 
	 * +(ComMessage *) createMsgFromBlMessage:(BLMessage *)message withId:(long long int)messageId{

	 * **/
	/*public static ComMessage createMsgFromBlMessage(Message message,long mid,String time){
		return ComMessageProtocol.createBasicMsg(message.getUserTo(), message.getType(), message.getOriginalText(), message.getGroupId(), mid,time);
	}*/
	
	//+(ComMessage *) createSuscribeGroupMsg:(NSArray*)array{
	/*public static ComMessage createSuscribeGroupMsg(Vector array){
	
		Vector groups=new Vector(array.size());
		
		for (int i=0; i<array.size();i++) {
			Hashtable dict=new Hashtable();
			if (((Group) array.elementAt(i)).getServerId()!= 0) {
                 
				dict.put("id", Long.toString(((Group) array.elementAt(i)).getServerId()));
			}
			if (((Group) array.elementAt(i)).getName() != null) {
                 
				dict.put("name", ((Group) array.elementAt(i)).getName());
			}
			//groups.addElement(new JSONObject(dict).toString());
			groups.addElement(new JSONObject(dict));

		}
		Hashtable args=new Hashtable();
		args.put("groups", new JSONArray(groups));
		
		System.out.println("grupos:"+args.toString());
		
		return ComMessage.createMessageWithCommand(SUSCRIBE_TO_CHANNELS, args);
		
	}*/
	//+(ComMessage *) createGroupDeleteMsg:(int)idGroup{
	public static ComMessage createGroupDeleteMsg(long idGroup){
		
		Hashtable<String, Object> args=new Hashtable<String, Object>();
		args.put("groupId", Long.toString(idGroup));
		return ComMessage.createMessageWithCommand(MESSAGE_TYPE_GROUP_DELETED, args);
	}
	
	
}


