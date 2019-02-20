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

    public volatile static boolean forkCompleted = false;

    public static void prepare(boolean isSystem) {
        // this flag is needed when loadModules
        XposedInit.startsSystemServer = isSystem;
    }

    public static void installBootstrapHooks(boolean isSystem) {
        // Initialize the Xposed framework
        try {
            XposedInit.initForZygote(isSystem);
        } catch (Throwable t) {
            Utils.logE("error during Xposed initialization", t);
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

    public static void onEnterChildProcess() {
        forkCompleted = true;
    }
}
