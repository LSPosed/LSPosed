package com.elderdrivers.riru.xposed.proxy.yahfa;

import com.elderdrivers.riru.xposed.Main;
import com.elderdrivers.riru.xposed.config.ConfigManager;
import com.elderdrivers.riru.xposed.entry.Router;
import com.elderdrivers.riru.xposed.util.PrebuiltMethodsDeopter;

import static com.elderdrivers.riru.xposed.util.FileUtils.getDataPathPrefix;

public class BlackWhiteListProxy {

    public static void forkAndSpecializePre(int uid, int gid, int[] gids, int debugFlags,
                                            int[][] rlimits, int mountExternal, String seInfo,
                                            String niceName, int[] fdsToClose, int[] fdsToIgnore,
                                            boolean startChildZygote, String instructionSet,
                                            String appDataDir) {
        // always enter here, make sure secondary zygote's modules is loaded only once
        // when isDynamicModulesMode is not on
        final boolean isDynamicModulesMode = Main.isDynamicModulesEnabled();
        ConfigManager.setDynamicModulesMode(isDynamicModulesMode);
        // call this to ensure the flag is set to false ASAP
        Router.prepare(false);
        PrebuiltMethodsDeopter.deoptBootMethods(); // do it once for secondary zygote
        if (!isDynamicModulesMode) {
            Router.loadModulesSafely();
            Main.closeFilesBeforeForkNative();
        }
    }

    public static void forkAndSpecializePost(int pid, String appDataDir) {
        // when this process is in white list or not in black list
        // installBootstrapHooks -> loadModules if needed
        final boolean isDynamicModulesMode = Main.isDynamicModulesEnabled();
        if (!isDynamicModulesMode) {
            Main.reopenFilesAfterForkNative();
        }
        Main.appDataDir = appDataDir;
        ConfigManager.setDynamicModulesMode(isDynamicModulesMode);
        Router.onEnterChildProcess();
        Router.installBootstrapHooks(false);
        Router.loadModulesSafely();
    }

    public static void forkSystemServerPre(int uid, int gid, int[] gids, int debugFlags,
                                           int[][] rlimits, long permittedCapabilities,
                                           long effectiveCapabilities) {
        // we always enter here whether black/white list is on or not
        final boolean isDynamicModulesMode = Main.isDynamicModulesEnabled();
        ConfigManager.setDynamicModulesMode(isDynamicModulesMode);
        PrebuiltMethodsDeopter.deoptBootMethods(); // do it once for main zygote
        // set startsSystemServer flag used when loadModules
        Router.prepare(true);
        // we never install bootstrap hooks here in black/white list mode
        // because installed hooks would be propagated to all child processes of main zygote
        // hence we cannot install hooks for processes like com.android.phone process who are
        // not from forkAndSpecialize as a side effect
        if (!isDynamicModulesMode) {
            Router.loadModulesSafely();
            Main.closeFilesBeforeForkNative();
        }
    }

    public static void forkSystemServerPost(int pid) {
        // should only here when system_server is in white list or not in black list
        // installBootstrapHooks -> loadModules if needed
        final boolean isDynamicModulesMode = Main.isDynamicModulesEnabled();
        if (!isDynamicModulesMode) {
            Main.reopenFilesAfterForkNative();
        }
        Main.appDataDir = getDataPathPrefix() + "android";
        ConfigManager.setDynamicModulesMode(isDynamicModulesMode);
        Router.onEnterChildProcess();
        Router.installBootstrapHooks(true);
        Router.loadModulesSafely();
    }
}
