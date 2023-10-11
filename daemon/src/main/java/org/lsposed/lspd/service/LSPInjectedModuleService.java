package org.lsposed.lspd.service;

import static org.lsposed.lspd.service.LSPModuleService.FILES_DIR;
import static org.lsposed.lspd.service.PackageService.PER_USER_RANGE;

import android.os.Binder;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import org.lsposed.lspd.models.Module;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.github.libxposed.service.IXposedService;

public class LSPInjectedModuleService extends ILSPInjectedModuleService.Stub {

    private static final String TAG = "LSPosedInjectedModuleService";

    private final String mPackageName;

    Map<String, Set<IRemotePreferenceCallback>> callbacks = new ConcurrentHashMap<>();

    LSPInjectedModuleService(String packageName) {
        mPackageName = packageName;
    }

    @Override
    public int getFrameworkPrivilege() {
        return IXposedService.FRAMEWORK_PRIVILEGE_ROOT;
    }

    @Override
    public Bundle requestRemotePreferences(String group, IRemotePreferenceCallback callback) {
        var bundle = new Bundle();
        var userId = Binder.getCallingUid() / PER_USER_RANGE;
        bundle.putSerializable("map", ConfigManager.getInstance().getModulePrefs(mPackageName, userId, group));
        if (callback != null) {
            var groupCallbacks = callbacks.computeIfAbsent(group, k -> ConcurrentHashMap.newKeySet());
            groupCallbacks.add(callback);
            try {
                callback.asBinder().linkToDeath(() -> groupCallbacks.remove(callback), 0);
            } catch (RemoteException e) {
                Log.w(TAG, "requestRemotePreferences: ", e);
            }
        }
        return bundle;
    }

    @Override
    public ParcelFileDescriptor openRemoteFile(String path) throws RemoteException {
        ConfigFileManager.ensureModuleFilePath(path);
        var userId = Binder.getCallingUid() / PER_USER_RANGE;
        try {
            var dir = ConfigFileManager.resolveModuleDir(mPackageName, FILES_DIR, userId, -1);
            return ParcelFileDescriptor.open(dir.resolve(path).toFile(), ParcelFileDescriptor.MODE_READ_ONLY);
        } catch (Throwable e) {
            throw new RemoteException(e.getMessage());
        }
    }

    @Override
    public String[] getRemoteFileList() throws RemoteException {
        var userId = Binder.getCallingUid() / PER_USER_RANGE;
        try {
            var dir = ConfigFileManager.resolveModuleDir(mPackageName, FILES_DIR, userId, -1);
            var files = dir.toFile().list();
            return files == null ? new String[0] : files;

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
