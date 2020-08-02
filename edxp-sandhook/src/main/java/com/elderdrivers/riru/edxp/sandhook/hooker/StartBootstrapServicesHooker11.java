package com.elderdrivers.riru.edxp.sandhook.hooker;

import com.elderdrivers.riru.common.KeepMembers;
import com.elderdrivers.riru.edxp._hooker.impl.StartBootstrapServices;
import com.swift.sandhook.SandHook;
import com.swift.sandhook.annotation.HookMethod;
import com.swift.sandhook.annotation.HookMethodBackup;
import com.swift.sandhook.annotation.HookReflectClass;
import com.swift.sandhook.annotation.Param;
import com.swift.sandhook.annotation.SkipParamCheck;
import com.swift.sandhook.annotation.ThisObject;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.annotation.ApiSensitive;
import de.robv.android.xposed.annotation.Level;

@ApiSensitive(Level.LOW)
@HookReflectClass("com.android.server.SystemServer")
public class StartBootstrapServicesHooker11 implements KeepMembers {
    public static String className = "com.android.server.SystemServer";
    public static String methodName = "startBootstrapServices";
    public static String methodSig = "(Lcom/android/server/utils/TimingsTraceAndSlog;)V";

    @HookMethodBackup("startBootstrapServices")
    @SkipParamCheck
    static Method backup;

    @HookMethod("startBootstrapServices")
    public static void hook(@ThisObject Object systemServer, @Param("com.android.server.utils.TimingsTraceAndSlog") Object traceAndSlog) throws Throwable {
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

    public static void backup(Object systemServer, Object traceAndSlog) throws Throwable {
        SandHook.callOriginByBackup(backup, systemServer, traceAndSlog);
    }
}
