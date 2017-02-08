package com.criptext.lib

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.amshove.kluent.`should equal`
import org.junit.Test

/**
 * Created by gabriel on 2/7/17.
 */

class `sanitize pending msgs for json file Test` {
    val parser = JsonParser()

    @Test
    fun `returns the same list if there are no errors`() {
        val json1 = parser.parse("{\"args\":{\"id\":\"-1486492225cG9\",\"rid\":\"imic29drtsv4z2nj5n42huxr\",\"msg\":\"hYOCsLnnI7SEX4lDs3FKmw==\",\"type\":\"1\",\"push\":\"{\\\"text\\\":\\\"Gabriel  sent you a message\\\",\\\"iosData\\\":{\\\"alert\\\":{\\\"loc-key\\\":\\\"pushtextKey\\\",\\\"loc-args\\\":\\\"[Gabriel ]\\\",\\\"session-id\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\",\\\"uid\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"},\\\"sound\\\":\\\"default\\\",\\\"category\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"},\\\"andData\\\":{\\\"loc-key\\\":\\\"pushtextKey\\\",\\\"loc-args\\\":\\\"[Gabriel ]\\\",\\\"session-id\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\",\\\"uid\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"}}\",\"params\":\"{}\",\"props\":\"{\\\"str\\\":\\\"0\\\",\\\"encr\\\":\\\"1\\\",\\\"device\\\":\\\"android\\\",\\\"old_id\\\":\\\"-1486492225cG9\\\"}\"},\"cmd\":200}").asJsonObject
        val json2 = parser.parse("{\"args\":{\"id\":\"-1986472235dC7\",\"rid\":\"imic29drtsv4z2nj5n42huxr\",\"msg\":\"iTOXsUmnI7SAX4lDs3FKap==\",\"type\":\"1\",\"push\":\"{\\\"text\\\":\\\"Gabriel  sent you a message\\\",\\\"iosData\\\":{\\\"alert\\\":{\\\"loc-key\\\":\\\"pushtextKey\\\",\\\"loc-args\\\":\\\"[Gabriel ]\\\",\\\"session-id\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\",\\\"uid\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"},\\\"sound\\\":\\\"default\\\",\\\"category\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"},\\\"andData\\\":{\\\"loc-key\\\":\\\"pushtextKey\\\",\\\"loc-args\\\":\\\"[Gabriel ]\\\",\\\"session-id\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\",\\\"uid\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"}}\",\"params\":\"{}\",\"props\":\"{\\\"str\\\":\\\"0\\\",\\\"encr\\\":\\\"1\\\",\\\"device\\\":\\\"android\\\",\\\"old_id\\\":\\\"-1486492225cG9\\\"}\"},\"cmd\":200}").asJsonObject
        val json3 = parser.parse("{\"args\":{\"id\":\"-1985371535qL4\",\"rid\":\"imic29drtsv4z2nj5n42huxr\",\"msg\":\"vFMZsUvnI9BAH4lDs3AMwo==\",\"type\":\"1\",\"push\":\"{\\\"text\\\":\\\"Gabriel  sent you a message\\\",\\\"iosData\\\":{\\\"alert\\\":{\\\"loc-key\\\":\\\"pushtextKey\\\",\\\"loc-args\\\":\\\"[Gabriel ]\\\",\\\"session-id\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\",\\\"uid\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"},\\\"sound\\\":\\\"default\\\",\\\"category\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"},\\\"andData\\\":{\\\"loc-key\\\":\\\"pushtextKey\\\",\\\"loc-args\\\":\\\"[Gabriel ]\\\",\\\"session-id\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\",\\\"uid\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"}}\",\"params\":\"{}\",\"props\":\"{\\\"str\\\":\\\"0\\\",\\\"encr\\\":\\\"1\\\",\\\"device\\\":\\\"android\\\",\\\"old_id\\\":\\\"-1486492225cG9\\\"}\"},\"cmd\":200}").asJsonObject

        val list = MonkeyJson.sanitizePendingMsgsForFile(listOf(json1, json2, json3))

        list.size `should equal` 3

        list[0].toString() `should equal` json1.toString()
        list[1].toString() `should equal` json2.toString()
        list[2].toString() `should equal` json3.toString()
    }

    @Test
    fun `ignores empty json objects and objects without the "args" attribute`() {
        val json1 = parser.parse("{\"args\":{\"id\":\"-1486492225cG9\",\"rid\":\"imic29drtsv4z2nj5n42huxr\",\"msg\":\"hYOCsLnnI7SEX4lDs3FKmw==\",\"type\":\"1\",\"push\":\"{\\\"text\\\":\\\"Gabriel  sent you a message\\\",\\\"iosData\\\":{\\\"alert\\\":{\\\"loc-key\\\":\\\"pushtextKey\\\",\\\"loc-args\\\":\\\"[Gabriel ]\\\",\\\"session-id\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\",\\\"uid\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"},\\\"sound\\\":\\\"default\\\",\\\"category\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"},\\\"andData\\\":{\\\"loc-key\\\":\\\"pushtextKey\\\",\\\"loc-args\\\":\\\"[Gabriel ]\\\",\\\"session-id\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\",\\\"uid\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"}}\",\"params\":\"{}\",\"props\":\"{\\\"str\\\":\\\"0\\\",\\\"encr\\\":\\\"1\\\",\\\"device\\\":\\\"android\\\",\\\"old_id\\\":\\\"-1486492225cG9\\\"}\"},\"cmd\":200}").asJsonObject
        val json2 = parser.parse("{}").asJsonObject
        val json3 = parser.parse("{\"vargs\":{\"id\":\"-1985371535qL4\",\"rid\":\"imic29drtsv4z2nj5n42huxr\",\"msg\":\"vFMZsUvnI9BAH4lDs3AMwo==\",\"type\":\"1\",\"push\":\"{\\\"text\\\":\\\"Gabriel  sent you a message\\\",\\\"iosData\\\":{\\\"alert\\\":{\\\"loc-key\\\":\\\"pushtextKey\\\",\\\"loc-args\\\":\\\"[Gabriel ]\\\",\\\"session-id\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\",\\\"uid\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"},\\\"sound\\\":\\\"default\\\",\\\"category\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"},\\\"andData\\\":{\\\"loc-key\\\":\\\"pushtextKey\\\",\\\"loc-args\\\":\\\"[Gabriel ]\\\",\\\"session-id\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\",\\\"uid\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"}}\",\"params\":\"{}\",\"props\":\"{\\\"str\\\":\\\"0\\\",\\\"encr\\\":\\\"1\\\",\\\"device\\\":\\\"android\\\",\\\"old_id\\\":\\\"-1486492225cG9\\\"}\"},\"cmd\":200}").asJsonObject
        val json4 = JsonObject()
        val json5 = parser.parse("{\"args\":{\"id\":\"-1986472235dC7\",\"rid\":\"imic29drtsv4z2nj5n42huxr\",\"msg\":\"iTOXsUmnI7SAX4lDs3FKap==\",\"type\":\"1\",\"push\":\"{\\\"text\\\":\\\"Gabriel  sent you a message\\\",\\\"iosData\\\":{\\\"alert\\\":{\\\"loc-key\\\":\\\"pushtextKey\\\",\\\"loc-args\\\":\\\"[Gabriel ]\\\",\\\"session-id\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\",\\\"uid\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"},\\\"sound\\\":\\\"default\\\",\\\"category\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"},\\\"andData\\\":{\\\"loc-key\\\":\\\"pushtextKey\\\",\\\"loc-args\\\":\\\"[Gabriel ]\\\",\\\"session-id\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\",\\\"uid\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"}}\",\"params\":\"{}\",\"props\":\"{\\\"str\\\":\\\"0\\\",\\\"encr\\\":\\\"1\\\",\\\"device\\\":\\\"android\\\",\\\"old_id\\\":\\\"-1486492225cG9\\\"}\"},\"cmd\":200}").asJsonObject

        val list = MonkeyJson.sanitizePendingMsgsForFile(listOf(json1, json2, json3, json4, json5))

        list.size `should equal` 2

        list[0].toString() `should equal` json1.toString()
        list[1].toString() `should equal` json5.toString()
    }

}