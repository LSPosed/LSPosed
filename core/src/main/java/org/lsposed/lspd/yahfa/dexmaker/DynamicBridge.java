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

package org.lsposed.lspd.yahfa.dexmaker;

import org.lsposed.lspd.util.Utils;

import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.LspHooker;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

public final class DynamicBridge {
    private static final ConcurrentHashMap<Executable, LspHooker> hookedInfo = new ConcurrentHashMap<>();

    public static void hookMethod(Executable hookMethod, XposedBridge.AdditionalHookInfo additionalHookInfo) {
        Utils.logD("hooking " + hookMethod);

        hookedInfo.compute(hookMethod, (method, hooker) -> {
            if (hooker == null) {
                Utils.logD("start to generate class for: " + hookMethod);
                try {
                    final HookerDexMaker dexMaker = new HookerDexMaker();
                    dexMaker.start(hookMethod, additionalHookInfo);
                    hooker = dexMaker.getHooker();
                } catch (Throwable e) {
                    Utils.logE("error occur when generating dex.", e);
                }
            } else {
                for (var callback : additionalHookInfo.callbacks.getSnapshot())
                    hooker.additionalInfo.callbacks.add((XC_MethodHook) callback);
            }
            return hooker;
        });

    }

    public static Object invokeOriginalMethod(Member method, Object thisObject, Object[] args)
            throws InvocationTargetException, IllegalAccessException {
        if (!(method instanceof Executable)) {
            throw new IllegalArgumentException("Only methods or constructors can be invoked.");
        }
        LspHooker hooker = hookedInfo.getOrDefault(method, null);
        if (hooker == null) {
            throw new IllegalStateException("method not hooked, cannot call original method.");
        }
        if (args == null) {
            args = new Object[0];
        }
        return hooker.invokeOriginalMethod(thisObject, args);
    }
}
