package com.criptext.monkeychatandroid.gcm;

import com.criptext.gcm.MonkeyRegistrationService;

import org.jetbrains.annotations.NotNull;

/**
 * Created by gesuwall on 6/1/16.
 */
public class SampleRegistrationService extends MonkeyRegistrationService {
    @NotNull
    @Override
    public String getGcm_defaultSenderId() {
        return "254014375838";
    }
}
