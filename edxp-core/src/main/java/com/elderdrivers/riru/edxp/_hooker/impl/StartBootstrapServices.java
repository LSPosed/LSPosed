package com.elderdrivers.riru.edxp._hooker.impl;

import android.os.Build;

import com.elderdrivers.riru.edxp.util.Hookers;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static com.elderdrivers.riru.edxp.util.Utils.logD;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class StartBootstrapServices extends XC_MethodHook {

    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
        if (XposedBridge.disableHooks) {
            return;
        }

        logD("SystemServer#startBootstrapServices() starts");

        try {
            XposedInit.loadedPackagesInProcess.add("android");

            XC_LoadPackage.LoadPackageParam lpparam = new XC_LoadPackage.LoadPackageParam(XposedBridge.sLoadedPackageCallbacks);
            lpparam.packageName = "android";
            lpparam.processName = "android"; // it's actually system_server, but other functions return this as well
            lpparam.classLoader = SystemMain.systemServerCL;
            lpparam.appInfo = null;
            lpparam.isFirstApplication = true;
            XC_LoadPackage.callAll(lpparam);

            // Huawei
            try {
                findAndHookMethod("com.android.server.pm.HwPackageManagerService",
                        SystemMain.systemServerCL, "isOdexMode",
                        XC_MethodReplacement.returnConstant(false));
            } catch (XposedHelpers.ClassNotFoundError | NoSuchMethodError ignored) {
            }

            try {
                String className = "com.android.server.pm." + (Build.VERSION.SDK_INT >= 23 ? "PackageDexOptimizer" : "PackageManagerService");
                findAndHookMethod(className, SystemMain.systemServerCL,
                        "dexEntryExists", String.class,
                        XC_MethodReplacement.returnConstant(true));
            } catch (XposedHelpers.ClassNotFoundError | NoSuchMethodError ignored) {
            }
        } catch (Throwable t) {
            Hookers.logE("error when hooking startBootstrapServices", t);
        }
    }
}
