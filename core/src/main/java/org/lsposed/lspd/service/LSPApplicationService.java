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

import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;
import android.util.Pair;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.lsposed.lspd.service.ServiceManager.TAG;

public class LSPApplicationService extends ILSPApplicationService.Stub {
    // <uid, pid>
    private final static Set<Pair<Integer, Integer>> cache = ConcurrentHashMap.newKeySet();
    private final static Set<IBinder> handles = ConcurrentHashMap.newKeySet();

    public boolean registerHeartBeat(int uid, int pid, IBinder handle) {
        try {
            handle.linkToDeath(new DeathRecipient() {
                @Override
                public void binderDied() {
                    Log.d(TAG, "pid=" + pid + " uid=" + uid + " is dead.");
                    cache.remove(new Pair<>(uid, pid));
                    handles.remove(handle);
                    handle.unlinkToDeath(this, 0);
                }
            }, 0);
            handles.add(handle);
            cache.add(new Pair<>(uid, pid));
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return false;
        }
    }

    @Override
    public boolean isResourcesHookEnabled() throws RemoteException {
        ensureRegistered();
        return ConfigManager.getInstance().resourceHook();
    }

    @Override
    public String[] getModulesList(String processName) throws RemoteException {
        ensureRegistered();
        int callingUid = Binder.getCallingUid();
        if (callingUid == 1000 && processName.equals("android")) {
            return ConfigManager.getInstance().getModulesPathForSystemServer();
        }
        return ConfigManager.getInstance().getModulesPathForProcess(processName, callingUid);
    }

    @Override
    public String getPrefsPath(String packageName) throws RemoteException {
        ensureRegistered();
        return ConfigManager.getInstance().getPrefsPath(packageName);
    }

    @Override
    public String getCachePath(String fileName) throws RemoteException {
        ensureRegistered();
        return ConfigManager.getInstance().getCachePath(fileName);
    }

    @Override
    public ParcelFileDescriptor getModuleLogger() throws RemoteException {
        ensureRegistered();
        return ConfigManager.getInstance().getModulesLog(ParcelFileDescriptor.MODE_WRITE_ONLY | ParcelFileDescriptor.MODE_APPEND);
    }

    @Override
    public IBinder requestModuleBinder() throws RemoteException {
        ensureRegistered();
        if (ConfigManager.getInstance().isModule(Binder.getCallingUid())) {
            ConfigManager.getInstance().ensureModulePrefsPermission(Binder.getCallingUid());
            return ServiceManager.getModuleService();
        } else return null;
    }

    @Override
    public IBinder requestManagerBinder() throws RemoteException {
        ensureRegistered();
        if (ConfigManager.getInstance().isManager(Binder.getCallingUid()))
            return ServiceManager.getManagerService();
        return null;
    }

    public boolean hasRegister(int uid, int pid) {
        return cache.contains(new Pair<>(uid, pid));
    }

    private void ensureRegistered() throws RemoteException {
        if (!hasRegister(Binder.getCallingUid(), Binder.getCallingPid()))
            throw new RemoteException("Not registered");
    }
}
