package com.criptext.comunication


import android.content.Intent
import com.google.gson.JsonObject
import com.google.gson.JsonParser

import java.io.File


class MOKMessage(var message_id: String,var sid: String,var rid: String,var msg: String,var datetime: String,var type: String) {

    var protocolCommand: Int = 0
    var protocolType: Int = 0
    var encr: String? = null
    var datetimeorder: Long = 0
    var file: File? = null
    var monkeyAction: Int = 0

    var params: JsonObject? = null
    var props: JsonObject? = null


    constructor(message_id: String, sid: String, rid: String, msg: String,
                datatime: String, type: String, params: JsonObject?, props: JsonObject?)
        : this(message_id, sid, rid, msg, datatime, type) {
        this.params = params
        this.props = props

    }
    constructor(intent: Intent): this(intent.getStringExtra(ID_KEY),
            intent.getStringExtra(SID_KEY),intent.getStringExtra(RID_KEY),
            intent.getStringExtra(MSG_KEY), intent.getStringExtra(DATE_KEY),
            intent.getStringExtra(TYPE_KEY)){
        val parser = JsonParser()
        this.params = parser.parse(intent.getStringExtra(PARAMS_KEY)).asJsonObject
        this.props = parser.parse(intent.getStringExtra(PROPS_KEY)).asJsonObject
    }

    fun toIntent(intent: Intent){
        intent.putExtra(ID_KEY, this.message_id)
        intent.putExtra(SID_KEY, this.sid)
        intent.putExtra(RID_KEY, this.rid)
        intent.putExtra(MSG_KEY, this.msg)
        intent.putExtra(DATE_KEY, this.datetime)
        intent.putExtra(TYPE_KEY, this.type)
        intent.putExtra(PARAMS_KEY, this.params.toString())
        intent.putExtra(PROPS_KEY, this.props.toString())
    }

    val fileType: Int
        get() = props?.get("file_type")?.asInt ?: MessageTypes.blMessageDefault

    /**
     * Obtiene el estado del mensaje. Este valor debe de compararse con las constantes de
     * MessageTypes.Status. Si props es null, o no tiene estado, retorna 0.
     * @return Si props es null, o no tiene estado, retorna 0. De lo contrario retorna el
     * * valor correspondiente de MessageTypes.Status.
     */
    val status: Int
        get() {
            if (props == null || props!!.get("status") == null)
                return 0

            return props!!.get("status").asInt
        }

    val oldId: String?
        get() {
            if (props == null || props!!.get("old_id") == null)
                return null

            return props!!.get("old_id").asString
        }

    val fileExtension: String?
        get() {
            if (props == null || props!!.get("ext") == null)
                return null

            return "." + props!!.get("ext").asString
        }

    val conversationID: String?
        get() = if (rid.startsWith("G:"))  rid else sid

    val senderId: String?
        get() = if (rid.startsWith("G:")) sid else rid

    val isGroupConversation: Boolean
        get() = rid.startsWith("G:")

    fun isMyOwnMessage(mySessionID: String) : Boolean{
        return sid.equals(mySessionID)
    }

    fun isMediaType(): Boolean{
        if(props==null)
            return false
        return props!!.has("file_type")
    }

    fun getMediaType(): Int{
        if(!isMediaType())
            return MessageTypes.FileTypes.Default
        return Integer.parseInt(props!!.get("file_type").asString)
    }

    companion object {
        val ID_KEY = "MOKMessage.id"
        val DATE_KEY = "MOKMessage.datetime"
        val SID_KEY = "MOKMessage.sid"
        val RID_KEY = "MOKMessage.rid"
        val MSG_KEY = "MOKMessage.msg"
        val TYPE_KEY = "MOKMessage.type"
        val PARAMS_KEY = "MOKMessage.params"
        val PROPS_KEY = "MOKMessage.props"
        val DATESORT_KEY = "MOKMessage.datetimeorder"
        val CONVERSATION_KEY = "MOKMessage.conversationID"
    }
}