package de.robv.android.xposed;

import java.lang.reflect.Member;
import java.util.concurrent.ConcurrentHashMap;

import static de.robv.android.xposed.XposedBridge.hookMethodNative;

public final class PendingHooks {

    private static final ConcurrentHashMap<Member, XposedBridge.AdditionalHookInfo>
            sPendingHookMethods = new ConcurrentHashMap<>();

    public synchronized static void hookPendingMethod(Class clazz) {
        for (Member member : sPendingHookMethods.keySet()) {
            if (member.getDeclaringClass().equals(clazz)) {
                hookMethodNative(member, clazz, 0, sPendingHookMethods.get(member));
            }
        }
    }

    public synchronized static void recordPendingMethod(Member hookMethod, XposedBridge.AdditionalHookInfo additionalInfo) {
        sPendingHookMethods.put(hookMethod, additionalInfo);
    }
}
