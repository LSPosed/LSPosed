package com.elderdrivers.riru.edxp.yahfa.config;

import com.elderdrivers.riru.edxp.hook.HookProvider;
import com.elderdrivers.riru.edxp.util.PrebuiltMethodsDeopter;
import com.elderdrivers.riru.edxp.yahfa.dexmaker.DexMakerUtils;
import com.elderdrivers.riru.edxp.yahfa.dexmaker.DynamicBridge;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;

import de.robv.android.xposed.XposedBridge;

public class YahfaHookProvider implements HookProvider {
    @Override
    public void hookMethod(Member method, XposedBridge.AdditionalHookInfo additionalInfo) {
        DynamicBridge.hookMethod(method, additionalInfo);
    }

    @Override
    public Object invokeOriginalMethod(Member method, Object thisObject, Object[] args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        return DynamicBridge.invokeOriginalMethod(method, thisObject, args);
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
