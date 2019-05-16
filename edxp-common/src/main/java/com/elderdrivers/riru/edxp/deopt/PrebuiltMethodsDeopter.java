package com.elderdrivers.riru.edxp.deopt;

import android.text.TextUtils;

import com.elderdrivers.riru.edxp.config.EdXpConfigGlobal;
import com.elderdrivers.riru.edxp.util.Utils;

import java.util.Arrays;

import de.robv.android.xposed.XposedHelpers;

import static com.elderdrivers.riru.edxp.deopt.InlinedMethodCallers.KEY_BOOT_IMAGE;
import static com.elderdrivers.riru.edxp.deopt.InlinedMethodCallers.KEY_BOOT_IMAGE_MIUI_RES;
import static com.elderdrivers.riru.edxp.deopt.InlinedMethodCallers.KEY_SYSTEM_SERVER;

public class PrebuiltMethodsDeopter {

    public static void deoptMethods(String where, ClassLoader cl) {
        String[][] callers = InlinedMethodCallers.get(where);
        if (callers == null) {
            return;
        }
        for (String[] caller : callers) {
            try {
                Class clazz = XposedHelpers.findClassIfExists(caller[0], cl);
                if (clazz == null) {
                    continue;
                }
                Object method = EdXpConfigGlobal.getHookProvider().findMethodNative(
                        clazz, caller[1], caller[2]);
                if (method != null) {
                    EdXpConfigGlobal.getHookProvider().deoptMethodNative(method);
                }
            } catch (Throwable throwable) {
                Utils.logE("error when deopting method: " + Arrays.toString(caller), throwable);
            }
        }
    }

    public static void deoptBootMethods() {
        // todo check if has been done before
        deoptMethods(KEY_BOOT_IMAGE, null);
        if (!TextUtils.isEmpty(Utils.getSysProp("ro.miui.ui.version.code"))
                && EdXpConfigGlobal.getConfig().isResourcesHookEnabled()) {
            //deopt these only for MIUI with resources hook enabled
            deoptMethods(KEY_BOOT_IMAGE_MIUI_RES, null);
        }
    }

    public static void deoptSystemServerMethods(ClassLoader sysCL) {
        deoptMethods(KEY_SYSTEM_SERVER, sysCL);
    }
}
