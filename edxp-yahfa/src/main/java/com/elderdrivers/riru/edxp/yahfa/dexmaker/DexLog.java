package com.elderdrivers.riru.edxp.yahfa.dexmaker;

import android.util.Log;

import com.elderdrivers.riru.edxp.yahfa.BuildConfig;

public class DexLog {

    public static final String TAG = "EdXposed";

    public static int v(String s) {
        return Log.v(TAG, s);
    }

    public static int i(String s) {
        return Log.i(TAG, s);
    }

    public static int d(String s) {
        if (BuildConfig.DEBUG) {
            return Log.d(TAG, s);
        }
        return 0;
    }

    public static int w(String s) {
        return Log.w(TAG, s);
    }

    public static int e(String s) {
        return Log.e(TAG, s);
    }

    public static int e(String s, Throwable t) {
        return Log.e(TAG, s, t);
    }
}
