package com.elderdrivers.riru.edxp.sandhook.config;

import android.util.Log;

import com.elderdrivers.riru.edxp.config.BaseHookProvider;
import com.elderdrivers.riru.edxp.deopt.PrebuiltMethodsDeopter;
import com.elderdrivers.riru.edxp.sandhook.dexmaker.DynamicBridge;
import com.swift.sandhook.xposedcompat.XposedCompat;
import com.swift.sandhook.xposedcompat.methodgen.SandHookXposedBridge;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;

import de.robv.android.xposed.XposedBridge;

public class SandHookProvider extends BaseHookProvider {
    @Override
    public void hookMethod(Member method, XposedBridge.AdditionalHookInfo additionalInfo) {
        if (SandHookXposedBridge.hooked(method) || DynamicBridge.hooked(method)) {
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
        } else {
            return DynamicBridge.invokeOriginalMethod(method, thisObject, args);
        }
    }

    @Override
    public Member findMethodNative(Member hookMethod) {
        return hookMethod;
    }

    @Override
    public void deoptMethods(String packageName, ClassLoader classLoader) {
        PrebuiltMethodsDeopter.deoptMethods(packageName, classLoader);
    }

    @Override
    public long getMethodId(Member member) {
        return 0;
    }
}
