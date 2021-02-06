package io.github.lsposed.lspd.nativebridge;

import java.lang.reflect.Method;

public class PendingHooks {
    public static native void recordPendingMethodNative(Method hookMethod, Class clazz);
}
