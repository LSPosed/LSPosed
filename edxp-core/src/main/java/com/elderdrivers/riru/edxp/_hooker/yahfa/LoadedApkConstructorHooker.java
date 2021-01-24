package com.elderdrivers.riru.edxp._hooker.yahfa;

import android.app.ActivityThread;
import android.content.pm.ApplicationInfo;
import android.content.res.CompatibilityInfo;

import com.elderdrivers.riru.common.KeepMembers;
import com.elderdrivers.riru.edxp._hooker.impl.LoadedApkCstr;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.annotation.ApiSensitive;
import de.robv.android.xposed.annotation.Level;

@ApiSensitive(Level.LOW)
public class LoadedApkConstructorHooker implements KeepMembers {
    public static String className = "android.app.LoadedApk";
    public static String methodName = "<init>";
    public static String methodSig = "(Landroid/app/ActivityThread;" +
            "Landroid/content/pm/ApplicationInfo;" +
            "Landroid/content/res/CompatibilityInfo;" +
            "Ljava/lang/ClassLoader;ZZZ)V";

    public static void hook(Object thiz, ActivityThread activityThread,
                            ApplicationInfo aInfo, CompatibilityInfo compatInfo,
                            ClassLoader baseLoader, boolean securityViolation,
                            boolean includeCode, boolean registerPackage) throws Throwable {

        final XC_MethodHook methodHook = new LoadedApkCstr();
        final XC_MethodHook.MethodHookParam param = new XC_MethodHook.MethodHookParam();
        param.thisObject = thiz;
        param.args = new Object[]{activityThread, aInfo, compatInfo, baseLoader, securityViolation,
                includeCode, registerPackage};
        methodHook.callBeforeHookedMethod(param);
        if (!param.returnEarly) {
            backup(thiz, activityThread, aInfo, compatInfo, baseLoader, securityViolation,
                    includeCode, registerPackage);
        }
        methodHook.callAfterHookedMethod(param);
    }

    public static void backup(Object thiz, ActivityThread activityThread,
                              ApplicationInfo aInfo, CompatibilityInfo compatInfo,
                              ClassLoader baseLoader, boolean securityViolation,
                              boolean includeCode, boolean registerPackage) {

    }
}