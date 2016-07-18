package com.criptext.comunication;


import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;



/** TODO: is there anything specific to add here? */
public class MessageManager{

	public static final String MESSAGE_CHARSET = "UTF-8";

	public MessageManager(){
	}	    
	public static ComMessage decodeMessage(ByteBuffer messageBuffer) {
		try {
			byte[] bytes = new byte[messageBuffer.remaining()];
			messageBuffer.get(bytes);
			//return jsonDecode(new String(Base64Coder.encode(bytes)));

			return jsonDecode( new String(bytes, MESSAGE_CHARSET));
		}catch (Exception e) {
			return null;	            	                
		}        	         
	}	    
	public static ComMessage decodeMessage(byte[] arg0)
	{			

		return jsonDecode(decodeToString(arg0));


	}	
	public static String decodeToString(byte[] arg0){
		try {            
			return new String(arg0, MESSAGE_CHARSET);
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}
	public static ComMessage jsonDecode(String jsonText)
	{ 		
		if(jsonText==null)
			return null;

		try {   

			System.out.println("Socket - Recieving Message: "+jsonText);
			//MessageManager.splitStringEvery(jsonText,jsonText.length()>1000?1000:jsonText.length()-1);
			
			JsonElement jelement = new JsonParser().parse(jsonText);
			JsonObject  jobject = jelement.getAsJsonObject();

			String tmp = jobject.get("cmd").getAsString();
			short cmd = new Short(tmp).shortValue();
			JsonObject jsonObj = jobject.get("args").getAsJsonObject();

			return new ComMessage(cmd , jsonObj);
		}
		catch(Exception e){		
			e.printStackTrace();
			return null;
		}		     
	}	
	public static byte[] encodeString(String json) {
		try {
			return json.getBytes(MESSAGE_CHARSET);
		} catch (UnsupportedEncodingException e) {
			System.out.println("exception"+e+", aqui excepcion");
			return null;
			//throw new Error("Required character set " + MESSAGE_CHARSET +             " not found", e);
		}
	}

	public static String[] splitStringEvery(String s, int interval) {
	    int arrayLength = (int) Math.ceil(((s.length() / (double)interval)));
	    String[] result = new String[arrayLength];

	    int j = 0;
	    int lastIndex = result.length - 1;
	    for (int i = 0; i < lastIndex; i++) {
	        result[i] = s.substring(j, j + interval);
	        j += interval;
	        System.out.println(result[i]);
	    } 
	    System.out.println(result[lastIndex]);
	    result[lastIndex] = s.substring(j);

	    return result;
	}
}
