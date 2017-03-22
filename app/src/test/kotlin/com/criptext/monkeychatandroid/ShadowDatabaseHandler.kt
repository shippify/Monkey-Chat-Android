package com.criptext.monkeychatandroid

import com.criptext.monkeychatandroid.models.DatabaseHandler
import com.criptext.monkeychatandroid.models.conversation.ConversationItem
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

/**
 * Created by gesuwall on 3/22/17.
 */

@Implements(DatabaseHandler::class)
public class ShadowDatabaseHandler {
    companion object {
        var nextConversationById: ConversationItem? = null
    }

    @Implementation
    public fun getConversationById(id: String): ConversationItem? {
        val c = nextConversationById
        nextConversationById = null
        return c
    }
}