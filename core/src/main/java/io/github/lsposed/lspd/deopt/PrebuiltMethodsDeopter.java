/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2020 EdXposed Contributors
 * Copyright (C) 2021 LSPosed Contributors
 */

package io.github.lsposed.lspd.deopt;

import android.text.TextUtils;

import io.github.lsposed.lspd.nativebridge.ConfigManager;
import io.github.lsposed.lspd.config.LSPdConfigGlobal;
import io.github.lsposed.lspd.util.Utils;

import java.util.Arrays;

import de.robv.android.xposed.XposedHelpers;

import static io.github.lsposed.lspd.deopt.InlinedMethodCallers.KEY_BOOT_IMAGE;
import static io.github.lsposed.lspd.deopt.InlinedMethodCallers.KEY_BOOT_IMAGE_MIUI_RES;
import static io.github.lsposed.lspd.deopt.InlinedMethodCallers.KEY_SYSTEM_SERVER;

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
                Object method = LSPdConfigGlobal.getHookProvider().findMethodNative(
                        clazz, caller[1], caller[2]);
                if (method != null) {
                    LSPdConfigGlobal.getHookProvider().deoptMethodNative(method);
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
                && ConfigManager.isResourcesHookEnabled()) {
            //deopt these only for MIUI with resources hook enabled
            deoptMethods(KEY_BOOT_IMAGE_MIUI_RES, null);
        }
    }

    public static void deoptSystemServerMethods(ClassLoader sysCL) {
        deoptMethods(KEY_SYSTEM_SERVER, sysCL);
    }
}
