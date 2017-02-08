package com.criptext.lib

import com.google.gson.JsonParser
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.junit.Test

/**
 * Created by gabriel on 2/3/17.
 */

class `Get last_seen from props Test` {
    val parser = JsonParser()

    @Test
    fun `returns the minimum last_seen value in a group conversations open response`() {
        val props = parser.parse("{\"online\":\"1\",\"members_online\":\"idm0yzeb459zpefgg3rw9udi,"
           + "imic29drtsv4z2nj5n42huxr,ife4c0qdb0dopbg538lg14i\",\"last_seen\":{\"idm0yzeb459zpefg"
           + "g3rw9udi\":\"1486149945\",\"iq3z0iac52afgopoa28uayvi\":\"1486139357\",\"idkh61jqs9ia"
           + "151u7edhd7vi\":\"1486142078\",\"ikitzrp5cszzy0qctq4ims4i\":\"1486151284\",\"imic29dr"
           + "tsv4z2nj5n42huxr\":\"1486150610\",\"ife4c0qdb0dopbg538lg14i\":\"1486150985\",\"idlk0"
           + "p519nvfmfgfzdbfn7b9\":\"1486150593\",\"if9ynf7looscygpvakhxs9k9\":\"1486054385\",\"i"
           + "ju8xj5eq1898l00rwbz9f6r\":\"1483931517\"}}").asJsonObject

        val last_seen = MonkeyJson.Companion.getLastSeenFromOpenResponseProps(props)
        last_seen `should equal` "1483931517"
    }

    @Test
    fun `returns the string last_seen value in a normal conversation open response`() {
        val props = parser.parse("{\"online\":\"1\",\"last_seen\": \"1486149945\"}")
                .asJsonObject

        val last_seen = MonkeyJson.Companion.getLastSeenFromOpenResponseProps(props)
        last_seen `should equal` "1486149945"
    }

    @Test
    fun `returns null if there is no last_seen value`() {
        val props = parser.parse("{\"online\":\"1\",\"members_online\":\"idm0yzeb459zpefgg3rw9udi\"}")
                .asJsonObject
        val last_seen = MonkeyJson.Companion.getLastSeenFromOpenResponseProps(props)
        last_seen `should be` null
    }

    @Test
    fun `returns null if last_seen object is empty`() {
        val props = parser.parse("{\"online\":\"1\",\"last_seen\": {}}")
                .asJsonObject
        val last_seen = MonkeyJson.Companion.getLastSeenFromOpenResponseProps(props)
        last_seen `should be` null
    }
}