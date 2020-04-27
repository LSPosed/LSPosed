package com.swift.sandhook.xposedcompat;

import android.os.Process;
import android.text.TextUtils;

import com.elderdrivers.riru.edxp.config.ConfigManager;
import com.elderdrivers.riru.edxp.util.FileUtils;
import com.elderdrivers.riru.edxp.util.ProcessUtils;
import com.elderdrivers.riru.edxp.util.ProxyClassLoader;
import com.swift.sandhook.wrapper.HookWrapper;
import com.swift.sandhook.xposedcompat.methodgen.SandHookXposedBridge;
import com.swift.sandhook.xposedcompat.utils.ApplicationUtils;

import java.io.File;
import java.lang.reflect.Member;

import de.robv.android.xposed.XposedBridge;

import static com.elderdrivers.riru.edxp.util.FileUtils.getDataPathPrefix;

public class XposedCompat {

    // TODO initialize these variables
    public static volatile File cacheDir;
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
        cacheDir = null;
        classLoader = null;
        sandHookXposedClassLoader = null;
    }

    public static File getCacheDir() {
        if (cacheDir == null) {
            String fixedAppDataDir = getDataPathPrefix() + getPackageName(ConfigManager.appDataDir) + "/";
            cacheDir = new File(fixedAppDataDir, "/cache/sandhook/"
                    + ProcessUtils.getProcessName(Process.myPid()).replace(":", "_") + "/");
        }
        return cacheDir;
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

    public static boolean clearCache() {
        try {
            FileUtils.delete(getCacheDir());
            getCacheDir().mkdirs();
            return true;
        } catch (Throwable throwable) {
            return false;
        }
    }

    public static void clearOatCache() {
        SandHookXposedBridge.clearOatFile();
    }

    public static String getPackageName(String dataDir) {
        if (TextUtils.isEmpty(dataDir)) {
            return "";
        }
        int lastIndex = dataDir.lastIndexOf("/");
        if (lastIndex < 0) {
            return dataDir;
        }
        return dataDir.substring(lastIndex + 1);
    }

}
