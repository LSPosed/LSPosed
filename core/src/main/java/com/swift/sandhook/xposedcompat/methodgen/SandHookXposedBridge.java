package com.swift.sandhook.xposedcompat.methodgen;

import android.os.Build;
import android.os.Process;
import android.os.Trace;

import io.github.lsposed.lspd.nativebridge.Yahfa;
import io.github.lsposed.lspd.util.ClassLoaderUtils;
import io.github.lsposed.lspd.util.FileUtils;
import com.swift.sandhook.SandHook;
import com.swift.sandhook.SandHookConfig;
import com.swift.sandhook.blacklist.HookBlackList;
import com.swift.sandhook.wrapper.HookWrapper;
import com.swift.sandhook.xposedcompat.XposedCompat;
import com.swift.sandhook.xposedcompat.hookstub.HookMethodEntity;
import com.swift.sandhook.xposedcompat.hookstub.HookStubManager;
import com.swift.sandhook.xposedcompat.utils.DexLog;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XposedBridge;

public final class SandHookXposedBridge {

    private static final Map<Member, Method> hookedInfo = new ConcurrentHashMap<>();
    private static HookMaker defaultHookMaker = XposedCompat.useNewCallBackup ? new HookerDexMakerNew() : new HookerDexMaker();

    public static Map<Member, HookMethodEntity> entityMap = new ConcurrentHashMap<>();

    public static void onForkPost() {
        XposedCompat.onForkProcess();
    }

    public static boolean hooked(Member member) {
        return hookedInfo.containsKey(member) || entityMap.containsKey(member);
    }

    public static synchronized void hookMethod(Member hookMethod, XposedBridge.AdditionalHookInfo additionalHookInfo) {

        if (!checkMember(hookMethod)) {
            return;
        }

        if (hookedInfo.containsKey(hookMethod) || entityMap.containsKey(hookMethod)) {
            DexLog.w("already hook method:" + hookMethod.toString());
            return;
        }

        Yahfa.recordHooked(hookMethod);  // in case static method got reset.

        try {
            Trace.beginSection("SandXposed");
            long timeStart = System.currentTimeMillis();
            HookMethodEntity stub = null;
            if (XposedCompat.useInternalStub && !HookBlackList.canNotHookByStub(hookMethod) && !HookBlackList.canNotHookByBridge(hookMethod)) {
                stub = HookStubManager.getHookMethodEntity(hookMethod, additionalHookInfo);
            }
            if (stub != null) {
                SandHook.hook(new HookWrapper.HookEntity(hookMethod, stub.hook, stub.backup, false));
                entityMap.put(hookMethod, stub);
            } else {
                HookMaker hookMaker;
                if (HookBlackList.canNotHookByBridge(hookMethod)) {
                    hookMaker = new HookerDexMaker();
                } else {
                    hookMaker = defaultHookMaker;
                }
                hookMaker.start(hookMethod, additionalHookInfo,
                        ClassLoaderUtils.createProxyClassLoader(
                                hookMethod.getDeclaringClass().getClassLoader()));
                hookedInfo.put(hookMethod, hookMaker.getCallBackupMethod());
            }
            DexLog.d("hook method <" + hookMethod.toString() + "> cost " + (System.currentTimeMillis() - timeStart) + " ms, by " + (stub != null ? "internal stub" : "dex maker"));
            Trace.endSection();
        } catch (Exception e) {
            DexLog.e("error occur when hook method <" + hookMethod.toString() + ">", e);
        }
    }

    public static void clearOatFile() {
        String fixedAppDataDir = XposedCompat.getCacheDir().getAbsolutePath();
        File dexOatDir = new File(fixedAppDataDir, "/hookers/oat/");
        if (!dexOatDir.exists())
            return;
        try {
            FileUtils.delete(dexOatDir);
            dexOatDir.mkdirs();
        } catch (Throwable throwable) {
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
            throws Throwable {
        return SandHook.callOriginMethod(method, thisObject, args);
    }

    public static void init() {
        SandHookConfig.libSandHookPath = "";
        SandHookConfig.libLoader = new SandHookConfig.LibLoader() {
            @Override
            public void loadLib() {
                //do it in loadDexAndInit
            }
        };
        SandHookConfig.DEBUG = true;
        SandHookConfig.compiler = false;
        //already impl in lspd
        SandHookConfig.delayHook = false;
        //use when call origin
        HookBlackList.methodBlackList.add("java.lang.reflect.isStatic");
        HookBlackList.methodBlackList.add("java.lang.reflect.Method.getModifiers");
        if (Build.VERSION.SDK_INT >= 29) {
            //unknown bug, disable tmp
            //TODO Fix
            XposedCompat.useInternalStub = false;
        }
        //in zygote disable compile
    }
}


