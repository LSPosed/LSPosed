package com.elderdrivers.riru.edxp.core;

import java.lang.reflect.Member;
import java.lang.reflect.Method;

public class Yahfa {

    public static native boolean backupAndHookNative(Object target, Method hook, Method backup);

    public static native void ensureMethodCached(Method hook, Method backup);

    // JNI.ToReflectedMethod() could return either Method or Constructor
    public static native Object findMethodNative(Class targetClass, String methodName, String methodSig);

    public static native void init(int SDK_version);

    public static native void setMethodNonCompilable(Member member);

    public static native boolean setNativeFlag(Member member, boolean isNative);
}
