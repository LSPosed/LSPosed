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

import static org.lsposed.lspd.service.ServiceManager.TAG;

import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;
import android.util.Pair;

import org.lsposed.lspd.util.Utils;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class LSPApplicationService extends ILSPApplicationService.Stub {
    // <uid, pid>
    private final static Set<Pair<Integer, Integer>> cache = ConcurrentHashMap.newKeySet();
    private final static Map<Integer, IBinder> handles = new ConcurrentHashMap<>();
    private final static Set<IBinder.DeathRecipient> recipients = ConcurrentHashMap.newKeySet();

    public boolean registerHeartBeat(int uid, int pid, IBinder handle) {
        try {
            var recipient = new DeathRecipient() {
                @Override
                public void binderDied() {
                    Log.d(TAG, "pid=" + pid + " uid=" + uid + " is dead.");
                    cache.remove(new Pair<>(uid, pid));
                    handles.remove(pid, handle);
                    handle.unlinkToDeath(this, 0);
                    recipients.remove(this);
                }
            };
            recipients.add(recipient);
            handle.linkToDeath(recipient, 0);
            handles.put(pid, handle);
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
    public Map<String, String> getModulesList(String processName) throws RemoteException {
        ensureRegistered();
        int callingUid = getCallingUid();
        if (callingUid == 1000 && processName.equals("android")) {
            return ConfigManager.getInstance().getModulesForSystemServer();
        }
        return ConfigManager.getInstance().getModulesForProcess(processName, callingUid);
    }

    @Override
    public String getPrefsPath(String packageName) throws RemoteException {
        ensureRegistered();
        return ConfigManager.getInstance().getPrefsPath(packageName, getCallingUid());
    }

    @Override
    public ParcelFileDescriptor getModuleLogger() throws RemoteException {
        ensureRegistered();
        return ConfigManager.getInstance().getModulesLog(ParcelFileDescriptor.MODE_WRITE_ONLY | ParcelFileDescriptor.MODE_APPEND);
    }

    @Override
    public IBinder requestModuleBinder() throws RemoteException {
        ensureRegistered();
        if (ConfigManager.getInstance().isModule(getCallingUid())) {
            ConfigManager.getInstance().ensureModulePrefsPermission(getCallingUid());
            return ServiceManager.getModuleService();
        }
        return null;
    }

    @Override
    public IBinder requestManagerBinder(String packageName) throws RemoteException {
        ensureRegistered();
        if (ConfigManager.getInstance().isManager(getCallingUid()) && ConfigManager.getInstance().isManager(packageName)) {
            var service = ServiceManager.getManagerService();
            if (Utils.isMIUI) {
                service.new ManagerGuard(handles.get(getCallingPid()));
            }
            return service;
        }
        return null;
    }

    public boolean hasRegister(int uid, int pid) {
        return cache.contains(new Pair<>(uid, pid));
    }

    private void ensureRegistered() throws RemoteException {
        if (!hasRegister(getCallingUid(), getCallingPid()))
            throw new RemoteException("Not registered");
    }
}
