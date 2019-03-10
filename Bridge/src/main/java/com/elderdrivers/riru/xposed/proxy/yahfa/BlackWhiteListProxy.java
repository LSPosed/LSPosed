package com.elderdrivers.riru.xposed.proxy.yahfa;

import com.elderdrivers.riru.xposed.Main;
import com.elderdrivers.riru.xposed.config.ConfigManager;
import com.elderdrivers.riru.xposed.entry.Router;
import com.elderdrivers.riru.xposed.util.PrebuiltMethodsDeopter;

import static com.elderdrivers.riru.xposed.Main.isAppNeedHook;
import static com.elderdrivers.riru.xposed.util.FileUtils.getDataPathPrefix;

/**
 * 1. Non dynamic mode
 * - system_server is whitelisted
 * * for all child processes of main zygote
 * What've been done in main zygote pre-forking system_server
 * 1) non dynamic flag set (no need to reset)
 * 2) boot image methods deopted (no need to redo)
 * 3) startSystemServer flag set to true (need to reset)
 * 4) workaround hooks installed (need to redo)
 * 5) module list loaded and initZygote called (no need to redo)
 * 6) close all fds (no need to redo because of 5))
 * * for all child processes of secondary zygote
 * 1) do the same things pre-forking first child process
 * - system_server is blacklisted:
 * * for all child processes of both main zygote and secondary zygote
 * 1) do the same things pre-forking first child process
 * 2. Dynamic mode:
 *  to be continued
 */
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
        // set startsSystemServer flag used when loadModules
        Router.prepare(isSystemServer);
        // deoptBootMethods once for all child processes of zygote
        PrebuiltMethodsDeopter.deoptBootMethods();
        // we never install bootstrap hooks here in black/white list mode except workaround hooks
        // because installed hooks would be propagated to all child processes of zygote
        Router.startWorkAroundHook();
        // loadModules once for all child processes of zygote
        Router.loadModulesSafely(true);
    }

    private static void onForkPostCommon(boolean isSystemServer, String appDataDir) {
        Main.appDataDir = appDataDir;
        Router.onEnterChildProcess();
        if (!isAppNeedHook(Main.appDataDir)) {
            // if is blacklisted, just stop here
            return;
        }
        final boolean isDynamicModulesMode = Main.isDynamicModulesEnabled();
        ConfigManager.setDynamicModulesMode(isDynamicModulesMode);
        Router.prepare(isSystemServer);
        PrebuiltMethodsDeopter.deoptBootMethods();
        Router.reopenFilesIfNeeded();
        Router.installBootstrapHooks(isSystemServer);
        if (isDynamicModulesMode) {
            Router.loadModulesSafely(false);
        }
    }
}
