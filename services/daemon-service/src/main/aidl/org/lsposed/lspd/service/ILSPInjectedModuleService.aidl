package org.lsposed.lspd.service;

import org.lsposed.lspd.service.IRemotePreferenceCallback;

interface ILSPInjectedModuleService {
    Bundle requestRemotePreferences(String group, IRemotePreferenceCallback callback);
}
