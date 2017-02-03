package com.criptext.lib

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.json.JSONObject

/**
 * Created by gabriel on 2/3/17.
 */

class ResponseParser {

    companion object {
    /**
     * An open response returns a string with the last seen value if is a conversation with an user.
     * If it's a group then it returns an object with the user's monkey id as key and last seen string
     * as value. Since we currently only support one last seen value, in groups we take the lowest
     * value of the object. This method will eventually be removed.
     * @param props props object of the open response
     * @param isGroup true if the open is from a group conversation.
     * @return
     */
        public fun getLastSeenFromOpenResponseProps(props: JsonObject, isGroup: Boolean): String? {
            if (!props.has("last_seen"))
                return null
            if (isGroup) {
                val lastSeenObj = props.get("last_seen").asJsonObject
                val it : Iterator<Map.Entry<String, JsonElement>> = lastSeenObj.entrySet().iterator()
                var result = Long.MAX_VALUE
                while (it.hasNext())
                    result = Math.min(it.next().value.asLong, result)
                return "" + result;
            } else return props.get("last_seen").asString
        }
    }
}
