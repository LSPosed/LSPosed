package com.elderdrivers.riru.edxp.whale.util;

import com.elderdrivers.riru.edxp.util.Utils;
import com.elderdrivers.riru.edxp.Main;

import java.util.Arrays;

import de.robv.android.xposed.XposedHelpers;

import static com.elderdrivers.riru.edxp.whale.util.InlinedMethodCallers.KEY_BOOT_IMAGE;
import static com.elderdrivers.riru.edxp.whale.util.InlinedMethodCallers.KEY_SYSTEM_SERVER;

public class PrebuiltMethodsDeopter {

    public static void deoptMethods(String where, ClassLoader cl) {
        String[][] callers = InlinedMethodCallers.get(where);
        if (callers == null) {
            return;
        }
        for (String[] caller : callers) {
            try {
                Object method = Main.findMethodNative(
                        XposedHelpers.findClass(caller[0], cl), caller[1], caller[2]);
                if (method != null) {
                    Main.deoptMethodNative(method);
                }
            } catch (Throwable throwable) {
                Utils.logE("error when deopting method: " + Arrays.toString(caller), throwable);
            }
        }
    }

    public static void deoptBootMethods() {
        // todo check if has been done before
        deoptMethods(KEY_BOOT_IMAGE, null);
    }

    public static void deoptSystemServerMethods(ClassLoader sysCL) {
        deoptMethods(KEY_SYSTEM_SERVER, sysCL);
    }
}
