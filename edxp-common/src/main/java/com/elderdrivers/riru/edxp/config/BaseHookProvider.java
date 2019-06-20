package com.elderdrivers.riru.edxp.config;

import com.elderdrivers.riru.edxp.core.Yahfa;
import com.elderdrivers.riru.edxp.deopt.PrebuiltMethodsDeopter;
import com.elderdrivers.riru.edxp.hook.HookProvider;

import java.lang.reflect.Member;

public abstract class BaseHookProvider implements HookProvider {

    @Override
    public void unhookMethod(Member method) {

    }

    public Member findMethodNative(Member hookMethod) {
        return hookMethod;
    }

    public long getMethodId(Member member) {
        return 0;
    }

    public Object findMethodNative(Class clazz, String methodName, String methodSig) {
        return null;
    }

    public void deoptMethods(String packageName, ClassLoader classLoader) {
        PrebuiltMethodsDeopter.deoptMethods(packageName, classLoader);
    }

    @Override
    public void deoptMethodNative(Object method) {

    }

    @Override
    public boolean initXResourcesNative() {
        return false;
    }

    @Override
    public void setNativeFlag(Member hookMethod, boolean isNative) {
        Yahfa.setNativeFlag(hookMethod, isNative);
    }
}
