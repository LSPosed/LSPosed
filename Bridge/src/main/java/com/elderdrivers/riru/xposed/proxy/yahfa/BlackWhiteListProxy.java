package com.elderdrivers.riru.xposed.proxy.yahfa;

import com.elderdrivers.riru.xposed.Main;
import com.elderdrivers.riru.xposed.config.ConfigManager;
import com.elderdrivers.riru.xposed.entry.Router;
import com.elderdrivers.riru.xposed.util.PrebuiltMethodsDeopter;

import static com.elderdrivers.riru.xposed.Main.isAppNeedHook;
import static com.elderdrivers.riru.xposed.util.FileUtils.getDataPathPrefix;

public class BlackWhiteListProxy {

    public static void forkAndSpecializePre(int uid, int gid, int[] gids, int debugFlags,
                                            int[][] rlimits, int mountExternal, String seInfo,
                                            String niceName, int[] fdsToClose, int[] fdsToIgnore,
                                            boolean startChildZygote, String instructionSet,
                                            String appDataDir) {
        final boolean isDynamicModulesMode = Main.isDynamicModulesEnabled();
        if (isDynamicModulesMode) {
            // should never happen
            return;
        }
        // only enter here when isDynamicModulesMode is off
        onForkPreForNonDynamicMode(false);
    }

    public static void forkAndSpecializePost(int pid, String appDataDir) {
        onForkPostCommon(false, appDataDir);
    }

    public static void forkSystemServerPre(int uid, int gid, int[] gids, int debugFlags,
                                           int[][] rlimits, long permittedCapabilities,
                                           long effectiveCapabilities) {
        final boolean isDynamicModulesMode = Main.isDynamicModulesEnabled();
        if (isDynamicModulesMode) {
            // should never happen
            return;
        }
        // only enter here when isDynamicModulesMode is off
        onForkPreForNonDynamicMode(true);
    }

    public static void forkSystemServerPost(int pid) {
        onForkPostCommon(true, getDataPathPrefix() + "android");
    }

    /**
     * Some details are different between main zygote and secondary zygote.
     */
    private static void onForkPreForNonDynamicMode(boolean isSystemServer) {
        ConfigManager.setDynamicModulesMode(false);
        // deoptBootMethods once for all child processes of zygote
        PrebuiltMethodsDeopter.deoptBootMethods();
        // set startsSystemServer flag used when loadModules
        Router.prepare(isSystemServer);
        // we never install bootstrap hooks here in black/white list mode except workaround hooks
        // because installed hooks would be propagated to all child processes of zygote
        Router.startWorkAroundHook();
        // loadModules once for all child processes of zygote
        Router.loadModulesSafely();
        // at last close all fds
        Main.closeFilesBeforeForkNative();
    }

    private static void onForkPostCommon(boolean isSystemServer, String appDataDir) {
        final boolean isDynamicModulesMode = Main.isDynamicModulesEnabled();
        // set common flags
        ConfigManager.setDynamicModulesMode(isDynamicModulesMode);
        Main.appDataDir = appDataDir;
        Router.onEnterChildProcess();

        if (!isDynamicModulesMode) {
            // initial stuffs have been done in forkSystemServerPre
            Main.reopenFilesAfterForkNative();
        }

        if (!isAppNeedHook(Main.appDataDir)) {
            // if is blacklisted, just stop here
            return;
        }

        if (isDynamicModulesMode) {
            // nothing has been done in forkSystemServerPre, we have to do the same here
            // except some workarounds specific for forkSystemServerPre
            PrebuiltMethodsDeopter.deoptBootMethods();
            Router.prepare(isSystemServer);
            Router.loadModulesSafely();
        }
        Router.installBootstrapHooks(isSystemServer);
    }
}
