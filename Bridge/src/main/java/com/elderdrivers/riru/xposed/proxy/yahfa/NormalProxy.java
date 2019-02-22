package com.elderdrivers.riru.xposed.proxy.yahfa;

import com.elderdrivers.riru.xposed.Main;
import com.elderdrivers.riru.xposed.config.ConfigManager;
import com.elderdrivers.riru.xposed.dexmaker.DynamicBridge;
import com.elderdrivers.riru.xposed.entry.Router;

import static com.elderdrivers.riru.xposed.util.FileUtils.getDataPathPrefix;

public class NormalProxy {

    public static void forkAndSpecializePre(int uid, int gid, int[] gids, int debugFlags,
                                            int[][] rlimits, int mountExternal, String seInfo,
                                            String niceName, int[] fdsToClose, int[] fdsToIgnore,
                                            boolean startChildZygote, String instructionSet,
                                            String appDataDir) {
        Main.appDataDir = appDataDir;
        Router.prepare(false);
        // install bootstrap hooks for secondary zygote
        Router.installBootstrapHooks(false);
        // load modules for secondary zygote
        Router.loadModulesSafely();
    }

    public static void forkAndSpecializePost(int pid, String appDataDir) {
        // TODO consider processes without forkAndSpecializePost called
        Router.onEnterChildProcess();
        DynamicBridge.onForkPost();
        if (ConfigManager.isDynamicModulesMode()) {
            // load modules for each app process on its forked
            Router.loadModulesSafely();
        }
    }

    public static void forkSystemServerPre(int uid, int gid, int[] gids, int debugFlags, int[][] rlimits,
                                           long permittedCapabilities, long effectiveCapabilities) {
        Main.appDataDir = getDataPathPrefix() + "android";
        Router.prepare(true);
        // install bootstrap hooks for main zygote as early as possible
        // in case we miss some processes not forked via forkAndSpecialize
        // for instance com.android.phone
        Router.installBootstrapHooks(true);
        // loadModules have to be executed in zygote even isDynamicModules is false
        // because if not global hooks installed in initZygote might not be
        // propagated to processes not forked via forkAndSpecialize
        Router.loadModulesSafely();
    }

    public static void forkSystemServerPost(int pid) {
        // in system_server process
        Router.onEnterChildProcess();
    }

}
