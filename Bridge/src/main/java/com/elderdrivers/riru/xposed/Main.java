package com.elderdrivers.riru.xposed;

import android.annotation.SuppressLint;
import android.os.Build;

import com.elderdrivers.riru.common.KeepAll;
import com.elderdrivers.riru.xposed.core.HookMethodResolver;
import com.elderdrivers.riru.xposed.entry.Router;

import java.lang.reflect.Method;

import static com.elderdrivers.riru.xposed.util.FileUtils.getDataPathPrefix;

@SuppressLint("DefaultLocale")
public class Main implements KeepAll {

    /**
     * When set to true, install bootstrap hooks and loadModules
     * for each process when it starts.
     * This means you can deactivate or activate every module
     * for the process you restart without rebooting.
     */
    private static final boolean DYNAMIC_LOAD_MODULES = false;
    //    private static String sForkAndSpecializePramsStr = "";
//    private static String sForkSystemServerPramsStr = "";
    public static String sAppDataDir = "";
    public static String sAppProcessName = "";

    static {
        init(Build.VERSION.SDK_INT);
        HookMethodResolver.init();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // entry points
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Deprecated
    public static void forkAndSpecializePre(int uid, int gid, int[] gids, int debugFlags,
                                            int[][] rlimits, int mountExternal, String seInfo,
                                            String niceName, int[] fdsToClose, int[] fdsToIgnore,
                                            boolean startChildZygote, String instructionSet, String appDataDir) {
//        sForkAndSpecializePramsStr = String.format(
//                "Zygote#forkAndSpecialize(%d, %d, %s, %d, %s, %d, %s, %s, %s, %s, %s, %s, %s)",
//                uid, gid, Arrays.toString(gids), debugFlags, Arrays.toString(rlimits),
//                mountExternal, seInfo, niceName, Arrays.toString(fdsToClose),
//                Arrays.toString(fdsToIgnore), startChildZygote, instructionSet, appDataDir);
        if (!DYNAMIC_LOAD_MODULES) {
            Router.onProcessForked(false);
        }
    }

    public static void forkAndSpecializePost(int pid, String appDataDir) {
//        Utils.logD(sForkAndSpecializePramsStr + " = " + pid);
        if (pid == 0) {
            // in app process
            sAppDataDir = appDataDir;
            if (DYNAMIC_LOAD_MODULES) {
                Router.onProcessForked(false);
            }
        } else {
            // in zygote process, res is child zygote pid
            // don't print log here, see https://github.com/RikkaApps/Riru/blob/77adfd6a4a6a81bfd20569c910bc4854f2f84f5e/riru-core/jni/main/jni_native_method.cpp#L55-L66
        }
    }

    public static void forkSystemServerPre(int uid, int gid, int[] gids, int debugFlags, int[][] rlimits,
                                           long permittedCapabilities, long effectiveCapabilities) {
//        sForkSystemServerPramsStr = String.format("Zygote#forkSystemServer(%d, %d, %s, %d, %s, %d, %d)",
//                uid, gid, Arrays.toString(gids), debugFlags, Arrays.toString(rlimits),
//                permittedCapabilities, effectiveCapabilities);
    }

    public static void forkSystemServerPost(int pid) {
//        Utils.logD(sForkSystemServerPramsStr + " = " + pid);
        if (pid == 0) {
            // in system_server process
            sAppDataDir = getDataPathPrefix() + "android/";
            Router.onProcessForked(true);
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
}
