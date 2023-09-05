package org.lsposed.lspd.nativebridge;

import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;

import dalvik.annotation.optimization.FastNative;

public class HookBridge {
    public static native boolean hookMethod(boolean useModernApi, Executable hookMethod, Class<?> hooker, int priority, Object callback);

    public static native boolean unhookMethod(boolean useModernApi, Executable hookMethod, Object callback);

    public static native boolean deoptimizeMethod(Executable method);

    public static native <T> T allocateObject(Class<T> clazz) throws InstantiationException;

    public static native Object invokeOriginalMethod(Executable method, Object thisObject, Object... args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException;

    public static native <T> Object invokeSpecialMethod(Executable method, char[] shorty, Class<T> clazz, Object thisObject, Object... args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException;

    @FastNative
    public static native boolean instanceOf(Object obj, Class<?> clazz);

    @FastNative
    public static native boolean setTrusted(Object cookie);

    public static native Object[][] callbackSnapshot(Class<?> hooker_callback, Executable method);
}
