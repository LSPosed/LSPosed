package com.elderdrivers.riru.edxp._hooker.impl;

import com.elderdrivers.riru.edxp.core.Main;
import com.elderdrivers.riru.edxp.deopt.PrebuiltMethodsDeopter;
import com.elderdrivers.riru.edxp.util.Hookers;

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
