package com.elderdrivers.riru.xposed.proxy.yahfa;

import com.elderdrivers.riru.xposed.Main;
import com.elderdrivers.riru.xposed.config.ConfigManager;
import com.elderdrivers.riru.xposed.entry.Router;

import static com.elderdrivers.riru.xposed.util.FileUtils.getDataPathPrefix;

public class BlackWhiteListProxy {

    public static void forkAndSpecializePre(int uid, int gid, int[] gids, int debugFlags,
                                            int[][] rlimits, int mountExternal, String seInfo,
                                            String niceName, int[] fdsToClose, int[] fdsToIgnore,
                                            boolean startChildZygote, String instructionSet,
                                            String appDataDir) {
        final boolean isDynamicModulesMode = Main.isDynamicModulesEnabled();
        ConfigManager.setDynamicModulesMode(isDynamicModulesMode);
        if (!isDynamicModulesMode) {
            Router.loadModulesSafely();
            Main.closeFilesBeforeForkNative();
        }
    }

    public static void forkAndSpecializePost(int pid, String appDataDir) {
        final boolean isDynamicModulesMode = Main.isDynamicModulesEnabled();
        if (!isDynamicModulesMode) {
            Main.reopenFilesAfterForkNative();
        }
        Main.appDataDir = appDataDir;
        ConfigManager.setDynamicModulesMode(isDynamicModulesMode);
        Router.prepare(false);
        Router.installBootstrapHooks(false);
        Router.loadModulesSafely();
    }

    public static void forkSystemServerPre(int uid, int gid, int[] gids, int debugFlags,
                                           int[][] rlimits, long permittedCapabilities,
                                           long effectiveCapabilities) {
        final boolean isDynamicModulesMode = Main.isDynamicModulesEnabled();
        ConfigManager.setDynamicModulesMode(isDynamicModulesMode);
        if (!isDynamicModulesMode) {
            Router.loadModulesSafely();
            Main.closeFilesBeforeForkNative();
        }
    }

    public static void forkSystemServerPost(int pid) {
        final boolean isDynamicModulesMode = Main.isDynamicModulesEnabled();
        if (!isDynamicModulesMode) {
            Main.reopenFilesAfterForkNative();
        }
        Main.appDataDir = getDataPathPrefix() + "android";
        ConfigManager.setDynamicModulesMode(isDynamicModulesMode);
        Router.prepare(true);
        Router.installBootstrapHooks(true);
        Router.loadModulesSafely();
    }
}
