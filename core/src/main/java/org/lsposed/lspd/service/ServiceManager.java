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
 * Copyright (C) 2021 LSPosed Contributors
 */

package org.lsposed.lspd.service;

import android.content.Context;
import android.ddm.DdmHandleAppName;
import android.os.IBinder;
import android.os.IServiceManager;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

import com.android.internal.os.BinderInternal;

import org.lsposed.lspd.BuildConfig;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

import hidden.HiddenApiBridge;

public class ServiceManager {
    private static LSPosedService mainService = null;
    final private static ConcurrentHashMap<String, LSPModuleService> moduleServices = new ConcurrentHashMap<>();
    private static LSPApplicationService applicationService = null;
    private static LSPManagerService managerService = null;
    private static LSPSystemServerService systemServerService = null;
    public static final String TAG = "LSPosedService";
    private static final File globalNamespace = new File("/proc/1/root");

    private static void waitSystemService(String name) {
        while (android.os.ServiceManager.getService(name) == null) {
            try {
                Log.i(TAG, "service " + name + " is not started, wait 1s.");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Log.i(TAG, Log.getStackTraceString(e));
            }
        }
    }

    public static IServiceManager getSystemServiceManager() {
        return IServiceManager.Stub.asInterface(HiddenApiBridge.Binder_allowBlocking(BinderInternal.getContextObject()));
    }

    // call by ourselves
    public static void start(String[] args) {
        if (!ConfigManager.getInstance().tryLock()) System.exit(0);

        for (String arg : args) {
            if (arg.equals("--from-service")) {
                Log.w(TAG, "LSPosed daemon is not started properly. Try for a late start...");
            }
        }
        Log.i(TAG, "starting server...");
        Log.i(TAG, String.format("version %s (%s)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));

        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            Log.e(TAG, "Uncaught exception", e);
            System.exit(1);
        });

        Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND);
        Looper.prepareMainLooper();
        mainService = new LSPosedService();
        applicationService = new LSPApplicationService();
        managerService = new LSPManagerService();
        systemServerService = new LSPSystemServerService();

        systemServerService.putBinderForSystemServer();

        DdmHandleAppName.setAppName("lspd", 0);

        waitSystemService("package");
        waitSystemService("activity");
        waitSystemService(Context.USER_SERVICE);
        waitSystemService(Context.APP_OPS_SERVICE);

        BridgeService.send(mainService, new BridgeService.Listener() {
            @Override
            public void onSystemServerRestarted() {
                Log.w(TAG, "system restarted...");
            }

            @Override
            public void onResponseFromBridgeService(boolean response) {
                if (response) {
                    Log.i(TAG, "sent service to bridge");
                } else {
                    Log.w(TAG, "no response from bridge");
                }
            }

            @Override
            public void onSystemServerDied() {
                Log.w(TAG, "system server died");
                systemServerService.putBinderForSystemServer();
            }
        });

        try {
            ConfigManager.grantManagerPermission();
        } catch (Throwable e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }

        Looper.loop();
        throw new RuntimeException("Main thread loop unexpectedly exited");
    }

    public static LSPModuleService getModuleService(String module) {
        return moduleServices.computeIfAbsent(module, LSPModuleService::new);
    }

    public static LSPApplicationService getApplicationService() {
        return applicationService;
    }

    public static LSPApplicationService requestApplicationService(int uid, int pid, IBinder heartBeat) {
        if (applicationService.registerHeartBeat(uid, pid, heartBeat))
            return applicationService;
        else return null;
    }

    public static LSPManagerService getManagerService() {
        return managerService;
    }

    public static boolean systemServerRequested() {
        return systemServerService.systemServerRequested();
    }

    public static File toGlobalNamespace(File file) {
        return new File(globalNamespace, file.getAbsolutePath());
    }

    public static File toGlobalNamespace(String path) {
        if (path == null) return null;
        if (path.startsWith("/")) return new File(globalNamespace, path);
        else return toGlobalNamespace(new File(path));
    }

    public static boolean existsInGlobalNamespace(File file) {
        return toGlobalNamespace(file).exists();
    }

    public static boolean existsInGlobalNamespace(String path) {
        return toGlobalNamespace(path).exists();
    }
}
