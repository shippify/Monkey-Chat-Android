package com.criptext.comunication;


import com.google.gson.JsonObject;

import java.io.File;


public class MOKMessage {
	
	private int protocolCommand;
	private int protocolType;
	private String encr;
	private String datetime;
	private long datetimeorder;
	private String msg;
	private String message_id;
	private String type;
	private String sid;
	private String rid;
	private JsonObject params;
	private File file;
	private int monkeyAction;

	public int getMonkeyAction() {
		return monkeyAction;
	}

	public void setMonkeyAction(int monkeyAction) {
		this.monkeyAction = monkeyAction;
	}

	public JsonObject getProps() {
		return props;
	}

	public void setProps(JsonObject props) {
		this.props = props;
	}

	private JsonObject props;
	public MOKMessage(){
		
	}
	
	public MOKMessage(String message_id, String sid, String rid, String msg, String datatime, String type, JsonObject params, JsonObject props){
		this.message_id = message_id;
		this.sid = sid;
		this.rid = rid;
		this.msg = msg;
		this.datetime = datatime;		
		this.type = type;
		this.params = params;
		this.props = props;
	}

	public int getProtocolCommand() {
		return protocolCommand;
	}

	public void setProtocolCommand(int protocolCommand) {
		this.protocolCommand = protocolCommand;
	}

	public int getProtocolType() {
		return protocolType;
	}

	public void setProtocolType(int protocolType) {
		this.protocolType = protocolType;
	}

	public String getDatetime() {
		return datetime;
	}

	public void setDatetime(String datetime) {
		this.datetime = datetime;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public String getMessage_id() {
		return message_id;
	}

	public void setMessage_id(String message_id) {
		this.message_id = message_id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getSid() {
		return sid;
	}

	public void setSid(String sid) {
		this.sid = sid;
	}

	public String getRid() {
		return rid;
	}

	public void setRid(String rid) {
		this.rid = rid;
	}

	public JsonObject getParams() {
		return params;
	}

	public void setParams(JsonObject params) {
		this.params = params;
	}

	public String getEncr() {
		return encr;
	}

	public void setEncr(String encr) {
		this.encr = encr;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {

		this.file = file;
	}

	/**
	 * Obtiene el estado del mensaje. Este valor debe de compararse con las constantes de
	 * MessageTypes.Status. Si props es null, o no tiene estado, retorna 0.
	 * @return Si props es null, o no tiene estado, retorna 0. De lo contrario retorna el
	 * valor correspondiente de MessageTypes.Status.
	 */
	public int getStatus(){
		if(props == null || props.get("status") == null)
			return 0;

		return props.get("status").getAsInt();
	}

	public String getOldId(){
		if(props == null || props.get("old_id") == null)
			return null;

		return props.get("old_id").getAsString();
	}

	public String getFileExtension(){
		if(props == null || props.get("ext") == null)
			return null;

		return "." + props.get("ext").getAsString();
	}
	public long getDatetimeorder() {
		return datetimeorder;
	}

	public void setDatetimeorder(long datetimeorder) {
		this.datetimeorder = datetimeorder;
	}


}