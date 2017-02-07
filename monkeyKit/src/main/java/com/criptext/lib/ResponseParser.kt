package com.criptext.lib

import com.google.gson.JsonElement
import com.google.gson.JsonObject

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
     * @return
     */
        public fun getLastSeenFromOpenResponseProps(props: JsonObject): String? {
            if (!props.has("last_seen"))
                return null

            try {
                val lastSeenObj = props.get("last_seen").asJsonObject
                val it : Iterator<Map.Entry<String, JsonElement>> = lastSeenObj.entrySet().iterator()
                var result = Long.MAX_VALUE
                while (it.hasNext())
                    result = Math.min(it.next().value.asLong, result)

                if (result == Long.MAX_VALUE)
                    return null
                else return "" + result;
            } catch (ex: IllegalStateException){
                return props.get("last_seen").asString
            }
        }
    }
}
