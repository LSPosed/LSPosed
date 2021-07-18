package org.lsposed.lspd.service;

interface ILSPApplicationService {
    IBinder requestModuleBinder();

    //TODO: after array copy bug fixed, use array instead of list
    boolean requestManagerBinder(String packageName, String path, out List<IBinder> binder);

    boolean isResourcesHookEnabled();

    Map getModulesList(String processName);

    String getPrefsPath(String packageName);

    ParcelFileDescriptor getModuleLogger();
}
