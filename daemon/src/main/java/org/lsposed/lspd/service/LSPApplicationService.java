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
 * Copyright (C) 2021 - 2022 LSPosed Contributors
 */

package org.lsposed.lspd.service;

import static org.lsposed.lspd.service.ServiceManager.TAG;

import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;
import android.util.Pair;

import org.lsposed.lspd.models.Module;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class LSPApplicationService extends ILSPApplicationService.Stub {
    final static int DEX_TRANSACTION_CODE = 1310096052;
    // <uid, pid>
    private final static Set<Pair<Integer, Integer>> cache = ConcurrentHashMap.newKeySet();
    private final static Map<Integer, IBinder> handles = new ConcurrentHashMap<>();
    private final static Set<IBinder.DeathRecipient> recipients = ConcurrentHashMap.newKeySet();

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        Log.d(TAG, "LSPApplicationService.onTransact: code=" + code);
        if (code == DEX_TRANSACTION_CODE) {
            var shm = ConfigManager.getInstance().getPreloadDex();
            // assume that write only a fd
            shm.writeToParcel(reply, 0);
            reply.writeLong(shm.getSize());
            return true;
        }
        return super.onTransact(code, data, reply, flags);
    }

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
    public List<Module> getModulesList(String processName) throws RemoteException {
        ensureRegistered();
        int pid = getCallingPid();
        int uid = getCallingUid();
        if (uid == 1000 && processName.equals("android")) {
            return ConfigManager.getInstance().getModulesForSystemServer();
        }
        if (ServiceManager.getManagerService().isRunningManager(pid, uid))
            return Collections.emptyList();
        return ConfigManager.getInstance().getModulesForProcess(processName, uid);
    }

    @Override
    public String getPrefsPath(String packageName) throws RemoteException {
        ensureRegistered();
        return ConfigManager.getInstance().getPrefsPath(packageName, getCallingUid());
    }

    @Override
    public Bundle requestRemotePreference(String packageName, int userId, IBinder callback) throws RemoteException {
        ensureRegistered();
        return null;
    }

    @Override
    public IBinder requestModuleBinder(String name) throws RemoteException {
        ensureRegistered();
        if (ConfigManager.getInstance().isModule(getCallingUid(), name)) {
            ConfigManager.getInstance().ensureModulePrefsPermission(getCallingUid(), name);
            return ServiceManager.getModuleService(name);
        } else return null;
    }

    @Override
    public ParcelFileDescriptor requestInjectedManagerBinder(List<IBinder> binder) throws RemoteException {
        ensureRegistered();
        var pid = getCallingPid();
        var uid = getCallingUid();
        if (ServiceManager.getManagerService().postStartManager(pid, uid) ||
                ConfigManager.getInstance().isManager(uid)) {
            var heartbeat = handles.get(pid);
            if (heartbeat != null) {
                binder.add(ServiceManager.getManagerService().obtainManagerBinder(heartbeat, pid, uid));
            }

        }
        return ConfigManager.getInstance().getManagerApk();
    }

    public boolean hasRegister(int uid, int pid) {
        return cache.contains(new Pair<>(uid, pid));
    }

    private void ensureRegistered() throws RemoteException {
        if (!hasRegister(getCallingUid(), getCallingPid()))
            throw new RemoteException("Not registered");
    }
}
