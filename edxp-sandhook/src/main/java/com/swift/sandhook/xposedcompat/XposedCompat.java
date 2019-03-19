package com.swift.sandhook.xposedcompat;

import com.swift.sandhook.xposedcompat.classloaders.ComposeClassLoader;
import com.swift.sandhook.xposedcompat.methodgen.SandHookXposedBridge;
import com.swift.sandhook.xposedcompat.utils.FileUtils;

import java.io.File;
import java.lang.reflect.Member;

import de.robv.android.xposed.XposedBridge;

public class XposedCompat {

    //try to use internal stub hooker & backup method to speed up hook
    public static volatile boolean useInternalStub = true;
    public static volatile boolean useNewDexMaker = true;
    public static volatile boolean retryWhenCallOriginError = false;

    private static ClassLoader sandHookXposedClassLoader;

    public static synchronized void hookMethod(Member hookMethod, XposedBridge.AdditionalHookInfo additionalHookInfo) {
        SandHookXposedBridge.hookMethod(hookMethod, additionalHookInfo);
    }

    public static ClassLoader getSandHookXposedClassLoader(ClassLoader appOriginClassLoader, ClassLoader sandBoxHostClassLoader) {
        if (sandHookXposedClassLoader != null) {
            return sandHookXposedClassLoader;
        } else {
            sandHookXposedClassLoader = new ComposeClassLoader(sandBoxHostClassLoader, appOriginClassLoader);
            return sandHookXposedClassLoader;
        }
    }

//    public static boolean clearCache() {
//        try {
//            FileUtils.delete(cacheDir);
//            cacheDir.mkdirs();
//            return true;
//        } catch (Throwable throwable) {
//            return false;
//        }
//    }
//
//    public static void clearOatCache() {
//        SandHookXposedBridge.clearOatFile();
//    }

}
