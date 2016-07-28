package com.criptext.lib

import android.content.ComponentName
import android.content.ServiceConnection
import android.support.v7.app.AppCompatActivity
import android.os.IBinder
import android.util.Log
import android.webkit.MimeTypeMap
import com.criptext.ClientData
import com.criptext.MonkeyKitSocketService
import com.criptext.comunication.MOKMessage
import com.criptext.comunication.MessageTypes
import com.criptext.comunication.PushMessage
import com.criptext.security.RandomStringBuilder
import com.google.gson.JsonObject
import org.apache.commons.io.FilenameUtils
import java.util.*

abstract class MKDelegateActivity : AppCompatActivity(), MonkeyKitDelegate {

    var service: MonkeyKitSocketService? = null

    abstract val serviceClassName: Class<*>

    val isSocketConnected: Boolean
        get () =  service?.isSocketConnected() ?: false

    private val thingsToDoOnceConnected = ArrayList<Runnable>()

    private val monkeyKitConnection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            Log.d("MKDelegateActivity", "service connected")
            val binder = p1 as MonkeyKitSocketService.MonkeyBinder
            val sService = binder.getService(this@MKDelegateActivity)
            service = sService

            onBoundToService()


        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            service = null
        }
    }

    override fun onStart() {
        super.onStart()
        MonkeyKitSocketService.bindMonkeyService(this, monkeyKitConnection, serviceClassName)

    }

    override fun onStop() {
        super.onStop()
        service = null
        unbindService(monkeyKitConnection)
    }

    abstract fun onBoundToService()

    /**
     * Crea un nuevo MOKMessage con Id unico y con un timestamp actual. Al crear un nuevo MOKMessage
     * para ser enviado siempre debe de usarse este metodo en lugar del constructor por defecto que
     * tiene MOKMessage ya que inicializa varios atributos de la manera correcta para ser enviado.
     * @param textMessage texto a enviar en el mensaje
     * @param sessionIDTo session ID del destinatario
     * @param type tipo del mensaje. Debe de ser uno de los valores de MessageTypes.FileTypes
     * @param params JsonObject con parametros adicionales a enviar.
     * @return Una nueva instancia de MOK Message lista para ser enviada por el socket.
     */

    private fun createMOKMessage(textMessage: String, sessionIDfrom: String, sessionIDTo: String,
        type: Int, params: JsonObject): MOKMessage{
        val datetimeorder = System.currentTimeMillis();
        val datetime = datetimeorder/1000;
        val srand = RandomStringBuilder.build(3);
        val idnegative = "-" + datetime;
        val message = MOKMessage(idnegative + srand, sessionIDfrom,sessionIDTo, textMessage,
               "" + datetime, "" + type, params, null);
        message.datetimeorder = datetimeorder;
        return message;
    }

    private fun createSendProps(old_id: String, encrypted: Boolean): JsonObject{
            val props = JsonObject();
            props.addProperty("str", "0");
            props.addProperty("encr", if(encrypted)"1" else "0");
            props.addProperty("device", "android");
            props.addProperty("old_id", old_id);
            return props;
        }
    fun persistFileMessageAndSend(filePath: String, monkeyIDFrom: String, monkeyIDTo: String, fileType: Int,
                                  params: JsonObject, pushMessage: PushMessage, isEncrypted: Boolean): MOKMessage{
        val newMessage = createMOKMessage(filePath, monkeyIDFrom, monkeyIDTo, fileType, params)
        val propsMessage = createSendProps(newMessage.message_id, isEncrypted);
                propsMessage.addProperty("cmpr", "gzip");
                propsMessage.addProperty("file_type", fileType);
                propsMessage.addProperty("ext", FilenameUtils.getExtension(filePath));
                propsMessage.addProperty("filename", FilenameUtils.getName(filePath));
                propsMessage.addProperty("mime_type",
                        MimeTypeMap.getSingleton().getMimeTypeFromExtension(FilenameUtils.getExtension(filePath)));

        newMessage.props = propsMessage;

        if(isSocketConnected){
            val pushStr = pushMessage.toString();
            service!!.fileUploader.sendFileMessage(newMessage, pushStr, isEncrypted)
        }
          else {
            thingsToDoOnceConnected.add(Runnable { service!!.fileUploader.
                    sendFileMessage(newMessage, pushMessage.toString(), isEncrypted) })
        }

        storeSentMessage(newMessage)
        return newMessage

    }

    fun persistMessageAndSend(text: String, monkeyIDFrom: String, monkeyIDTo: String, params: JsonObject,
                              pushMessage: PushMessage, isEncrypted: Boolean): MOKMessage{
        val newMessage = createMOKMessage(text, monkeyIDFrom, monkeyIDTo,
                MessageTypes.blMessageDefault, params)
        if(isSocketConnected){
            service!!.sendMessage(newMessage, pushMessage, isEncrypted)
        } else {
            thingsToDoOnceConnected.add(Runnable {
                service!!.sendMessage(newMessage, pushMessage, isEncrypted)
            })
        }
        storeSentMessage(newMessage)
        return newMessage
    }

    fun resendFile(fileMessageId: String){
        if(isSocketConnected){
            service!!.fileUploader.resendFile(fileMessageId)
        } else
            thingsToDoOnceConnected.add(Runnable { service!!.fileUploader.resendFile(fileMessageId) })
    }

    abstract fun storeSentMessage(message: MOKMessage)

    override fun onSocketConnected() {
        for(toDo in thingsToDoOnceConnected)
            toDo.run()
        thingsToDoOnceConnected.clear()
    }

    companion object{
        val  CONNECTION_NOT_READY_ERROR = "MonkeyKitSocketService is not ready yet."
    }
}
