package com.elderdrivers.riru.edxp.sandhook.proxy;

import com.elderdrivers.riru.edxp.Main;
import com.elderdrivers.riru.edxp.config.ConfigManager;
import com.elderdrivers.riru.edxp.deopt.PrebuiltMethodsDeopter;
import com.elderdrivers.riru.edxp.framework.Zygote;
import com.elderdrivers.riru.edxp.sandhook.entry.Router;

import static com.elderdrivers.riru.edxp.util.FileUtils.getDataPathPrefix;

public class NormalProxy {

    public static void forkAndSpecializePre(int uid, int gid, int[] gids, int debugFlags,
                                            int[][] rlimits, int mountExternal, String seInfo,
                                            String niceName, int[] fdsToClose, int[] fdsToIgnore,
                                            boolean startChildZygote, String instructionSet,
                                            String appDataDir) {
        // mainly for secondary zygote
        Router.onForkStart();
        Router.initResourcesHook();
        // call this to ensure the flag is set to false ASAP
        Router.prepare(false);
        PrebuiltMethodsDeopter.deoptBootMethods(); // do it once for secondary zygote
        // install bootstrap hooks for secondary zygote
        Router.installBootstrapHooks(false);
        // only load modules for secondary zygote
        Router.loadModulesSafely(true);
        Zygote.closeFilesBeforeFork();
    }

    public static void forkAndSpecializePost(int pid, String appDataDir, String niceName) {
        // TODO consider processes without forkAndSpecializePost called
        Main.setAppDataDir(appDataDir);
        Main.niceName = niceName;
        Router.prepare(false);
        Zygote.reopenFilesAfterFork();
        Router.onEnterChildProcess();
        // load modules for each app process on its forked if dynamic modules mode is on
        Router.loadModulesSafely(false);
        Router.onForkFinish();
    }

    public static void forkSystemServerPre(int uid, int gid, int[] gids, int debugFlags, int[][] rlimits,
                                           long permittedCapabilities, long effectiveCapabilities) {
        Router.onForkStart();
        Router.initResourcesHook();
        // set startsSystemServer flag used when loadModules
        Router.prepare(true);
        PrebuiltMethodsDeopter.deoptBootMethods(); // do it once for main zygote
        // install bootstrap hooks for main zygote as early as possible
        // in case we miss some processes not forked via forkAndSpecialize
        // for instance com.android.phone
        Router.installBootstrapHooks(true);
        // loadModules have to be executed in zygote even isDynamicModules is false
        // because if not global hooks installed in initZygote might not be
        // propagated to processes not forked via forkAndSpecialize
        Router.loadModulesSafely(true);
        Zygote.closeFilesBeforeFork();
    }

    public static void forkSystemServerPost(int pid) {
        // in system_server process
        Main.setAppDataDir(getDataPathPrefix() + "android");
        Main.niceName = "system_server";
        Router.prepare(true);
        Zygote.reopenFilesAfterFork();
        Router.onEnterChildProcess();
        // reload module list if dynamic mode is on
        Router.loadModulesSafely(false);
        Router.onForkFinish();
    }

}
