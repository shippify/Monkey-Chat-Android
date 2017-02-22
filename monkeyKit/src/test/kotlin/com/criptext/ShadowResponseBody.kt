package com.criptext

import okhttp3.ResponseBody
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import java.io.IOException

/**
 * Created by gesuwall on 2/22/17.
 */

@Implements(ResponseBody::class)
class ShadowResponseBody {

    lateinit var responseString: String
    private var consumed = false

    fun string(): String {
        if (consumed)
            throw IOException("response is closed")
        consumed = true
        return responseString
    }

}