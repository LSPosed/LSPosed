package io.github.lsposed.lspd.core;

public class ResourcesHook {

    public static native boolean initXResourcesNative();

    public static native boolean removeFinalFlagNative(Class clazz);

}
