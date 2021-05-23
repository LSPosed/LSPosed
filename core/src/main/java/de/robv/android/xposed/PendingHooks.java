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

package de.robv.android.xposed;

import static org.lsposed.lspd.nativebridge.PendingHooks.recordPendingMethodNative;

import org.lsposed.lspd.yahfa.hooker.YahfaHooker;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PendingHooks {

    // GuardedBy("PendingHooks.class")
    private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<Executable, XposedBridge.AdditionalHookInfo>>
            sPendingHooks = new ConcurrentHashMap<>();

    public synchronized static void hookPendingMethod(Class<?> clazz) {
        if (sPendingHooks.containsKey(clazz)) {
            for (Map.Entry<Executable, XposedBridge.AdditionalHookInfo> hook : sPendingHooks.get(clazz).entrySet()) {
                YahfaHooker.hookMethod(hook.getKey(), hook.getValue());
            }
            sPendingHooks.remove(clazz);
        }
    }

    public synchronized static void recordPendingMethod(Method hookMethod,
                                                        XposedBridge.AdditionalHookInfo additionalInfo) {
        ConcurrentHashMap<Executable, XposedBridge.AdditionalHookInfo> pending =
                sPendingHooks.computeIfAbsent(hookMethod.getDeclaringClass(), aClass -> new ConcurrentHashMap<>());

        pending.put(hookMethod, additionalInfo);
        recordPendingMethodNative(hookMethod, hookMethod.getDeclaringClass());
    }

    public synchronized void cleanUp() {
        sPendingHooks.clear();
    }

}
