package io.github.lsposed.lspd.service;

import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;
import android.util.Pair;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.lsposed.lspd.service.ServiceManager.TAG;

public class LSPApplicationService extends ILSPApplicationService.Stub {
    // <uid, pid>
    private final static Set<Pair<Integer, Integer>> cache = ConcurrentHashMap.newKeySet();
    private final static Set<IBinder> handles = ConcurrentHashMap.newKeySet();

    @Override
    public void registerHeartBeat(IBinder handle) throws RemoteException {
        handles.add(handle);
        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();

        cache.add(new Pair<>(uid, pid));
        handle.linkToDeath(new DeathRecipient() {
            @Override
            public void binderDied() {
                Log.d(TAG, "pid=" + pid + " uid=" + uid + " is dead.");
                cache.remove(new Pair<>(uid, pid));
                handles.remove(handle);
                handle.unlinkToDeath(this, 0);
            }
        }, 0);
    }

    @Override
    public int getVariant() throws RemoteException {
        ensureRegistered();
        return ConfigManager.getInstance().variant();
    }

    @Override
    public boolean isResourcesHookEnabled() throws RemoteException {
        ensureRegistered();
        return ConfigManager.getInstance().resourceHook();
    }

    @Override
    public String[] getModulesList() throws RemoteException {
        ensureRegistered();
        return ConfigManager.getInstance().getModulesPathForUid(Binder.getCallingUid());
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

    // TODO: check if module
    @Override
    public IBinder requestModuleBinder() throws RemoteException {
        ensureRegistered();
        return ServiceManager.getModuleService();
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
