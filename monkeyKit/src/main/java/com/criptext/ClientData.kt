package com.criptext

import android.content.Intent

/**
 * Created by gesuwall on 5/26/16.
 */

class ClientData(val fullname: String, val appId: String, val appKey: String,
                 val monkeyId: String, val sdomain: String, val sport: Int) {

    val password: String
    get() = "$appId:$appKey"

    constructor(intent: Intent): this (fullname = intent.getStringExtra(FULLNAME_KEY),
             appId = intent.getStringExtra(APP_ID_KEY), appKey = intent.getStringExtra(APP_KEY_KEY),
            monkeyId = intent.getStringExtra(MONKEY_ID_KEY), sport = intent.getIntExtra(SPORT_KEY, 1139),
            sdomain = intent.getStringExtra(SDOMAIN_KEY))

    fun fillIntent(intent: Intent){
        intent.putExtra(ClientData.FULLNAME_KEY, fullname)
        intent.putExtra(ClientData.MONKEY_ID_KEY, monkeyId)
        intent.putExtra(ClientData.APP_ID_KEY, appId)
        intent.putExtra(ClientData.APP_KEY_KEY, appKey)
        intent.putExtra(ClientData.SDOMAIN_KEY, sdomain)
        intent.putExtra(ClientData.SPORT_KEY, sport)
    }
    companion object {
        val FULLNAME_KEY = "MonkeyKit.fullname"
        val APP_ID_KEY = "MonkeyKit.appId"
        val APP_KEY_KEY = "MonkeyKit.appKey"
        val MONKEY_ID_KEY = "MonkeyKit.monkeyId"
        val SDOMAIN_KEY = "MonkeyKit.sdomain"
        val SPORT_KEY = "MonkeyKit.sport"

        fun throwMissingFullnameException(){
            throw IllegalArgumentException("full name not found. You must include a string with" +
            " a valid user full name in your intent's extras. Use ClientData.FULLNAME_KEY as key")
        }

        fun throwMissingAppIdException(){
            throw IllegalArgumentException("App ID not found. You must include a string with" +
            " a valid App ID in your intent's extras. Use ClientData.APP_ID_KEY as key")
        }

        fun throwMissingMonkeyIdException(){
            throw IllegalArgumentException("Monkey ID not found. You must include a string with" +
            " a valid monkey id in your intent's extras. Use ClientData.MONKEY_ID_KEY as key. " +
            "If you don't have a Monkey ID yet, use MonkeyInit class to get one from the server.")
        }

        fun throwMissingAppKeyException(){
            throw IllegalArgumentException("App Id not found. You must include a string with" +
            " a valid app id in your intent's extras. Use ClientData.APP_ID_KEY as key")
        }

    }

}
