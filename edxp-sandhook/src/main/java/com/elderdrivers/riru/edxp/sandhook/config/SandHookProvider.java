package com.elderdrivers.riru.edxp.sandhook.config;

import com.elderdrivers.riru.edxp.hook.HookProvider;
import com.elderdrivers.riru.edxp.sandhook.dexmaker.DexMakerUtils;
import com.elderdrivers.riru.edxp.sandhook.dexmaker.DynamicBridge;
import com.elderdrivers.riru.edxp.sandhook.util.PrebuiltMethodsDeopter;
import com.swift.sandhook.xposedcompat.XposedCompat;
import com.swift.sandhook.xposedcompat.methodgen.SandHookXposedBridge;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;

import de.robv.android.xposed.XposedBridge;

public class SandHookProvider implements HookProvider {
    @Override
    public void hookMethod(Member method, XposedBridge.AdditionalHookInfo additionalInfo) {
        XposedCompat.hookMethod(method, additionalInfo);
    }

    @Override
    public Object invokeOriginalMethod(Member method, Object thisObject, Object[] args) throws Throwable {
        return SandHookXposedBridge.invokeOriginalMethod(method, thisObject, args);
    }

    @Override
    public Member findMethodNative(Member hookMethod) {
        return DexMakerUtils.findMethodNative(hookMethod);
    }

    @Override
    public void deoptMethods(String packageName, ClassLoader classLoader) {
        PrebuiltMethodsDeopter.deoptMethods(packageName, classLoader);
    }
}
