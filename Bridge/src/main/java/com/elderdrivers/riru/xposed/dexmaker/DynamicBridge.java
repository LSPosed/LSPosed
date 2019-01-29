package com.elderdrivers.riru.xposed.dexmaker;

import android.app.AndroidAppHelper;

import com.elderdrivers.riru.xposed.Main;
import com.elderdrivers.riru.xposed.util.FileUtils;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XposedBridge;

import static com.elderdrivers.riru.xposed.dexmaker.DexMakerUtils.shouldUseInMemoryHook;
import static com.elderdrivers.riru.xposed.util.FileUtils.getDataPathPrefix;

public final class DynamicBridge {

    private static final HashMap<Member, Method> hookedInfo = new HashMap<>();
    private static final HookerDexMaker dexMaker = new HookerDexMaker();
    private static final AtomicBoolean dexPathInited = new AtomicBoolean(false);
    private static final File dexDir;
    private static final File dexOptDir;

    static {
        // we always choose to use device encrypted storage data on android n and later
        // in case some app is installing hooks before phone is unlocked
        String fixedAppDataDir = getDataPathPrefix() + AndroidAppHelper.currentPackageName() + "/";
        dexDir = new File(fixedAppDataDir, "/cache/edhookers/"
                + Main.sAppProcessName.replace(":", "_") + "/");
        dexOptDir = new File(dexDir, "oat");
    }

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
            if (!shouldUseInMemoryHook()) {
                // under Android Oreo, using DexClassLoader
                if (dexPathInited.compareAndSet(false, true)) {
                    // delete previous compiled dex to prevent potential crashing
                    // TODO find a way to reuse them in consideration of performance
                    try {
                        dexDir.mkdirs();
                        DexLog.d(Main.sAppProcessName + " deleting dir: " + dexOptDir.getAbsolutePath());
                        FileUtils.delete(dexOptDir);
                    } catch (Throwable throwable) {
                    }
                }
            }
            dexMaker.start(hookMethod, additionalHookInfo,
                    hookMethod.getDeclaringClass().getClassLoader(), dexDir.getAbsolutePath());
            hookedInfo.put(hookMethod, dexMaker.getCallBackupMethod());
        } catch (Exception e) {
            DexLog.e("error occur when generating dex. dexDir=" + dexDir, e);
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
        Method callBackup = hookedInfo.get(method);
        if (callBackup == null) {
            throw new IllegalStateException("method not hooked, cannot call original method.");
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


