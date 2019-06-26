package com.elderdrivers.riru.edxp.core;

import android.annotation.SuppressLint;
import android.os.Process;

import com.elderdrivers.riru.common.KeepAll;
import com.elderdrivers.riru.edxp.common.BuildConfig;
import com.elderdrivers.riru.edxp.config.ConfigManager;
import com.elderdrivers.riru.edxp.framework.ProcessHelper;
import com.elderdrivers.riru.edxp.util.Utils;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;

import static com.elderdrivers.riru.edxp.proxy.BaseProxy.onBlackListed;

@SuppressLint("DefaultLocale")
public class Main implements KeepAll {

    private static final boolean logEnabled = BuildConfig.DEBUG;
    private static String forkAndSpecializePramsStr = "";
    private static String forkSystemServerPramsStr = "";

    private static final AtomicReference<EdxpImpl> edxpImplRef = new AtomicReference<>(null);

    static {
        loadEdxpImpls();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // entry points
    ///////////////////////////////////////////////////////////////////////////////////////////////

    public static void forkAndSpecializePre(int uid, int gid, int[] gids, int debugFlags,
                                            int[][] rlimits, int mountExternal, String seInfo,
                                            String niceName, int[] fdsToClose, int[] fdsToIgnore,
                                            boolean startChildZygote, String instructionSet,
                                            String appDataDir) {
        if (isBlackListedProcess(uid)) {
            return;
        }
        final EdxpImpl edxp = getEdxpImpl();
        if (edxp == null || !edxp.isInitialized()) {
            return;
        }
        if (logEnabled) {
            forkAndSpecializePramsStr = String.format(
                    "Zygote#forkAndSpecialize(%d, %d, %s, %d, %s, %d, %s, %s, %s, %s, %s, %s, %s)",
                    uid, gid, Arrays.toString(gids), debugFlags, Arrays.toString(rlimits),
                    mountExternal, seInfo, niceName, Arrays.toString(fdsToClose),
                    Arrays.toString(fdsToIgnore), startChildZygote, instructionSet, appDataDir);
        }

        if (ConfigManager.isBlackWhiteListEnabled()) {
            edxp.getBlackWhiteListProxy().forkAndSpecializePre(uid, gid, gids, debugFlags, rlimits,
                    mountExternal, seInfo, niceName, fdsToClose, fdsToIgnore, startChildZygote,
                    instructionSet, appDataDir);
        } else {
            edxp.getNormalProxy().forkAndSpecializePre(uid, gid, gids, debugFlags, rlimits, mountExternal,
                    seInfo, niceName, fdsToClose, fdsToIgnore, startChildZygote, instructionSet,
                    appDataDir);
        }
    }

    public static void forkAndSpecializePost(int pid, String appDataDir, String niceName) {
        if (isBlackListedProcess(Process.myUid())) {
            onBlackListed();
            return;
        }
        final EdxpImpl edxp = getEdxpImpl();
        if (edxp == null || !edxp.isInitialized()) {
            return;
        }
        if (pid == 0) {
            if (logEnabled) {
                Utils.logI(forkAndSpecializePramsStr + " = " + Process.myPid());
            }
            if (ConfigManager.isBlackWhiteListEnabled()) {
                edxp.getBlackWhiteListProxy().forkAndSpecializePost(pid, appDataDir, niceName);
            } else {
                edxp.getNormalProxy().forkAndSpecializePost(pid, appDataDir, niceName);
            }
        } else {
            // in zygote process, res is child zygote pid
            // don't print log here, see https://github.com/RikkaApps/Riru/blob/77adfd6a4a6a81bfd20569c910bc4854f2f84f5e/riru-core/jni/main/jni_native_method.cpp#L55-L66
        }
    }

    public static void forkSystemServerPre(int uid, int gid, int[] gids, int debugFlags, int[][] rlimits,
                                           long permittedCapabilities, long effectiveCapabilities) {
        final EdxpImpl edxp = getEdxpImpl();
        if (edxp == null || !edxp.isInitialized()) {
            return;
        }
        if (logEnabled) {
            forkSystemServerPramsStr = String.format("Zygote#forkSystemServer(%d, %d, %s, %d, %s, %d, %d)",
                    uid, gid, Arrays.toString(gids), debugFlags, Arrays.toString(rlimits),
                    permittedCapabilities, effectiveCapabilities);
        }
        if (ConfigManager.isBlackWhiteListEnabled()) {
            edxp.getBlackWhiteListProxy().forkSystemServerPre(uid, gid, gids, debugFlags, rlimits,
                    permittedCapabilities, effectiveCapabilities);
        } else {
            edxp.getNormalProxy().forkSystemServerPre(uid, gid, gids, debugFlags, rlimits,
                    permittedCapabilities, effectiveCapabilities);
        }
    }

    public static void forkSystemServerPost(int pid) {
        final EdxpImpl edxp = getEdxpImpl();
        if (edxp == null || !edxp.isInitialized()) {
            return;
        }
        if (pid == 0) {
            if (logEnabled) {
                Utils.logI(forkSystemServerPramsStr + " = " + Process.myPid());
            }
            if (ConfigManager.isBlackWhiteListEnabled()) {
                edxp.getBlackWhiteListProxy().forkSystemServerPost(pid);
            } else {
                edxp.getNormalProxy().forkSystemServerPost(pid);
            }
        } else {
            // in zygote process, res is child zygote pid
            // don't print log here, see https://github.com/RikkaApps/Riru/blob/77adfd6a4a6a81bfd20569c910bc4854f2f84f5e/riru-core/jni/main/jni_native_method.cpp#L55-L66
        }
    }

    public static synchronized boolean setEdxpImpl(EdxpImpl edxp) {
        return edxpImplRef.compareAndSet(null, edxp);
    }

    public static synchronized EdxpImpl getEdxpImpl() {
        return edxpImplRef.get();
    }

    @EdxpImpl.Variant
    public static synchronized int getEdxpVariant() {
        return getEdxpImpl().getVariant();
    }

    private static void loadEdxpImpls() {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                Iterator<EdxpImpl> iterator = ServiceLoader.load(
                        EdxpImpl.class, Main.class.getClassLoader()).iterator();
                try {
                    while (iterator.hasNext()) {
                        iterator.next();
                    }
                } catch (Throwable t) {
                    Utils.logE("error when loadEdxpImpls", t);
                }
                return null;
            }
        });
    }

    private static boolean isBlackListedProcess(int uid) {
        return ProcessHelper.isIsolated(uid)
                || ProcessHelper.isRELROUpdater(uid)
                || ProcessHelper.isWebViewZygote(uid);
    }
}
