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

import android.os.IBinder;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;

import org.lsposed.lspd.models.Module;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class LSPApplicationService extends ILSPApplicationService.Stub {
    final static int DEX_TRANSACTION_CODE = 1310096052;
    final static int OBFUSCATION_MAP_TRANSACTION_CODE = 724533732;
    // key: <uid, pid>
    private final static Map<Pair<Integer, Integer>, ProcessInfo> processes = new ConcurrentHashMap<>();

    static class ProcessInfo implements DeathRecipient {
        final int uid;
        final int pid;
        final String processName;
        final IBinder heartBeat;

        ProcessInfo(int uid, int pid, String processName, IBinder heartBeat) throws RemoteException {
            this.uid = uid;
            this.pid = pid;
            this.processName = processName;
            this.heartBeat = heartBeat;
            heartBeat.linkToDeath(this, 0);
            Log.d(TAG, "register " + this);
            processes.put(new Pair<>(uid, pid), this);
        }

        @Override
        public void binderDied() {
            Log.d(TAG, this + " is dead");
            heartBeat.unlinkToDeath(this, 0);
            processes.remove(new Pair<>(uid, pid), this);
        }

        @NonNull
        @Override
        public String toString() {
            return "ProcessInfo{" +
                    "uid=" + uid +
                    ", pid=" + pid +
                    ", processName='" + processName + '\'' +
                    ", heartBeat=" + heartBeat +
                    '}';
        }
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        Log.d(TAG, "LSPApplicationService.onTransact: code=" + code);
        switch (code) {
            case DEX_TRANSACTION_CODE: {
                var shm = ConfigManager.getInstance().getPreloadDex();
                if (shm == null) return false;
                // assume that write only a fd
                shm.writeToParcel(reply, 0);
                reply.writeLong(shm.getSize());
                return true;
            }
            case OBFUSCATION_MAP_TRANSACTION_CODE: {
                var obfuscation = ConfigManager.getInstance().dexObfuscate();
                var signatures = ObfuscationManager.getSignatures();
                reply.writeInt(signatures.size() * 2);
                for (Map.Entry<String, String> entry : signatures.entrySet()) {
                    reply.writeString(entry.getKey());
                    // return val = key if obfuscation disabled
                    reply.writeString(obfuscation ? entry.getValue() : entry.getKey());
                }
                return true;
            }
        }
        return super.onTransact(code, data, reply, flags);
    }

    public boolean registerHeartBeat(int uid, int pid, String processName, IBinder heartBeat) {
        try {
            new ProcessInfo(uid, pid, processName, heartBeat);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    private List<Module> getAllModulesList() throws RemoteException {
        var processInfo = ensureRegistered();
        if (processInfo.uid == Process.SYSTEM_UID && processInfo.processName.equals("system")) {
            return ConfigManager.getInstance().getModulesForSystemServer();
        }
        if (ServiceManager.getManagerService().isRunningManager(processInfo.pid, processInfo.uid))
            return Collections.emptyList();
        return ConfigManager.getInstance().getModulesForProcess(processInfo.processName, processInfo.uid);
    }

    @Override
    public List<Module> getLegacyModulesList() throws RemoteException {
        return getAllModulesList().stream().filter(m -> m.file.legacy).collect(Collectors.toList());
    }

    @Override
    public List<Module> getModulesList() throws RemoteException {
        return getAllModulesList().stream().filter(m -> !m.file.legacy).collect(Collectors.toList());
    }

    @Override
    public String getPrefsPath(String packageName) throws RemoteException {
        ensureRegistered();
        return ConfigManager.getInstance().getPrefsPath(packageName, getCallingUid());
    }

    @Override
    public ParcelFileDescriptor requestInjectedManagerBinder(List<IBinder> binder) throws RemoteException {
        var processInfo = ensureRegistered();
        if (ServiceManager.getManagerService().postStartManager(processInfo.pid, processInfo.uid) ||
                ConfigManager.getInstance().isManager(processInfo.uid)) {
            binder.add(ServiceManager.getManagerService().obtainManagerBinder(processInfo.heartBeat, processInfo.pid, processInfo.uid));
        }
        return ConfigManager.getInstance().getManagerApk();
    }

    public boolean hasRegister(int uid, int pid) {
        return processes.containsKey(new Pair<>(uid, pid));
    }

    @NonNull
    private ProcessInfo ensureRegistered() throws RemoteException {
        var uid = getCallingUid();
        var pid = getCallingPid();
        var key = new Pair<>(uid, pid);
        ProcessInfo processInfo = processes.getOrDefault(key, null);
        if (processInfo == null || uid != processInfo.uid || pid != processInfo.pid) {
            processes.remove(key, processInfo);
            Log.w(TAG, "non-authorized: info=" + processInfo + " uid=" + uid + " pid=" + pid);
            throw new RemoteException("Not registered");
        }
        return processInfo;
    }
}
