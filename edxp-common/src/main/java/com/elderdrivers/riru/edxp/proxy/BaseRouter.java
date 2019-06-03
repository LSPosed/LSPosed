package com.elderdrivers.riru.edxp.proxy;

import android.text.TextUtils;

import com.elderdrivers.riru.edxp._hooker.yahfa.SystemMainHooker;
import com.elderdrivers.riru.edxp.core.yahfa.HookMain;
import com.elderdrivers.riru.edxp.entry.yahfa.AppBootstrapHookInfo;
import com.elderdrivers.riru.edxp.entry.yahfa.SysBootstrapHookInfo;
import com.elderdrivers.riru.edxp.entry.yahfa.SysInnerHookInfo;
import com.elderdrivers.riru.edxp.entry.yahfa.WorkAroundHookInfo;
import com.elderdrivers.riru.edxp.util.Utils;

import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedInit;

public abstract class BaseRouter implements Router {

    protected volatile boolean forkCompleted = false;

    protected volatile AtomicBoolean bootstrapHooked = new AtomicBoolean(false);

    public void initResourcesHook() {
        startWorkAroundHook(); // for OnePlus devices
        XposedBridge.initXResources();
    }

    public void prepare(boolean isSystem) {
        // this flag is needed when loadModules
        XposedInit.startsSystemServer = isSystem;
    }

    public void onForkStart() {
        forkCompleted = false;
    }

    public void onForkFinish() {
        forkCompleted = true;
    }

    public boolean isForkCompleted() {
        return forkCompleted;
    }

    public void installBootstrapHooks(boolean isSystem) {
        // Initialize the Xposed framework
        try {
            if (!bootstrapHooked.compareAndSet(false, true)) {
                return;
            }
            startBootstrapHook(isSystem);
            XposedInit.initForZygote(isSystem);
        } catch (Throwable t) {
            Utils.logE("error during Xposed initialization", t);
            XposedBridge.disableHooks = true;
        }
    }

    public void loadModulesSafely(boolean isInZygote) {
        try {
            // FIXME some coredomain app can't reading modules.list
            XposedInit.loadModules(isInZygote);
        } catch (Exception exception) {
            Utils.logE("error loading module list", exception);
        }
    }

    public String parsePackageName(String appDataDir) {
        if (TextUtils.isEmpty(appDataDir)) {
            return "";
        }
        int lastIndex = appDataDir.lastIndexOf("/");
        if (lastIndex < 1) {
            return "";
        }
        return appDataDir.substring(lastIndex + 1);
    }


    public void startBootstrapHook(boolean isSystem) {
        Utils.logD("startBootstrapHook starts: isSystem = " + isSystem);
        ClassLoader classLoader = XposedBridge.BOOTCLASSLOADER;
        if (isSystem) {
            HookMain.doHookDefault(
                    BaseRouter.class.getClassLoader(),
                    classLoader,
                    SysBootstrapHookInfo.class.getName());
        } else {
            HookMain.doHookDefault(
                    BaseRouter.class.getClassLoader(),
                    classLoader,
                    AppBootstrapHookInfo.class.getName());
        }
    }

    public void startSystemServerHook() {
        HookMain.doHookDefault(
                BaseRouter.class.getClassLoader(),
                SystemMainHooker.systemServerCL,
                SysInnerHookInfo.class.getName());
    }

    public void startWorkAroundHook() {
        HookMain.doHookDefault(
                BaseRouter.class.getClassLoader(),
                XposedBridge.BOOTCLASSLOADER,
                WorkAroundHookInfo.class.getName());
    }
}
