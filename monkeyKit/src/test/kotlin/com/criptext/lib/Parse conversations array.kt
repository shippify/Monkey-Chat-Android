package com.criptext.lib

import org.amshove.kluent.`should equal`
import org.amshove.kluent.`should not be`
import org.junit.Test

/**
 * Created by gesuwall on 2/21/17.
 */

class `Parse conversations array` {

    @Test
    fun `should get all the conversations in the get conversations response`() {
        val json = """{"data":{"conversations":[{"id":"G:idkgwf6ghcmyfvvrxqiwwmi-152","last_modified":"1487687198","members_last_seen":{"iq3z0iac52afgopoa28uayvi":"1487619172","idm0yzeb459zpefgg3rw9udi":"1487691497","idkh61jqs9ia151u7edhd7vi":"1487693661","ikitzrp5cszzy0qctq4ims4i":"1487631877","imic29drtsv4z2nj5n42huxr":"1487692289","ife4c0qdb0dopbg538lg14i":"1487687479","idlk0p519nvfmfgfzdbfn7b9":"1487689226","iju8xj5eq1898l00rwbz9f6r":"1483931517","if9ynf7looscygpvakhxs9k9":"1487632006"},"unread":1,"last_seen":"1487631877","last_message":{"id":"23852879","app":"idkgwf6ghcmyfvvrxqiwwmi","sid":"ife4c0qdb0dopbg538lg14i","rid":"G:idkgwf6ghcmyfvvrxqiwwmi-152","read_by":"imic29drtsv4z2nj5n42huxr,idm0yzeb459zpefgg3rw9udi,idkh61jqs9ia151u7edhd7vi","params":"{}","datetime":"1487687198","type":"1","msg":"5iLj09Uju1OpmUAGA80lOA==","props":"{\"str\":\"0\",\"encr\":\"1\",\"device\":\"android\",\"old_id\":\"-1487687198LRt\"}"},"members":["idkhfyofde2n4reo5g0hpvi","iq3z0iac52afgopoa28uayvi","ife4c0qdb0dopbg538lg14i","idlk0p519nvfmfgfzdbfn7b9","idkh61jqs9ia151u7edhd7vi","ikitzrp5cszzy0qctq4ims4i","idm0yzeb459zpefgg3rw9udi","","if9ynf7looscygpvakhxs9k9","imic29drtsv4z2nj5n42huxr","ilpk9oih1qa0s652sr79zfr"],"info":{"avatar":"https:\/\/api.criptext.com\/avatars\/avatar_G:idkgwf6ghcmyfvvrxqiwwmi-152.png","admin":"idkh61jqs9ia151u7edhd7vi,idlk0p519nvfmfgfzdbfn7b9","name":"Criptext-Dev"}},{"unread":0,"id":"ife4c0qdb0dopbg538lg14i","last_modified":"1487288245","members_last_seen":"1487288639","last_seen":"1487636248","last_message":{"id":"23316820","app":"idkgwf6ghcmyfvvrxqiwwmi","sid":"ife4c0qdb0dopbg538lg14i","rid":"ikitzrp5cszzy0qctq4ims4i","read_by":"ikitzrp5cszzy0qctq4ims4i","params":"{}","datetime":"1487288245","type":"1","msg":"uAowicUuQ+lcbjVk\/ZeNqQ==","props":"{\"str\":\"0\",\"encr\":\"1\",\"device\":\"android\",\"old_id\":\"-1487288245E8K\"}"},"info":{"avatar":"https:\/\/monkey.criptext.com\/user\/icon\/default\/ife4c0qdb0dopbg538lg14i","name":"Luis"}},{"unread":0,"id":"imic29drtsv4z2nj5n42huxr","last_modified":"1487257683","members_last_seen":"1487262171","last_seen":"1487529988","last_message":{"id":"23258803","app":"idkgwf6ghcmyfvvrxqiwwmi","sid":"imic29drtsv4z2nj5n42huxr","rid":"ikitzrp5cszzy0qctq4ims4i","read_by":"ikitzrp5cszzy0qctq4ims4i","params":"{}","datetime":"1487257683","type":"1","msg":"4hRctN\/5I\/9Y49GT9qKzMOuWbkw9auLC6NtOqNZuGNk=","props":"{\"str\":\"0\",\"encr\":\"1\",\"device\":\"android\",\"old_id\":\"-1487257681oQj\"}"},"info":{"avatar":"https:\/\/secure.criptext.com\/user\/icon\/default\/imic29drtsv4z2nj5n42huxr","monkeyId":"imic29drtsv4z2nj5n42huxr","name":"Pedro Aim"}}]}}"""
        val array = MonkeyJson.parseConversationsList(json)
        array.size `should equal` 2

        val conversations = array[0]
        val undecrypted = array[1]

        conversations.size `should equal` 3

        for (c in undecrypted)
            c.lastMessage `should not be` null

    }
}