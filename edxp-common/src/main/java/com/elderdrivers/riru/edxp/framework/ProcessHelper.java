package com.elderdrivers.riru.edxp.framework;

import android.os.Process;

import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.annotation.ApiSensitive;
import de.robv.android.xposed.annotation.Level;

@ApiSensitive(Level.MIDDLE)
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
     * First uid used for fully isolated sandboxed processes spawned from an app zygote
     */
    public static final int FIRST_APP_ZYGOTE_ISOLATED_UID = 90000;
    /**
     * Number of UIDs we allocate per application zygote
     */
    public static final int NUM_UIDS_PER_APP_ZYGOTE = 100;
    /**
     * Last uid used for fully isolated sandboxed processes spawned from an app zygote
     */
    public static final int LAST_APP_ZYGOTE_ISOLATED_UID = 98999;

    /**
     * Range of uids allocated for a user.
     */
    public static final int PER_USER_RANGE = 100000;

    // @see UserHandle#getAppId(int)
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
        return (boolean) XposedHelpers.callStaticMethod(
                Process.class, "isIsolated", uid);
    }

    /**
     * Whether a UID belongs to a regular app. *Note* "Not a regular app" does not mean
     * "it's system", because of isolated UIDs. Use {@link #isCore} for that.
     */
    public static boolean isApp(int uid) {
        if (uid > 0) {
            final int appId = getAppId(uid);
            return appId >= Process.FIRST_APPLICATION_UID && appId <= Process.LAST_APPLICATION_UID;
        } else {
            return false;
        }
    }

    /**
     * Whether a UID belongs to a system core component or not.
     */
    public static boolean isCore(int uid) {
        if (uid >= 0) {
            final int appId = getAppId(uid);
            return appId < Process.FIRST_APPLICATION_UID;
        } else {
            return false;
        }
    }
}
