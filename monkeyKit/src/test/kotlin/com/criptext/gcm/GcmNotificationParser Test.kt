package com.criptext.gcm

import org.amshove.kluent.`should equal`
import org.junit.Test

/**
 * Created by gesuwall on 3/15/17.
 */

class `GcmNotificationParser Test`() {

    @Test
    fun `converts a string of arguments separated by comma to array`() {
        val csvArguments = "arg1,arg2,arg3,arg4"
        val parser = GcmNotificationParser()
        val arrayArguments = parser.localizedArgumentsToArray(csvArguments)
        arrayArguments.size `should equal` 4
        arrayArguments[1] `should equal` "arg2"
    }
}