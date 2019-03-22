package com.elderdrivers.riru.edxp.sandhook.entry;

import android.app.AndroidAppHelper;
import android.text.TextUtils;

import com.elderdrivers.riru.edxp.config.EdXpConfigGlobal;
import com.elderdrivers.riru.edxp.sandhook.config.SandHookEdxpConfig;
import com.elderdrivers.riru.edxp.sandhook.config.SandHookProvider;
import com.elderdrivers.riru.edxp.sandhook.core.HookMain;
import com.elderdrivers.riru.edxp.sandhook.dexmaker.DynamicBridge;
import com.elderdrivers.riru.edxp.sandhook.entry.bootstrap.AppBootstrapHookInfo;
import com.elderdrivers.riru.edxp.sandhook.entry.bootstrap.SysBootstrapHookInfo;
import com.elderdrivers.riru.edxp.sandhook.entry.bootstrap.SysInnerHookInfo;
import com.elderdrivers.riru.edxp.sandhook.entry.bootstrap.WorkAroundHookInfo;
import com.elderdrivers.riru.edxp.sandhook.entry.hooker.SystemMainHooker;
import com.elderdrivers.riru.edxp.util.Utils;
import com.swift.sandhook.SandHookConfig;
import com.swift.sandhook.xposedcompat.XposedCompat;

import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedInit;

import static de.robv.android.xposed.XposedInit.startsSystemServer;

public class Router {

    public volatile static boolean forkCompleted = false;

    private static volatile AtomicBoolean bootstrapHooked = new AtomicBoolean(false);

    static boolean useSandHook;

    static {
        useSandHook = EdXpConfigGlobal.getHookProvider() instanceof SandHookProvider;
    }


    public static void prepare(boolean isSystem) {
        // this flag is needed when loadModules
        startsSystemServer = isSystem;
//        InstallerChooser.setup();
    }

    public static void checkHookState(String appDataDir) {
        // determine whether allow xposed or not
//        XposedBridge.disableHooks = ConfigManager.shouldHook(parsePackageName(appDataDir));
    }

    private static String parsePackageName(String appDataDir) {
        if (TextUtils.isEmpty(appDataDir)) {
            return "";
        }
        int lastIndex = appDataDir.lastIndexOf("/");
        if (lastIndex < 1) {
            return "";
        }
        return appDataDir.substring(lastIndex + 1);
    }

    public static void installBootstrapHooks(boolean isSystem) {
        // Initialize the Xposed framework
        try {
            if (!bootstrapHooked.compareAndSet(false, true)) {
                return;
            }
            Router.startBootstrapHook(isSystem);
            XposedInit.initForZygote(isSystem);
        } catch (Throwable t) {
            Utils.logE("error during Xposed initialization", t);
            XposedBridge.disableHooks = true;
        }
    }

    public static void loadModulesSafely(boolean isInZygote) {
        try {
            // FIXME some coredomain app can't reading modules.list
            XposedInit.loadModules(isInZygote);
        } catch (Exception exception) {
            Utils.logE("error loading module list", exception);
        }
    }

    public static void startBootstrapHook(boolean isSystem) {
        Utils.logD("startBootstrapHook starts: isSystem = " + isSystem);
        ClassLoader classLoader = XposedBridge.BOOTCLASSLOADER;
        if (isSystem) {
            if (useSandHook) {
                XposedCompat.addHookers(classLoader, SysBootstrapHookInfo.hookItems);
            } else {
                HookMain.doHookDefault(
                    Router.class.getClassLoader(),
                    classLoader,
                    SysBootstrapHookInfo.class.getName());
            }
        } else {
            if (useSandHook) {
                XposedCompat.addHookers(classLoader, AppBootstrapHookInfo.hookItems);
            } else {
                HookMain.doHookDefault(
                        Router.class.getClassLoader(),
                        classLoader,
                        AppBootstrapHookInfo.class.getName());
            }
        }
    }

    public static void startSystemServerHook() {
        if (useSandHook) {
            XposedCompat.addHookers(SystemMainHooker.systemServerCL, SysInnerHookInfo.hookItems);
        } else {
            HookMain.doHookDefault(
                    Router.class.getClassLoader(),
                    SystemMainHooker.systemServerCL,
                    SysInnerHookInfo.class.getName());
        }
    }

    public static void startWorkAroundHook() {
        if (useSandHook) {
            XposedCompat.addHookers(XposedBridge.BOOTCLASSLOADER, WorkAroundHookInfo.hookItems);
        } else {
            HookMain.doHookDefault(
                    Router.class.getClassLoader(),
                    XposedBridge.BOOTCLASSLOADER,
                    WorkAroundHookInfo.class.getName());
        }
    }

    public static void onEnterChildProcess() {
        forkCompleted = true;
        DynamicBridge.onForkPost();
        //enable compile in child process
        //SandHook.enableCompiler(!XposedInit.startsSystemServer);
    }

    public static void logD(String prefix) {
        Utils.logD(String.format("%s: pkg=%s, prc=%s", prefix, AndroidAppHelper.currentPackageName(),
                AndroidAppHelper.currentProcessName()));
    }

    public static void logE(String prefix, Throwable throwable) {
        Utils.logE(String.format("%s: pkg=%s, prc=%s", prefix, AndroidAppHelper.currentPackageName(),
                AndroidAppHelper.currentProcessName()), throwable);
    }

    public static void injectConfig() {
        EdXpConfigGlobal.sConfig = new SandHookEdxpConfig();
        EdXpConfigGlobal.sHookProvider = new SandHookProvider();
        SandHookConfig.compiler = !startsSystemServer;
    }
}
