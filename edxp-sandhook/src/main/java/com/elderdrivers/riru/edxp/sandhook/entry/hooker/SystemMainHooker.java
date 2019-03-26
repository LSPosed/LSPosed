package com.elderdrivers.riru.edxp.sandhook.entry.hooker;

import android.app.ActivityThread;

import com.elderdrivers.riru.common.KeepMembers;
import com.elderdrivers.riru.edxp.deopt.PrebuiltMethodsDeopter;
import com.elderdrivers.riru.edxp.sandhook.entry.Router;
import com.swift.sandhook.SandHook;
import com.swift.sandhook.annotation.HookClass;
import com.swift.sandhook.annotation.HookMethod;
import com.swift.sandhook.annotation.HookMethodBackup;

import java.lang.reflect.Method;

import de.robv.android.xposed.XposedBridge;


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
        if (XposedBridge.disableHooks) {
            return backup();
        }
        Router.logD("ActivityThread#systemMain() starts");
        ActivityThread activityThread = backup();
        try {
            // get system_server classLoader
            systemServerCL = Thread.currentThread().getContextClassLoader();
            // deopt methods in SYSTEMSERVERCLASSPATH
            PrebuiltMethodsDeopter.deoptSystemServerMethods(systemServerCL);
            Router.startSystemServerHook();
        } catch (Throwable t) {
            Router.logE("error when hooking systemMain", t);
        }
        return activityThread;
    }

    public static ActivityThread backup() throws Throwable {
        return (ActivityThread) SandHook.callOriginByBackup(backup, null);
    }
}