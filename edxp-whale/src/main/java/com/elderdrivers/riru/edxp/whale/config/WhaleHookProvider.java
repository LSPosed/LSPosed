package com.elderdrivers.riru.edxp.whale.config;

import com.elderdrivers.riru.edxp.Main;
import com.elderdrivers.riru.edxp.config.BaseHookProvider;
import com.lody.whale.WhaleRuntime;

import java.lang.reflect.Member;
import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.XposedBridge;

public class WhaleHookProvider extends BaseHookProvider {

    private static final Map<Member, Long> sHookedMethodSlotMap = new HashMap<>();

    @Override
    public void unhookMethod(Member method) {
        synchronized (sHookedMethodSlotMap) {
            sHookedMethodSlotMap.remove(method);
        }
    }

    @Override
    public void hookMethod(Member method, XposedBridge.AdditionalHookInfo additionalInfo) {
        long slot = WhaleRuntime.hookMethodNative(method.getDeclaringClass(), method, additionalInfo);
        synchronized (sHookedMethodSlotMap) {
            sHookedMethodSlotMap.put(method, slot);
        }
    }

    @Override
    public Object invokeOriginalMethod(Member method, long methodId, Object thisObject, Object[] args) throws Throwable {
        long slot = sHookedMethodSlotMap.get(method);
        return WhaleRuntime.invokeOriginalMethodNative(slot, thisObject, args);
    }

    @Override
    public Member findMethodNative(Member hookMethod) {
        return hookMethod;
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
    public long getMethodId(Member member) {
        return WhaleRuntime.getMethodSlot(member);
    }

    @Override
    public boolean initXResourcesNative() {
        return Main.initXResourcesNative();
    }
}
