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

package org.lsposed.lspd.deopt;

import static org.lsposed.lspd.deopt.InlinedMethodCallers.KEY_BOOT_IMAGE;
import static org.lsposed.lspd.deopt.InlinedMethodCallers.KEY_BOOT_IMAGE_MIUI_RES;
import static org.lsposed.lspd.deopt.InlinedMethodCallers.KEY_SYSTEM_SERVER;

import org.lsposed.lspd.nativebridge.HookBridge;
import org.lsposed.lspd.util.Utils;

import java.lang.reflect.Executable;
import java.util.Arrays;

import de.robv.android.xposed.XposedHelpers;

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
                Executable method = null;
                if (method != null) {
                    HookBridge.deoptimizeMethod(method);
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

    public static void deoptResourceMethods() {
        if (Utils.isMIUI) {
            //deopt these only for MIUI
            deoptMethods(KEY_BOOT_IMAGE_MIUI_RES, null);
        }
    }

    public static void deoptSystemServerMethods(ClassLoader sysCL) {
        deoptMethods(KEY_SYSTEM_SERVER, sysCL);
    }
}
