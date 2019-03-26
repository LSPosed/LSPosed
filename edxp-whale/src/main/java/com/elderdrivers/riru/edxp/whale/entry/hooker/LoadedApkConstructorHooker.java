package com.elderdrivers.riru.edxp.whale.entry.hooker;

import android.app.ActivityThread;
import android.app.AndroidAppHelper;
import android.app.LoadedApk;
import android.content.pm.ApplicationInfo;
import android.content.res.CompatibilityInfo;
import android.util.Log;

import com.elderdrivers.riru.common.KeepMembers;
import com.elderdrivers.riru.edxp.whale.entry.Router;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static com.elderdrivers.riru.edxp.util.ClassLoaderUtils.replaceParentClassLoader;

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

        if (XposedBlackListHooker.shouldDisableHooks("")) {
            backup(thiz, activityThread, aInfo, compatInfo, baseLoader, securityViolation,
                    includeCode, registerPackage);
            return;
        }

        Router.logD("LoadedApk#<init> starts");
        backup(thiz, activityThread, aInfo, compatInfo, baseLoader, securityViolation,
                includeCode, registerPackage);

        try {
            LoadedApk loadedApk = (LoadedApk) thiz;
            String packageName = loadedApk.getPackageName();
            Object mAppDir = XposedHelpers.getObjectField(thiz, "mAppDir");
            Router.logD("LoadedApk#<init> ends: " + mAppDir);

            if (XposedBlackListHooker.shouldDisableHooks(packageName)) {
                return;
            }

            if (packageName.equals("android")) {
                Router.logD("LoadedApk#<init> is android, skip: " + mAppDir);
                return;
            }

            // mIncludeCode checking should go ahead of loadedPackagesInProcess added checking
            if (!XposedHelpers.getBooleanField(loadedApk, "mIncludeCode")) {
                Router.logD("LoadedApk#<init> mIncludeCode == false: " + mAppDir);
                return;
            }

            if (!XposedInit.loadedPackagesInProcess.add(packageName)) {
                Router.logD("LoadedApk#<init> has been loaded before, skip: " + mAppDir);
                return;
            }

            // OnePlus magic...
            if (Log.getStackTraceString(new Throwable()).
                    contains("android.app.ActivityThread$ApplicationThread.schedulePreload")) {
                Router.logD("LoadedApk#<init> maybe oneplus's custom opt, skip");
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
            Router.logE("error when hooking LoadedApk.<init>", t);
        }
    }

    public static void backup(Object thiz, ActivityThread activityThread,
                              ApplicationInfo aInfo, CompatibilityInfo compatInfo,
                              ClassLoader baseLoader, boolean securityViolation,
                              boolean includeCode, boolean registerPackage) {

    }
}