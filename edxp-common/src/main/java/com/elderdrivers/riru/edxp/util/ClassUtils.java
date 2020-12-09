package com.elderdrivers.riru.edxp.util;

import android.os.Build;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;

import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.annotation.ApiSensitive;
import de.robv.android.xposed.annotation.Level;

public class ClassUtils {

    @ApiSensitive(Level.MIDDLE)
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
     * 9.0+:    kInitialized = 14 uint8_t
     * 11.0+:   kVisiblyInitialized = 15 uint8_t
     */
    @ApiSensitive(Level.MIDDLE)
    public static boolean isInitialized(Class clazz) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return getClassStatus(clazz, true) == 15;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return getClassStatus(clazz, true) == 14;
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O_MR1) {
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
