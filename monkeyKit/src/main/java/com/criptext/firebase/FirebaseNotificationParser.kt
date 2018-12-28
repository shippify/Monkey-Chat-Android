package com.criptext.firebase

import java.util.*

/**
 * Created by gesuwall on 3/15/17.
 */

class FirebaseNotificationParser {

    fun localizedArgumentsToArray(localizedArgumentsSeparatedByCommas: String): Array<String> {
        val newArgs = StringTokenizer(localizedArgumentsSeparatedByCommas, ",");
        val tokenList = LinkedList<String>()
        while (newArgs.hasMoreTokens()) {
            tokenList.add(newArgs.nextToken())
        }
        return tokenList.toTypedArray()
    }
}