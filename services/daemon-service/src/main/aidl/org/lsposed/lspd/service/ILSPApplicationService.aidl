package org.lsposed.lspd.service;

import org.lsposed.lspd.models.Module;

interface ILSPApplicationService {
    IBinder requestModuleBinder(String name);

    List<Module> getLegacyModulesList();

    List<Module> getModulesList();

    String getPrefsPath(String packageName);

    Bundle requestRemotePreferences(String packageName, int userId, String group, IBinder callback);

    ParcelFileDescriptor requestInjectedManagerBinder(out List<IBinder> binder);
}
