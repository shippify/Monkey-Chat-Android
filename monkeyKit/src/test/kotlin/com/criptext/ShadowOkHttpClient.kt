package com.criptext

import com.criptext.http.MonkeyKitAPI
import com.github.kittinunf.result.Result
import okhttp3.*
import okio.BufferedSource
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.internal.ShadowExtractor
import java.util.*

/**
 * Created by gesuwall on 2/22/17.
 */

@Implements(OkHttpClient::class)
class ShadowOkHttpClient {


    companion object {
        //A queue might be eventually better
        val networkQueue: Queue<Result<Response, Exception>> = LinkedList()

        fun pushResponse(resStr: String) {
            val nextRespBody = object: ResponseBody() {
                override fun contentLength() = resStr.length.toLong()

                override fun contentType() = MediaType.parse("text/plain; charset=utf-8 ")

                override fun source(): BufferedSource {
                    throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
                }

                override fun close() {
                }

            }
            val shadowResponseBody = ShadowExtractor.extract(nextRespBody) as ShadowResponseBody
            shadowResponseBody.responseString = resStr

            networkQueue.offer(Result.of {
                Response.Builder()
                    .request(MonkeyKitAPI.getConversations("asd", 1, 0L)) //Whatever
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .body(nextRespBody).build()
            })
        }

        fun pushException(e: Exception) {
            networkQueue.offer(Result.error(e))
        }
    }

    @Implementation
    fun newCall(request: Request): Call {
        return object : Call {
            override fun cancel() { }

            override fun execute(): Response {
                val res = networkQueue.poll()
                return when (res) {
                    is Result.Success -> res.value
                    is Result.Failure -> throw res.error
                }
            }

            override fun isCanceled() = false

            override fun isExecuted() = false

            override fun request() = request

            override fun enqueue(responseCallback: Callback) {

            }

        }
    }


}