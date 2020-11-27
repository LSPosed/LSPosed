package com.elderdrivers.riru.edxp.framework;

import com.elderdrivers.riru.edxp.util.Utils;

import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.annotation.ApiSensitive;
import de.robv.android.xposed.annotation.Level;

@ApiSensitive(Level.LOW)
public class Zygote {
    public static void allowFileAcrossFork(String path) {
        try {
            Class zygote = XposedHelpers.findClass("com.android.internal.os.Zygote", null);
            XposedHelpers.callStaticMethod(zygote, "nativeAllowFileAcrossFork", path);
        } catch (Throwable throwable) {
            Utils.logE("error when allowFileAcrossFork", throwable);
        }
    }
}
