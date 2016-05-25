package com.criptext.lib;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import java.util.UUID;

/**
 * Created by daniel on 12/11/15.
 */

public class KeyStoreCriptext {

    public static void putString(Context context, String key, String value){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(key,KeyStoreCriptext.encryptString(value)).apply();
    }

    public static String getString(Context context,String key){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String value = prefs.getString(key,"");
        if(value.equals(""))
            return "";
        else
            return KeyStoreCriptext.decryptString(value);
    }

    public static void putInt(Context context, String key, int value){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putInt(key, value).apply();
    }

    public static int getInt(Context context,String key){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(key,0);
    }

    public static String encryptString(String initialText) {
        String uuid1 = UUID.randomUUID().toString().substring(0,1);
        String uuid2 = UUID.randomUUID().toString().substring(0,1);
        String encryptedText=uuid1+initialText+uuid2;
        return encryptedText;
    }

    public static String decryptString(String cipherText) {
        String decryptedText=cipherText.substring(1,cipherText.length()-1);
        return decryptedText;
    }

}
