package com.elderdrivers.riru.edxp._hooker.impl;

import android.app.AndroidAppHelper;
import android.app.LoadedApk;
import android.util.Log;

import com.elderdrivers.riru.edxp.hooker.XposedBlackListHooker;
import com.elderdrivers.riru.edxp.util.Hookers;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

// when a package is loaded for an existing process, trigger the callbacks as well
// ed: remove resources related hooking
public class LoadedApkCstr extends XC_MethodHook {

    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        if (XposedBlackListHooker.shouldDisableHooks("")) {
            return;
        }

        Hookers.logD("LoadedApk#<init> starts");

        try {
            LoadedApk loadedApk = (LoadedApk) param.thisObject;
            String packageName = loadedApk.getPackageName();
            Object mAppDir = XposedHelpers.getObjectField(loadedApk, "mAppDir");
            Hookers.logD("LoadedApk#<init> ends: " + mAppDir);

            if (XposedBlackListHooker.shouldDisableHooks(packageName)) {
                return;
            }

            if (packageName.equals("android")) {
                Hookers.logD("LoadedApk#<init> is android, skip: " + mAppDir);
                return;
            }

            // mIncludeCode checking should go ahead of loadedPackagesInProcess added checking
            if (!XposedHelpers.getBooleanField(loadedApk, "mIncludeCode")) {
                Hookers.logD("LoadedApk#<init> mIncludeCode == false: " + mAppDir);
                return;
            }

            if (!XposedInit.loadedPackagesInProcess.add(packageName)) {
                Hookers.logD("LoadedApk#<init> has been loaded before, skip: " + mAppDir);
                return;
            }

            // OnePlus magic...
            if (Log.getStackTraceString(new Throwable()).
                    contains("android.app.ActivityThread$ApplicationThread.schedulePreload")) {
                Hookers.logD("LoadedApk#<init> maybe oneplus's custom opt, skip");
                return;
            }

            XC_LoadPackage.LoadPackageParam lpparam = new XC_LoadPackage.LoadPackageParam(XposedBridge.sLoadedPackageCallbacks);
            lpparam.packageName = packageName;
            lpparam.processName = AndroidAppHelper.currentProcessName();
            lpparam.classLoader = loadedApk.getClassLoader();
            lpparam.appInfo = loadedApk.getApplicationInfo();
            lpparam.isFirstApplication = false;
            XC_LoadPackage.callAll(lpparam);
        } catch (Throwable t) {
            Hookers.logE("error when hooking LoadedApk.<init>", t);
        }
    }
}
