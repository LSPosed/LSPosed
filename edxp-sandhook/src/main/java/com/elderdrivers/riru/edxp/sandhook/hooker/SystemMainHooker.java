package com.elderdrivers.riru.edxp.sandhook.hooker;

import android.app.ActivityThread;

import com.elderdrivers.riru.common.KeepMembers;
import com.elderdrivers.riru.edxp._hooker.impl.SystemMain;
import com.swift.sandhook.SandHook;
import com.swift.sandhook.annotation.HookClass;
import com.swift.sandhook.annotation.HookMethod;
import com.swift.sandhook.annotation.HookMethodBackup;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.annotation.ApiSensitive;
import de.robv.android.xposed.annotation.Level;


@ApiSensitive(Level.LOW)
// system_server initialization
// ed: only support sdk >= 21 for now
@HookClass(ActivityThread.class)
public class SystemMainHooker implements KeepMembers {

    public static String className = "android.app.ActivityThread";
    public static String methodName = "systemMain";
    public static String methodSig = "()Landroid/app/ActivityThread;";

    public static ClassLoader systemServerCL;

    @HookMethodBackup("systemMain")
    static Method backup;

    @HookMethod("systemMain")
    public static ActivityThread hook() throws Throwable {
        final XC_MethodHook methodHook = new SystemMain();
        final XC_MethodHook.MethodHookParam param = new XC_MethodHook.MethodHookParam();
        param.thisObject = null;
        param.args = new Object[]{};
        methodHook.callBeforeHookedMethod(param);
        if (!param.returnEarly) {
            param.setResult(backup());
        }
        methodHook.callAfterHookedMethod(param);
        return (ActivityThread) param.getResult();
    }

    public static ActivityThread backup() throws Throwable {
        return (ActivityThread) SandHook.callOriginByBackup(backup, null);
    }
}