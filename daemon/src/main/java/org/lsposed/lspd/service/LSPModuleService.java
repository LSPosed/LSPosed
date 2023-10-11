/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2021 LSPosed Contributors
 */

package org.lsposed.lspd.service;

import static org.lsposed.lspd.service.PackageService.PER_USER_RANGE;

import android.content.AttributionSource;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.NonNull;

import org.lsposed.daemon.BuildConfig;
import org.lsposed.lspd.models.Module;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import io.github.libxposed.service.IXposedScopeCallback;
import io.github.libxposed.service.IXposedService;

public class LSPModuleService extends IXposedService.Stub {

    private final static String TAG = "LSPosedModuleService";

    private final static Set<Integer> uidSet = ConcurrentHashMap.newKeySet();
    private final static Map<Module, LSPModuleService> serviceMap = Collections.synchronizedMap(new WeakHashMap<>());

    public final static String FILES_DIR = "files";

    private final @NonNull
    Module loadedModule;

    static void uidClear() {
        uidSet.clear();
    }

    static void uidStarts(int uid) {
        if (!uidSet.contains(uid)) {
            uidSet.add(uid);
            var module = ConfigManager.getInstance().getModule(uid);
            if (module != null && module.file != null && !module.file.legacy) {
                var service = serviceMap.computeIfAbsent(module, LSPModuleService::new);
                service.sendBinder(uid);
            }
        }
    }

    static void uidGone(int uid) {
        uidSet.remove(uid);
    }

    private void sendBinder(int uid) {
        var name = loadedModule.packageName;
        try {
            int userId = uid / PackageService.PER_USER_RANGE;
            var authority = name + AUTHORITY_SUFFIX;
            var provider = ActivityManagerService.getContentProvider(authority, userId);
            if (provider == null) {
                Log.d(TAG, "no service provider for " + name);
                return;
            }
            var extra = new Bundle();
            extra.putBinder("binder", asBinder());
            Bundle reply = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                reply = provider.call(new AttributionSource.Builder(1000).setPackageName("android").build(), authority, SEND_BINDER, null, extra);
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
                reply = provider.call("android", null, authority, SEND_BINDER, null, extra);
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                reply = provider.call("android", authority, SEND_BINDER, null, extra);
            } else {
                reply = provider.call("android", SEND_BINDER, null, extra);
            }
            if (reply != null) {
                Log.d(TAG, "sent module binder to " + name);
            } else {
                Log.w(TAG, "failed to send module binder to " + name);
            }
        } catch (Throwable e) {
            Log.w(TAG, "failed to send module binder for uid " + uid, e);
        }
    }

    LSPModuleService(@NonNull Module module) {
        loadedModule = module;
    }

    private int ensureModule() throws RemoteException {
        var appId = Binder.getCallingUid() % PER_USER_RANGE;
        if (loadedModule.appId != appId) {
            throw new RemoteException("Module " + loadedModule.packageName + " is not for uid " + Binder.getCallingUid());
        }
        return Binder.getCallingUid() / PER_USER_RANGE;
    }

    @Override
    public int getAPIVersion() throws RemoteException {
        ensureModule();
        return API;
    }

    @Override
    public String getFrameworkName() throws RemoteException {
        ensureModule();
        return "LSPosed";
    }

    @Override
    public String getFrameworkVersion() throws RemoteException {
        ensureModule();
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public long getFrameworkVersionCode() throws RemoteException {
        ensureModule();
        return BuildConfig.VERSION_CODE;
    }

    @Override
    public int getFrameworkPrivilege() throws RemoteException {
        ensureModule();
        return IXposedService.FRAMEWORK_PRIVILEGE_ROOT;
    }

    @Override
    public List<String> getScope() throws RemoteException {
        ensureModule();
        ArrayList<String> res = new ArrayList<>();
        var scope = ConfigManager.getInstance().getModuleScope(loadedModule.packageName);
        if (scope == null) return res;
        for (var s : scope) {
            res.add(s.packageName);
        }
        return res;
    }

    @Override
    public void requestScope(String packageName, IXposedScopeCallback callback) throws RemoteException {
        var userId = ensureModule();
        if (ConfigManager.getInstance().scopeRequestBlocked(loadedModule.packageName)) {
            callback.onScopeRequestDenied(packageName);
        } else {
            LSPNotificationManager.requestModuleScope(loadedModule.packageName, userId, packageName, callback);
            callback.onScopeRequestPrompted(packageName);
        }
    }

    @Override
    public String removeScope(String packageName) throws RemoteException {
        var userId = ensureModule();
        try {
            if (!ConfigManager.getInstance().removeModuleScope(loadedModule.packageName, packageName, userId)) {
                return "Invalid request";
            }
            return null;
        } catch (Throwable e) {
            return e.getMessage();
        }
    }

    @Override
    public Bundle requestRemotePreferences(String group) throws RemoteException {
        var userId = ensureModule();
        var bundle = new Bundle();
        bundle.putSerializable("map", ConfigManager.getInstance().getModulePrefs(loadedModule.packageName, userId, group));
        return bundle;
    }

    @Override
    public void updateRemotePreferences(String group, Bundle diff) throws RemoteException {
        var userId = ensureModule();
        Map<String, Object> values = new ArrayMap<>();
        if (diff.containsKey("delete")) {
            var deletes = (Set<?>) diff.getSerializable("delete");
            for (var key : deletes) {
                values.put((String) key, null);
            }
        }
        if (diff.containsKey("put")) {
            try {
                var puts = (Map<?, ?>) diff.getSerializable("put");
                for (var entry : puts.entrySet()) {
                    values.put((String) entry.getKey(), entry.getValue());
                }
            } catch (Throwable e) {
                Log.e(TAG, "updateRemotePreferences: ", e);
            }
        }
        try {
            ConfigManager.getInstance().updateModulePrefs(loadedModule.packageName, userId, group, values);
            ((LSPInjectedModuleService) loadedModule.service).onUpdateRemotePreferences(group, diff);
        } catch (Throwable e) {
            throw new RemoteException(e.getMessage());
        }
    }

    @Override
    public void deleteRemotePreferences(String group) throws RemoteException {
        var userId = ensureModule();
        ConfigManager.getInstance().deleteModulePrefs(loadedModule.packageName, userId, group);
    }

    @Override
    public String[] listRemoteFiles() throws RemoteException {
        var userId = ensureModule();
        try {
            var dir = ConfigFileManager.resolveModuleDir(loadedModule.packageName, FILES_DIR, userId, Binder.getCallingUid());
            var files = dir.toFile().list();
            return files == null ? new String[0] : files;
        } catch (IOException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    @Override
    public ParcelFileDescriptor openRemoteFile(String path) throws RemoteException {
        var userId = ensureModule();
        ConfigFileManager.ensureModuleFilePath(path);
        try {
            var dir = ConfigFileManager.resolveModuleDir(loadedModule.packageName, FILES_DIR, userId, Binder.getCallingUid());
            return ParcelFileDescriptor.open(dir.resolve(path).toFile(), ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_READ_WRITE);
        } catch (IOException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    @Override
    public boolean deleteRemoteFile(String path) throws RemoteException {
        var userId = ensureModule();
        ConfigFileManager.ensureModuleFilePath(path);
        try {
            var dir = ConfigFileManager.resolveModuleDir(loadedModule.packageName, FILES_DIR, userId, Binder.getCallingUid());
            return dir.resolve(path).toFile().delete();
        } catch (IOException e) {
            throw new RemoteException(e.getMessage());
        }
    }
}
