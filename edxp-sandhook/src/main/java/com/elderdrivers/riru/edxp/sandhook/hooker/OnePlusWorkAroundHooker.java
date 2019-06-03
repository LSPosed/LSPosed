package com.elderdrivers.riru.edxp.sandhook.hooker;

import com.elderdrivers.riru.common.KeepMembers;
import com.elderdrivers.riru.edxp._hooker.impl.OneplusWorkaround;
import com.elderdrivers.riru.edxp.core.yahfa.HookMain;
import com.swift.sandhook.SandHook;
import com.swift.sandhook.annotation.HookClass;
import com.swift.sandhook.annotation.HookMethod;
import com.swift.sandhook.annotation.HookMethodBackup;
import com.swift.sandhook.annotation.SkipParamCheck;

import java.lang.reflect.Method;

import dalvik.system.BaseDexClassLoader;
import de.robv.android.xposed.XC_MethodHook;

@HookClass(BaseDexClassLoader.class)
public class OnePlusWorkAroundHooker implements KeepMembers {

    static {
        HookMain.addHookItemWhiteList(OnePlusWorkAroundHooker.class.getName());
    }

    public static String className = "dalvik.system.BaseDexClassLoader";
    public static String methodName = "inCompatConfigList";
    public static String methodSig = "(ILjava/lang/String;)Z";

    @HookMethodBackup("inCompatConfigList")
    @SkipParamCheck
    static Method backup;

    @HookMethod("inCompatConfigList")
    public static boolean hook(int type, String packageName) throws Throwable {
        final XC_MethodHook methodHook = new OneplusWorkaround();
        final XC_MethodHook.MethodHookParam param = new XC_MethodHook.MethodHookParam();
        param.thisObject = null;
        param.args = new Object[]{type, packageName};
        methodHook.callBeforeHookedMethod(param);
        if (!param.returnEarly) {
            param.setResult(backup(type, packageName));
        }
        methodHook.callAfterHookedMethod(param);
        return (boolean) param.getResult();
    }

    public static boolean backup(int type, String packageName) throws Throwable {
        return (boolean) SandHook.callOriginByBackup(backup, null, type, packageName);
    }
}