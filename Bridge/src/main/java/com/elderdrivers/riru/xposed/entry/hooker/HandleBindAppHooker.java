package com.elderdrivers.riru.xposed.entry.hooker;

import android.app.ActivityThread;
import android.app.LoadedApk;
import android.content.ComponentName;
import android.content.pm.ApplicationInfo;
import android.content.res.CompatibilityInfo;

import com.elderdrivers.riru.common.KeepMembers;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static com.elderdrivers.riru.xposed.util.ClassLoaderUtils.replaceParentClassLoader;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static de.robv.android.xposed.XposedInit.INSTALLER_PACKAGE_NAME;
import static de.robv.android.xposed.XposedInit.loadedPackagesInProcess;
import static de.robv.android.xposed.XposedInit.logD;
import static de.robv.android.xposed.XposedInit.logE;

// normal process initialization (for new Activity, Service, BroadcastReceiver etc.)
public class HandleBindAppHooker implements KeepMembers {

    public static String className = "android.app.ActivityThread";
    public static String methodName = "handleBindApplication";
    public static String methodSig = "(Landroid/app/ActivityThread$AppBindData;)V";

    public static void hook(Object thiz, Object bindData) {
        if (XposedBridge.disableHooks) {
            backup(thiz, bindData);
            return;
        }
        try {
            logD("ActivityThread#handleBindApplication() starts");
            ActivityThread activityThread = (ActivityThread) thiz;
            ApplicationInfo appInfo = (ApplicationInfo) getObjectField(bindData, "appInfo");
            String reportedPackageName = appInfo.packageName.equals("android") ? "system" : appInfo.packageName;
            ComponentName instrumentationName = (ComponentName) getObjectField(bindData, "instrumentationName");
            if (instrumentationName != null) {
                logD("Instrumentation detected, disabling framework for");
                XposedBridge.disableHooks = true;
                return;
            }
            CompatibilityInfo compatInfo = (CompatibilityInfo) getObjectField(bindData, "compatInfo");
            if (appInfo.sourceDir == null) {
                return;
            }

            setObjectField(activityThread, "mBoundApplication", bindData);
            loadedPackagesInProcess.add(reportedPackageName);
            LoadedApk loadedApk = activityThread.getPackageInfoNoCheck(appInfo, compatInfo);

            replaceParentClassLoader(loadedApk.getClassLoader());

            XC_LoadPackage.LoadPackageParam lpparam = new XC_LoadPackage.LoadPackageParam(XposedBridge.sLoadedPackageCallbacks);
            lpparam.packageName = reportedPackageName;
            lpparam.processName = (String) getObjectField(bindData, "processName");
            lpparam.classLoader = loadedApk.getClassLoader();
            lpparam.appInfo = appInfo;
            lpparam.isFirstApplication = true;
            XC_LoadPackage.callAll(lpparam);

            if (reportedPackageName.equals(INSTALLER_PACKAGE_NAME)) {
                XposedInstallerHooker.hookXposedInstaller(lpparam.classLoader);
            }
        } catch (Throwable t) {
            logE("error when hooking bindApp", t);
        }
        backup(thiz, bindData);
    }

    public static void backup(Object thiz, Object bindData) {
    }
}