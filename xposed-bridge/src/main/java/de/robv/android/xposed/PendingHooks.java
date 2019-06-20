package de.robv.android.xposed;

import com.elderdrivers.riru.edxp.config.EdXpConfigGlobal;

import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import static de.robv.android.xposed.XposedBridge.hookMethodNative;

public final class PendingHooks {

    // GuardedBy("PendingHooks.class")
    private static final ConcurrentHashMap<Member, XposedBridge.AdditionalHookInfo>
            sPendingHookMethods = new ConcurrentHashMap<>();
    // GuardedBy("PendingHooks.class")
    private static final HashSet<Member> sNonNativeMethods = new HashSet<>();

    public synchronized static void hookPendingMethod(Class clazz) {
        for (Member member : sPendingHookMethods.keySet()) {
            if (member.getDeclaringClass().equals(clazz)) {
                hookMethodNative(member, clazz, 0, sPendingHookMethods.get(member));
            }
        }
    }

    public synchronized static void removeNativeFlags(Class clazz) {
        for (Member member : sPendingHookMethods.keySet()) {
            if (member.getDeclaringClass().equals(clazz) && sNonNativeMethods.contains(member)) {
                EdXpConfigGlobal.getHookProvider().setNativeFlag(member, false);
            }
        }
    }

    public synchronized static void recordPendingMethod(Member hookMethod,
                                                        XposedBridge.AdditionalHookInfo additionalInfo) {
        if (!Modifier.isNative(hookMethod.getModifiers())) {
            // record non-native methods for later native flag temporary removing
            sNonNativeMethods.add(hookMethod);
        }
        EdXpConfigGlobal.getHookProvider().setNativeFlag(hookMethod, true);
        sPendingHookMethods.put(hookMethod, additionalInfo);
        recordPendingMethodNative("L" +
                hookMethod.getDeclaringClass().getName().replace(".", "/") + ";");
    }

    public synchronized void cleanUp() {
        sPendingHookMethods.clear();
        // sNonNativeMethods should be cleared very carefully because their
        // pre-set native flag have to be removed if its hooking is cancelled
        // before its class is initialized
//        sNonNativeMethods.clear();
    }

    private static native void recordPendingMethodNative(String classDesc);
}
