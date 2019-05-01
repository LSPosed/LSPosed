package com.elderdrivers.riru.edxp.yahfa.config;

import com.elderdrivers.riru.edxp.Main;
import com.elderdrivers.riru.edxp.config.BaseHookProvider;
import com.elderdrivers.riru.edxp.yahfa.dexmaker.DexMakerUtils;
import com.elderdrivers.riru.edxp.yahfa.dexmaker.DynamicBridge;

import java.lang.reflect.Member;

import de.robv.android.xposed.XposedBridge;

public class YahfaHookProvider extends BaseHookProvider {

    @Override
    public void hookMethod(Member method, XposedBridge.AdditionalHookInfo additionalInfo) {
        DynamicBridge.hookMethod(method, additionalInfo);
    }

    @Override
    public Object invokeOriginalMethod(Member method, long methodId, Object thisObject, Object[] args) throws Throwable {
        return DynamicBridge.invokeOriginalMethod(method, thisObject, args);
    }

    @Override
    public Member findMethodNative(Member hookMethod) {
        return DexMakerUtils.findMethodNative(hookMethod);
    }

    @Override
    public Object findMethodNative(Class clazz, String methodName, String methodSig) {
        return Main.findMethodNative(clazz, methodName, methodSig);
    }

    @Override
    public void deoptMethodNative(Object method) {
        Main.deoptMethodNative(method);
    }

    @Override
    public boolean initXResourcesNative() {
        return Main.initXResourcesNative();
    }

    @Override
    public boolean removeFinalFlagNative(Class clazz) {
        return Main.removeFinalFlagNative(clazz);
    }
}
