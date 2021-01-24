package com.elderdrivers.riru.edxp.util;

import android.app.AndroidAppHelper;

public class Hookers {

    public static void logD(String prefix) {
        Utils.logD(String.format("%s: pkg=%s, prc=%s", prefix, AndroidAppHelper.currentPackageName(),
                AndroidAppHelper.currentProcessName()));
    }

    public static void logE(String prefix, Throwable throwable) {
        Utils.logE(String.format("%s: pkg=%s, prc=%s", prefix, AndroidAppHelper.currentPackageName(),
                AndroidAppHelper.currentProcessName()), throwable);
    }

}
