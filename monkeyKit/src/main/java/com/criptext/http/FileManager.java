package com.criptext.http;

import android.util.Base64;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.criptext.MonkeyKitSocketService;
import com.criptext.comunication.CBTypes;
import com.criptext.comunication.Compressor;
import com.criptext.comunication.MOKMessage;
import com.criptext.comunication.MessageTypes;
import com.criptext.comunication.MonkeyHttpResponse;
import com.criptext.lib.KeyStoreCriptext;
import com.criptext.security.AESUtil;
import com.criptext.security.RandomStringBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by gesuwall on 6/23/16.
 */
public class FileManager extends AQueryHttp{
    final HashMap<String, FileMOKMessage> pendingFiles  = new HashMap<>();

    public FileManager(MonkeyKitSocketService service, AESUtil aesUtil) {
        super(service, aesUtil);
    }

    /**
         * Descarga un archivo del servidor de MonkeyKit por HTTP.
         * @param filepath ruta absoluta del lugar donde se guardara el archivo
         * @param propsStr string con los props del MOKMessage que tenia el mensaje al ser transmitido
         * @param sender_id session ID del usuario que envio el archivo
         * @param monkeyHttpResponse callback con el codigo que se desee ejecutar una vez que la descarga termine.
         */
        public void downloadFile(String filepath, final String propsStr, final String sender_id,
                                 final MonkeyHttpResponse monkeyHttpResponse){

            MonkeyKitSocketService service = serviceRef.get();
            final JsonObject props = new JsonParser().parse(propsStr).getAsJsonObject();
            final String claves= KeyStoreCriptext.getString(service,sender_id);
            String name = FilenameUtils.getBaseName(filepath);
            File target = new File(filepath);
            final String URL = MonkeyKitSocketService.Companion.getHttpsURL()+"/file/open/"+ name;
            System.out.println("MONKEY - Descargando:"+ filepath + " " + URL);
            aq.auth(handle).download(URL, target, new AjaxCallback<File>() {
                public void callback(String url, File file, com.androidquery.callback.AjaxStatus status) {
                    if (file != null) {
                        try {
                            String finalContent = "";
                            byte[] finalData = null;
                            //COMPRUEBO SI DESENCRIPTO EL CONTENIDO DEL ARCHIVO
                            if (props.get("encr").getAsString().compareTo("1") == 0) {
                                String[] claveArray = claves.split(":");
                                finalData = aesUtil.decryptWithCustomKeyAndIV(IOUtils.toByteArray(new FileInputStream(file.getAbsolutePath())),
                                        claveArray[0], claveArray[1]);
                                //COMPRUEBO SI ES DESDE EL WEB
                                if (props.get("device").getAsString().compareTo("web") == 0) {
                                    finalContent = new String(finalData, "UTF-8");
                                    finalContent = finalContent.substring(finalContent.indexOf(",") + 1, finalContent.length());
                                    finalData = Base64.decode(finalContent.getBytes(), 0);
                                }
                            }
                            //COMPRUEBO SI EL ARCHIVO ESTA COMPRIMIDO
                            if (props.has("cmpr")) {
                                if (props.get("cmpr").getAsString().compareTo("gzip") == 0 && finalData!=null) {
                                    Compressor compressor = new Compressor();
                                    finalData = compressor.gzipDeCompress(finalData);
                                }
                            }

                            //VALIDACION
                            if(finalData==null)
                                finalData=new byte[]{};

                            //VUELVO A GUARDAR EL ARCHIVO
                            FileOutputStream fos = new FileOutputStream(file);
                            fos.write(finalData);
                            fos.close();
                            //System.out.println("TAM FILE:" + finalData.length);

                            //LE PONGO LA EXTENSION SI LA TIENE
                            if (props.has("ext")) {
                                System.out.println("MONKEY - giving permissions " + file.getAbsolutePath());
                                Runtime.getRuntime().exec("chmod 777 " + file.getAbsolutePath());
                                //file.renameTo(new File(file.getAbsolutePath()+"."+message.getProps().get("ext").getAsString()));
                            }

                            //message.setMsg(file.getAbsolutePath());
                            //message.setFile(file);

                            //EXCUTE CALLBACK
                            Log.d("FileManager", "Download complete");
                            monkeyHttpResponse.OnSuccess();
                        } catch (Exception e) {
                            e.printStackTrace();
                            monkeyHttpResponse.OnError();
                        }
                    } else {
                        System.out.println("MONKEY - File failed to donwload - " + status.getCode() + " - " + status.getMessage());
                        monkeyHttpResponse.OnError();
                    }
                }

            });
        }



        private JSONObject createSendJSON(String idnegative, String sessionIDTo, String elmensaje,
                                          String pushMessage, JsonObject params, JsonObject props){

            JSONObject args=new JSONObject();
            JSONObject json=new JSONObject();
            JSONObject pushObject = new JSONObject();

            try {
                pushObject.put("key", "text");
                pushObject.put("value", pushMessage);

                args.put("id", idnegative);
                args.put("sid", this.monkeyID);
                args.put("rid", sessionIDTo);
                args.put("msg", aesUtil.encrypt(elmensaje));
                args.put("type", MessageTypes.MOKText);
                args.put("push", pushObject.toString());
                if (params != null)
                    args.put("params", params.toString());
                if (props != null)
                    args.put("props", props.toString());

                json.put("args", args);
                json.put("cmd", MessageTypes.MOKProtocolMessage);
            } catch(Exception ex){
                ex.printStackTrace();
            }

            return json;
        }



        private void handleSentFile(JSONObject json, MOKMessage newMessage){
            System.out.println(json);
            try {
                JSONObject response = json.getJSONObject("data");
                System.out.println("MONKEY - sendFileMessage ok - " + response.toString() + " - " + response.getString("messageId"));
                JsonObject props = new JsonObject();
                props.addProperty("status", MessageTypes.Status.delivered);
                props.addProperty("old_id", newMessage.getMessage_id());
                MonkeyKitSocketService service = serviceRef.get();
                if(service != null)
                    service.processMessageFromHandler(CBTypes.onAcknowledgeReceived
                            , new Object[]{newMessage.getRid()
                                    ,this.monkeyID,response.getString("messageId")
                                    ,newMessage.getMessage_id(), false, 2});

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    public void resendFile(String fileMessageId){
        FileMOKMessage fileMOKMessage = pendingFiles.get(fileMessageId);
        if(fileMOKMessage != null) {
            if (!fileMOKMessage.failed) {
                Log.e("FileManager", "File " + fileMessageId + " is already sending!");
                return;
            } else {
                fileMOKMessage.failed = false;
            }

            sendFileMessage(fileMOKMessage.message, fileMOKMessage.pushStr,fileMOKMessage.isEncrypted);
        } else
            Log.e("MonkeyKit", "FileManager tried to resend a file that has not been sent yet!");
    }
    /**
     * Envia un archivo a traves de MonkeyKit. Se envia un mensaje por el socket con metadata del archivo
     * y posteriormente el archivo es subido por HTTP al servidor
     * @param newMessage MOKMessage a enviar
     * @param pushMessage Mensaje a mostrar en el push notification
     * @return el MOKMessage enviado.
     */
    public MOKMessage sendFileMessage(final MOKMessage newMessage, final String pushMessage, final boolean encrypted){

        Log.d("FileManager", "send file: " + newMessage.getMsg());
        if(!pendingFiles.containsKey(newMessage.getMessage_id()))
            try {

                JSONObject args = new JSONObject();
                JSONObject paramsMessage = new JSONObject();

                args.put("sid",this.monkeyID);
                args.put("rid",newMessage.getRid());

                if(newMessage.getParams() != null) {
                    paramsMessage = new JSONObject(newMessage.getParams().toString());
                }

                args.put("params", paramsMessage);
                args.put("id", newMessage.getMessage_id());
                args.put("push", pushMessage);

                final Map<String, Object> params = new HashMap<String, Object>();

                byte[] finalData= IOUtils.toByteArray(new FileInputStream(newMessage.getMsg()));

                newMessage.getProps().addProperty("size",finalData.length);
                args.put("props", new JSONObject(newMessage.getProps().toString()));

                //COMPRIMIMOS CON GZIP
                Compressor compressor = new Compressor();
                finalData = compressor.gzipCompress(finalData);

                //ENCRIPTAMOS
                finalData=encrypted ? aesUtil.encrypt(finalData) : finalData;

                params.put("file", finalData);
                params.put("data", args.toString());

                uploadFile(params, newMessage);

                return newMessage;
            }  catch (Exception e) {
                e.printStackTrace();
            }

        return newMessage;
    }

        public void uploadFile(Map<String, Object> params, final MOKMessage newMessage){
            System.out.println("send file: " + params);
            aq.auth(handle).ajax(MonkeyKitSocketService.Companion.getHttpsURL() + "/file/new", params,
                    JSONObject.class, new AjaxCallback<JSONObject>() {
                @Override
                public void callback(String url, JSONObject json, AjaxStatus status) {
                    if (json != null) {
                        pendingFiles.remove(newMessage.getMessage_id());
                        handleSentFile(json, newMessage);
                    } else {
                        FileMOKMessage fileMessage = pendingFiles.get(newMessage.getMessage_id());
                        fileMessage.failed = true;
                        System.out.println("MONKEY - sendFileMessage error - " + status.getCode() + " - " + status.getMessage());
                        MonkeyKitSocketService service = serviceRef.get();
                        if(service != null)
                            service.processMessageFromHandler(CBTypes.onFileFailsUpload, new Object[]{newMessage});
                    }
                }
            });
        }

     class FileMOKMessage{
         final MOKMessage message;
         final String pushStr;
         final boolean isEncrypted;
         boolean failed;

        FileMOKMessage(MOKMessage message, String pushStr, boolean isEncrypted){
            this.message = message;
            this.pushStr = pushStr;
            this.isEncrypted = isEncrypted;
            this.failed = false;
        }
    }
}
