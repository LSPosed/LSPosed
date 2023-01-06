package org.lsposed.lspd.service;

import static org.lsposed.lspd.service.PackageService.PER_USER_RANGE;

import android.os.Binder;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import org.lsposed.lspd.models.Module;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.github.libxposed.service.IXposedService;

public class LSPInjectedModuleService extends ILSPInjectedModuleService.Stub {
    private final Module loadedModule;

    Map<String, Set<IRemotePreferenceCallback>> callbacks = new ConcurrentHashMap<>();

    LSPInjectedModuleService(Module module) {
        loadedModule = module;
    }

    @Override
    public int getFrameworkPrivilege() {
        return IXposedService.FRAMEWORK_PRIVILEGE_ROOT;
    }

    @Override
    public Bundle requestRemotePreferences(String group, IRemotePreferenceCallback callback) {
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

    @Override
    public ParcelFileDescriptor openRemoteFile(String path) throws RemoteException {
        try {
            var absolutePath = ConfigFileManager.resolveModulePath(loadedModule.packageName, path);
            return ParcelFileDescriptor.open(absolutePath.toFile(), ParcelFileDescriptor.MODE_READ_ONLY);
        } catch (Throwable e) {
            throw new RemoteException(e.getMessage());
        }
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
