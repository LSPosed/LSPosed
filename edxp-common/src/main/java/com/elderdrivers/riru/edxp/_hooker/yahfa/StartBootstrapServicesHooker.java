package com.elderdrivers.riru.edxp._hooker.yahfa;

import com.elderdrivers.riru.common.KeepMembers;
import com.elderdrivers.riru.edxp._hooker.impl.StartBootstrapServices;

import de.robv.android.xposed.XC_MethodHook;

public class StartBootstrapServicesHooker implements KeepMembers {
    public static String className = "com.android.server.SystemServer";
    public static String methodName = "startBootstrapServices";
    public static String methodSig = "()V";

    public static void hook(Object systemServer) throws Throwable {
        final XC_MethodHook methodHook = new StartBootstrapServices();
        final XC_MethodHook.MethodHookParam param = new XC_MethodHook.MethodHookParam();
        param.thisObject = systemServer;
        param.args = new Object[]{};
        methodHook.callBeforeHookedMethod(param);
        if (!param.returnEarly) {
            backup(systemServer);
        }
        methodHook.callAfterHookedMethod(param);
    }

    public static void backup(Object systemServer) {

    }
}
