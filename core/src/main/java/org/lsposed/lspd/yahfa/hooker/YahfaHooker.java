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
 * Copyright (C) 2021 LSPosed Contributors
 */

package org.lsposed.lspd.yahfa.hooker;

import android.os.Build;

import org.lsposed.lspd.nativebridge.ClassLinker;
import org.lsposed.lspd.nativebridge.Yahfa;
import org.lsposed.lspd.util.ClassUtils;
import org.lsposed.lspd.yahfa.dexmaker.DynamicBridge;

import java.lang.reflect.Executable;

import de.robv.android.xposed.XposedBridge.AdditionalHookInfo;

public class YahfaHooker {
    public static void init() {
        int sdkVersion = Build.VERSION.SDK_INT;
        if (Build.VERSION.PREVIEW_SDK_INT != 0) {
            sdkVersion += 1;
        }
        Yahfa.init(sdkVersion);
    }

    public static void hookMethod(Executable method, AdditionalHookInfo additionalInfo) {
        DynamicBridge.hookMethod(method, additionalInfo);
    }

    public static boolean shouldDelayHook(Executable member) {
        return ClassUtils.shouldDelayHook(member);
    }

    public static Object invokeOriginalMethod(Executable method, Object thisObject, Object[] args) throws Throwable {
        return DynamicBridge.invokeOriginalMethod(method, thisObject, args);
    }

    public static void deoptMethodNative(Executable method) {
        ClassLinker.setEntryPointsToInterpreter(method);
    }
}
