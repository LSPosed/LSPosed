package com.elderdrivers.riru.edxp.core;

import android.annotation.SuppressLint;

import com.elderdrivers.riru.common.KeepAll;
import com.elderdrivers.riru.edxp.util.Utils;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;

import static com.elderdrivers.riru.edxp.core.EdxpImpl.NONE;

@SuppressLint("DefaultLocale")
public class Main implements KeepAll {
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
        // won't be loaded
    }

    public static void forkAndSpecializePost(int pid, String appDataDir, String niceName) {
        final EdxpImpl edxp = getEdxpImpl();
        if (edxp == null || !edxp.isInitialized()) {
            Utils.logE("Not started up");
            return;
        }
        if (pid == 0) {
            edxp.getNormalProxy().forkAndSpecializePost(pid, appDataDir, niceName);
        }
    }

    public static void forkSystemServerPre(int uid, int gid, int[] gids, int debugFlags, int[][] rlimits,
                                           long permittedCapabilities, long effectiveCapabilities) {
        // Won't load
    }

    public static void forkSystemServerPost(int pid) {
        final EdxpImpl edxp = getEdxpImpl();
        if (edxp == null || !edxp.isInitialized()) {
            return;
        }
        if (pid == 0) {
            edxp.getNormalProxy().forkSystemServerPost(pid);
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
        // We don't have Manifest now, so we have to load manually.
        try {
            Class.forName("com.elderdrivers.riru.edxp.sandhook.core.SandHookEdxpImpl");
        }catch(Throwable ignored) {
            Utils.logD("not using sandhook");
        }
        try {
            Class.forName("com.elderdrivers.riru.edxp.yahfa.core.YahfaEdxpImpl");
        }catch(Throwable ignored) {
            Utils.logD("not using yahfa");
        }
    }
}
