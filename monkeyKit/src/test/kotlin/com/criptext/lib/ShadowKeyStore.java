package com.criptext.lib;

import android.content.Context;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/**
 * Created by gesuwall on 2/22/17.
 */
@Implements(KeyStoreCriptext.class)
public class ShadowKeyStore {
    static boolean syncedBefore = false;
    static long lastSync = 0L;

    @Implementation
    public static String getString(Context context, String key) {
        return key + "iv:key" + key ;
    }

    @Implementation
    public static boolean hasSyncedBefore(Context ctx) {
        return syncedBefore;
    }

    @Implementation
    public static void setFirstSyncSuccess(Context ctx) {
        syncedBefore = true;
    }

    @Implementation
    public static void setLastSync(Context ctx, Long value) {
        System.out.println("set " + value);
        lastSync = value;
    }

    @Implementation
    public static Long getLastSync(Context ctx) {
        return lastSync;
    }
}
