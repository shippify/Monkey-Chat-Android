package com.criptext.security;

import java.security.AlgorithmParameters;
import java.security.SecureRandom;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import com.criptext.comunication.NewBase64;
import com.criptext.lib.KeyStoreCriptext;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

public class AESUtil {
 
    private static final String password = "criptext";
    private static String salt;
    private static int pswdIterations = 65536  ;
    private static int keySize = 256;
    private byte[] ivBytes;
    private Cipher cipherENC;
    private Cipher cipherDEC;
    
    public String strKey;
    public String strIV;
    
    public AESUtil(Context context, String sessionId) throws Exception{

        SecretKeySpec secret;
        final String key = KeyStoreCriptext.getString(context, sessionId);
        if(key.compareTo("")==0){
            System.out.println("AES - NO TIENE KEY");
            //NO TIENE KEY
            PRNGFixes.apply();
            String newCryptoSafeString=newCryptoSafeString();
            secret = new SecretKeySpec(newCryptoSafeString.getBytes("UTF-8"), "AES");
            strKey=Base64.encodeToString(newCryptoSafeString.getBytes("UTF-8"),Base64.NO_WRAP);
        }
        else{
            System.out.println("AES - YA TIENE KEY EN KEYSTORE");
            //SI TIENE KEY
            strKey=key.split(":")[0];
            secret = new SecretKeySpec(Base64.decode(strKey.getBytes("UTF-8"), Base64.NO_WRAP), "AES");
        }

        //System.out.println("strKey:***"+strKey+"***");

        cipherENC = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipherDEC = Cipher.getInstance("AES/CBC/PKCS5Padding");

        AlgorithmParameters params = cipherENC.getParameters();

        if(key.compareTo("")==0){
            System.out.println("AES - NO TIENE IV");
            //NO TIENE KEY
            if(params!=null)
                ivBytes = params.getParameterSpec(IvParameterSpec.class).getIV();
            else {
                System.out.println("AES - AlgorithmParameters es null tengo que sacarlo manualmente");
                ivBytes = iv();
            }

            strIV=Base64.encodeToString(ivBytes,Base64.NO_WRAP);

            cipherENC.init(Cipher.ENCRYPT_MODE, secret, new IvParameterSpec(ivBytes));
            cipherDEC.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(ivBytes));

            if(sessionId.length()>0)
                KeyStoreCriptext.putString(context, sessionId, strKey + ":" + strIV);
        }
        else{
            //System.out.println("AES - SI TIENE IV - ***"+KeyStoreCriptext.Companion.getString(context, sessionId).split(":")[1]+"***");
            //SI TIENE KEY
            strIV = AESUtil.stripGarbage(KeyStoreCriptext.getString(context, sessionId).split(":")[1]);
            Log.d("AESUtil", "generated key: " + strKey + " IV: " + strIV);
            cipherENC.init(Cipher.ENCRYPT_MODE, secret, new IvParameterSpec(Base64.decode(strIV.getBytes("UTF-8"), Base64.NO_WRAP)));
            cipherDEC.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(Base64.decode(strIV.getBytes("UTF-8"), Base64.NO_WRAP)));
        }

        //System.out.println("strIV:***" + strIV + "***");
    }

    public AESUtil(String keyAndIV){

        if(keyAndIV == null || keyAndIV.length() < 3 || keyAndIV.indexOf(':') == -1)
            throw new IllegalArgumentException("keyAndIV string must have this pattern <key>:<iv>");
        try {
            System.out.println("AES - SI TIENE KEY");
            //SI TIENE KEY
            String[] array = keyAndIV.split(":");
            strKey = array[0];
            strIV = array[1];
            SecretKeySpec secret = new SecretKeySpec(Base64.decode(strKey.getBytes("UTF-8"), Base64.NO_WRAP), "AES");

            cipherENC = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipherDEC = Cipher.getInstance("AES/CBC/PKCS5Padding");

            AlgorithmParameters params = cipherENC.getParameters();
            cipherENC.init(Cipher.ENCRYPT_MODE, secret, new IvParameterSpec(Base64.decode(strIV.getBytes("UTF-8"), Base64.NO_WRAP)));
            cipherDEC.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(Base64.decode(strIV.getBytes("UTF-8"), Base64.NO_WRAP)));
        } catch (Exception ex){
            ex.printStackTrace();
        }

    }

    static void generateNewKeys(Context context, String monkeyId){

        if (monkeyId == null || monkeyId.isEmpty()) {
            throw new IllegalArgumentException("MonkeyId can't be empty or null. If you don't have a Monkey ID " +
                    "use MonkeyInit class");
        }
        try {
            SecretKeySpec secret;
            System.out.println("AES - GENERATING NEW KEYS");
            //NO TIENE KEY
            PRNGFixes.apply();
            String newCryptoSafeString = newCryptoSafeString();
            secret = new SecretKeySpec(newCryptoSafeString.getBytes("UTF-8"), "AES");
            String  strKey = Base64.encodeToString(newCryptoSafeString.getBytes("UTF-8"), Base64.NO_WRAP);
            Cipher cipherENC = Cipher.getInstance("AES/CBC/PKCS5Padding");
            Cipher cipherDEC = Cipher.getInstance("AES/CBC/PKCS5Padding");

            AlgorithmParameters params = cipherENC.getParameters();
            byte[] ivBytes;
            if(params!=null)
                ivBytes = params.getParameterSpec(IvParameterSpec.class).getIV();
            else {
                System.out.println("AES - AlgorithmParameters is null");
                ivBytes = iv();
            }

            String strIV = Base64.encodeToString(ivBytes,Base64.NO_WRAP);

            cipherENC.init(Cipher.ENCRYPT_MODE, secret, new IvParameterSpec(ivBytes));
            cipherDEC.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(ivBytes));

            KeyStoreCriptext.putStringBlocking(context, monkeyId, strKey + ":" + strIV);

        } catch(Exception ex){
            ex.printStackTrace();
        }
    }
    /***/
    
    static final char[] values = new char[]{'2','3','4','5','6','7','8','9','c','v','b','n','m',
        'a','s','d','f','g','h','j','k','l','q','w','e','r','t','y','u','i','o','p'}; 
    
    public static String newCryptoSafeString(){
  	  SecureRandom random = new SecureRandom();
  	  byte[] bytes = new byte[32];
  	  random.nextBytes(bytes);
  	  String key = "";
  	  for(byte b : bytes){
  		 int i = Math.abs(b%32);
  		 key += values[i];
  	  }  	  
  	  return key;
    }
    
    /***************************************************************************/
    
    public String encrypt(String plainText) throws Exception {   
    	 
        byte[] encryptedTextBytes = cipherENC.doFinal(plainText.getBytes("UTF-8"));
        return Base64.encodeToString(encryptedTextBytes,Base64.NO_WRAP);
    }
    
    public byte[] encrypt(byte[] bytesText) throws Exception {
        return cipherENC.doFinal(bytesText);        
    }
    
    public String decrypt(String encryptedText) throws Exception {

        byte[] encryptedTextBytes = Base64.decode(encryptedText.getBytes("UTF-8"), Base64.NO_WRAP); 
        byte[] decryptedTextBytes = null;

        decryptedTextBytes = cipherDEC.doFinal(encryptedTextBytes);        
        return new String(decryptedTextBytes);
    }
    
    /***************************************************************************/
    
    public String encryptWithCustomKeyAndIV(String plainText, String key) throws Exception {
        
    	System.out.println("MONKEY - Encriptado msg con key:***"+key+"***");
    	
        SecretKeySpec secret = new SecretKeySpec(Base64.decode(key, Base64.NO_WRAP), "AES");
 
        //encrypt the message
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secret);
        
        byte[] encryptedTextBytes = cipher.doFinal(plainText.getBytes("UTF-8"));
        return Base64.encodeToString(encryptedTextBytes,Base64.NO_WRAP);
    }
 
    public static String decryptWithCustomKeyAndIV(String encryptedText, String key, String iv) throws Exception {
    	 
    	//System.out.println("MONKEY - Desencriptado msg con key:***"+key+"***iv:"+stripGarbage(iv));
    	
    	byte[] encryptedTextBytes;
    	encryptedTextBytes = NewBase64.decode(encryptedText,Base64.NO_WRAP);
    	
        SecretKeySpec secret = new SecretKeySpec(Base64.decode(key, Base64.NO_WRAP), "AES");
        
        // Decrypt the message
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(Base64.decode(stripGarbage(iv), Base64.NO_WRAP)));
 
        byte[] decryptedTextBytes = null;

        //try {
            //Si esto esta en un try catch no se manda la exception en el throws
            decryptedTextBytes = cipher.doFinal(encryptedTextBytes);
        //} catch (IllegalBlockSizeException e) {
        //    e.printStackTrace();
        //}
 
        return new String(decryptedTextBytes);
    }
    
    public byte[] decryptWithCustomKeyAndIV(byte[] encryptedTextBytes, String key, String iv) throws Exception {
   	 
    	System.out.println("MONKEY - Desencriptado msg bytes de amigo con key:***"+key+"***iv:"+stripGarbage(iv));
    	
        SecretKeySpec secret = new SecretKeySpec(Base64.decode(key, Base64.NO_WRAP), "AES");
        
        // Decrypt the message
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(Base64.decode(stripGarbage(iv), Base64.NO_WRAP)));
 
        byte[] decryptedTextBytes = null;
        try {
            decryptedTextBytes = cipher.doFinal(encryptedTextBytes);
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
 
        return decryptedTextBytes;
    }
    
    /***************************************************************************/
    
    public String encryptForTest(String plainText) throws Exception {   
         
        //get salt
        salt = generateSalt();      
        byte[] saltBytes = salt.getBytes("UTF-8");
         
        // Derive the key
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(), 
                saltBytes, 
                pswdIterations, 
                keySize
                );
 
        SecretKey secretKey = factory.generateSecret(spec);
        SecretKeySpec secret = new SecretKeySpec(secretKey.getEncoded(), "AES");
        System.out.println("xxx:key:"+Base64.encodeToString(secretKey.getEncoded(),0));
 
        //encrypt the message
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secret);
        AlgorithmParameters params = cipher.getParameters();
        ivBytes = params.getParameterSpec(IvParameterSpec.class).getIV();
        System.out.println("xxx:iv:"+Base64.encodeToString(ivBytes,0));
        
        byte[] encryptedTextBytes = cipher.doFinal(plainText.getBytes("UTF-8"));
        return Base64.encodeToString(encryptedTextBytes,0);
    }
    
    public String decryptForTest(String encryptedText) throws Exception {
 
        byte[] saltBytes = salt.getBytes("UTF-8");
        byte[] encryptedTextBytes = Base64.decode(encryptedText, 0);
 
        // Derive the key
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(), 
                saltBytes, 
                pswdIterations, 
                keySize
                );
 
        SecretKey secretKey = factory.generateSecret(spec);
        SecretKeySpec secret = new SecretKeySpec(secretKey.getEncoded(), "AES");
        
        // Decrypt the message
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(ivBytes));
     
 
        byte[] decryptedTextBytes = null;
        try {
            decryptedTextBytes = cipher.doFinal(encryptedTextBytes);
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
 
        return new String(decryptedTextBytes);
    }
   
    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte bytes[] = new byte[20];
        random.nextBytes(bytes);
        String s = new String(bytes);
        return s;
    }
    
    private static byte[] iv(){
        byte[] iv=null;
        try {
            KeyGenerator kgen = KeyGenerator.getInstance("AES");
            kgen.init(128);
            SecretKey skey = kgen.generateKey();
            iv = skey.getEncoded();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return iv;
    }

    private static String stripGarbage(String s) {
		
		StringBuilder sb = new StringBuilder(s.length());
		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);
			if ((ch >= 'A' && ch <= 'Z') || 
					(ch >= 'a' && ch <= 'z') ||
					(ch >= '0' && ch <= '9') ||
					ch == '%' || ch == '_' ||
					ch == '-' || ch == '!' ||
					ch == '.' || ch == '~' ||
					ch == '(' || ch == ')' ||
					ch == '*' || ch == '\'' ||
					ch == ';' || ch == '/' ||
					ch == '?' || ch == ':' ||
					ch == '@' || ch == '=' ||
					ch == '&' || ch == '$' ||
					ch == '+' || ch == ',') {
				sb.append(ch);	            
			}
			else
				break;
		}
		return sb.toString();
	}
}