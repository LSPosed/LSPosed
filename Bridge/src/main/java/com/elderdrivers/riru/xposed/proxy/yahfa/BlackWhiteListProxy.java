package com.elderdrivers.riru.xposed.proxy.yahfa;

import com.elderdrivers.riru.xposed.Main;
import com.elderdrivers.riru.xposed.entry.Router;

import static com.elderdrivers.riru.xposed.util.FileUtils.getDataPathPrefix;

public class BlackWhiteListProxy {

    public static void forkAndSpecializePre(int uid, int gid, int[] gids, int debugFlags,
                                            int[][] rlimits, int mountExternal, String seInfo,
                                            String niceName, int[] fdsToClose, int[] fdsToIgnore,
                                            boolean startChildZygote, String instructionSet,
                                            String appDataDir) {
    }

    public static void forkAndSpecializePost(int pid, String appDataDir) {
        Main.appDataDir = appDataDir;
        Router.prepare(false);
        Router.installBootstrapHooks(false);
        Router.loadModulesSafely();
    }

    public static void forkSystemServerPre(int uid, int gid, int[] gids, int debugFlags, int[][] rlimits,
                                           long permittedCapabilities, long effectiveCapabilities) {

    }

    public static void forkSystemServerPost(int pid) {
        Main.appDataDir = getDataPathPrefix() + "android";
        Router.prepare(true);
        Router.installBootstrapHooks(true);
        Router.loadModulesSafely();
    }
}
