package com.elderdrivers.riru.edxp.yahfa.dexmaker;


import com.elderdrivers.riru.edxp.config.ConfigManager;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XposedBridge;

import static com.elderdrivers.riru.edxp.util.FileUtils.getDataPathPrefix;
import static com.elderdrivers.riru.edxp.util.FileUtils.getPackageName;
import static com.elderdrivers.riru.edxp.util.ProcessUtils.getCurrentProcessName;
import static com.elderdrivers.riru.edxp.yahfa.dexmaker.DexMakerUtils.shouldUseInMemoryHook;

public final class DynamicBridge {

    private static final HashMap<Member, Method> hookedInfo = new HashMap<>();
    private static final HookerDexMaker dexMaker = new HookerDexMaker();
    private static final AtomicBoolean dexPathInited = new AtomicBoolean(false);
    private static File dexDir;
    private static File dexOptDir;

    /**
     * Reset dexPathInited flag once we enter child process
     * since it might have been set to true in zygote process
     */
    public static void onForkPost() {
        dexPathInited.set(false);
    }

    public static synchronized void hookMethod(Member hookMethod, XposedBridge.AdditionalHookInfo additionalHookInfo) {
        DexLog.d("hooking " + hookMethod);
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
                setupDexCachePath();
            }
            dexMaker.start(hookMethod, additionalHookInfo,
                    hookMethod.getDeclaringClass().getClassLoader(), getDexDirPath());
            hookedInfo.put(hookMethod, dexMaker.getCallBackupMethod());
        } catch (Exception e) {
            DexLog.e("error occur when generating dex. dexDir=" + dexDir, e);
        }
    }

    private static String getDexDirPath() {
        if (dexDir == null) {
            return null;
        }
        return dexDir.getAbsolutePath();
    }

    private static void setupDexCachePath() {
        // using file based DexClassLoader
        if (!dexPathInited.compareAndSet(false, true)) {
            return;
        }
        try {
            // we always choose to use device encrypted storage data on android N and later
            // in case some app is installing hooks before phone is unlocked
            String fixedAppDataDir = getDataPathPrefix() + getPackageName(ConfigManager.appDataDir) + "/";
            dexDir = new File(fixedAppDataDir, "/cache/edhookers/"
                    + getCurrentProcessName(ConfigManager.appProcessName).replace(":", "_") + "/");
            dexOptDir = new File(dexDir, "oat");
            dexDir.mkdirs();
            DexLog.d(ConfigManager.appProcessName + " deleting dir: " + dexOptDir.getAbsolutePath());
        } catch (Throwable throwable) {
            DexLog.e("error when init dex path", throwable);
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


