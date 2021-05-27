package org.lsposed.lspd.service;

interface ILSPApplicationService {
    IBinder requestModuleBinder() = 2;

    IBinder requestManagerBinder(String packageName) = 3;

    boolean isResourcesHookEnabled() = 5;

    Map getModulesList(String processName) = 6;

    String getPrefsPath(String packageName) = 7;

    ParcelFileDescriptor getModuleLogger() = 9;
}
