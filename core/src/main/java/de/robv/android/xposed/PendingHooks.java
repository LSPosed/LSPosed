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

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static de.robv.android.xposed.XposedBridge.hookMethodNative;
import static io.github.lsposed.lspd.nativebridge.PendingHooks.recordPendingMethodNative;

public final class PendingHooks {

    // GuardedBy("PendingHooks.class")
    private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<Member, XposedBridge.AdditionalHookInfo>>
            sPendingHooks = new ConcurrentHashMap<>();

    public synchronized static void hookPendingMethod(Class<?> clazz) {
        if (sPendingHooks.containsKey(clazz)) {
            for (Map.Entry<Member, XposedBridge.AdditionalHookInfo> hook : sPendingHooks.get(clazz).entrySet()) {
                hookMethodNative(hook.getKey(), clazz, 0, hook.getValue());
            }
            sPendingHooks.remove(clazz);
        }
    }

    public synchronized static void recordPendingMethod(Method hookMethod,
                                                        XposedBridge.AdditionalHookInfo additionalInfo) {
        ConcurrentHashMap<Member, XposedBridge.AdditionalHookInfo> pending =
                sPendingHooks.computeIfAbsent(hookMethod.getDeclaringClass(),
                        new Function<Class<?>, ConcurrentHashMap<Member, XposedBridge.AdditionalHookInfo>>() {
                            @Override
                            public ConcurrentHashMap<Member, XposedBridge.AdditionalHookInfo> apply(Class<?> aClass) {
                                return new ConcurrentHashMap<>();
                            }
                        });

        pending.put(hookMethod, additionalInfo);
        recordPendingMethodNative(hookMethod, hookMethod.getDeclaringClass());
    }

    public synchronized void cleanUp() {
        sPendingHooks.clear();
    }

}
