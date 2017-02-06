package com.criptext.monkeychatandroid.state

import com.criptext.comunication.MOKMessage
import com.criptext.comunication.MessageTypes
import com.criptext.http.HttpSync
import com.criptext.monkeychatandroid.BuildConfig
import com.criptext.monkeychatandroid.ShadowModel
import com.criptext.monkeychatandroid.SyncState
import com.criptext.monkeychatandroid.models.conversation.ConversationItem
import com.criptext.monkeychatandroid.models.message.MessageItem
import com.criptext.monkeykitui.conversation.MonkeyConversation
import com.criptext.monkeykitui.recycler.MonkeyItem
import com.google.gson.JsonObject
import org.amshove.kluent.`should equal`
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Created by gabriel on 2/3/17.
 */

@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, shadows=arrayOf(ShadowModel::class))
class `SyncState with new messages Test` {

    lateinit var state : ChatState
    val myMonkeyId = "myid"
    val myFriend1Id = "herid"
    val myFriend2Id = "hisid"
    val myFriend3Id = "groupid"

    @Before
    fun initialize() {
        state = ChatState(myMonkeyId, "testuser")
    }

    @Test
    fun `returns the correct number of new messages for active conversation`() {
        val oldMessage1 = MessageItem(myFriend1Id, myFriend1Id, "0", "msg0", 0L, 0L,
                true, MonkeyItem.MonkeyItemType.text)
        val oldMessage2 = MessageItem(myMonkeyId, myFriend1Id, "1", "msg1", 1L, 1L,
                false, MonkeyItem.MonkeyItemType.text)

        val oldMessage3 = MessageItem(myMonkeyId, myFriend2Id, "2", "msg2", 2L, 2L,
                false, MonkeyItem.MonkeyItemType.text)
        state.addNewMessagesList(myFriend1Id, listOf(oldMessage1, oldMessage2))
        state.addNewMessagesList(myFriend2Id, listOf(oldMessage3))
        state.getLoadedMessages(myFriend3Id)
        state.activeConversationItem = ConversationItem(myFriend1Id, "Friend 1", 2L, "msg2", 0,
                false, "", "", MonkeyConversation.ConversationStatus.deliveredMessage.ordinal)

        val newMessage1 = MOKMessage("3", myFriend1Id, myMonkeyId, "msg3", "3", MessageTypes.MOKText,
                JsonObject(), JsonObject())
        val newMessage2 = MOKMessage("4", myFriend1Id, myMonkeyId, "msg4", "4", MessageTypes.MOKText,
                JsonObject(), JsonObject())
        val newMessage3 = MOKMessage("5", myFriend2Id, myMonkeyId, "msg5", "5", MessageTypes.MOKText,
                JsonObject(), JsonObject())
        val newMessage4 = MOKMessage("6", myFriend3Id, myMonkeyId, "msg6", "6", MessageTypes.MOKText,
                JsonObject(), JsonObject())

        val syncData = HttpSync.SyncData(myMonkeyId,
                HttpSync.SyncResponse(listOf(newMessage1, newMessage2, newMessage3, newMessage4),
                        listOf(), listOf()))

        state.messagesMap.entries.size `should equal` 3
        state.getLoadedMessages(myFriend1Id).size `should equal` 2
        state.getLoadedMessages(myFriend2Id).size `should equal` 1
        state.getLoadedMessages(myFriend3Id).size `should equal` 0
        state.activeConversationId `should equal` myFriend1Id

        val activeConversationNewMessages = SyncState.withNewMessages(state, syncData)
        activeConversationNewMessages `should equal` 2

        //should clear all loaded messages except active conversation
        state.getLoadedMessages(myFriend1Id).size `should equal` 2
        state.getLoadedMessages(myFriend2Id).size `should equal` 0
        state.getLoadedMessages(myFriend3Id).size `should equal` 0
    }
}