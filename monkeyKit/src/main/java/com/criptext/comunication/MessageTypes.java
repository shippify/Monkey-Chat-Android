package com.criptext.comunication;

/**
 * @author ***
 */
public class MessageTypes {

	public static final int MessageSocketUnauthorized = -3;
	public static final int MessageSocketDisconnected = -2;
	public static final int MessageSocketConnected = -1;

	public class FileTypes {
		public static final int Default = 0;
        public static final int Audio = 1;
        public static final int Photo = 3;
        public static final int Document = 4;
        public static final int ShareAFriend = 14;
	}

    public static final int blMessageDefault = 0;
    public static final int blMessageAudio = 1;
    public static final int blMessagePhoto = 3;
    public static final int blMessageDocument = 4;
    public static final int blMessageShareAFriend = 14;

	public class Status {
		public static final int sending = 50;
		public static final int delivered = 51;
		public static final int read = 52;
	}

	public class NotificationTypes {
		public static final int GroupAdded = 5;
        public static final int GroupNewMember = 7;
        public static final int GroupRemovedMember = 8;
        public static final int UserGroupsUpdate=46;
        public static final int ScreenCapture=60;
	}

    public static final int blMessageGroupAdded = 5;
    public static final int blMessageGroupNewMember = 7;
    public static final int blMessageGroupRemovedMember = 8;
    public static final int blMessageUserGroupsUpdate=46;
    public static final int blMessageScreenCapture=60;

    //COMANDOS
	public static final int MOKProtocolMessage = 200;
	public static final int MOKProtocolMessageHasKeys = 199;
	public static final int MOKProtocolMessageNoKeys = 198;
	public static final int MOKProtocolMessageWrongKeys = 197;
	public static final int MOKProtocolMessageBatch = 196;

	public static final int MOKProtocolGet = 201;
	public static final int MOKProtocolTransaction = 202;
	public static final int MOKProtocolOpen = 203;
	public static final int MOKProtocolSet = 204;
	public static final int MOKProtocolAck = 205;
	public static final int MOKProtocolDelete = 207;
	public static final int MOKProtocolClose = 208;
	public static final int MOKProtocolSync = 209;

	//TIPOS DE MENSAJES
	public static final String MOKText = "1";
	public static final String MOKFile = "2";
	public static final String MOKTempNote = "3";
	public static final String MOKNotif = "4";
	public static final String MOKAlert = "5";
	public static final String MOKOpen = "203";

	//MONKEY_TYPES
	public static final int MOKGroupCreate = 1;
	public static final int MOKGroupDelete = 2;
	public static final int MOKGroupNewMember = 3;
	public static final int MOKGroupRemoveMember = 4;
	public static final int MOKGroupJoined = 5;

}
