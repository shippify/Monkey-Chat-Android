package com.criptext.lib;

import java.io.UnsupportedEncodingException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

import android.util.Base64;
import android.util.Log;


public class RSAUtil {
	
	public byte []encodedPubKey;

    //FOR PUBLIC AND PRIVATE GENERATED KEYS
    PublicKey pubKey;
    PrivateKey privKey;
    String pubKeyStr;
    String privKeyStr;

    public RSAUtil(byte []encodedPubKey){
		this.encodedPubKey=encodedPubKey;
	}

    public RSAUtil(){

    }
	
	public String encrypt(String original) {
		
		try {
			
			X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(this.encodedPubKey);
			KeyFactory kf = KeyFactory.getInstance("RSA");
			PublicKey pkPublic = kf.generatePublic(publicKeySpec);
			
			Cipher pkCipher = Cipher.getInstance("RSA/ECB/PKCS1PADDING");
			pkCipher.init(Cipher.ENCRYPT_MODE, pkPublic);
			
			//String stemp=encodeURIComponent(original);
			byte[] encryptedInByte = pkCipher.doFinal(original.getBytes());
			
			String encryptedInString = new String(Base64.encodeToString(encryptedInByte,0));			
			return encryptedInString;
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return "";

    }
	
	public String desencrypt(String original){
		
		try {

			byte[] rsaEncodedMessage = Base64.decode(original,0);
			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1PADDING");
			Log.d("RSADecrypt", "byte size: " + rsaEncodedMessage.length);
			cipher.init(Cipher.DECRYPT_MODE, privKey);

			byte[] desencryptedInByte  = cipher.doFinal(rsaEncodedMessage);
			
			String desencryptedInString = new String(desencryptedInByte,"UTF_8");	
			//System.out.println("ANDROID   mas: "+desencryptedInString);
			desencryptedInString=stripGarbage(desencryptedInString);
			
			if(desencryptedInString.endsWith("%"))
				desencryptedInString=desencryptedInString.substring(0, desencryptedInString.length()-1);
				
			String stemp=decodeURIComponent(desencryptedInString);
			
			//System.out.println("RSA-DESENCRIPTADO : "+stemp);
			return stemp;
			
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		return "";
	}

	public static String stripGarbage(String s) {
		s=s.replace("+", "%2B");
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
	
	public static final String ALLOWED_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.!~*'()";

	public static String encodeURIComponent(String input) {
		
		if(input.isEmpty()) {
			return input;
			}
		int l = input.length();
		StringBuilder o = new StringBuilder(l * 3);
		try {
			for (int i = 0; i < l; i++) {
				String e = input.substring(i, i + 1);
				if (ALLOWED_CHARS.indexOf(e) == -1) {
					byte[] b = e.getBytes("utf-8");
					o.append(getHex(b));
					continue;
				}
				o.append(e);
		}
			return o.toString();
		} catch(UnsupportedEncodingException e) {
    	   e.printStackTrace();
		}
		return input;
	}

	private static String getHex(byte buf[]) {
		StringBuilder o = new StringBuilder(buf.length * 3);
		for (int i = 0; i < buf.length; i++) {
			int n = (int) buf[i] & 0xff;
			o.append("%");
			if (n < 0x10) {
				o.append("0");
			}
			o.append(Long.toString(n, 16).toUpperCase());
		}
		return o.toString();
	}
	
	public static String decodeURIComponent(String encodedURI) {
		char actualChar;

		StringBuffer buffer = new StringBuffer();
		int bytePattern, sumb = 0;
		for (int i = 0, more = -1; i < encodedURI.length(); i++) {
			actualChar = encodedURI.charAt(i);

			switch (actualChar) {
			case '%': {
				actualChar = encodedURI.charAt(++i);
				int hb = (Character.isDigit(actualChar) ? actualChar - '0'
						: 10 + Character.toLowerCase(actualChar) - 'a') & 0xF;
				actualChar = encodedURI.charAt(++i);
				int lb = (Character.isDigit(actualChar) ? actualChar - '0'
						: 10 + Character.toLowerCase(actualChar) - 'a') & 0xF;
				bytePattern = (hb << 4) | lb;
				break;
			}
			case '+': {
				bytePattern = ' ';
				break;
			}
			default: {
				bytePattern = actualChar;
			}
		}

		if ((bytePattern & 0xc0) == 0x80) { // 10xxxxxx
			sumb = (sumb << 6) | (bytePattern & 0x3f);
			if (--more == 0)
				buffer.append((char) sumb);
			} else if ((bytePattern & 0x80) == 0x00) { // 0xxxxxxx
				buffer.append((char) bytePattern);
			} else if ((bytePattern & 0xe0) == 0xc0) { // 110xxxxx
					sumb = bytePattern & 0x1f;
					more = 1;
			} else if ((bytePattern & 0xf0) == 0xe0) { // 1110xxxx
				sumb = bytePattern & 0x0f;
				more = 2;
			} else if ((bytePattern & 0xf8) == 0xf0) { // 11110xxx
				sumb = bytePattern & 0x07;
				more = 3;
			} else if ((bytePattern & 0xfc) == 0xf8) { // 111110xx
				sumb = bytePattern & 0x03;
				more = 4;
			} else { // 1111110x
				sumb = bytePattern & 0x01;
				more = 5;
			}
			}
		return buffer.toString();
	}

    public void generateKeys(){
        try {
            KeyPairGenerator generator;
            generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(1024, new SecureRandom());
            KeyPair pair = generator.generateKeyPair();
            pubKey = pair.getPublic();
            privKey = pair.getPrivate();

            byte[] publicKeyBytes = pubKey.getEncoded();
            pubKeyStr = Base64.encodeToString(publicKeyBytes,Base64.NO_WRAP);
            byte[] privKeyBytes = privKey.getEncoded();
            privKeyStr = Base64.encodeToString(privKeyBytes,Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
