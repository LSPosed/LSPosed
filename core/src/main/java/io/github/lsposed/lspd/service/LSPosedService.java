package io.github.lsposed.lspd.service;

import android.content.pm.PackageInfo;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import java.util.List;

import de.robv.android.xposed.XposedBridge;
import io.github.xposed.xposedservice.IXposedService;

public class LSPosedService extends IXposedService.Stub {
    public static final String TAG = "LSPosedService";

    // call by ourselves
    public static void start() {
        Log.i(TAG, "starting server...");

        Looper.prepare();
        new LSPosedService();
        Looper.loop();

        Log.i(TAG, "server exited");
        System.exit(0);
    }

    public LSPosedService() {
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
    public List<PackageInfo> getInstalledPackagesFromAllUsers(int flags) throws RemoteException {
        return PackageService.getInstalledPackagesFromAllUsers(flags);
    }
}
