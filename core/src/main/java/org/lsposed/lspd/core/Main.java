/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2020 EdXposed Contributors
 * Copyright (C) 2021 LSPosed Contributors
 */

package org.lsposed.lspd.core;

import android.annotation.SuppressLint;
import android.app.ActivityThread;
import android.content.pm.ApplicationInfo;
import android.content.res.CompatibilityInfo;
import android.ddm.DdmHandleAppName;
import android.os.Environment;
import android.os.IBinder;

import java.io.File;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedInit;
import org.lsposed.lspd.config.LSPApplicationServiceClient;
import org.lsposed.lspd.deopt.PrebuiltMethodsDeopter;
import org.lsposed.lspd.hooker.HandleBindAppHooker;
import org.lsposed.lspd.hooker.LoadedApkCstrHooker;
import org.lsposed.lspd.hooker.StartBootstrapServicesHooker;
import org.lsposed.lspd.hooker.SystemMainHooker;
import org.lsposed.lspd.nativebridge.ModuleLogger;
import org.lsposed.lspd.service.ServiceManager;
import org.lsposed.lspd.util.Utils;
import org.lsposed.lspd.util.Versions;
import org.lsposed.lspd.yahfa.hooker.YahfaHooker;

import static org.lsposed.lspd.config.LSPApplicationServiceClient.serviceClient;

@SuppressLint("DefaultLocale")
public class Main {
    public static void startBootstrapHook(boolean isSystem, String appDataDir) {
        Utils.logD("startBootstrapHook starts: isSystem = " + isSystem);
        ClassLoader classLoader = Main.class.getClassLoader();
        if (isSystem) {
            XposedHelpers.findAndHookMethod("android.app.ActivityThread", classLoader,
                    "systemMain", new SystemMainHooker());
        }
        XposedHelpers.findAndHookMethod("android.app.ActivityThread", classLoader,
                "handleBindApplication",
                "android.app.ActivityThread$AppBindData",
                new HandleBindAppHooker(appDataDir));
        XposedHelpers.findAndHookConstructor("android.app.LoadedApk", classLoader,
                ActivityThread.class, ApplicationInfo.class, CompatibilityInfo.class,
                ClassLoader.class, boolean.class, boolean.class, boolean.class,
                new LoadedApkCstrHooker());
    }

    public static void startSystemServerHook() {
        StartBootstrapServicesHooker sbsHooker = new StartBootstrapServicesHooker();
        Object[] paramTypesAndCallback = Versions.hasR() ?
                new Object[]{"com.android.server.utils.TimingsTraceAndSlog", sbsHooker} :
                new Object[]{sbsHooker};
        XposedHelpers.findAndHookMethod("com.android.server.SystemServer",
                SystemMainHooker.systemServerCL,
                "startBootstrapServices", paramTypesAndCallback);
    }
    private static void installBootstrapHooks(boolean isSystem, String appDataDir) {
        // Initialize the Xposed framework
        try {
            startBootstrapHook(isSystem, appDataDir);
            XposedInit.hookResources();
        } catch (Throwable t) {
            Utils.logE("error during Xposed initialization", t);
        }
    }

    private static void loadModulesSafely() {
        try {
            XposedInit.loadModules();
        } catch (Exception exception) {
            Utils.logE("error loading module list", exception);
        }
    }
    private static void forkPostCommon(boolean isSystem, String appDataDir, String niceName) {
        // init logger
        YahfaHooker.init();
        ModuleLogger.initLogger(serviceClient.getModuleLogger());
        XposedBridge.initXResources();
        XposedInit.startsSystemServer = isSystem;
        PrebuiltMethodsDeopter.deoptBootMethods(); // do it once for secondary zygote
        installBootstrapHooks(isSystem, appDataDir);
        Utils.logI("Loading modules for " + niceName);
        loadModulesSafely();
    }

    public static void forkAndSpecializePost(String appDataDir, String niceName, IBinder binder) {
        LSPApplicationServiceClient.Init(binder, niceName);
        forkPostCommon(false, appDataDir, niceName);
    }

    public static void forkSystemServerPost(IBinder binder) {
        LSPApplicationServiceClient.Init(binder, "android");
        forkPostCommon(true,
                new File(Environment.getDataDirectory(), "android").toString(), "system_server");
    }

    public static void main(String[] args) {
        for (String arg : args) {
            if (arg.equals("--debug")) {
                DdmHandleAppName.setAppName("lspd", 0);
            }
        }
        ServiceManager.start();
    }
}
