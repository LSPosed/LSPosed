package io.github.lsposed.lspd._hooker.impl;

import android.app.LoadedApk;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.lsposed.lspd.hooker.XposedInstallerHooker;
import io.github.lsposed.lspd.nativebridge.ConfigManager;
import io.github.lsposed.lspd.util.Hookers;
import io.github.lsposed.lspd.util.InstallerVerifier;

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

            if (packageName.equals(ConfigManager.getInstallerPackageName())) {
                if (InstallerVerifier.verifyInstallerSignature(loadedApk.getApplicationInfo())) {
                    XposedInstallerHooker.hookXposedInstaller(lpparam.classLoader);
                } else {
                    InstallerVerifier.hookXposedInstaller(classLoader);
                }
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
