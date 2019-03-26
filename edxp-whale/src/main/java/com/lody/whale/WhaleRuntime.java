package com.lody.whale;

import android.os.Build;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import de.robv.android.xposed.XposedBridge;

/**
 * @author Lody
 * <p>
 * NOTICE: Do not move or rename any methods in this class.
 */
public class WhaleRuntime {

    static {
        System.loadLibrary("whale.edxp");
    }

    private static String getShorty(Member member) {
        return VMHelper.getShorty(member);
    }

    public static long[] countInstancesOfClasses(Class[] classes, boolean assignable) {
        if (Build.VERSION.SDK_INT < 27) {
            throw new UnsupportedOperationException("Not support countInstancesOfClasses on your device yet.");
        }
        try {
            Class<?> clazz = Class.forName("dalvik.system.VMDebug");
            Method method = clazz.getDeclaredMethod("countInstancesOfClasses", Class[].class, boolean.class);
            return (long[]) method.invoke(null, classes, assignable);
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    public static Object[][] getInstancesOfClasses(Class[] classes, boolean assignable) {
        if (Build.VERSION.SDK_INT < 28) {
            throw new UnsupportedOperationException("Not support getInstancesOfClasses on your device yet.");
        }
        try {
            Class<?> clazz = Class.forName("dalvik.system.VMDebug");
            Method method = clazz.getDeclaredMethod("getInstancesOfClasses", Class[].class, boolean.class);
            return (Object[][]) method.invoke(null, classes, assignable);
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    public static Object handleHookedMethod(Member member, long slot, Object additionInfo, Object thisObject, Object[] args) throws Throwable {
        return XposedBridge.handleHookedMethod(member, slot, additionInfo, thisObject, args);
    }

    public static native Object invokeOriginalMethodNative(long slot, Object thisObject, Object[] args)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException;

    public static native long getMethodSlot(Member member) throws IllegalArgumentException;

    public static native long hookMethodNative(Class<?> declClass, Member method, Object additionInfo);

    public static native void setObjectClassNative(Object object, Class<?> parent);

    public static native Object cloneToSubclassNative(Object object, Class<?> subClass);

    public static native void removeFinalFlagNative(Class<?> cl);

    public static native void enforceDisableHiddenAPIPolicy();

    private static native void reserved0();

    private static native void reserved1();
}
