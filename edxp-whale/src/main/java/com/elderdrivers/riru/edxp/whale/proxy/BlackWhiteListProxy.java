package com.elderdrivers.riru.edxp.whale.proxy;

import android.text.TextUtils;

import com.elderdrivers.riru.edxp.Main;
import com.elderdrivers.riru.edxp.config.ConfigManager;
import com.elderdrivers.riru.edxp.deopt.PrebuiltMethodsDeopter;
import com.elderdrivers.riru.edxp.util.ProcessUtils;
import com.elderdrivers.riru.edxp.util.Utils;
import com.elderdrivers.riru.edxp.whale.entry.Router;

import de.robv.android.xposed.XposedBridge;

import static com.elderdrivers.riru.edxp.Main.isAppNeedHook;
import static com.elderdrivers.riru.edxp.util.FileUtils.getDataPathPrefix;

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
 * to be continued
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

    public static void forkAndSpecializePost(int pid, String appDataDir, String niceName) {
        onForkPostCommon(false, appDataDir, niceName);
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
        onForkPostCommon(true, getDataPathPrefix() + "android", "system_server");
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
        // TODO maybe just save initZygote callbacks and call them when whitelisted process forked?
        Router.loadModulesSafely(true);
        Main.closeFilesBeforeForkNative();
    }

    private static void onForkPostCommon(boolean isSystemServer, String appDataDir, String niceName) {
        Main.appDataDir = appDataDir;
        Main.niceName = niceName;
        final boolean isDynamicModulesMode = Main.isDynamicModulesEnabled();
        ConfigManager.setDynamicModulesMode(isDynamicModulesMode);
        Router.onEnterChildProcess();
        if (!isDynamicModulesMode) {
            Main.reopenFilesAfterForkNative();
        }
        if (!checkNeedHook(appDataDir, niceName)) {
            // if is blacklisted, just stop here
            return;
        }
        Router.prepare(isSystemServer);
        PrebuiltMethodsDeopter.deoptBootMethods();
        Router.installBootstrapHooks(isSystemServer);
        if (isDynamicModulesMode) {
            Router.loadModulesSafely(false);
        }
    }

    private static boolean checkNeedHook(String appDataDir, String niceName) {
        boolean needHook;
        if (TextUtils.isEmpty(appDataDir)) {
            Utils.logE("niceName:" + niceName + ", procName:"
                    + ProcessUtils.getCurrentProcessName(Main.appProcessName) + ", appDataDir is null, blacklisted!");
            needHook = false;
        } else {
            // FIXME some process cannot read app_data_file because of MLS, e.g. bluetooth
            needHook = isAppNeedHook(appDataDir);
        }
        if (!needHook) {
            // clean up the scene
            onBlackListed();
        }
        return needHook;
    }

    private static void onBlackListed() {
        XposedBridge.clearLoadedPackages();
        XposedBridge.clearInitPackageResources();
    }
}
