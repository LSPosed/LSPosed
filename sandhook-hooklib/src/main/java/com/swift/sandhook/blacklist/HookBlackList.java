package com.swift.sandhook.blacklist;

import com.swift.sandhook.SandHookConfig;

import java.lang.reflect.Member;
import java.util.HashSet;
import java.util.Set;

public class HookBlackList {

    public static Set<String> methodBlackList = new HashSet<>();
    public static Set<Class> classBlackList = new HashSet<>();

    public static Set<String> methodUseInHookBridge = new HashSet<>();
    public static Set<String> methodUseInHookStub = new HashSet<>();

    static {
        methodBlackList.add("java.lang.reflect.Method.invoke");
        methodBlackList.add("java.lang.reflect.AccessibleObject.setAccessible");

        methodUseInHookBridge.add("java.lang.Class.getDeclaredField");
        methodUseInHookBridge.add("java.lang.reflect.InvocationTargetException.getCause");

        methodUseInHookStub.add("java.lang.Object.equals");
        methodUseInHookStub.add("java.lang.Class.isPrimitive");
    }

    public final static boolean canNotHook(Member origin) {
        if (classBlackList.contains(origin.getDeclaringClass()))
            return true;
        String name = origin.getDeclaringClass().getName() + "." + origin.getName();
        return methodBlackList.contains(name);
    }

    public final static boolean canNotHookByBridge(Member origin) {
        String name = origin.getDeclaringClass().getName() + "." + origin.getName();
        return methodUseInHookBridge.contains(name);
    }

    public final static boolean canNotHookByStub(Member origin) {
        if (SandHookConfig.SDK_INT >= 29 && Thread.class.equals(origin.getDeclaringClass())) {
            return true;
        }
        String name = origin.getDeclaringClass().getName() + "." + origin.getName();
        return methodUseInHookStub.contains(name);
    }

}
