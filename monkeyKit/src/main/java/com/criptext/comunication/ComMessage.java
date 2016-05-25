package com.criptext.comunication;

import java.nio.ByteBuffer;
import java.util.Hashtable;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

public class ComMessage {
	public static final String MESSAGE_CHARSET = "UTF-8";	
	final JsonElement args;
	
	final short cmd;
	
	private transient ByteBuffer encodedForm = null;
    
	public ComMessage(short vid,JsonElement vargs){//,LinkedHashMap<String,?> argumentos
		cmd=vid;
		args=vargs;		
	}
	
	@SuppressWarnings("unchecked")
	public static ComMessage createMessageWithCommand(int cmd,Hashtable args){
		/*String jsontext;
		jsontext="{\"cmd\":"+cmd+",\"args\":{";
		Enumeration e = args.keys();
		 while (e.hasMoreElements()) {
		  Object key = e.nextElement();
		  Object value = args.get(key);
		  jsontext=jsontext+"\""+key+"\":"+value+",";
		  // do whatever you need with the pair, like
		  // System.out.println("'" + key + "' associated with '" + value + "'");
		 }
		 jsontext=jsontext.substring(0, jsontext.length()-2)+"}}";
		 try {
			JSONObject json=new JSONObject(jsontext);
			return new ComMessage((short)cmd,json);
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}*/
		
		
		@SuppressWarnings("rawtypes")
		Hashtable map=new Hashtable();
		map.put("cmd", Integer.toString(cmd));
		map.put("args", new Gson().toJsonTree(args));
		
		return new ComMessage((short)cmd, new Gson().toJsonTree(args));
		
	}
	
    public short getMessageCmd(){    	
    	return cmd;
    }
    
    public Object getArg(String sarg){
    	try{
    		return (Object)args.getAsJsonObject().get(sarg);
    	}catch(Exception e){
    		return null;
    	
    	}
    }
    public boolean getBooleanArg(String sarg){
    	try{
    		return args.getAsJsonObject().get(sarg).getAsBoolean();
    	}catch(Exception e){
    		return false;
    	
    	}
    }
    public JsonElement getArgs(){return this.args;}
    
    public String getJson(){ return this.args.toString();}
    
    /*public JSONArray getVectorArg(String sarg){
    	try{
    		return args.getJSONArray(sarg);
    	}catch(Exception e){
    		
    	}
    }*/
	public ByteBuffer encodeMessage(){
		
		if (encodedForm!=null){			
			return encodedForm;
		}
		
		/*
		else if(args!=null){			
			StringBuilder json=new StringBuilder();              
	       // json.append("{\"cmd\":").append(id).append(",\"args\":{").append(roomsEnJSON).append("}");
	        return null;
		//	return encodeString(json.toString());
		}
		*/
		else
			return null;
	}
	
	public static void main(String args[]){
		System.out.println("test");
	}
	
	//this works to send a message json string

}
