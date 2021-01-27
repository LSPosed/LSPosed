package com.elderdrivers.riru.edxp.proxy;

import com.elderdrivers.riru.edxp.config.ConfigManager;
import com.elderdrivers.riru.edxp.deopt.PrebuiltMethodsDeopter;
import com.elderdrivers.riru.edxp.util.Utils;

import de.robv.android.xposed.SELinuxHelper;
import de.robv.android.xposed.XposedInit;

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
        forkPostCommon(pid, false, appDataDir, niceName);
    }

    public void forkSystemServerPre(int uid, int gid, int[] gids, int debugFlags, int[][] rlimits,
                                    long permittedCapabilities, long effectiveCapabilities) {
    }

    public void forkSystemServerPost(int pid) {
        forkPostCommon(pid, true,
                getDataPathPrefix() + "android", "system_server");
    }


    private void forkPostCommon(int pid, boolean isSystem, String appDataDir, String niceName) {
        SELinuxHelper.initOnce();
        mRouter.initResourcesHook();
        mRouter.prepare(isSystem);
        PrebuiltMethodsDeopter.deoptBootMethods(); // do it once for secondary zygote
        ConfigManager.appDataDir = appDataDir;
        ConfigManager.niceName = niceName;
        mRouter.installBootstrapHooks(isSystem);
        XposedInit.prefsBasePath = ConfigManager.getPrefsPath("");
        mRouter.onEnterChildProcess();
        Utils.logI("Loading modules for " + niceName);
        mRouter.loadModulesSafely(true);
    }

}
