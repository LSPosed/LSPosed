package io.github.lsposed.lspd.service;

import android.content.Context;
import android.os.Looper;
import android.util.Log;

public class ServiceManager {
    private static LSPosedService mainService = null;
    private static LSPModuleService moduleService = null;
    private static LSPApplicationService applicationService = null;
    private static LSPManagerService managerService = null;
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

    // call by ourselves
    public static void start() {
        Log.i(TAG, "starting server...");

        Looper.prepare();
        mainService = new LSPosedService();
        moduleService = new LSPModuleService();
        applicationService = new LSPApplicationService();
        managerService = new LSPManagerService();

        android.os.ServiceManager.addService("serial", mainService);

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
        });

        try {
            ConfigManager.getInstance().grantManagerPermission();
        } catch (Throwable e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }

        Looper.loop();

        Log.i(TAG, "server exited");
        System.exit(0);
    }

    public static LSPModuleService getModuleService() {
        return moduleService;
    }

    public static LSPApplicationService getApplicationService() {
        return applicationService;
    }

    public static LSPManagerService getManagerService() {
        return managerService;
    }

}
