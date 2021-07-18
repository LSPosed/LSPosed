package org.lsposed.lspd.service;

interface ILSPApplicationService {
    IBinder requestModuleBinder();

    boolean requestManagerBinder(String packageName, String path, out IBinder[] binder);

    boolean isResourcesHookEnabled();

    Map getModulesList(String processName);

    String getPrefsPath(String packageName);

    ParcelFileDescriptor getModuleLogger();
}
