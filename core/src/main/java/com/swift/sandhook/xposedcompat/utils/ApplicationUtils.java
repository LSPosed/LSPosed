package com.swift.sandhook.xposedcompat.utils;

import android.app.Application;

import java.lang.reflect.Method;

public class ApplicationUtils {

    private static Class classActivityThread;
    private static Method currentApplicationMethod;

    static Application application;

    public static Application currentApplication() {
        if (application != null)
            return application;
        if (currentApplicationMethod == null) {
            try {
                classActivityThread = Class.forName("android.app.ActivityThread");
                currentApplicationMethod = classActivityThread.getDeclaredMethod("currentApplication");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (currentApplicationMethod == null)
            return null;
        try {
            application = (Application) currentApplicationMethod.invoke(null);
        } catch (Exception e) {
        }
        return application;
    }

}
