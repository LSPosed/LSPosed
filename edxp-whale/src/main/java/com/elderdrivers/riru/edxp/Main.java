package com.elderdrivers.riru.edxp;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Process;

import com.elderdrivers.riru.common.KeepAll;
import com.elderdrivers.riru.edxp.config.InstallerChooser;
import com.elderdrivers.riru.edxp.whale.core.HookMethodResolver;
import com.elderdrivers.riru.edxp.whale.entry.Router;
import com.elderdrivers.riru.edxp.whale.proxy.BlackWhiteListProxy;
import com.elderdrivers.riru.edxp.whale.proxy.NormalProxy;
import com.elderdrivers.riru.edxp.util.Utils;

import java.lang.reflect.Method;
import java.util.Arrays;

@SuppressLint("DefaultLocale")
public class Main implements KeepAll {

    public static String appDataDir = "";
    public static String niceName = "";
    public static String appProcessName = "";
    private static String forkAndSpecializePramsStr = "";
    private static String forkSystemServerPramsStr = "";

    static {
        init(Build.VERSION.SDK_INT);
        HookMethodResolver.init();
        Router.injectConfig();
        InstallerChooser.setInstallerPackageName(getInstallerPkgName());
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // entry points
    ///////////////////////////////////////////////////////////////////////////////////////////////

    public static void forkAndSpecializePre(int uid, int gid, int[] gids, int debugFlags,
                                            int[][] rlimits, int mountExternal, String seInfo,
                                            String niceName, int[] fdsToClose, int[] fdsToIgnore,
                                            boolean startChildZygote, String instructionSet,
                                            String appDataDir) {
        if (BuildConfig.DEBUG) {
            forkAndSpecializePramsStr = String.format(
                    "Zygote#forkAndSpecialize(%d, %d, %s, %d, %s, %d, %s, %s, %s, %s, %s, %s, %s)",
                    uid, gid, Arrays.toString(gids), debugFlags, Arrays.toString(rlimits),
                    mountExternal, seInfo, niceName, Arrays.toString(fdsToClose),
                    Arrays.toString(fdsToIgnore), startChildZygote, instructionSet, appDataDir);
        }
        if (isBlackWhiteListEnabled()) {
            BlackWhiteListProxy.forkAndSpecializePre(uid, gid, gids, debugFlags, rlimits,
                    mountExternal, seInfo, niceName, fdsToClose, fdsToIgnore, startChildZygote,
                    instructionSet, appDataDir);
        } else {
            NormalProxy.forkAndSpecializePre(uid, gid, gids, debugFlags, rlimits, mountExternal,
                    seInfo, niceName, fdsToClose, fdsToIgnore, startChildZygote, instructionSet,
                    appDataDir);
        }
    }

    public static void forkAndSpecializePost(int pid, String appDataDir, String niceName) {
        if (pid == 0) {
            Utils.logD(forkAndSpecializePramsStr + " = " + Process.myPid());
            if (isBlackWhiteListEnabled()) {
                BlackWhiteListProxy.forkAndSpecializePost(pid, appDataDir, niceName);
            } else {
                NormalProxy.forkAndSpecializePost(pid, appDataDir, niceName);
            }
        } else {
            // in zygote process, res is child zygote pid
            // don't print log here, see https://github.com/RikkaApps/Riru/blob/77adfd6a4a6a81bfd20569c910bc4854f2f84f5e/riru-core/jni/main/jni_native_method.cpp#L55-L66
        }
    }

    public static void forkSystemServerPre(int uid, int gid, int[] gids, int debugFlags, int[][] rlimits,
                                           long permittedCapabilities, long effectiveCapabilities) {
        if (BuildConfig.DEBUG) {
            forkSystemServerPramsStr = String.format("Zygote#forkSystemServer(%d, %d, %s, %d, %s, %d, %d)",
                    uid, gid, Arrays.toString(gids), debugFlags, Arrays.toString(rlimits),
                    permittedCapabilities, effectiveCapabilities);
        }
        if (isBlackWhiteListEnabled()) {
            BlackWhiteListProxy.forkSystemServerPre(uid, gid, gids, debugFlags, rlimits,
                    permittedCapabilities, effectiveCapabilities);
        } else {
            NormalProxy.forkSystemServerPre(uid, gid, gids, debugFlags, rlimits,
                    permittedCapabilities, effectiveCapabilities);
        }
    }

    public static void forkSystemServerPost(int pid) {
        if (pid == 0) {
            Utils.logD(forkSystemServerPramsStr + " = " + Process.myPid());
            if (isBlackWhiteListEnabled()) {
                BlackWhiteListProxy.forkSystemServerPost(pid);
            } else {
                NormalProxy.forkSystemServerPost(pid);
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

    public static native void setMethodNonCompilable(Object member);

    public static native void ensureMethodCached(Method hook, Method backup);

    // JNI.ToReflectedMethod() could return either Method or Constructor
    public static native Object findMethodNative(Class targetClass, String methodName, String methodSig);

    private static native void init(int SDK_version);

    public static native String getInstallerPkgName();

    public static native boolean isBlackWhiteListEnabled();

    public static native boolean isDynamicModulesEnabled();

    public static native boolean isAppNeedHook(String appDataDir);

    // prevent from fatal error caused by holding not whitelisted file descriptors when forking zygote
    // https://github.com/rovo89/Xposed/commit/b3ba245ad04cd485699fb1d2ebde7117e58214ff
    public static native void closeFilesBeforeForkNative();

    public static native void reopenFilesAfterForkNative();

    public static native void deoptMethodNative(Object object);

    public static native long suspendAllThreads();

    public static native void resumeAllThreads(long obj);

    public static native int waitForGcToComplete(long thread);
}
