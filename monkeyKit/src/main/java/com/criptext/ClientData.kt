package com.criptext

import android.content.Intent

/**
 * Created by gesuwall on 5/26/16.
 */

class ClientData {
    lateinit var fullname: String
    var monkeyId: String? = null

    lateinit var appId: String
    lateinit var appKey: String

    constructor (fullname: String, urlUser: String,  urlPass: String) {
        this.fullname = fullname
        this.appId = urlUser
        this.appKey = urlPass
    }

    constructor(fullname: String, urlUser: String,  urlPass: String, monkeyId: String):
        this(fullname, urlUser, urlPass){
        this.monkeyId = monkeyId
    }

    constructor(intent: Intent){

        val intentFullname = intent.getStringExtra(FULLNAME_KEY)
        if(intentFullname != null)
            fullname = intentFullname
        else
            throw IllegalArgumentException("full name not found. You must include a string with" +
                    " a valid user full name in your intent's extras. Use ClientData.FULLNAME_KEY as key")

        val intentAppId = intent.getStringExtra(APP_ID_KEY)
        if(intentAppId != null)
            appId = intentAppId
        else
            throw IllegalArgumentException("App Id not found. You must include a string with" +
                    " a valid app id in your intent's extras. Use ClientData.APP_ID_KEY as key")

        val intentAppKey = intent.getStringExtra(APP_KEY_KEY)
        if(intentAppKey != null)
            appKey = intentAppKey
        else
            throw IllegalArgumentException("App key not found. You must include a string with" +
                    " a valid app key in your intent's extras. Use ClientData.APP_KEY_KEY as key")

        monkeyId = intent.getStringExtra(MONKEY_ID_KEY)
    }

    companion object {
        val FULLNAME_KEY = "MonkeyKit.fullname"
        val APP_ID_KEY = "MonkeyKit.appId"
        val APP_KEY_KEY = "MonkeyKit.appKey"
        val MONKEY_ID_KEY = "MonkeyKit.monkeyId"
    }
}
