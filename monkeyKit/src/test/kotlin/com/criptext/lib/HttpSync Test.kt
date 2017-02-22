package com.criptext.lib

import com.criptext.ClientData
import com.criptext.FakeContext
import com.criptext.ShadowOkHttpClient
import com.criptext.ShadowResponseBody
import com.criptext.http.HttpSync
import com.criptext.security.AESUtil
import com.criptext.security.ShadowAESUtil
import org.amshove.kluent.`should equal`
import org.amshove.kluent.`should not be`
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * Created by gesuwall on 2/22/17.
 */

@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, shadows=arrayOf(ShadowOkHttpClient::class,
        ShadowAESUtil::class, ShadowResponseBody::class, ShadowKeyStore::class), manifest = "TestManifest.xml")
class `HttpSync Test` {
    val completeResponse = """{"data":{"messages":[{"rid":"G:idkgwf6ghcmyfvvrxqiwwmi-Aeroiyx5clkpf5bce228minhr529","app":"idkgwf6ghcmyfvvrxqiwwmi","msg":"VDtylu9dIPQpSd5q1y4EUQ==","datetime":"1487781741","type":"1","params":"null","props":"{\"device\":\"web\",\"encr\":1,\"encoding\":\"utf8\"}","sid":"imic29drtsv4z2nj5n42huxr","id":"23981846"},{"params":"null","props":"{\"device\":\"web\",\"encr\":1,\"encoding\":\"utf8\"}","datetime":"1487781743","rid":"G:idkgwf6ghcmyfvvrxqiwwmi-Aeroiyx5clkpf5bce228minhr529","app":"idkgwf6ghcmyfvvrxqiwwmi","type":"1","sid":"imic29drtsv4z2nj5n42huxr","msg":"Hml7eH5PSAYkxzUWVOM5sg==","id":"23981850"},{"datetime":"1487781745","msg":"bt3ZGZvqiJcieJzz2eCMuQ==","rid":"G:idkgwf6ghcmyfvvrxqiwwmi-Aeroiyx5clkpf5bce228minhr529","app":"idkgwf6ghcmyfvvrxqiwwmi","sid":"imic29drtsv4z2nj5n42huxr","type":"1","props":"{\"device\":\"web\",\"encr\":1,\"encoding\":\"utf8\"}","params":"null","id":"23981852"},{"app":"idkgwf6ghcmyfvvrxqiwwmi","params":"null","datetime":"1487781747","rid":"G:idkgwf6ghcmyfvvrxqiwwmi-Aeroiyx5clkpf5bce228minhr529","sid":"imic29drtsv4z2nj5n42huxr","props":"{\"device\":\"web\",\"encr\":1,\"encoding\":\"utf8\"}","type":"1","msg":"vdfTubFs/UG8jGyoyomvgw==","id":"23981855"},{"datetime":"1487781764","rid":"ikitzrp5cszzy0qctq4ims4i","props":"{\"device\":\"web\",\"encr\":1,\"encoding\":\"utf8\"}","msg":"SN1O072A2GwG0WY+z0CQ+w==","sid":"idm0yzeb459zpefgg3rw9udi","app":"idkgwf6ghcmyfvvrxqiwwmi","params":"null","type":"1","id":"23981861"},{"rid":"ikitzrp5cszzy0qctq4ims4i","datetime":"1487781768","type":"1","props":"{\"device\":\"web\",\"encr\":1,\"encoding\":\"utf8\"}","msg":"lHYBssQKEYLgHY5KicFMMw==","params":"null","sid":"imic29drtsv4z2nj5n42huxr","app":"idkgwf6ghcmyfvvrxqiwwmi","id":"23981867"},{"app":"idkgwf6ghcmyfvvrxqiwwmi","msg":"O/SP0dyt2Of9oHaHVC8WGw==","datetime":"1487781768","params":"null","type":"1","props":"{\"device\":\"web\",\"encr\":1,\"encoding\":\"utf8\"}","rid":"ikitzrp5cszzy0qctq4ims4i","sid":"imic29drtsv4z2nj5n42huxr","id":"23981868"},{"msg":"uQMgZa6Prax+SW5BDSSOCA==","type":"1","params":"null","props":"{\"device\":\"web\",\"encr\":1,\"encoding\":\"utf8\"}","rid":"ikitzrp5cszzy0qctq4ims4i","sid":"imic29drtsv4z2nj5n42huxr","app":"idkgwf6ghcmyfvvrxqiwwmi","datetime":"1487781769","id":"23981870"},{"params":"null","datetime":"1487781770","sid":"imic29drtsv4z2nj5n42huxr","msg":"p4PMMcQrOahzhYYQfR3nDg==","props":"{\"device\":\"web\",\"encr\":1,\"encoding\":\"utf8\"}","rid":"ikitzrp5cszzy0qctq4ims4i","type":"1","app":"idkgwf6ghcmyfvvrxqiwwmi","id":"23981873"},{"sid":"imic29drtsv4z2nj5n42huxr","datetime":"1487781770","rid":"ikitzrp5cszzy0qctq4ims4i","params":"null","type":"1","msg":"OLlqK19HF0DSKhVfKUZ3Nw==","props":"{\"device\":\"web\",\"encr\":1,\"encoding\":\"utf8\"}","app":"idkgwf6ghcmyfvvrxqiwwmi","id":"23981875"}],"remaining":0}}"""

    val responsePart1 = """{"data":{"messages":[{"rid":"G:idkgwf6ghcmyfvvrxqiwwmi-Aeroiyx5clkpf5bce228minhr529","app":"idkgwf6ghcmyfvvrxqiwwmi","msg":"VDtylu9dIPQpSd5q1y4EUQ==","datetime":"1487781741","type":"1","params":"null","props":"{\"device\":\"web\",\"encr\":1,\"encoding\":\"utf8\"}","sid":"imic29drtsv4z2nj5n42huxr","id":"23981846"},{"params":"null","props":"{\"device\":\"web\",\"encr\":1,\"encoding\":\"utf8\"}","datetime":"1487781743","rid":"G:idkgwf6ghcmyfvvrxqiwwmi-Aeroiyx5clkpf5bce228minhr529","app":"idkgwf6ghcmyfvvrxqiwwmi","type":"1","sid":"imic29drtsv4z2nj5n42huxr","msg":"Hml7eH5PSAYkxzUWVOM5sg==","id":"23981850"},{"datetime":"1487781745","msg":"bt3ZGZvqiJcieJzz2eCMuQ==","rid":"G:idkgwf6ghcmyfvvrxqiwwmi-Aeroiyx5clkpf5bce228minhr529","app":"idkgwf6ghcmyfvvrxqiwwmi","sid":"imic29drtsv4z2nj5n42huxr","type":"1","props":"{\"device\":\"web\",\"encr\":1,\"encoding\":\"utf8\"}","params":"null","id":"23981852"},{"app":"idkgwf6ghcmyfvvrxqiwwmi","params":"null","datetime":"1487781747","rid":"G:idkgwf6ghcmyfvvrxqiwwmi-Aeroiyx5clkpf5bce228minhr529","sid":"imic29drtsv4z2nj5n42huxr","props":"{\"device\":\"web\",\"encr\":1,\"encoding\":\"utf8\"}","type":"1","msg":"vdfTubFs/UG8jGyoyomvgw==","id":"23981855"},{"datetime":"1487781764","rid":"ikitzrp5cszzy0qctq4ims4i","props":"{\"device\":\"web\",\"encr\":1,\"encoding\":\"utf8\"}","msg":"SN1O072A2GwG0WY+z0CQ+w==","sid":"idm0yzeb459zpefgg3rw9udi","app":"idkgwf6ghcmyfvvrxqiwwmi","params":"null","type":"1","id":"23981861"}],"remaining":5}}"""
    val responsePart2 = """{"data":{"messages":[{"rid":"ikitzrp5cszzy0qctq4ims4i","datetime":"1487781768","type":"1","props":"{\"device\":\"web\",\"encr\":1,\"encoding\":\"utf8\"}","msg":"lHYBssQKEYLgHY5KicFMMw==","params":"null","sid":"imic29drtsv4z2nj5n42huxr","app":"idkgwf6ghcmyfvvrxqiwwmi","id":"23981867"},{"app":"idkgwf6ghcmyfvvrxqiwwmi","msg":"O/SP0dyt2Of9oHaHVC8WGw==","datetime":"1487781768","params":"null","type":"1","props":"{\"device\":\"web\",\"encr\":1,\"encoding\":\"utf8\"}","rid":"ikitzrp5cszzy0qctq4ims4i","sid":"imic29drtsv4z2nj5n42huxr","id":"23981868"},{"msg":"uQMgZa6Prax+SW5BDSSOCA==","type":"1","params":"null","props":"{\"device\":\"web\",\"encr\":1,\"encoding\":\"utf8\"}","rid":"ikitzrp5cszzy0qctq4ims4i","sid":"imic29drtsv4z2nj5n42huxr","app":"idkgwf6ghcmyfvvrxqiwwmi","datetime":"1487781769","id":"23981870"},{"params":"null","datetime":"1487781770","sid":"imic29drtsv4z2nj5n42huxr","msg":"p4PMMcQrOahzhYYQfR3nDg==","props":"{\"device\":\"web\",\"encr\":1,\"encoding\":\"utf8\"}","rid":"ikitzrp5cszzy0qctq4ims4i","type":"1","app":"idkgwf6ghcmyfvvrxqiwwmi","id":"23981873"},{"sid":"imic29drtsv4z2nj5n42huxr","datetime":"1487781770","rid":"ikitzrp5cszzy0qctq4ims4i","params":"null","type":"1","msg":"OLlqK19HF0DSKhVfKUZ3Nw==","props":"{\"device\":\"web\",\"encr\":1,\"encoding\":\"utf8\"}","app":"idkgwf6ghcmyfvvrxqiwwmi","id":"23981875"}],"remaining":0}}"""

    @Test
    fun `should sync with a single response`() {
        ShadowOkHttpClient.pushResponse(completeResponse)
        val httpSync = HttpSync(FakeContext(),
                ClientData("Test User", "appid", "appkey", "myId", "secure.criptxt.com", 8080),
                AESUtil(FakeContext(), "myid"))
        val batch = httpSync.execute(0L, 50)
        var receivedMessages = 0
        batch.newMessages.forEach { pair ->
            receivedMessages += pair.value.size
        }
        receivedMessages `should equal` 10
    }

    @Test
    fun `should sync with a multiple responses`() {
        ShadowOkHttpClient.pushResponse(responsePart1)
        ShadowOkHttpClient.pushResponse(responsePart2)
        val httpSync = HttpSync(FakeContext(),
                ClientData("Test User", "appid", "appkey", "myId", "secure.criptxt.com", 8080),
                AESUtil(FakeContext(), "myid"))
        val batch = httpSync.execute(0L, 50)
        var receivedMessages = 0
        batch.newMessages.forEach { pair ->
            receivedMessages += pair.value.size
        }
        receivedMessages `should equal` 10
    }

    @Test
    fun `should sync with network errors and multiple responses`() {
        ShadowOkHttpClient.pushException(IOException("bad connection"))
        ShadowOkHttpClient.pushResponse(responsePart1)
        ShadowOkHttpClient.pushException(IOException("bad connection"))
        ShadowOkHttpClient.pushResponse(responsePart2)
        val httpSync = HttpSync(FakeContext(),
                ClientData("Test User", "appid", "appkey", "myId", "secure.criptxt.com", 8080),
                AESUtil(FakeContext(), "myid"))
        val batch = httpSync.execute(0L, 50)
        var receivedMessages = 0
        batch.newMessages.forEach { pair ->
            receivedMessages += pair.value.size
        }
        receivedMessages `should equal` 10
    }
}