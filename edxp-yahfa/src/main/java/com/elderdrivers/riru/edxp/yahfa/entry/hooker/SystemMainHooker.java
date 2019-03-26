package com.elderdrivers.riru.edxp.yahfa.entry.hooker;

import android.app.ActivityThread;

import com.elderdrivers.riru.common.KeepMembers;
import com.elderdrivers.riru.edxp.deopt.PrebuiltMethodsDeopter;
import com.elderdrivers.riru.edxp.yahfa.entry.Router;

import de.robv.android.xposed.XposedBridge;


// system_server initialization
// ed: only support sdk >= 21 for now
public class SystemMainHooker implements KeepMembers {

    public static String className = "android.app.ActivityThread";
    public static String methodName = "systemMain";
    public static String methodSig = "()Landroid/app/ActivityThread;";

    public static ClassLoader systemServerCL;

    public static ActivityThread hook() {
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

    public static ActivityThread backup() {
        return null;
    }
}