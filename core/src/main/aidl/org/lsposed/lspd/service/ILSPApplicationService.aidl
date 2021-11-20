package org.lsposed.lspd.service;

import org.lsposed.lspd.models.Module;

interface ILSPApplicationService {
    List<Module> getModulesList(String processName);

    String getPrefsPath(String packageName);

    Bundle requestRemotePreference(String packageName, int userId, IBinder callback);

    ParcelFileDescriptor requestInjectedManagerBinder(out List<IBinder> binder);

    boolean isSELinuxEnabled();

    boolean isSELinuxEnforced();

    String getSELinuxContext();
}
