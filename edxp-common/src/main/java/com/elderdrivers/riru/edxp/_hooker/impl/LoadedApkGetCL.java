package com.elderdrivers.riru.edxp._hooker.impl;

import android.app.LoadedApk;

import com.elderdrivers.riru.edxp.config.ConfigManager;
import com.elderdrivers.riru.edxp.hooker.SliceProviderFix;
import com.elderdrivers.riru.edxp.hooker.XposedInstallerHooker;
import com.elderdrivers.riru.edxp.util.Hookers;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static com.elderdrivers.riru.edxp.hooker.SliceProviderFix.SYSTEMUI_PACKAGE_NAME;

public class LoadedApkGetCL extends XC_MethodHook {

    private final LoadedApk loadedApk;
    private final String packageName;
    private final String processName;
    private final boolean isFirstApplication;
    private Unhook unhook;

    public LoadedApkGetCL(LoadedApk loadedApk, String packageName, String processName,
                          boolean isFirstApplication) {
        this.loadedApk = loadedApk;
        this.packageName = packageName;
        this.processName = processName;
        this.isFirstApplication = isFirstApplication;
    }

    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {

        try {

            Hookers.logD("LoadedApk#getClassLoader starts");

            LoadedApk loadedApk = (LoadedApk) param.thisObject;

            if (loadedApk != this.loadedApk) {
                return;
            }

            Object mAppDir = XposedHelpers.getObjectField(loadedApk, "mAppDir");
            ClassLoader classLoader = (ClassLoader) param.getResult();
            Hookers.logD("LoadedApk#getClassLoader ends: " + mAppDir + " -> " + classLoader);

            if (classLoader == null) {
                return;
            }

            XC_LoadPackage.LoadPackageParam lpparam = new XC_LoadPackage.LoadPackageParam(
                    XposedBridge.sLoadedPackageCallbacks);
            lpparam.packageName = this.packageName;
            lpparam.processName = this.processName;
            lpparam.classLoader = classLoader;
            lpparam.appInfo = loadedApk.getApplicationInfo();
            lpparam.isFirstApplication = this.isFirstApplication;
            XC_LoadPackage.callAll(lpparam);

            if (this.packageName.equals(ConfigManager.getInstallerPackageName())) {
                XposedInstallerHooker.hookXposedInstaller(lpparam.classLoader);
            }
            if (this.packageName.equals(SYSTEMUI_PACKAGE_NAME)) {
                SliceProviderFix.hook();
            }

        } catch (Throwable t) {
            Hookers.logE("error when hooking LoadedApk#getClassLoader", t);
        } finally {
            if (unhook != null) {
                unhook.unhook();
            }
        }
    }

    public void setUnhook(Unhook unhook) {
        this.unhook = unhook;
    }

    public Unhook getUnhook() {
        return unhook;
    }
}
