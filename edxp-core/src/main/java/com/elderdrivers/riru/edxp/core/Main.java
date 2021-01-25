package com.elderdrivers.riru.edxp.core;

import android.annotation.SuppressLint;

import com.elderdrivers.riru.common.KeepAll;
import com.elderdrivers.riru.edxp.config.ConfigManager;
import com.elderdrivers.riru.edxp.util.Utils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;

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
        String file_name = ConfigManager.getMiscPath() + "/variant";
        int variant = EdxpImpl.NONE;
        try {
            String f = new String(Files.readAllBytes(Paths.get(file_name))).trim();
            variant = Integer.parseInt(f);
        } catch (Exception e) {
            Utils.logE("loadEdxpImpls: ", e);
        }

        Utils.logD("Loading variant " + variant);

        try {
            switch (variant) {
                case EdxpImpl.YAHFA:
                    Class.forName("com.elderdrivers.riru.edxp.yahfa.core.YahfaEdxpImpl");
                    break;
                case EdxpImpl.SANDHOOK:
                    Class.forName("com.elderdrivers.riru.edxp.sandhook.core.SandHookEdxpImpl");
                    break;
                default:
                    Utils.logE("Unsupported variant " + variant);

            }
        } catch (ClassNotFoundException e) {
            Utils.logE("loadEdxpImpls: Class not found", e);
        }
    }
}
