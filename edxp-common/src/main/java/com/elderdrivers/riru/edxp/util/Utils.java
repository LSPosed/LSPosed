package com.elderdrivers.riru.edxp.util;

import android.util.Log;

import com.elderdrivers.riru.edxp.common.BuildConfig;

import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.annotation.ApiSensitive;
import de.robv.android.xposed.annotation.Level;


public class Utils {

    public static final String LOG_TAG = "EdXposed";

    public static void logD(Object msg) {
        if (BuildConfig.DEBUG)
            Log.d(LOG_TAG, msg.toString());
    }

    public static void logD(String msg, Throwable throwable) {
        if (BuildConfig.DEBUG)
            Log.d(LOG_TAG, msg, throwable);
    }

    public static void logW(String msg) {
        Log.w(LOG_TAG, msg);
    }

    public static void logW(String msg, Throwable throwable) {
        Log.w(LOG_TAG, msg, throwable);
    }

    public static void logI(String msg) {
        Log.i(LOG_TAG, msg);
    }

    public static void logI(String msg, Throwable throwable) {
        Log.i(LOG_TAG, msg, throwable);
    }

    public static void logE(String msg) {
        Log.e(LOG_TAG, msg);
    }

    public static void logE(String msg, Throwable throwable) {
        Log.e(LOG_TAG, msg, throwable);
    }

    @ApiSensitive(Level.LOW)
    public static String getSysProp(String key) {
        try {
            Class sysProps = XposedHelpers.findClassIfExists("android.os.SystemProperties", null);
            if (sysProps != null) {
                return (String) XposedHelpers.callStaticMethod(sysProps, "get", key);
            }
        } catch (Throwable throwable) {
            Utils.logE("error when get sys prop", throwable);
        }
        return "";
    }
}
