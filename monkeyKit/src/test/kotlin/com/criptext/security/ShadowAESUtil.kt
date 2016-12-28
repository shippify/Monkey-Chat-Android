package com.criptext.security

import android.content.Context
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

/**
 * Created by gesuwall on 12/23/16.
 */

@Implements (AESUtil::class)
class ShadowAESUtil {

    companion object {
        @Implementation
        fun decryptWithCustomKeyAndIV(encryptedText: String, key: String, iv: String) {

        }
    }

    @Implementation
    fun __constructor__(keyAndIV: String) {

    }


    @Implementation
    fun __constructor__(ctx: Context?, keyAndIV: String) {

    }

    @Implementation
    fun decrypt(inputString: String): String {
        return inputString
    }

}