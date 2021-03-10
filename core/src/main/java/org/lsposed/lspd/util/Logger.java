package org.lsposed.lspd.util;

import android.util.Log;

public class Logger {
    private static final String TAG = "LSPosed";

    public static int e(String msg) {
        return Log.e(TAG, msg);
    }

    public static int e(String msg, Throwable e) {
        return Log.e(TAG, msg, e);
    }

    public static int e(Throwable e) {
        return Log.e(TAG, e.getMessage(), e);
    }

    public static int d(String msg) {
        return Log.d(TAG, msg);
    }

    public static int w(String msg) {
        return Log.w(TAG, msg);
    }
}
