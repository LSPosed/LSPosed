package io.github.lsposed.lspd.service;

import android.content.pm.PackageInfo;
import android.os.IBinder;
import android.os.RemoteException;

import de.robv.android.xposed.XposedBridge;
import io.github.lsposed.lspd.ILSPManagerService;
import io.github.lsposed.lspd.utils.ParceledListSlice;

public class LSPManagerService extends ILSPManagerService.Stub {

    LSPManagerService() {
    }

    @Override
    public IBinder asBinder() {
        return this;
    }

    @Override
    public int getVersion() {
        return XposedBridge.getXposedVersion();
    }

    @Override
    public ParceledListSlice<PackageInfo> getInstalledPackagesFromAllUsers(int flags) throws RemoteException {
        return PackageService.getInstalledPackagesFromAllUsers(flags);
    }
}
