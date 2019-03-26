package com.elderdrivers.riru.edxp.deopt;

import java.util.HashMap;

/**
 * Providing a whitelist of methods which are the callers of the target methods we want to hook.
 * Because the target methods are inlined into the callers, we deoptimize the callers to
 * run in intercept mode to make target methods hookable.
 * <p>
 * Only for methods which are included in pre-compiled framework codes.
 * TODO recompile system apps and priv-apps since their original dex files are available
 */
public class InlinedMethodCallers {

    public static final String KEY_BOOT_IMAGE = "boot_image";
    public static final String KEY_SYSTEM_SERVER = "system_server";

    /**
     * Key should be {@link #KEY_BOOT_IMAGE}, {@link #KEY_SYSTEM_SERVER}, or a package name
     * of system apps or priv-apps i.e. com.android.systemui
     */
    private static final HashMap<String, String[][]> CALLERS = new HashMap<>();

    /**
     * format for each row: {className, methodName, methodSig}
     */
    private static final String[][] BOOT_IMAGE = {
            // callers of Application#attach(Context)
            {"android.app.Instrumentation", "newApplication", "(Ljava/lang/ClassLoader;Ljava/lang/String;Landroid/content/Context;)Landroid/app/Application;"}
    };

    private static final String[][] SYSTEM_SERVER = {};

    private static final String[][] SYSTEM_UI = {};

    static {
        CALLERS.put(KEY_BOOT_IMAGE, BOOT_IMAGE);
        CALLERS.put(KEY_SYSTEM_SERVER, SYSTEM_SERVER);
        CALLERS.put("com.android.systemui", SYSTEM_UI);
    }

    public static HashMap<String, String[][]> getAll() {
        return CALLERS;
    }

    public static String[][] get(String where) {
        return CALLERS.get(where);
    }
}
