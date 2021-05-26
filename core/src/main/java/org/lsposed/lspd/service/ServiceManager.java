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
import android.util.Log;

import com.android.internal.os.BinderInternal;

import org.lsposed.lspd.BuildConfig;

import hidden.HiddenApiBridge;

public class ServiceManager {
    private static LSPosedService mainService = null;
    private static LSPModuleService moduleService = null;
    private static LSPApplicationService applicationService = null;
    private static LSPManagerService managerService = null;
    private static LSPSystemServerService systemServerService = null;
    public static final String TAG = "LSPosedService";

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
            if (arg.equals("--debug")) {
                DdmHandleAppName.setAppName("lspd", 0);
            }
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

        Looper.prepareMainLooper();
        mainService = new LSPosedService();
        moduleService = new LSPModuleService();
        applicationService = new LSPApplicationService();
        managerService = new LSPManagerService();
        systemServerService = new LSPSystemServerService();

        systemServerService.putBinderForSystemServer();

        android.os.Process.killProcess(android.system.Os.getppid());

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

        //noinspection InfiniteLoopStatement
        while (true) {
            try {
                Looper.loop();
            } catch (Throwable e) {
                Log.i(TAG, "server exited with " + Log.getStackTraceString(e));
                Log.i(TAG, "restarting");
            }
        }
    }

    public static LSPModuleService getModuleService() {
        return moduleService;
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
}
