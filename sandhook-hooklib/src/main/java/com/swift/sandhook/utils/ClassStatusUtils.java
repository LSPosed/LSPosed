package com.swift.sandhook.utils;

import com.swift.sandhook.SandHookConfig;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;

public class ClassStatusUtils {

    static Field fieldStatusOfClass;

    static {
        try {
            fieldStatusOfClass = Class.class.getDeclaredField("status");
            fieldStatusOfClass.setAccessible(true);
        } catch (NoSuchFieldException e) {
        }
    }

    public static int getClassStatus(Class clazz, boolean isUnsigned) {
        if (clazz == null) {
            return 0;
        }
        int status = 0;
        try {
            status = fieldStatusOfClass.getInt(clazz);
        } catch (Throwable e) {
        }
        if (isUnsigned) {
            status = (int) (toUnsignedLong(status) >> (32 - 4));
        }
        return status;
    }

    public static long toUnsignedLong(int x) {
        return ((long) x) & 0xffffffffL;
    }


    /**
     * 5.0-8.0: kInitialized = 10 int
     * 8.1:     kInitialized = 11 int
     * 9.0:     kInitialized = 14 uint8_t
     */
    public static boolean isInitialized(Class clazz) {
        if (fieldStatusOfClass == null)
            return true;
        if (SandHookConfig.SDK_INT >= 28) {
            return getClassStatus(clazz, true) == 14;
        } else if (SandHookConfig.SDK_INT == 27) {
            return getClassStatus(clazz, false) == 11;
        } else {
            return getClassStatus(clazz, false) == 10;
        }
    }

    public static boolean isStaticAndNoInited(Member hookMethod) {
        if (hookMethod == null || hookMethod instanceof Constructor) {
            return false;
        }
        Class declaringClass = hookMethod.getDeclaringClass();
        return Modifier.isStatic(hookMethod.getModifiers())
                && !ClassStatusUtils.isInitialized(declaringClass);
    }

}
