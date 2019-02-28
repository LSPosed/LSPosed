package com.elderdrivers.riru.xposed;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Process;

import com.elderdrivers.riru.common.KeepAll;
import com.elderdrivers.riru.xposed.core.HookMethodResolver;
import com.elderdrivers.riru.xposed.proxy.yahfa.BlackWhiteListProxy;
import com.elderdrivers.riru.xposed.proxy.yahfa.NormalProxy;
import com.elderdrivers.riru.xposed.util.Utils;

import java.lang.reflect.Method;
import java.util.Arrays;

@SuppressLint("DefaultLocale")
public class Main implements KeepAll {

    public static String appDataDir = "";
    public static String appProcessName = "";
    private static String forkAndSpecializePramsStr = "";
    private static String forkSystemServerPramsStr = "";

    static {
        init(Build.VERSION.SDK_INT);
        HookMethodResolver.init();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // entry points
    ///////////////////////////////////////////////////////////////////////////////////////////////

    public static void forkAndSpecializePre(int uid, int gid, int[] gids, int debugFlags,
                                            int[][] rlimits, int mountExternal, String seInfo,
                                            String niceName, int[] fdsToClose, int[] fdsToIgnore,
                                            boolean startChildZygote, String instructionSet,
                                            String appDataDir, boolean isBlackWhiteListMode,
                                            boolean isDynamicModulesMode) {
        if (BuildConfig.DEBUG) {
            forkAndSpecializePramsStr = String.format(
                    "Zygote#forkAndSpecialize(%d, %d, %s, %d, %s, %d, %s, %s, %s, %s, %s, %s, %s)",
                    uid, gid, Arrays.toString(gids), debugFlags, Arrays.toString(rlimits),
                    mountExternal, seInfo, niceName, Arrays.toString(fdsToClose),
                    Arrays.toString(fdsToIgnore), startChildZygote, instructionSet, appDataDir,
                    isDynamicModulesMode);
        }
        if (isBlackWhiteListMode) {
            BlackWhiteListProxy.forkAndSpecializePre(uid, gid, gids, debugFlags, rlimits,
                    mountExternal, seInfo, niceName, fdsToClose, fdsToIgnore, startChildZygote,
                    instructionSet, appDataDir, isDynamicModulesMode);
        } else {
            NormalProxy.forkAndSpecializePre(uid, gid, gids, debugFlags, rlimits, mountExternal,
                    seInfo, niceName, fdsToClose, fdsToIgnore, startChildZygote, instructionSet,
                    appDataDir, isDynamicModulesMode);
        }
    }

    public static void forkAndSpecializePost(int pid, String appDataDir,
                                             boolean isBlackWhiteListMode,
                                             boolean isDynamicModulesMode) {
        if (pid == 0) {
            Utils.logD(forkAndSpecializePramsStr + " = " + Process.myPid());
            if (isBlackWhiteListMode) {
                BlackWhiteListProxy.forkAndSpecializePost(pid, appDataDir, isDynamicModulesMode);
            } else {
                NormalProxy.forkAndSpecializePost(pid, appDataDir, isDynamicModulesMode);
            }
        } else {
            // in zygote process, res is child zygote pid
            // don't print log here, see https://github.com/RikkaApps/Riru/blob/77adfd6a4a6a81bfd20569c910bc4854f2f84f5e/riru-core/jni/main/jni_native_method.cpp#L55-L66
        }
    }

    public static void forkSystemServerPre(int uid, int gid, int[] gids, int debugFlags, int[][] rlimits,
                                           long permittedCapabilities, long effectiveCapabilities,
                                           boolean isBlackWhiteListMode, boolean isDynamicModulesMode) {
        if (BuildConfig.DEBUG) {
            forkSystemServerPramsStr = String.format("Zygote#forkSystemServer(%d, %d, %s, %d, %s, %d, %d)",
                    uid, gid, Arrays.toString(gids), debugFlags, Arrays.toString(rlimits),
                    permittedCapabilities, effectiveCapabilities);
        }
        if (isBlackWhiteListMode) {
            BlackWhiteListProxy.forkSystemServerPre(uid, gid, gids, debugFlags, rlimits,
                    permittedCapabilities, effectiveCapabilities, isDynamicModulesMode);
        } else {
            NormalProxy.forkSystemServerPre(uid, gid, gids, debugFlags, rlimits,
                    permittedCapabilities, effectiveCapabilities, isDynamicModulesMode);
        }
    }

    public static void forkSystemServerPost(int pid, boolean isBlackWhiteListMode,
                                            boolean isDynamicModulesMode) {
        if (pid == 0) {
            Utils.logD(forkSystemServerPramsStr + " = " + Process.myPid());
            if (isBlackWhiteListMode) {
                BlackWhiteListProxy.forkSystemServerPost(pid, isDynamicModulesMode);
            } else {
                NormalProxy.forkSystemServerPost(pid, isDynamicModulesMode);
            }
        } else {
            // in zygote process, res is child zygote pid
            // don't print log here, see https://github.com/RikkaApps/Riru/blob/77adfd6a4a6a81bfd20569c910bc4854f2f84f5e/riru-core/jni/main/jni_native_method.cpp#L55-L66
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // native methods
    ///////////////////////////////////////////////////////////////////////////////////////////////

    public static native boolean backupAndHookNative(Object target, Method hook, Method backup);

    public static native void ensureMethodCached(Method hook, Method backup);

    // JNI.ToReflectedMethod() could return either Method or Constructor
    public static native Object findMethodNative(Class targetClass, String methodName, String methodSig);

    private static native void init(int SDK_version);

    public static native String getInstallerPkgName();

    // prevent from fatal error caused by holding not whitelisted file descriptors when forking zygote
    // https://github.com/rovo89/Xposed/commit/b3ba245ad04cd485699fb1d2ebde7117e58214ff
    public static native void closeFilesBeforeForkNative();

    public static native void reopenFilesAfterForkNative();

    public static native void deoptMethodNative(Object object);

    public static native long suspendAllThreads();

    public static native void resumeAllThreads(long obj);
}
