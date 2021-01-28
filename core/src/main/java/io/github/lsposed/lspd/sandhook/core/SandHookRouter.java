package io.github.lsposed.lspd.sandhook.core;

import io.github.lsposed.lspd.config.LSPdConfigGlobal;
import io.github.lsposed.lspd.proxy.BaseRouter;
import io.github.lsposed.lspd.sandhook.config.SandHookProvider;
import io.github.lsposed.lspd.sandhook.entry.AppBootstrapHookInfo;
import io.github.lsposed.lspd.sandhook.entry.SysBootstrapHookInfo;
import io.github.lsposed.lspd.sandhook.entry.SysInnerHookInfo;
import io.github.lsposed.lspd.sandhook.hooker.SystemMainHooker;
import io.github.lsposed.lspd.util.Utils;
import com.swift.sandhook.xposedcompat.XposedCompat;
import com.swift.sandhook.xposedcompat.methodgen.SandHookXposedBridge;

import de.robv.android.xposed.XposedBridge;

public class SandHookRouter extends BaseRouter {

    public SandHookRouter() {
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

    public void onEnterChildProcess() {
        SandHookXposedBridge.onForkPost();
        //enable compile in child process
        //SandHook.enableCompiler(!XposedInit.startsSystemServer);
    }

    public void injectConfig() {
        LSPdConfigGlobal.sHookProvider = new SandHookProvider();
    }

}
