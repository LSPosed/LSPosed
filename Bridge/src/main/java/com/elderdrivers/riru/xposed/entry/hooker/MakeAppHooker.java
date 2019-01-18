package com.elderdrivers.riru.xposed.entry.hooker;

import android.app.AndroidAppHelper;
import android.app.Application;
import android.app.Instrumentation;
import android.app.LoadedApk;

import com.elderdrivers.riru.common.KeepMembers;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static com.elderdrivers.riru.xposed.entry.hooker.XposedInstallerHooker.hookXposedInstaller;
import static com.elderdrivers.riru.xposed.util.ClassLoaderUtils.replaceParentClassLoader;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedInit.INSTALLER_PACKAGE_NAME;
import static de.robv.android.xposed.XposedInit.logD;
import static de.robv.android.xposed.XposedInit.logE;


public class MakeAppHooker implements KeepMembers {

    public static String className = "android.app.LoadedApk";
    public static String methodName = "makeApplication";
    public static String methodSig = "(ZLandroid/app/Instrumentation;)Landroid/app/Application;";

    public static Application hook(Object thiz, boolean forceDefaultAppClass,
                                   Instrumentation instrumentation) {
        if (XposedBridge.disableHooks) {
            return backup(thiz, forceDefaultAppClass, instrumentation);
        }
        logD("LoadedApk#makeApplication() starts");
        boolean shouldHook = getObjectField(thiz, "mApplication") == null;
        logD("LoadedApk#makeApplication() shouldHook == " + shouldHook);
        Application application = backup(thiz, forceDefaultAppClass, instrumentation);
        if (shouldHook) {
            try {
                LoadedApk loadedApk = (LoadedApk) thiz;
                String packageName = loadedApk.getPackageName();

                if (!getBooleanField(loadedApk, "mIncludeCode")) {
                    logD("LoadedApk#makeApplication() mIncludeCode == false");
                    return application;
                }

                logD("LoadedApk#makeApplication() mIncludeCode == true");

                replaceParentClassLoader(loadedApk.getClassLoader());

                XC_LoadPackage.LoadPackageParam lpparam = new XC_LoadPackage.LoadPackageParam(
                        XposedBridge.sLoadedPackageCallbacks);
                lpparam.packageName = packageName;
                lpparam.processName = AndroidAppHelper.currentProcessName();
                lpparam.classLoader = loadedApk.getClassLoader();
                lpparam.appInfo = loadedApk.getApplicationInfo();
                lpparam.isFirstApplication = true;
                XC_LoadPackage.callAll(lpparam);
                if (packageName.equals(INSTALLER_PACKAGE_NAME)) {
                    hookXposedInstaller(lpparam.classLoader);
                }
            } catch (Throwable t) {
                logE("error when hooking LoadedApk#makeApplication", t);
            }
        }
        return application;
    }

    public static Application backup(Object thiz, boolean forceDefaultAppClass,
                                     Instrumentation instrumentation) {
        return null;
    }
}
