package com.criptext.lib

import org.amshove.kluent.`should equal`
import org.junit.Test

/**
 * Created by gabriel on 2/7/17.
 */

class `Parse pending msgs from file Test` {
    val separator = "\n{sp]\n"

    @Test
    fun `returns the same list if there are no errors`() {
        val json1 = "{\"args\":{\"id\":\"-1486492225cG9\",\"rid\":\"imic29drtsv4z2nj5n42huxr\",\"msg\":\"hYOCsLnnI7SEX4lDs3FKmw==\",\"type\":\"1\",\"push\":\"{\\\"text\\\":\\\"Gabriel  sent you a message\\\",\\\"iosData\\\":{\\\"alert\\\":{\\\"loc-key\\\":\\\"pushtextKey\\\",\\\"loc-args\\\":\\\"[Gabriel ]\\\",\\\"session-id\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\",\\\"uid\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"},\\\"sound\\\":\\\"default\\\",\\\"category\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"},\\\"andData\\\":{\\\"loc-key\\\":\\\"pushtextKey\\\",\\\"loc-args\\\":\\\"[Gabriel ]\\\",\\\"session-id\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\",\\\"uid\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"}}\",\"params\":\"{}\",\"props\":\"{\\\"str\\\":\\\"0\\\",\\\"encr\\\":\\\"1\\\",\\\"device\\\":\\\"android\\\",\\\"old_id\\\":\\\"-1486492225cG9\\\"}\"},\"cmd\":200}"
        val json2 = "{\"args\":{\"id\":\"-1986472235dC7\",\"rid\":\"imic29drtsv4z2nj5n42huxr\",\"msg\":\"iTOXsUmnI7SAX4lDs3FKap==\",\"type\":\"1\",\"push\":\"{\\\"text\\\":\\\"Gabriel  sent you a message\\\",\\\"iosData\\\":{\\\"alert\\\":{\\\"loc-key\\\":\\\"pushtextKey\\\",\\\"loc-args\\\":\\\"[Gabriel ]\\\",\\\"session-id\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\",\\\"uid\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"},\\\"sound\\\":\\\"default\\\",\\\"category\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"},\\\"andData\\\":{\\\"loc-key\\\":\\\"pushtextKey\\\",\\\"loc-args\\\":\\\"[Gabriel ]\\\",\\\"session-id\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\",\\\"uid\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"}}\",\"params\":\"{}\",\"props\":\"{\\\"str\\\":\\\"0\\\",\\\"encr\\\":\\\"1\\\",\\\"device\\\":\\\"android\\\",\\\"old_id\\\":\\\"-1486492225cG9\\\"}\"},\"cmd\":200}"
        val json3 = "{\"args\":{\"id\":\"-1985371535qL4\",\"rid\":\"imic29drtsv4z2nj5n42huxr\",\"msg\":\"vFMZsUvnI9BAH4lDs3AMwo==\",\"type\":\"1\",\"push\":\"{\\\"text\\\":\\\"Gabriel  sent you a message\\\",\\\"iosData\\\":{\\\"alert\\\":{\\\"loc-key\\\":\\\"pushtextKey\\\",\\\"loc-args\\\":\\\"[Gabriel ]\\\",\\\"session-id\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\",\\\"uid\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"},\\\"sound\\\":\\\"default\\\",\\\"category\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"},\\\"andData\\\":{\\\"loc-key\\\":\\\"pushtextKey\\\",\\\"loc-args\\\":\\\"[Gabriel ]\\\",\\\"session-id\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\",\\\"uid\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"}}\",\"params\":\"{}\",\"props\":\"{\\\"str\\\":\\\"0\\\",\\\"encr\\\":\\\"1\\\",\\\"device\\\":\\\"android\\\",\\\"old_id\\\":\\\"-1486492225cG9\\\"}\"},\"cmd\":200}"

        val fileContents = "$json1$separator$json2$separator$json3"

        val list = MonkeyJson.parsePendingMsgsFromFile(fileContents, separator)

        list.size `should equal` 3

        list[0].toString() `should equal` json1
        list[1].toString() `should equal` json2
        list[2].toString() `should equal` json3
    }

    @Test
    fun `returns only successfully parsed json objects`() {
        val malformed1 = "\"args\":{\"id\":\"-1486492225cG9\",\"rid\":\"imic29drtsv4z2nj5n42huxr\",\"msg\":\"hYOCsLnnI7SEX4lDs3FKmw==\",\"type\":\"1\",\"push\":\"{\\\"text\\\":\\\"Gabriel  sent you a message\\\",\\\"iosData\\\":{\\\"alert\\\":{\\\"loc-key\\\":\\\"pushtextKey\\\",\\\"loc-args\\\":\\\"[Gabriel ]\\\",\\\"session-id\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\",\\\"uid\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"},\\\"sound\\\":\\\"default\\\",\\\"category\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"},\\\"andData\\\":{\\\"loc-key\\\":\\\"pushtextKey\\\",\\\"loc-args\\\":\\\"[Gabriel ]\\\",\\\"session-id\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\",\\\"uid\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"}}\",\"params\":\"{}\",\"props\":\"{\\\"str\\\":\\\"0\\\",\\\"encr\\\":\\\"1\\\",\\\"device\\\":\\\"android\\\",\\\"old_id\\\":\\\"-1486492225cG9\\\"}\"},\"cmd\":200}"
        val json2 = "{\"args\":{\"id\":\"-1986472235dC7\",\"rid\":\"imic29drtsv4z2nj5n42huxr\",\"msg\":\"iTOXsUmnI7SAX4lDs3FKap==\",\"type\":\"1\",\"push\":\"{\\\"text\\\":\\\"Gabriel  sent you a message\\\",\\\"iosData\\\":{\\\"alert\\\":{\\\"loc-key\\\":\\\"pushtextKey\\\",\\\"loc-args\\\":\\\"[Gabriel ]\\\",\\\"session-id\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\",\\\"uid\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"},\\\"sound\\\":\\\"default\\\",\\\"category\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"},\\\"andData\\\":{\\\"loc-key\\\":\\\"pushtextKey\\\",\\\"loc-args\\\":\\\"[Gabriel ]\\\",\\\"session-id\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\",\\\"uid\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"}}\",\"params\":\"{}\",\"props\":\"{\\\"str\\\":\\\"0\\\",\\\"encr\\\":\\\"1\\\",\\\"device\\\":\\\"android\\\",\\\"old_id\\\":\\\"-1486492225cG9\\\"}\"},\"cmd\":200}"
        val json3 = "{\"args\":{\"id\":\"-1985371535qL4\",\"rid\":\"imic29drtsv4z2nj5n42huxr\",\"msg\":\"vFMZsUvnI9BAH4lDs3AMwo==\",\"type\":\"1\",\"push\":\"{\\\"text\\\":\\\"Gabriel  sent you a message\\\",\\\"iosData\\\":{\\\"alert\\\":{\\\"loc-key\\\":\\\"pushtextKey\\\",\\\"loc-args\\\":\\\"[Gabriel ]\\\",\\\"session-id\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\",\\\"uid\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"},\\\"sound\\\":\\\"default\\\",\\\"category\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"},\\\"andData\\\":{\\\"loc-key\\\":\\\"pushtextKey\\\",\\\"loc-args\\\":\\\"[Gabriel ]\\\",\\\"session-id\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\",\\\"uid\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"}}\",\"params\":\"{}\",\"props\":\"{\\\"str\\\":\\\"0\\\",\\\"encr\\\":\\\"1\\\",\\\"device\\\":\\\"android\\\",\\\"old_id\\\":\\\"-1486492225cG9\\\"}\"},\"cmd\":200}"

        val fileContents = "$malformed1$separator$json2$separator$json3"
        val list = MonkeyJson.parsePendingMsgsFromFile(fileContents, separator)

        list.size `should equal` 2

        list[0].toString() `should equal` json2
        list[1].toString() `should equal` json3

    }

    @Test
    fun `ignores empty json objects`() {
        val empty1 = "{}"
        val json2 = "{\"args\":{\"id\":\"-1986472235dC7\",\"rid\":\"imic29drtsv4z2nj5n42huxr\",\"msg\":\"iTOXsUmnI7SAX4lDs3FKap==\",\"type\":\"1\",\"push\":\"{\\\"text\\\":\\\"Gabriel  sent you a message\\\",\\\"iosData\\\":{\\\"alert\\\":{\\\"loc-key\\\":\\\"pushtextKey\\\",\\\"loc-args\\\":\\\"[Gabriel ]\\\",\\\"session-id\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\",\\\"uid\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"},\\\"sound\\\":\\\"default\\\",\\\"category\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"},\\\"andData\\\":{\\\"loc-key\\\":\\\"pushtextKey\\\",\\\"loc-args\\\":\\\"[Gabriel ]\\\",\\\"session-id\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\",\\\"uid\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"}}\",\"params\":\"{}\",\"props\":\"{\\\"str\\\":\\\"0\\\",\\\"encr\\\":\\\"1\\\",\\\"device\\\":\\\"android\\\",\\\"old_id\\\":\\\"-1486492225cG9\\\"}\"},\"cmd\":200}"
        val json3 = "{\"args\":{\"id\":\"-1985371535qL4\",\"rid\":\"imic29drtsv4z2nj5n42huxr\",\"msg\":\"vFMZsUvnI9BAH4lDs3AMwo==\",\"type\":\"1\",\"push\":\"{\\\"text\\\":\\\"Gabriel  sent you a message\\\",\\\"iosData\\\":{\\\"alert\\\":{\\\"loc-key\\\":\\\"pushtextKey\\\",\\\"loc-args\\\":\\\"[Gabriel ]\\\",\\\"session-id\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\",\\\"uid\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"},\\\"sound\\\":\\\"default\\\",\\\"category\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"},\\\"andData\\\":{\\\"loc-key\\\":\\\"pushtextKey\\\",\\\"loc-args\\\":\\\"[Gabriel ]\\\",\\\"session-id\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\",\\\"uid\\\":\\\"ikitzrp5cszzy0qctq4ims4i\\\"}}\",\"params\":\"{}\",\"props\":\"{\\\"str\\\":\\\"0\\\",\\\"encr\\\":\\\"1\\\",\\\"device\\\":\\\"android\\\",\\\"old_id\\\":\\\"-1486492225cG9\\\"}\"},\"cmd\":200}"

        val fileContents = "$empty1$separator$json2$separator$json3"
        val list = MonkeyJson.parsePendingMsgsFromFile(fileContents, separator)

        list.size `should equal` 2

        list[0].toString() `should equal` json2
        list[1].toString() `should equal` json3

    }
}
