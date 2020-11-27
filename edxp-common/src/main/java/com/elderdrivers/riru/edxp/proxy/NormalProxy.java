package com.elderdrivers.riru.edxp.proxy;

import com.elderdrivers.riru.edxp.config.ConfigManager;
import com.elderdrivers.riru.edxp.deopt.PrebuiltMethodsDeopter;

import de.robv.android.xposed.SELinuxHelper;

import static com.elderdrivers.riru.edxp.util.FileUtils.getDataPathPrefix;

public class NormalProxy extends BaseProxy {

    public NormalProxy(Router router) {
        super(router);
    }

    public void forkAndSpecializePre(int uid, int gid, int[] gids, int debugFlags,
                                     int[][] rlimits, int mountExternal, String seInfo,
                                     String niceName, int[] fdsToClose, int[] fdsToIgnore,
                                     boolean startChildZygote, String instructionSet,
                                     String appDataDir) {
    }

    public void forkAndSpecializePost(int pid, String appDataDir, String niceName) {
        SELinuxHelper.initOnce();
        mRouter.initResourcesHook();
        // call this to ensure the flag is set to false ASAP
        mRouter.prepare(false);
        PrebuiltMethodsDeopter.deoptBootMethods(); // do it once for secondary zygote
        // install bootstrap hooks for secondary zygote
        mRouter.installBootstrapHooks(false);
        // TODO consider processes without forkAndSpecializePost being called
        forkPostCommon(pid, false, appDataDir, niceName);
    }

    public void forkSystemServerPre(int uid, int gid, int[] gids, int debugFlags, int[][] rlimits,
                                    long permittedCapabilities, long effectiveCapabilities) {
    }

    public void forkSystemServerPost(int pid) {
        SELinuxHelper.initOnce();
        mRouter.initResourcesHook();
        // set startsSystemServer flag used when loadModules
        mRouter.prepare(true);
        PrebuiltMethodsDeopter.deoptBootMethods(); // do it once for main zygote
        // install bootstrap hooks for main zygote as early as possible
        // in case we miss some processes not forked via forkAndSpecialize
        // for instance com.android.phone
        mRouter.installBootstrapHooks(true);
        // in system_server process
        forkPostCommon(pid, true,
                getDataPathPrefix() + "android", "system_server");
    }


    private void forkPostCommon(int pid, boolean isSystem, String appDataDir, String niceName) {
        ConfigManager.appDataDir = appDataDir;
        ConfigManager.niceName = niceName;
        mRouter.prepare(isSystem);
        mRouter.onEnterChildProcess();
        mRouter.loadModulesSafely(true);
    }

}
