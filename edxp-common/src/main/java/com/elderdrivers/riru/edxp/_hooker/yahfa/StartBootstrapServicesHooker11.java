package com.elderdrivers.riru.edxp._hooker.yahfa;

import com.elderdrivers.riru.common.KeepMembers;
import com.elderdrivers.riru.edxp._hooker.impl.StartBootstrapServices;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.annotation.ApiSensitive;
import de.robv.android.xposed.annotation.Level;

@ApiSensitive(Level.LOW)
public class StartBootstrapServicesHooker11 implements KeepMembers {
    public static String className = "com.android.server.SystemServer";
    public static String methodName = "startBootstrapServices";
    public static String methodSig = "(Lcom/android/server/utils/TimingsTraceAndSlog;)V";

    public static void hook(Object systemServer, Object traceAndSlog) throws Throwable {
        final XC_MethodHook methodHook = new StartBootstrapServices();
        final XC_MethodHook.MethodHookParam param = new XC_MethodHook.MethodHookParam();
        param.thisObject = systemServer;
        param.args = new Object[]{traceAndSlog};
        methodHook.callBeforeHookedMethod(param);
        if (!param.returnEarly) {
            backup(systemServer, traceAndSlog);
        }
        methodHook.callAfterHookedMethod(param);
    }

    public static void backup(Object systemServer, Object traceAndSlog) {

    }
}
