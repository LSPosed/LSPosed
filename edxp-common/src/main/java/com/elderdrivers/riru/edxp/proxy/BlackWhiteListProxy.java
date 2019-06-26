package com.elderdrivers.riru.edxp.proxy;

import android.text.TextUtils;

import com.elderdrivers.riru.edxp.config.ConfigManager;
import com.elderdrivers.riru.edxp.deopt.PrebuiltMethodsDeopter;
import com.elderdrivers.riru.edxp.util.ProcessUtils;
import com.elderdrivers.riru.edxp.util.Utils;

import de.robv.android.xposed.XposedBridge;

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
public class BlackWhiteListProxy extends BaseProxy {

    public BlackWhiteListProxy(Router router) {
        super(router);
    }

    public void forkAndSpecializePre(int uid, int gid, int[] gids, int debugFlags,
                                     int[][] rlimits, int mountExternal, String seInfo,
                                     String niceName, int[] fdsToClose, int[] fdsToIgnore,
                                     boolean startChildZygote, String instructionSet,
                                     String appDataDir) {
        final boolean isDynamicModulesMode = ConfigManager.isDynamicModulesEnabled();
        if (isDynamicModulesMode) {
            onForkPreForDynamicMode(false);
        } else {
            onForkPreForNonDynamicMode(false);
        }
    }

    public void forkAndSpecializePost(int pid, String appDataDir, String niceName) {
        onForkPostCommon(false, appDataDir, niceName);
    }

    public void forkSystemServerPre(int uid, int gid, int[] gids, int debugFlags,
                                    int[][] rlimits, long permittedCapabilities,
                                    long effectiveCapabilities) {
        final boolean isDynamicModulesMode = ConfigManager.isDynamicModulesEnabled();
        if (isDynamicModulesMode) {
            onForkPreForDynamicMode(true);
        } else {
            onForkPreForNonDynamicMode(true);
        }
    }

    public void forkSystemServerPost(int pid) {
        onForkPostCommon(true, getDataPathPrefix() + "android", "system_server");
    }

    private void onForkPreForDynamicMode(boolean isSystemServer) {
        mRouter.onForkStart();
        mRouter.initResourcesHook();
        mRouter.prepare(isSystemServer);
        mRouter.loadModulesSafely(true, false);
    }

    /**
     * Some details are different between main zygote and secondary zygote.
     */
    private void onForkPreForNonDynamicMode(boolean isSystemServer) {
        mRouter.onForkStart();
        mRouter.initResourcesHook();
        // set startsSystemServer flag used when loadModules
        mRouter.prepare(isSystemServer);
        // deoptBootMethods once for all child processes of zygote
        PrebuiltMethodsDeopter.deoptBootMethods();
        // we never install bootstrap hooks here in black/white list mode except workaround hooks
        // because installed hooks would be propagated to all child processes of zygote
        mRouter.startWorkAroundHook();
        // loadModules once for all child processes of zygote
        mRouter.loadModulesSafely(true, false);
    }

    private void onForkPostCommon(boolean isSystemServer, String appDataDir, String niceName) {
        ConfigManager.appDataDir = appDataDir;
        ConfigManager.niceName = niceName;
        final boolean isDynamicModulesMode = ConfigManager.isDynamicModulesEnabled();
        mRouter.onEnterChildProcess();
        if (!checkNeedHook(appDataDir, niceName)) {
            // if is blacklisted, just stop here
            mRouter.onForkFinish();
            return;
        }
        if (isDynamicModulesMode) {
            mRouter.initResourcesHook();
        }
        mRouter.prepare(isSystemServer);
        PrebuiltMethodsDeopter.deoptBootMethods();
        mRouter.installBootstrapHooks(isSystemServer);

        // under dynamic modules mode, don't call initZygote when loadModule
        // cuz loaded module won't has that chance to do it
        if (isDynamicModulesMode) {
            mRouter.loadModulesSafely(false, false);
        }
        // call all initZygote callbacks
        XposedBridge.callInitZygotes();

        mRouter.onForkFinish();
    }

    private boolean checkNeedHook(String appDataDir, String niceName) {
        boolean needHook;
        if (TextUtils.isEmpty(appDataDir)) {
            Utils.logE("niceName:" + niceName + ", procName:"
                    + ProcessUtils.getCurrentProcessName(ConfigManager.appProcessName) + ", appDataDir is null, blacklisted!");
            needHook = false;
        } else {
            // FIXME some process cannot read app_data_file because of MLS, e.g. bluetooth
            needHook = ConfigManager.isAppNeedHook(appDataDir);
        }
        if (!needHook) {
            // clean up the scene
            onBlackListed();
        }
        return needHook;
    }
}
