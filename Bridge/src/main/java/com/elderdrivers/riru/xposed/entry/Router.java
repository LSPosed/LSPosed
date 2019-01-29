package com.elderdrivers.riru.xposed.entry;

import com.elderdrivers.riru.xposed.core.HookMain;
import com.elderdrivers.riru.xposed.entry.bootstrap.AppBootstrapHookInfo;
import com.elderdrivers.riru.xposed.entry.bootstrap.SysBootstrapHookInfo;
import com.elderdrivers.riru.xposed.entry.bootstrap.SysInnerHookInfo;
import com.elderdrivers.riru.xposed.entry.hooker.SystemMainHooker;
import com.elderdrivers.riru.xposed.util.Utils;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedInit;

public class Router {

    public static void onProcessForked(boolean isSystem) {
        // Initialize the Xposed framework and modules
        try {
            XposedInit.initForZygote(isSystem);
        } catch (Throwable t) {
            Utils.logE("Errors during Xposed initialization", t);
            XposedBridge.disableHooks = true;
        }
    }

    public static void loadModulesSafely() {
        try {
            // FIXME some coredomain app can't reading modules.list
            XposedInit.loadModules();
        } catch (Exception exception) {
            Utils.logE("error loading module list", exception);
        }
    }

    public static void startBootstrapHook(boolean isSystem) {
        Utils.logD("startBootstrapHook starts: isSystem = " + isSystem);
        ClassLoader classLoader = XposedBridge.BOOTCLASSLOADER;
        if (isSystem) {
            HookMain.doHookDefault(
                    Router.class.getClassLoader(),
                    classLoader,
                    SysBootstrapHookInfo.class.getName());
        } else {
            HookMain.doHookDefault(
                    Router.class.getClassLoader(),
                    classLoader,
                    AppBootstrapHookInfo.class.getName());
        }
    }

    public static void startSystemServerHook() {
        HookMain.doHookDefault(
                Router.class.getClassLoader(),
                SystemMainHooker.systemServerCL,
                SysInnerHookInfo.class.getName());
    }
}
