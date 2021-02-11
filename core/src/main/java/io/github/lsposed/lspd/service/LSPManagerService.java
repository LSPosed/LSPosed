package io.github.lsposed.lspd.service;

import android.content.pm.PackageInfo;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import de.robv.android.xposed.XposedBridge;
import io.github.lsposed.lspd.ILSPManagerService;
import io.github.lsposed.lspd.utils.ParceledListSlice;

import static io.github.lsposed.lspd.service.Service.TAG;

public class LSPManagerService extends ILSPManagerService.Stub {

    public LSPManagerService() {
        BridgeService.send(this, new BridgeService.Listener() {
            @Override
            public void onSystemServerRestarted() {
                Log.w(TAG, "system restarted...");
            }

            @Override
            public void onResponseFromBridgeService(boolean response) {
                if (response) {
                    Log.i(TAG, "sent service to bridge");
                } else {
                    Log.w(TAG, "no response from bridge");
                }
            }
        });
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
