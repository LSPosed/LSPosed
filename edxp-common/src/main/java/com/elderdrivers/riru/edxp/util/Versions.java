package com.elderdrivers.riru.edxp.util;

import android.os.Build;

public class Versions {

    public static boolean hasR() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R;
    }

    public static boolean hasQ() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    }
}
