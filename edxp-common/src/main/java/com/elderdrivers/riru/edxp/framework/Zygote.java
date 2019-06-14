package com.elderdrivers.riru.edxp.framework;

import com.elderdrivers.riru.edxp.util.Utils;

import de.robv.android.xposed.XposedHelpers;

public class Zygote {

    // prevent from fatal error caused by holding not whitelisted file descriptors when forking zygote
    // https://github.com/rovo89/Xposed/commit/b3ba245ad04cd485699fb1d2ebde7117e58214ff
    public static native void closeFilesBeforeFork();

    public static native void reopenFilesAfterFork();

    public static void allowFileAcrossFork(String path) {
        try {
            Class zygote = XposedHelpers.findClass("com.android.internal.os.Zygote", null);
            XposedHelpers.callStaticMethod(zygote, "nativeAllowFileAcrossFork", path);
        } catch (Throwable throwable) {
            Utils.logE("error when allowFileAcrossFork", throwable);
        }
    }
}
