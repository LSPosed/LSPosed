package com.elderdrivers.riru.edxp.sandhook.config;

import android.util.Log;

import com.elderdrivers.riru.edxp.art.ClassLinker;
import com.elderdrivers.riru.edxp.config.BaseHookProvider;
import com.elderdrivers.riru.edxp.core.ResourcesHook;
import com.elderdrivers.riru.edxp.core.Yahfa;
import com.swift.sandhook.xposedcompat.XposedCompat;
import com.swift.sandhook.xposedcompat.methodgen.SandHookXposedBridge;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import de.robv.android.xposed.XposedBridge;

import static com.elderdrivers.riru.edxp.util.ClassUtils.shouldDelayHook;

public class SandHookProvider extends BaseHookProvider {
    @Override
    public void hookMethod(Member method, XposedBridge.AdditionalHookInfo additionalInfo) {
        if (methodHooked(method)) {
            return;
        }
        if (method.getDeclaringClass() == Log.class) {
            Log.e(XposedBridge.TAG, "some one hook Log!");
            return;
        }
        XposedCompat.hookMethod(method, additionalInfo);
    }

    @Override
    public Object invokeOriginalMethod(Member method, long methodId, Object thisObject, Object[] args) throws Throwable {
        if (SandHookXposedBridge.hooked(method)) {
            try {
                return SandHookXposedBridge.invokeOriginalMethod(method, thisObject, args);
            } catch (Throwable throwable) {
                throw new InvocationTargetException(throwable);
            }
        } else if (super.methodHooked(method)){
            return super.invokeOriginalMethod(method, methodId, thisObject, args);
        } else {
            if (method instanceof Constructor) {
                return ((Constructor) method).newInstance(args);
            } else {
                return ((Method) method).invoke(thisObject, args);
            }
        }
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
    public long getMethodId(Member member) {
        return 0;
    }

    @Override
    public boolean initXResourcesNative() {
        return ResourcesHook.initXResourcesNative();
    }

    @Override
    public boolean removeFinalFlagNative(Class clazz) {
        return ResourcesHook.removeFinalFlagNative(clazz);
    }

    @Override
    public boolean methodHooked(Member target) {
        return SandHookXposedBridge.hooked(target) || super.methodHooked(target);
    }
}
