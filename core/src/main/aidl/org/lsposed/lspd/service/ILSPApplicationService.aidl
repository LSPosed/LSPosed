package org.lsposed.lspd.service;

import org.lsposed.lspd.models.Module;

interface ILSPApplicationService {
    IBinder requestModuleBinder(String name);

    boolean requestManagerBinder(String packageName, String path, out List<IBinder> binder);

    boolean isResourcesHookEnabled();

    List<Module> getModulesList(String processName);

    String getPrefsPath(String packageName);

    ParcelFileDescriptor getModuleLogger();

    Bundle requestRemotePreference(String packageName, int userId, IBinder callback);
}
