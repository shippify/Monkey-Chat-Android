package com.criptext.monkeychatandroid.models

import com.criptext.comunication.MOKNotification
import com.criptext.comunication.MessageTypes
import com.criptext.monkeychatandroid.BuildConfig
import com.criptext.monkeychatandroid.ShadowDatabaseHandler
import com.criptext.monkeychatandroid.ShadowModel
import com.criptext.monkeychatandroid.models.conversation.ConversationItem
import com.criptext.monkeykitui.conversation.MonkeyConversation
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.amshove.kluent.`should equal`
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.internal.ShadowExtractor
import java.util.*

/**
 * Created by gesuwall on 3/22/17.
 */

@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, shadows = arrayOf(ShadowDatabaseHandler::class, ShadowModel::class))
class `NotificationMessages Test` {

    fun assertConversationWasSaved(conversationItem: ConversationItem) {
        val shadowConv = ShadowExtractor.extract(conversationItem) as ShadowModel
        shadowConv.saved `should equal` true
    }

    @Test
    fun `updateGroupWithCreateNotification creates a new group if it doesn't exist in database`() {
        val parser =  JsonParser();
        val props = "{\"monkey_action\":1,\"group_id\":\"G:idkgwf6ghcmyfvvrxqiwwmi-Aeroiz95g8bxxeisgja48m0od2t9\",\"members\":\"iw277tl1cj23plr0ncy58kt9,iz95g8bxxeisgja48m0od2t9,iw5g69pcj7ov8mawvn2ymn29,iw3mq5qyeuy8xig43jy30udi,idkh61jqs9ia151u7edhd7vi,iw5gxvd1ycsxfoqevxxswcdi,iw5gi7g4hu2yyzwwuoqd7vi,iw5g84jo9jdvglocu9pb9,ixt9i8j7n4wa9gx38bnmte29,ixw6p4v4p5ac1ojcl7toi529,imic29drtsv4z2nj5n42huxr,if9ynf7looscygpvakhxs9k9,idm0yzeb459zpefgg3rw9udi\",\"info\":{\"status\":\"0\",\"name\":\"Teeeesto Nocturno - Customer\",\"avatar\":\"https://secure.criptext.com/user/icon/default/G:idkgwf6ghcmyfvvrxqiwwmi-Aeroiz95g8bxxeisgja48m0od2t9\",\"creationDate\":\"1487295688\",\"operators\":\"idm0yzeb459zpefgg3rw9udi,idkh61jqs9ia151u7edhd7vi,if9ynf7looscygpvakhxs9k9,imic29drtsv4z2nj5n42huxr,iw277tl1cj23plr0ncy58kt9,iw3mq5qyeuy8xig43jy30udi,iw5g69pcj7ov8mawvn2ymn29,iw5g84jo9jdvglocu9pb9,iw5gi7g4hu2yyzwwuoqd7vi,iw5gxvd1ycsxfoqevxxswcdi,ixt9i8j7n4wa9gx38bnmte29,ixw6p4v4p5ac1ojcl7toi529\",\"admin\":\"iz95g8bxxeisgja48m0od2t9\",\"client\":\"iz95g8bxxeisgja48m0od2t9\"}}"
        val not = MOKNotification("23324155", "iz95g8bxxeisgja48m0od2t9", "G:idkgwf6ghcmyfvvrxqiwwmi-Aeroiz95g8bxxeisgja48m0od2t9",
                parser.parse("{}") as JsonObject, parser.parse(props) as JsonObject, 1487295690)
        val conv = NotificationMessages().updateGroupWithCreateNotification(not)
        conv.getName() `should equal` "Teeeesto Nocturno - Customer"
        assertConversationWasSaved(conv)
    }
    @Test
    fun `parseGroupNotifications adds members to a group if addMember notification is received`() {
        val parser =  JsonParser();
        val props = """{"monkey_action":3,"new_member":"imic29drtsv4z2nj5n42huxr"}"""
        val not = MOKNotification("23324155", "imic29drtsv4z2nj5n42huxr", "G:294",
                parser.parse("{}") as JsonObject, parser.parse(props) as JsonObject, 1487295690)
        val group = ConversationItem("G:294", "Unknown", 0, "Write to contact", 0, true,
                "fdf45,gdfg5,svcfhfh3", "", MonkeyConversation.ConversationStatus.empty.ordinal);
        ShadowDatabaseHandler.nextConversationById = group
        NotificationMessages().parseGroupNotifications(HashSet<String>(), not, MessageTypes.MOKGroupNewMember)
        group.groupMembers `should equal` "fdf45,gdfg5,svcfhfh3,imic29drtsv4z2nj5n42huxr"
        assertConversationWasSaved(group)
    }
    @Test
    fun `parseGroupNotifications adds a missing conversation if addMember notification is received but conversation is not in database`() {
        val parser =  JsonParser();
        val props = """{"monkey_action":3,"new_member":"imic29drtsv4z2nj5n42huxr"}"""
        val not = MOKNotification("23324155", "imic29drtsv4z2nj5n42huxr", "G:294",
                parser.parse("{}") as JsonObject, parser.parse(props) as JsonObject, 1487295690)
        ShadowDatabaseHandler.nextConversationById = null
        val missingConversations = HashSet<String>()
        NotificationMessages().parseGroupNotifications(missingConversations, not, MessageTypes.MOKGroupNewMember)
        missingConversations.contains("G:294") `should equal` true
    }
    @Test
    fun `parseGroupNotifications removes members from a group if removeMember notification is received`() {
        val parser =  JsonParser();
        val props = """{"monkey_action":4}"""
        val not = MOKNotification("23324155", "imic29drtsv4z2nj5n42huxr", "G:294",
                parser.parse("{}") as JsonObject, parser.parse(props) as JsonObject, 1487295690)
        val group = ConversationItem("G:294", "Unknown", 0, "Write to contact", 0, true,
                "fdf45,gdfg5,imic29drtsv4z2nj5n42huxr,svcfhfh3", "", MonkeyConversation.ConversationStatus.empty.ordinal)
        ShadowDatabaseHandler.nextConversationById = group
        NotificationMessages().parseGroupNotifications(HashSet<String>(), not, MessageTypes.MOKGroupRemoveMember)
        group.groupMembers `should equal` "fdf45,gdfg5,svcfhfh3"
        assertConversationWasSaved(group)
    }
    @Test
    fun `parseGroupNotifications doesn't do anything if if removeMember notification is received but conversation is not in database`() {
        val parser =  JsonParser();
        val props = """{"monkey_action":4}"""
        val not = MOKNotification("23324155", "imic29drtsv4z2nj5n42huxr", "G:294",
                parser.parse("{}") as JsonObject, parser.parse(props) as JsonObject, 1487295690)
        ShadowDatabaseHandler.nextConversationById = null
        val missingConversations = HashSet<String>()
        NotificationMessages().parseGroupNotifications(missingConversations, not, MessageTypes.MOKGroupRemoveMember)
        missingConversations.contains("G:294") `should equal` false
    }
}
