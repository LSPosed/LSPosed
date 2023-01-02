package org.lsposed.lspd.service;

import static org.lsposed.lspd.service.PackageService.PER_USER_RANGE;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import org.lsposed.lspd.models.Module;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class LSPInjectedModuleService extends ILSPInjectedModuleService.Stub {
    private final Module loadedModule;
    private final LSPModuleService moduleService;

    Map<String, Set<IRemotePreferenceCallback>> callbacks = new ConcurrentHashMap<>();

    LSPInjectedModuleService(Module module) {
        loadedModule = module;
        moduleService = new LSPModuleService(module);
    }

    LSPModuleService getModuleService() {
        return moduleService;
    }

    @Override
    public Bundle requestRemotePreferences(String group, IRemotePreferenceCallback callback) throws RemoteException {
        var bundle = new Bundle();
        var userId = Binder.getCallingUid() % PER_USER_RANGE;
        bundle.putSerializable("map", ConfigManager.getInstance().getModulePrefs(loadedModule.packageName, userId, group));
        if (callback != null) {
            var groupCallbacks = callbacks.computeIfAbsent(group, k -> ConcurrentHashMap.newKeySet());
            groupCallbacks.add(callback);
            callback.asBinder().unlinkToDeath(() -> groupCallbacks.remove(callback), 0);
        }
        return bundle;
    }

    void onUpdateRemotePreferences(String group, Bundle diff) {
        var groupCallbacks = callbacks.get(group);
        if (groupCallbacks != null) {
            for (var callback : groupCallbacks) {
                try {
                    callback.onUpdate(diff);
                } catch (RemoteException e) {
                    groupCallbacks.remove(callback);
                }
            }
        }
    }
}
