package com.swift.sandhook.xposedcompat;

import io.github.lsposed.lspd.util.ProxyClassLoader;
import com.swift.sandhook.wrapper.HookWrapper;
import com.swift.sandhook.xposedcompat.methodgen.SandHookXposedBridge;
import com.swift.sandhook.xposedcompat.utils.ApplicationUtils;

import java.lang.reflect.Member;

import de.robv.android.xposed.XposedBridge;

public class XposedCompat {
    public static volatile ClassLoader classLoader;

    //try to use internal stub hooker & backup method to speed up hook
    public static volatile boolean useInternalStub = true;
    public static volatile boolean useNewCallBackup = true;
    public static volatile boolean retryWhenCallOriginError = false;

    private static ClassLoader sandHookXposedClassLoader;

    public static void addHookers(ClassLoader classLoader, Class[] hookers) {
        if (hookers == null)
            return;
        for (Class hooker : hookers) {
            try {
                HookWrapper.addHookClass(classLoader, hooker);
            } catch (Throwable throwable) {
            }
        }
    }

    public static void onForkProcess() {
        classLoader = null;
        sandHookXposedClassLoader = null;
    }

    public static ClassLoader getClassLoader() {
        if (classLoader == null) {
            classLoader = getSandHookXposedClassLoader(ApplicationUtils.currentApplication().getClassLoader(), XposedCompat.class.getClassLoader());
        }
        return classLoader;
    }

    public static synchronized void hookMethod(Member hookMethod, XposedBridge.AdditionalHookInfo additionalHookInfo) {
        SandHookXposedBridge.hookMethod(hookMethod, additionalHookInfo);
    }

    public static ClassLoader getSandHookXposedClassLoader(ClassLoader appOriginClassLoader, ClassLoader sandBoxHostClassLoader) {
        if (sandHookXposedClassLoader != null) {
            return sandHookXposedClassLoader;
        } else {
            sandHookXposedClassLoader = new ProxyClassLoader(sandBoxHostClassLoader, appOriginClassLoader);
            return sandHookXposedClassLoader;
        }
    }
}
