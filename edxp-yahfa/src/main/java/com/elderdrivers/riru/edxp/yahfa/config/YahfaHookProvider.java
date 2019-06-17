package com.elderdrivers.riru.edxp.yahfa.config;

import com.elderdrivers.riru.edxp.art.ClassLinker;
import com.elderdrivers.riru.edxp.config.BaseHookProvider;
import com.elderdrivers.riru.edxp.core.ResourcesHook;
import com.elderdrivers.riru.edxp.core.Yahfa;
import com.elderdrivers.riru.edxp.yahfa.dexmaker.DynamicBridge;

import java.lang.reflect.Member;

import de.robv.android.xposed.XposedBridge;

import static com.elderdrivers.riru.edxp.util.ClassUtils.shouldDelayHook;

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
        return shouldDelayHook(hookMethod) ? null : hookMethod;
    }

    @Override
    public Object findMethodNative(Class clazz, String methodName, String methodSig) {
        return Yahfa.findMethodNative(clazz, methodName, methodSig);
    }

    @Override
    public void deoptMethodNative(Object method) {
        ClassLinker.setEntryPointsToInterpreter((Member) method);
    }

    @Override
    public boolean initXResourcesNative() {
        return ResourcesHook.initXResourcesNative();
    }

    @Override
    public boolean removeFinalFlagNative(Class clazz) {
        return ResourcesHook.removeFinalFlagNative(clazz);
    }
}
