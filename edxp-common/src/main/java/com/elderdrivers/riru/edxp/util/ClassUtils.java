package com.elderdrivers.riru.edxp.util;

import android.os.Build;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;

import de.robv.android.xposed.XposedHelpers;

public class ClassUtils {

    public static int getClassStatus(Class clazz, boolean isUnsigned) {
        if (clazz == null) {
            return 0;
        }
        int status = XposedHelpers.getIntField(clazz, "status");
        if (isUnsigned) {
            status = (int) (Integer.toUnsignedLong(status) >> (32 - 4));
        }
        return status;
    }


    /**
     * 5.0-8.0: kInitialized = 10 int
     * 8.1:     kInitialized = 11 int
     * 9.0:     kInitialized = 14 uint8_t
     */
    public static boolean isInitialized(Class clazz) {
        if (Build.VERSION.SDK_INT >= 28) {
            return getClassStatus(clazz, true) == 14;
        } else if (Build.VERSION.SDK_INT == 27) {
            return getClassStatus(clazz, false) == 11;
        } else {
            return getClassStatus(clazz, false) == 10;
        }
    }

    public static boolean shouldDelayHook(Member hookMethod) {
        if (hookMethod == null || hookMethod instanceof Constructor) {
            return false;
        }
        Class declaringClass = hookMethod.getDeclaringClass();
        return Modifier.isStatic(hookMethod.getModifiers())
                && !ClassUtils.isInitialized(declaringClass);
    }

}
