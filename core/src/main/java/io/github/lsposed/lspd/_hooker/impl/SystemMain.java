package io.github.lsposed.lspd._hooker.impl;

import io.github.lsposed.lspd.core.Main;
import io.github.lsposed.lspd.deopt.PrebuiltMethodsDeopter;
import io.github.lsposed.lspd.util.Hookers;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

// system_server initialization
// ed: only support sdk >= 21 for now
public class SystemMain extends XC_MethodHook {

    public static volatile ClassLoader systemServerCL;

    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        if (XposedBridge.disableHooks) {
            return;
        }
        Hookers.logD("ActivityThread#systemMain() starts");
        try {
            // get system_server classLoader
            systemServerCL = Thread.currentThread().getContextClassLoader();
            // deopt methods in SYSTEMSERVERCLASSPATH
            PrebuiltMethodsDeopter.deoptSystemServerMethods(systemServerCL);
            Main.getEdxpImpl().getRouter().startSystemServerHook();
        } catch (Throwable t) {
            Hookers.logE("error when hooking systemMain", t);
        }
    }

}
