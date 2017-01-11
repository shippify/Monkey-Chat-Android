package com.criptext.monkeychatandroid

import com.criptext.monkeychatandroid.models.conversation.ConversationItem
import com.criptext.monkeychatandroid.models.conversation.TransactionCreator
import com.criptext.monkeychatandroid.models.message.MessageItem
import com.criptext.monkeykitui.conversation.MonkeyConversation
import com.criptext.monkeykitui.recycler.MonkeyItem
import org.amshove.kluent.`should equal`
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Created by gesuwall on 1/11/17.
 */

@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, shadows=arrayOf(ShadowModel::class))
class `TransactionCreator Test` {

    @Test
    fun `fromSentMessage() should set the conversation as sending, if the sent message was not delivered yet`() {
        val msg = "Good morning"
        val msgTime = 3L
        val c = ConversationItem("test", "0", 1L, "", 0, false, "", "", MonkeyConversation.ConversationStatus.receivedMessage.ordinal)
        val m = MessageItem("testid", "test", "1", msg, msgTime, msgTime, false, MonkeyItem.MonkeyItemType.text)
        m.setStatus(MonkeyItem.DeliveryStatus.sending.ordinal)
        val t = TransactionCreator.fromSentMessage(m, false)
        t.updateConversation(c)
        c.status `should equal` MonkeyConversation.ConversationStatus.sendingMessage.ordinal
        c.getSecondaryText() `should equal` msg
        c.getDatetime() `should equal` msgTime
    }

    @Test
    fun `fromSentMessage() should set the conversation as delivered, if the sent message was delivered`() {
        val msg = "Good morning"
        val msgTime = 3L
        val c = ConversationItem("test", "0", 1L, "", 0, false, "", "", MonkeyConversation.ConversationStatus.receivedMessage.ordinal)
        val m = MessageItem("testid", "test", "1", msg, msgTime, msgTime, false, MonkeyItem.MonkeyItemType.text)
        m.setStatus(MonkeyItem.DeliveryStatus.delivered.ordinal)
        val t = TransactionCreator.fromSentMessage(m, false)
        t.updateConversation(c)
        c.status `should equal` MonkeyConversation.ConversationStatus.deliveredMessage.ordinal
        c.getSecondaryText() `should equal` msg
        c.getDatetime() `should equal` msgTime
    }

    @Test
    fun `fromSentMessage() should set the conversation as read, if the sent message was read`() {
        val msg = "Good morning"
        val msgTime = 3L
        val c = ConversationItem("test", "0", 1L, "", 0, false, "", "", MonkeyConversation.ConversationStatus.receivedMessage.ordinal)
        val m = MessageItem("testid", "test", "1", msg, msgTime, msgTime, false, MonkeyItem.MonkeyItemType.text)
        m.setStatus(MonkeyItem.DeliveryStatus.delivered.ordinal)
        val t = TransactionCreator.fromSentMessage(m, true)
        t.updateConversation(c)
        c.status `should equal` MonkeyConversation.ConversationStatus.sentMessageRead.ordinal
        c.getSecondaryText() `should equal` msg
        c.getDatetime() `should equal` msgTime
    }

    @Test
    fun `fromContactOpenedConversation() should change conversation status from 'delivered' to 'read'`() {
        val msgTime = 3L
        val c = ConversationItem("test", "0", 1L, "", 0, false, "", "", MonkeyConversation.ConversationStatus.deliveredMessage.ordinal)
        val t = TransactionCreator.fromContactOpenedConversation(6L)
        t.updateConversation(c)
        c.status `should equal` MonkeyConversation.ConversationStatus.sentMessageRead.ordinal
   }
}