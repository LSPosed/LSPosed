package com.elderdrivers.riru.edxp.sandhook.hooker;

import android.app.ActivityThread;

import com.elderdrivers.riru.common.KeepMembers;
import com.elderdrivers.riru.edxp._hooker.impl.HandleBindApp;
import com.swift.sandhook.SandHook;
import com.swift.sandhook.annotation.HookClass;
import com.swift.sandhook.annotation.HookMethod;
import com.swift.sandhook.annotation.HookMethodBackup;
import com.swift.sandhook.annotation.Param;
import com.swift.sandhook.annotation.SkipParamCheck;
import com.swift.sandhook.annotation.ThisObject;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.annotation.ApiSensitive;
import de.robv.android.xposed.annotation.Level;

@ApiSensitive(Level.LOW)
@HookClass(ActivityThread.class)
public class HandleBindAppHooker implements KeepMembers {

    public static String className = "android.app.ActivityThread";
    public static String methodName = "handleBindApplication";
    public static String methodSig = "(Landroid/app/ActivityThread$AppBindData;)V";

    @HookMethodBackup("handleBindApplication")
    @SkipParamCheck
    static Method backup;

    @HookMethod("handleBindApplication")
    public static void hook(@ThisObject ActivityThread thiz, @Param("android.app.ActivityThread$AppBindData") Object bindData) throws Throwable {
        final XC_MethodHook methodHook = new HandleBindApp();
        final XC_MethodHook.MethodHookParam param = new XC_MethodHook.MethodHookParam();
        param.thisObject = thiz;
        param.args = new Object[]{bindData};
        methodHook.callBeforeHookedMethod(param);
        if (!param.returnEarly) {
            backup(thiz, bindData);
        }
        methodHook.callAfterHookedMethod(param);
    }

    public static void backup(Object thiz, Object bindData) throws Throwable {
        SandHook.callOriginByBackup(backup, thiz, bindData);
    }
}