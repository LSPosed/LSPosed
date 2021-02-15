package io.github.lsposed.lspd.service;

import android.os.Looper;
import android.util.Log;

public class ServiceManager {
    private static LSPosedService mainService = null;
    private static LSPModuleService moduleService = null;
    private static LSPApplicationService applicationService = null;
    private static LSPManagerService managerService = null;
    public static final String TAG = "LSPosedService";
    // call by ourselves
    public static void start() {
        Log.i(TAG, "starting server...");

        Looper.prepare();
        mainService = new LSPosedService();
        moduleService = new LSPModuleService();
        applicationService = new LSPApplicationService();
        managerService = new LSPManagerService();
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
