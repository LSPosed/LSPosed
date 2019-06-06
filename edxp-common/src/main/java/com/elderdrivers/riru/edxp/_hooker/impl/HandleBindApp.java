package com.elderdrivers.riru.edxp._hooker.impl;

import android.app.ActivityThread;
import android.app.LoadedApk;
import android.content.ComponentName;
import android.content.pm.ApplicationInfo;
import android.content.res.CompatibilityInfo;

import com.elderdrivers.riru.edxp.config.ConfigManager;
import com.elderdrivers.riru.edxp.hooker.SliceProviderFix;
import com.elderdrivers.riru.edxp.hooker.XposedBlackListHooker;
import com.elderdrivers.riru.edxp.hooker.XposedInstallerHooker;
import com.elderdrivers.riru.edxp.util.Hookers;
import com.elderdrivers.riru.edxp.util.Utils;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static com.elderdrivers.riru.edxp.config.InstallerChooser.INSTALLER_PACKAGE_NAME;
import static com.elderdrivers.riru.edxp.hooker.SliceProviderFix.SYSTEMUI_PACKAGE_NAME;
import static com.elderdrivers.riru.edxp.hooker.XposedBlackListHooker.BLACK_LIST_PACKAGE_NAME;

// normal process initialization (for new Activity, Service, BroadcastReceiver etc.)
public class HandleBindApp extends XC_MethodHook {

    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
        if (XposedBlackListHooker.shouldDisableHooks("")) {
            return;
        }
        try {
            Hookers.logD("ActivityThread#handleBindApplication() starts");
            ActivityThread activityThread = (ActivityThread) param.thisObject;
            Object bindData = param.args[0];
            ApplicationInfo appInfo = (ApplicationInfo) XposedHelpers.getObjectField(bindData, "appInfo");
            // save app process name here for later use
            ConfigManager.appProcessName = (String) XposedHelpers.getObjectField(bindData, "processName");
            String reportedPackageName = appInfo.packageName.equals("android") ? "system" : appInfo.packageName;
            Utils.logD("processName=" + ConfigManager.appProcessName +
                    ", packageName=" + reportedPackageName + ", appDataDir=" + ConfigManager.appDataDir);

            if (XposedBlackListHooker.shouldDisableHooks(reportedPackageName)) {
                return;
            }

            ComponentName instrumentationName = (ComponentName) XposedHelpers.getObjectField(bindData, "instrumentationName");
            if (instrumentationName != null) {
                Hookers.logD("Instrumentation detected, disabling framework for");
                XposedBridge.disableHooks = true;
                return;
            }
            CompatibilityInfo compatInfo = (CompatibilityInfo) XposedHelpers.getObjectField(bindData, "compatInfo");
            if (appInfo.sourceDir == null) {
                return;
            }

            XposedHelpers.setObjectField(activityThread, "mBoundApplication", bindData);
            XposedInit.loadedPackagesInProcess.add(reportedPackageName);
            LoadedApk loadedApk = activityThread.getPackageInfoNoCheck(appInfo, compatInfo);

            XC_LoadPackage.LoadPackageParam lpparam = new XC_LoadPackage.LoadPackageParam(XposedBridge.sLoadedPackageCallbacks);
            lpparam.packageName = reportedPackageName;
            lpparam.processName = (String) XposedHelpers.getObjectField(bindData, "processName");
            lpparam.classLoader = loadedApk.getClassLoader();
            lpparam.appInfo = appInfo;
            lpparam.isFirstApplication = true;
            XC_LoadPackage.callAll(lpparam);

            if (reportedPackageName.equals(INSTALLER_PACKAGE_NAME)) {
                XposedInstallerHooker.hookXposedInstaller(lpparam.classLoader);
            }
            if (reportedPackageName.equals(BLACK_LIST_PACKAGE_NAME)) {
                XposedBlackListHooker.hook(lpparam.classLoader);
            }
            if (reportedPackageName.equals(SYSTEMUI_PACKAGE_NAME)) {
                SliceProviderFix.hook();
            }
        } catch (Throwable t) {
            Hookers.logE("error when hooking bindApp", t);
        }
    }
}
