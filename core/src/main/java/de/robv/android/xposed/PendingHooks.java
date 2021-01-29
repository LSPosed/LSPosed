package de.robv.android.xposed;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static de.robv.android.xposed.XposedBridge.hookMethodNative;

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

    private static native void recordPendingMethodNative(Method hookMethod, Class clazz);
}
