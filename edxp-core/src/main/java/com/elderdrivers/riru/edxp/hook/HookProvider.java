package com.elderdrivers.riru.edxp.hook;

import java.lang.reflect.Member;

import de.robv.android.xposed.XposedBridge;

public interface HookProvider {

    void hookMethod(Member method, XposedBridge.AdditionalHookInfo additionalInfo);

    void unhookMethod(Member method);

    Object invokeOriginalMethod(Member method, long methodId, Object thisObject, Object[] args) throws Throwable;

    Member findMethodNative(Member hookMethod);

    void deoptMethods(String packageName, ClassLoader classLoader);

    long getMethodId(Member member);

    Object findMethodNative(Class clazz, String methodName, String methodSig);

    void deoptMethodNative(Object method);

    boolean initXResourcesNative();

    boolean removeFinalFlagNative(Class clazz);

    boolean methodHooked(Member target);

}
