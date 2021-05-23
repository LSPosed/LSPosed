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

import org.lsposed.lspd.util.Logger;

import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.LspHooker;
import de.robv.android.xposed.XposedBridge;

public final class DynamicBridge {
    private static final ConcurrentHashMap<Executable, LspHooker> hookedInfo = new ConcurrentHashMap<>();
    private static final HookerDexMaker dexMaker = new HookerDexMaker();

    public static synchronized void hookMethod(Executable hookMethod, XposedBridge.AdditionalHookInfo additionalHookInfo) {
        Logger.d("hooking " + hookMethod);

        if (hookedInfo.containsKey(hookMethod)) {
            Logger.w("already hook method:" + hookMethod.toString());
            return;
        }

        Logger.d("start to generate class for: " + hookMethod);
        try {
            dexMaker.start(hookMethod, additionalHookInfo,
                    hookMethod.getDeclaringClass().getClassLoader());
            hookedInfo.put(hookMethod, dexMaker.getHooker());
        } catch (Throwable e) {
            Logger.e("error occur when generating dex.", e);
        }
    }

    public static Object invokeOriginalMethod(Member method, Object thisObject, Object[] args)
            throws InvocationTargetException, IllegalAccessException {
        LspHooker hooker = hookedInfo.get(method);
        if (hooker == null) {
            throw new IllegalStateException("method not hooked, cannot call original method.");
        }
        if (args == null) {
            args = new Object[0];
        }
        return hooker.invokeOriginalMethod(thisObject, args);
    }
}


