package de.robv.android.xposed;

import com.elderdrivers.riru.edxp.config.EdXpConfigGlobal;
import com.jaredrummler.apkparser.utils.Utils;

import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static de.robv.android.xposed.XposedBridge.hookMethodNative;
import static de.robv.android.xposed.XposedBridge.log;

public final class PendingHooks {

    // GuardedBy("PendingHooks.class")
    private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<Member, XposedBridge.AdditionalHookInfo>>
            sPendingHooks = new ConcurrentHashMap<>();

    public synchronized static void hookPendingMethod(Class<?> clazz) {
        if (sPendingHooks.containsKey(clazz)) {
            for (Map.Entry<Member, XposedBridge.AdditionalHookInfo> hook : sPendingHooks.get(clazz).entrySet()) {
                hookMethodNative(hook.getKey(), clazz, 0, hook.getValue());
            }
        }
    }

    public synchronized static void recordPendingMethod(Member hookMethod,
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
        Thread currentThread = Thread.currentThread();
        long nativePeer = XposedHelpers.getLongField(currentThread, "nativePeer");
        recordPendingMethodNative(nativePeer, hookMethod.getDeclaringClass());
    }

    public synchronized void cleanUp() {
        sPendingHooks.clear();
    }

    private static native void recordPendingMethodNative(long thread, Class clazz);
}
