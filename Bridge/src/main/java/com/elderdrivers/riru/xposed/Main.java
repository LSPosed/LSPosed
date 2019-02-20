package com.elderdrivers.riru.xposed;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Process;

import com.elderdrivers.riru.common.KeepAll;
import com.elderdrivers.riru.xposed.core.HookMethodResolver;
import com.elderdrivers.riru.xposed.dexmaker.DynamicBridge;
import com.elderdrivers.riru.xposed.entry.Router;
import com.elderdrivers.riru.xposed.util.Utils;

import java.lang.reflect.Method;
import java.util.Arrays;

import static com.elderdrivers.riru.xposed.util.FileUtils.getDataPathPrefix;

@SuppressLint("DefaultLocale")
public class Main implements KeepAll {

    public static String appDataDir = "";
    public static String appProcessName = "";
    /**
     * When set to true, install bootstrap hooks and loadModules
     * for each process when it starts.
     * This means you can deactivate or activate every module
     * for the process you restart without rebooting.
     */
    public static boolean isDynamicModules = false;
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
                                            String appDataDir, boolean isDynamicModules) {
        if (BuildConfig.DEBUG) {
            forkAndSpecializePramsStr = String.format(
                    "Zygote#forkAndSpecialize(%d, %d, %s, %d, %s, %d, %s, %s, %s, %s, %s, %s, %s)",
                    uid, gid, Arrays.toString(gids), debugFlags, Arrays.toString(rlimits),
                    mountExternal, seInfo, niceName, Arrays.toString(fdsToClose),
                    Arrays.toString(fdsToIgnore), startChildZygote, instructionSet, appDataDir);
        }
        Main.appDataDir = appDataDir;
        Main.isDynamicModules = isDynamicModules;
        Router.prepare(false);
        // install bootstrap hooks for secondary zygote
        Router.installBootstrapHooks(false);
        // load modules for secondary zygote
        Router.loadModulesSafely();
    }

    public static void forkAndSpecializePost(int pid, String appDataDir) {
        if (pid == 0) {
            Utils.logD(forkAndSpecializePramsStr + " = " + Process.myPid());
            Router.onEnterChildProcess();
            DynamicBridge.onForkPost();
            if (isDynamicModules) {
                // load modules for each app process on its forked
                Router.loadModulesSafely();
            }
        } else {
            // in zygote process, res is child zygote pid
            // don't print log here, see https://github.com/RikkaApps/Riru/blob/77adfd6a4a6a81bfd20569c910bc4854f2f84f5e/riru-core/jni/main/jni_native_method.cpp#L55-L66
        }
    }

    public static void forkSystemServerPre(int uid, int gid, int[] gids, int debugFlags, int[][] rlimits,
                                           long permittedCapabilities, long effectiveCapabilities,
                                           boolean isDynamicModules) {
        if (BuildConfig.DEBUG) {
            forkSystemServerPramsStr = String.format("Zygote#forkSystemServer(%d, %d, %s, %d, %s, %d, %d)",
                    uid, gid, Arrays.toString(gids), debugFlags, Arrays.toString(rlimits),
                    permittedCapabilities, effectiveCapabilities);
        }
        Main.appDataDir = getDataPathPrefix() + "android";
        Main.isDynamicModules = isDynamicModules;
        Router.prepare(true);
        // install bootstrap hooks for main zygote as early as possible
        // in case we miss some processes not forked via forkAndSpecialize
        // for instance com.android.phone
        Router.installBootstrapHooks(true);
        // loadModules have to be executed in zygote even isDynamicModules is false
        // because if not global hooks installed in initZygote might not be
        // propagated to processes not forked via forkAndSpecialize
        Router.loadModulesSafely();
    }

    public static void forkSystemServerPost(int pid) {
        if (pid == 0) {
            Utils.logD(forkSystemServerPramsStr + " = " + Process.myPid());
            // in system_server process
            Router.onEnterChildProcess();
        } else {
            // in zygote process, res is child zygote pid
            // don't print log here, see https://github.com/RikkaApps/Riru/blob/77adfd6a4a6a81bfd20569c910bc4854f2f84f5e/riru-core/jni/main/jni_native_method.cpp#L55-L66
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // native methods
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("all")
    public static native boolean backupAndHookNative(Object target, Method hook, Method backup);

    @SuppressWarnings("all")
    public static native void ensureMethodCached(Method hook, Method backup);

    @SuppressWarnings("all")
    // JNI.ToReflectedMethod() could return either Method or Constructor
    public static native Object findMethodNative(Class targetClass, String methodName, String methodSig);

    @SuppressWarnings("all")
    private static native void init(int SDK_version);
}
