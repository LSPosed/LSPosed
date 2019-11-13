package com.elderdrivers.riru.edxp.sandhook.core;

import com.elderdrivers.riru.edxp.config.EdXpConfigGlobal;
import com.elderdrivers.riru.edxp.proxy.BaseRouter;
import com.elderdrivers.riru.edxp.sandhook.config.SandHookEdxpConfig;
import com.elderdrivers.riru.edxp.sandhook.config.SandHookProvider;
import com.elderdrivers.riru.edxp.sandhook.entry.AppBootstrapHookInfo;
import com.elderdrivers.riru.edxp.sandhook.entry.SysBootstrapHookInfo;
import com.elderdrivers.riru.edxp.sandhook.entry.SysInnerHookInfo;
import com.elderdrivers.riru.edxp.sandhook.entry.WorkAroundHookInfo;
import com.elderdrivers.riru.edxp.sandhook.hooker.SystemMainHooker;
import com.elderdrivers.riru.edxp.util.Utils;
import com.swift.sandhook.xposedcompat.XposedCompat;
import com.swift.sandhook.xposedcompat.methodgen.SandHookXposedBridge;

import de.robv.android.xposed.XposedBridge;

public class SandHookRouter extends BaseRouter {

    public SandHookRouter() {
        useXposedApi = true;
    }

    private static boolean useSandHook = false;

    public void startBootstrapHook(boolean isSystem) {
        if (useSandHook) {
            Utils.logD("startBootstrapHook starts: isSystem = " + isSystem);
            ClassLoader classLoader = XposedBridge.BOOTCLASSLOADER;
            if (isSystem) {
                XposedCompat.addHookers(classLoader, SysBootstrapHookInfo.hookItems);
            } else {
                XposedCompat.addHookers(classLoader, AppBootstrapHookInfo.hookItems);
            }
        } else {
            super.startBootstrapHook(isSystem);
        }
    }

    public void startSystemServerHook() {
        if (useSandHook) {
            XposedCompat.addHookers(SystemMainHooker.systemServerCL, SysInnerHookInfo.hookItems);
        } else {
            super.startSystemServerHook();
        }
    }

    public void startWorkAroundHook() {
        if (useSandHook) {
            XposedCompat.addHookers(XposedBridge.BOOTCLASSLOADER, WorkAroundHookInfo.hookItems);
        } else {
            super.startWorkAroundHook();
        }
    }

    public void onEnterChildProcess() {
        SandHookXposedBridge.onForkPost();
        //enable compile in child process
        //SandHook.enableCompiler(!XposedInit.startsSystemServer);
    }

    public void injectConfig() {
        EdXpConfigGlobal.sConfig = new SandHookEdxpConfig();
        EdXpConfigGlobal.sHookProvider = new SandHookProvider();
    }

}
