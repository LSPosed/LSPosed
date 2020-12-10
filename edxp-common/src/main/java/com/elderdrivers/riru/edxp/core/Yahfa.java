package com.elderdrivers.riru.edxp.core;

import java.lang.reflect.Member;
import java.lang.reflect.Method;

public class Yahfa {

    public static native boolean backupAndHookNative(Object target, Method hook, Method backup);

    // JNI.ToReflectedMethod() could return either Method or Constructor
    public static native Member findMethodNative(Class targetClass, String methodName, String methodSig);

    public static native void init(int sdkVersion);

    public static native void setMethodNonCompilable(Member member);

    public static native boolean setNativeFlag(Member member, boolean isNative);

    public static native void recordHooked(Member member);

    public static native boolean isHooked(Member member);

    public static native void makeInitializedClassesVisiblyInitialized(long thread, boolean wait);
}
