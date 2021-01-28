package io.github.lsposed.lspd.proxy;

import android.app.ActivityThread;
import android.content.pm.ApplicationInfo;
import android.content.res.CompatibilityInfo;
import android.text.TextUtils;

import io.github.lsposed.lspd._hooker.impl.HandleBindApp;
import io.github.lsposed.lspd._hooker.impl.LoadedApkCstr;
import io.github.lsposed.lspd._hooker.impl.StartBootstrapServices;
import io.github.lsposed.lspd._hooker.impl.SystemMain;
import io.github.lsposed.lspd.util.Utils;
import io.github.lsposed.lspd.util.Versions;

import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedInit;
import de.robv.android.xposed.annotation.ApiSensitive;
import de.robv.android.xposed.annotation.Level;

public abstract class BaseRouter implements Router {

    protected volatile AtomicBoolean bootstrapHooked = new AtomicBoolean(false);

    public void initResourcesHook() {
        XposedBridge.initXResources();
    }

    public void prepare(boolean isSystem) {
        // this flag is needed when loadModules
        XposedInit.startsSystemServer = isSystem;
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

    public void loadModulesSafely(boolean callInitZygote) {
        try {
            XposedInit.loadModules(callInitZygote);
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


    @ApiSensitive(Level.LOW)
    public void startBootstrapHook(boolean isSystem) {
        Utils.logD("startBootstrapHook starts: isSystem = " + isSystem);
        ClassLoader classLoader = BaseRouter.class.getClassLoader();
        if (isSystem) {
            XposedHelpers.findAndHookMethod("android.app.ActivityThread", classLoader,
                    "systemMain", new SystemMain());
        }
        XposedHelpers.findAndHookMethod("android.app.ActivityThread", classLoader,
                "handleBindApplication",
                "android.app.ActivityThread$AppBindData",
                new HandleBindApp());
        XposedHelpers.findAndHookConstructor("android.app.LoadedApk", classLoader,
                ActivityThread.class, ApplicationInfo.class, CompatibilityInfo.class,
                ClassLoader.class, boolean.class, boolean.class, boolean.class,
                new LoadedApkCstr());
    }

    public void startSystemServerHook() {
        StartBootstrapServices sbsHooker = new StartBootstrapServices();
        Object[] paramTypesAndCallback = Versions.hasR() ?
                new Object[]{"com.android.server.utils.TimingsTraceAndSlog", sbsHooker} :
                new Object[]{sbsHooker};
        XposedHelpers.findAndHookMethod("com.android.server.SystemServer",
                SystemMain.systemServerCL,
                "startBootstrapServices", paramTypesAndCallback);
    }
}
