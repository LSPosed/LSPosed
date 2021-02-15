package io.github.lsposed.lspd.service;

import android.os.Binder;
import android.os.IBinder;
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
        handle.linkToDeath(() -> {
            Log.d(TAG, "pid=" + pid + " uid=" + uid + " is dead.");
            cache.remove(new Pair<>(uid, pid));
            handles.remove(handle);
        }, 0);
    }

    // TODO: check if module
    @Override
    public IBinder requestModuleBinder() {
        if (!hasRegister(Binder.getCallingUid(), Binder.getCallingPid()))
            return null;
        return ServiceManager.getModuleService();
    }

    // TODO: check if manager
    @Override
    public IBinder requestManagerBinder() {
        if (!hasRegister(Binder.getCallingUid(), Binder.getCallingPid()))
            return null;
        return ServiceManager.getManagerService();
    }

    public boolean hasRegister(int uid, int pid) {
        return cache.contains(new Pair<>(uid, pid));
    }
}
