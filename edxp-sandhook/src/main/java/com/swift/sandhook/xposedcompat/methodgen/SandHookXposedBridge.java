package com.swift.sandhook.xposedcompat.methodgen;

import android.os.Process;
import android.os.Trace;

import com.elderdrivers.riru.edxp.Main;
import com.swift.sandhook.SandHook;
import com.swift.sandhook.SandHookConfig;
import com.swift.sandhook.wrapper.HookWrapper;
import com.swift.sandhook.xposedcompat.XposedCompat;
import com.swift.sandhook.xposedcompat.hookstub.HookMethodEntity;
import com.swift.sandhook.xposedcompat.hookstub.HookStubManager;
import com.swift.sandhook.xposedcompat.utils.DexLog;
import com.swift.sandhook.xposedcompat.utils.FileUtils;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XposedBridge;

import static com.elderdrivers.riru.edxp.util.FileUtils.getDataPathPrefix;
import static com.elderdrivers.riru.edxp.util.FileUtils.getPackageName;
import static com.elderdrivers.riru.edxp.util.ProcessUtils.getCurrentProcessName;

public final class SandHookXposedBridge {

    private static final HashMap<Member, Method> hookedInfo = new HashMap<>();
    private static HookMaker hookMaker = XposedCompat.useNewDexMaker ? new HookerDexMakerNew() : new HookerDexMaker();
    private static final AtomicBoolean dexPathInited = new AtomicBoolean(false);
    private static File dexDir;

    public static Map<Member, HookMethodEntity> entityMap = new HashMap<>();

    public static void onForkPost() {
        dexPathInited.set(false);
    }

    public static synchronized void hookMethod(Member hookMethod, XposedBridge.AdditionalHookInfo additionalHookInfo) {

        if (!checkMember(hookMethod)) {
            return;
        }

        if (hookedInfo.containsKey(hookMethod) || entityMap.containsKey(hookMethod)) {
            DexLog.w("already hook method:" + hookMethod.toString());
            return;
        }

        try {
            setupDexCachePath();
            Trace.beginSection("SandHook-Xposed");
            long timeStart = System.currentTimeMillis();
            HookMethodEntity stub = null;
            if (XposedCompat.useInternalStub) {
                stub = HookStubManager.getHookMethodEntity(hookMethod);
            }
            if (stub != null) {
                SandHook.hook(new HookWrapper.HookEntity(hookMethod, stub.hook, stub.backup, false));
                entityMap.put(hookMethod, stub);
            } else {
                hookMaker.start(hookMethod, additionalHookInfo,
                        null, dexDir == null ? null : dexDir.getAbsolutePath());
                hookedInfo.put(hookMethod, hookMaker.getCallBackupMethod());
            }
            DexLog.d("hook method <" + hookMethod.toString() + "> cost " + (System.currentTimeMillis() - timeStart) + " ms, by " + (stub != null ? "internal stub." : "dex maker"));
            Trace.endSection();
        } catch (Exception e) {
            DexLog.e("error occur when hook method <" + hookMethod.toString() + ">", e);
        }
    }

    private static void setupDexCachePath() {
        // using file based DexClassLoader
        if (!dexPathInited.compareAndSet(false, true)) {
            return;
        }
        try {
            // we always choose to use device encrypted storage data on android N and later
            // in case some app is installing hooks before phone is unlocked
            String fixedAppDataDir = getDataPathPrefix() + getPackageName(Main.appDataDir) + "/";
            dexDir = new File(fixedAppDataDir, "/cache/sandxposed/"
                    + getCurrentProcessName(Main.appProcessName).replace(":", "_") + "/");
            dexDir.mkdirs();
        } catch (Throwable throwable) {
            com.elderdrivers.riru.edxp.sandhook.dexmaker.DexLog.e("error when init dex path", throwable);
        }
    }

//    public static void clearOatFile() {
//        String fixedAppDataDir = XposedCompat.cacheDir.getAbsolutePath();
//        File dexOatDir = new File(fixedAppDataDir, "/sandxposed/oat/");
//        if (!dexOatDir.exists())
//            return;
//        try {
//            FileUtils.delete(dexOatDir);
//            dexOatDir.mkdirs();
//        } catch (Throwable throwable) {
//        }
//    }

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
            throws Throwable {
        Method callBackup = hookedInfo.get(method);
        if (callBackup == null) {
            //method hook use internal stub
            return SandHook.callOriginMethod(method, thisObject, args);
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

    public static void setLibPath() {
        if (Process.is64Bit()) {
            SandHookConfig.libSandHookPath = "/system/lib64/libsandhook.edxp.so";
        } else {
            SandHookConfig.libSandHookPath = "/system/lib/libsandhook.edxp.so";
        }
    }
}


