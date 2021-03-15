package org.lsposed.lspd.service;

interface ILSPApplicationService {
    IBinder requestModuleBinder() = 2;

    IBinder requestManagerBinder() = 3;

    boolean isResourcesHookEnabled() = 5;

    String[] getModulesList(String processName) = 6;

    String getPrefsPath(String packageName) = 7;

    String getCachePath(String fileName) = 8;

    ParcelFileDescriptor getModuleLogger() = 9;
}
