package com.elderdrivers.riru.xposed.entry.hooker;

import android.app.ActivityThread;
import android.app.AndroidAppHelper;
import android.app.LoadedApk;
import android.content.pm.ApplicationInfo;
import android.content.res.CompatibilityInfo;

import com.elderdrivers.riru.common.KeepMembers;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static com.elderdrivers.riru.xposed.util.ClassLoaderUtils.replaceParentClassLoader;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedInit.loadedPackagesInProcess;
import static de.robv.android.xposed.XposedInit.logD;
import static de.robv.android.xposed.XposedInit.logE;

// when a package is loaded for an existing process, trigger the callbacks as well
// ed: remove resources related hooking
public class LoadedApkConstructorHooker implements KeepMembers {
    public static String className = "android.app.LoadedApk";
    public static String methodName = "<init>";
    public static String methodSig = "(Landroid/app/ActivityThread;" +
            "Landroid/content/pm/ApplicationInfo;" +
            "Landroid/content/res/CompatibilityInfo;" +
            "Ljava/lang/ClassLoader;ZZZ)V";

    public static void hook(Object thiz, ActivityThread activityThread,
                            ApplicationInfo aInfo, CompatibilityInfo compatInfo,
                            ClassLoader baseLoader, boolean securityViolation,
                            boolean includeCode, boolean registerPackage) {

        if (XposedBridge.disableHooks) {
            backup(thiz, activityThread, aInfo, compatInfo, baseLoader, securityViolation,
                    includeCode, registerPackage);
            return;
        }

        logD("LoadedApk#<init> starts");
        backup(thiz, activityThread, aInfo, compatInfo, baseLoader, securityViolation,
                includeCode, registerPackage);

        try {
            LoadedApk loadedApk = (LoadedApk) thiz;
            String packageName = loadedApk.getPackageName();
            Object mAppDir = getObjectField(thiz, "mAppDir");
            logD("LoadedApk#<init> ends: " + mAppDir);
            if (packageName.equals("android")) {
                logD("LoadedApk#<init> is android, skip: " + mAppDir);
                return;
            }

            if (!loadedPackagesInProcess.add(packageName)) {
                logD("LoadedApk#<init> has been loaded before, skip: " + mAppDir);
                return;
            }

            if (!getBooleanField(loadedApk, "mIncludeCode")) {
                logD("LoadedApk#<init> mIncludeCode == false: " + mAppDir);
                return;
            }

            replaceParentClassLoader(loadedApk.getClassLoader());

            XC_LoadPackage.LoadPackageParam lpparam = new XC_LoadPackage.LoadPackageParam(XposedBridge.sLoadedPackageCallbacks);
            lpparam.packageName = packageName;
            lpparam.processName = AndroidAppHelper.currentProcessName();
            lpparam.classLoader = loadedApk.getClassLoader();
            lpparam.appInfo = loadedApk.getApplicationInfo();
            lpparam.isFirstApplication = false;
            XC_LoadPackage.callAll(lpparam);
        } catch (Throwable t) {
            logE("error when hooking LoadedApk.<init>", t);
        }
    }

    public static void backup(Object thiz, ActivityThread activityThread,
                              ApplicationInfo aInfo, CompatibilityInfo compatInfo,
                              ClassLoader baseLoader, boolean securityViolation,
                              boolean includeCode, boolean registerPackage) {

    }
}