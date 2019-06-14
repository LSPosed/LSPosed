package com.elderdrivers.riru.edxp.framework;

import de.robv.android.xposed.XposedHelpers;

public class ProcessHelper {

    static {
        // WEBVIEW_ZYGOTE_UID differ among versions
        WEBVIEW_ZYGOTE_UID = XposedHelpers.getStaticIntField(android.os.Process.class, "WEBVIEW_ZYGOTE_UID");
    }

    /**
     * Defines the UID/GID for the shared RELRO file updater process.
     */
    public static final int SHARED_RELRO_UID = 1037;

    /**
     * Defines the UID/GID for the WebView zygote process.
     */
    public static final int WEBVIEW_ZYGOTE_UID;

    /**
     * First uid used for fully isolated sandboxed processes (with no permissions of their own)
     */
    public static final int FIRST_ISOLATED_UID = 99000;
    /**
     * Last uid used for fully isolated sandboxed processes (with no permissions of their own)
     */
    public static final int LAST_ISOLATED_UID = 99999;

    /**
     * Range of uids allocated for a user.
     */
    public static final int PER_USER_RANGE = 100000;

    public static int getAppId(int uid) {
        return uid % PER_USER_RANGE;
    }

    public static boolean isRELROUpdater(int uid) {
        return getAppId(uid) == SHARED_RELRO_UID;
    }

    public static boolean isWebViewZygote(int uid) {
        return getAppId(uid) == WEBVIEW_ZYGOTE_UID;
    }

    public static boolean isIsolated(int uid) {
        uid = getAppId(uid);
        return uid >= FIRST_ISOLATED_UID && uid <= LAST_ISOLATED_UID;
    }
}
