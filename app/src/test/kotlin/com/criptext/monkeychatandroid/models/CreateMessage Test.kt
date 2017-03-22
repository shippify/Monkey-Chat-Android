package com.criptext.monkeychatandroid.models

import com.criptext.comunication.MOKMessage
import com.criptext.comunication.MessageTypes
import com.criptext.monkeychatandroid.BuildConfig
import com.criptext.monkeychatandroid.ShadowModel
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.amshove.kluent.`should equal`
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Created by gesuwall on 1/17/17.
 */

@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, shadows=arrayOf(ShadowModel::class))
class `CreateMessage Test` {
    private val db = DatabaseHandler()

    fun newMOKMessage(id: String, sid: String, rid: String, msg: String, timestamp: Long,
                      type: String, props: JsonObject?, params: JsonObject?): MOKMessage {
        val mokMessage = MOKMessage(id, sid, rid, msg, "" + (timestamp/1000L),
                MessageTypes.MOKText)
        mokMessage.type = type
        mokMessage.datetimeorder = timestamp
        mokMessage.params = params
        mokMessage.props = props
        return mokMessage
    }

    @Test
    fun `createMessage converts a text MOKMessage to MessageItem`() {
        val mokMessage = newMOKMessage("0", "0", "1", "Hello World!", System.currentTimeMillis(),
                MessageTypes.MOKText, JsonObject(), JsonObject())

        val messageItem = db.createMessage(mokMessage, "/path/to/av", "1")

        messageItem.getMessageId() `should equal` mokMessage.message_id
        messageItem.getMessageText() `should equal` mokMessage.msg
        messageItem.getSenderId() `should equal` mokMessage.sid
        messageItem.getConversationId() `should equal` mokMessage.getConversationID("1")
        messageItem.timestamp.toString() `should equal` mokMessage.datetime
        messageItem.timestampOrder `should equal` mokMessage.datetimeorder

        messageItem.jsonParams `should equal` mokMessage.params
        messageItem.jsonProps `should equal` mokMessage.props
    }

    @Test
    fun `createMessage converts a voice note MOKMessage to MessageItem`() {
        val parser = JsonParser()
        val props = parser.parse("{\"file_type\":1,\"ext\":\"mp3\",\"encr\":\"0\",\"mime_type\":"
                + "\"audio/mpeg3\",\"external_url\":\"http://translate.google.com/translate_tts?tl="
                + "es&q=Hola, soy el nuevo audio bot de Criptext. Amo a Criptext y en especial a "
                + "Luis. Los espero para una demo en Panamá&client=12345\",\"old_id\":-1484239586,"
                + "\"new_id\":19539169}").asJsonObject
        val params = parser.parse("{\"length\":3}").asJsonObject
        val mokMessage = newMOKMessage("19539162", "0", "1", "f_ixulxdzio0tg1cmh9xcac3di", System.currentTimeMillis(),
                MessageTypes.MOKFile, props, params)

        val messageItem = db.createMessage(mokMessage, "/path/to/av", "1")
        messageItem.getMessageType() `should equal` MessageTypes.blMessageAudio
        messageItem.getAudioDuration() `should equal` 3000L
    }

    @Test
    fun `createMessage converts a voice note MOKMessage to MessageItem, even with null params`() {
        val parser = JsonParser()
        val props = parser.parse("{\"file_type\":1,\"ext\":\"mp3\",\"encr\":\"0\",\"mime_type\":"
                + "\"audio/mpeg3\",\"external_url\":\"http://translate.google.com/translate_tts?tl="
                + "es&q=Hola, soy el nuevo audio bot de Criptext. Amo a Criptext y en especial a "
                + "Luis. Los espero para una demo en Panamá&client=12345\",\"old_id\":-1484239586,"
                + "\"new_id\":19539169}").asJsonObject
        val mokMessage = newMOKMessage("19539162", "0", "1", "f_ixulxdzio0tg1cmh9xcac3di", System.currentTimeMillis(),
                MessageTypes.MOKFile, props, null)

        val messageItem = db.createMessage(mokMessage, "/path/to/av", "1")
        messageItem.getMessageType() `should equal` MessageTypes.blMessageAudio
        messageItem.getAudioDuration() `should equal` 0L
    }
}