package com.elderdrivers.riru.edxp.yahfa.config;

import com.elderdrivers.riru.edxp.hook.HookProvider;
import com.elderdrivers.riru.edxp.yahfa.dexmaker.DexMakerUtils;
import com.elderdrivers.riru.edxp.yahfa.dexmaker.DynamicBridge;
import com.elderdrivers.riru.edxp.yahfa.util.PrebuiltMethodsDeopter;

import java.lang.reflect.Member;

import de.robv.android.xposed.XposedBridge;

public class YahfaHookProvider implements HookProvider {
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
    public void deoptMethods(String packageName, ClassLoader classLoader) {
        PrebuiltMethodsDeopter.deoptMethods(packageName, classLoader);
    }

    @Override
    public long getMethodId(Member member) {
        return 0;
    }
}
