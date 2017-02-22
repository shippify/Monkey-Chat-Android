package com.criptext.security;

import android.content.Context;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/**
 * Created by gesuwall on 12/23/16.
 */

@Implements (AESUtil.class)
public class ShadowAESUtil {

        @Implementation
        public static String decryptWithCustomKeyAndIV(String encryptedText, String key, String iv) {
            return "decrypted:" + encryptedText;

        }

    @Implementation
    public void __constructor__(String keyAndIV) {

    }


    @Implementation
    public void __constructor__(Context ctx, String keyAndIV) {

    }

    @Implementation
    public String decrypt(String inputString){
        return inputString;
    }

}