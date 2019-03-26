package com.elderdrivers.riru.edxp.whale.config;

import com.elderdrivers.riru.edxp.config.BaseHookProvider;
import com.lody.whale.WhaleRuntime;

import java.lang.reflect.Member;

import de.robv.android.xposed.XposedBridge;

public class WhaleHookProvider extends BaseHookProvider {

    @Override
    public void hookMethod(Member method, XposedBridge.AdditionalHookInfo additionalInfo) {
        WhaleRuntime.hookMethodNative(method.getDeclaringClass(), method, additionalInfo);
    }

    @Override
    public Object invokeOriginalMethod(Member method, long methodId, Object thisObject, Object[] args) throws Throwable {
        return WhaleRuntime.invokeOriginalMethodNative(methodId, thisObject, args);
    }

    @Override
    public Member findMethodNative(Member hookMethod) {
        return hookMethod;
    }

    @Override
    public long getMethodId(Member member) {
        return WhaleRuntime.getMethodSlot(member);
    }
}
