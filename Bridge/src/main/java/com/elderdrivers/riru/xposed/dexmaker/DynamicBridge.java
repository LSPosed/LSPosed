package com.elderdrivers.riru.xposed.dexmaker;

import android.app.AndroidAppHelper;
import android.os.Build;

import com.elderdrivers.riru.xposed.Main;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;

import de.robv.android.xposed.XposedBridge;

public final class DynamicBridge {

    private static HashMap<Member, HookerDexMaker> hookedInfo = new HashMap<>();

    public static synchronized void hookMethod(Member hookMethod, XposedBridge.AdditionalHookInfo additionalHookInfo) {

        if (!checkMember(hookMethod)) {
            return;
        }

        if (hookedInfo.containsKey(hookMethod)) {
            DexLog.w("already hook method:" + hookMethod.toString());
            return;
        }

        DexLog.d("start to generate class for: " + hookMethod);
        try {
            // for Android Oreo and later use InMemoryClassLoader
            String dexDirPath = "";
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                // under Android Oreo, using DexClassLoader
                String dataDir = Main.sAppDataDir;
                String processName = AndroidAppHelper.currentProcessName();
                File dexDir = new File(dataDir, "cache/edhookers/" + processName + "/");
                dexDir.mkdirs();
                dexDirPath = dexDir.getAbsolutePath();
            }
            HookerDexMaker dexMaker = new HookerDexMaker();
            dexMaker.start(hookMethod, additionalHookInfo,
                    hookMethod.getDeclaringClass().getClassLoader(), dexDirPath);
            hookedInfo.put(hookMethod, dexMaker);
        } catch (Exception e) {
            DexLog.e("error occur when generating dex", e);
        }
    }

    private static boolean checkMember(Member member) {

        if (member instanceof Method) {
            return true;
        } else if (member instanceof Constructor<?>) {
            return true;
        } else if (member.getDeclaringClass().isInterface()) {
            DexLog.e("Cannot hook interfaces: " + member.toString());
            return false;
        } else if (Modifier.isAbstract(member.getModifiers())) {
            DexLog.e("Cannot hook abstract methods: " + member.toString());
            return false;
        } else {
            DexLog.e("Only methods and constructors can be hooked: " + member.toString());
            return false;
        }
    }

    public static Object invokeOriginalMethod(Member method, Object thisObject, Object[] args)
            throws InvocationTargetException, IllegalAccessException {
        HookerDexMaker dexMaker = hookedInfo.get(method);
        if (dexMaker == null) {
            throw new IllegalStateException("method not hooked, cannot call original method.");
        }
        Method callBackup = dexMaker.getCallBackupMethod();
        if (callBackup == null) {
            throw new IllegalStateException("original method is null, something must be wrong!");
        }
        if (!Modifier.isStatic(callBackup.getModifiers())) {
            throw new IllegalStateException("original method is not static, something must be wrong!");
        }
        callBackup.setAccessible(true);
        if (args == null) {
            args = new Object[0];
        }
        final int argsSize = args.length;
        if (Modifier.isStatic(method.getModifiers())) {
            return callBackup.invoke(null, args);
        } else {
            Object[] newArgs = new Object[argsSize + 1];
            newArgs[0] = thisObject;
            for (int i = 1; i < newArgs.length; i++) {
                newArgs[i] = args[i - 1];
            }
            return callBackup.invoke(null, newArgs);
        }
    }
}


