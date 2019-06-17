package com.elderdrivers.riru.edxp.whale.config;

import com.elderdrivers.riru.edxp.art.ClassLinker;
import com.elderdrivers.riru.edxp.art.Heap;
import com.elderdrivers.riru.edxp.config.BaseHookProvider;
import com.elderdrivers.riru.edxp.core.ResourcesHook;
import com.elderdrivers.riru.edxp.core.Yahfa;
import com.elderdrivers.riru.edxp.util.Utils;
import com.lody.whale.WhaleRuntime;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import static com.elderdrivers.riru.edxp.util.ClassUtils.shouldDelayHook;

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
//        resolveStaticMethod(method);
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
        return WhaleRuntime.getMethodSlot(member);
    }

    @Override
    public boolean initXResourcesNative() {
        return ResourcesHook.initXResourcesNative();
    }

    @Override
    public boolean removeFinalFlagNative(Class clazz) {
        return ResourcesHook.removeFinalFlagNative(clazz);
    }


    /**
     * the static method is lazy resolved, when not resolved, the entry point is a trampoline of
     * a bridge, we can not hook these entry. this method force the static method to be resolved.
     */
    public static void resolveStaticMethod(Member method) {
        //ignore result, just call to trigger resolve
        if (method == null)
            return;
        try {
            if (method instanceof Method && Modifier.isStatic(method.getModifiers())) {
                ((Method) method).setAccessible(true);
                ((Method) method).invoke(new Object(), getFakeArgs((Method) method));
            }
        } catch (Exception ignored) {
            // we should never make a successful call.
        }
    }

    private static Object[] getFakeArgs(Method method) {
        return method.getParameterTypes().length == 0 ? new Object[]{new Object()} : null;
    }
}
