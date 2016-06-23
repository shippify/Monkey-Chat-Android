package com.criptext.http;

import android.util.Base64;
import android.webkit.MimeTypeMap;

import com.androidquery.AQuery;
import com.androidquery.auth.BasicHandle;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.criptext.ClientData;
import com.criptext.MonkeyKitSocketService;
import com.criptext.comunication.CBTypes;
import com.criptext.comunication.Compressor;
import com.criptext.comunication.MOKMessage;
import com.criptext.comunication.MessageTypes;
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
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by gesuwall on 6/23/16.
 */
public class AQueryHttp {
    WeakReference<MonkeyKitSocketService> serviceRef;
    final String monkeyID ;
    final AESUtil aesUtil;
    final AQuery aq;
    final BasicHandle handle;


    public AQueryHttp(MonkeyKitSocketService service, AESUtil aesUtil){
        serviceRef = new WeakReference(service);
        ClientData clientData = service.getServiceClientData();
        monkeyID = clientData.getMonkeyId();
        this.aesUtil = aesUtil;
        aq = new AQuery(service.getApplicationContext());
        handle = new BasicHandle(clientData.getAppId(), clientData.getAppKey());
    }

    /**
     * Created by gesuwall on 6/17/16.
     */
    static class FileUploader extends AQueryHttp {

        public FileUploader(MonkeyKitSocketService service, AESUtil aesUtil) {
            super(service, aesUtil);
        }

        /************************************************************************/


    }
}
