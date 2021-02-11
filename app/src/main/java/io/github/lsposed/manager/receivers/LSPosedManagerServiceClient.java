package io.github.lsposed.manager.receivers;

import android.content.pm.PackageInfo;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.List;

import io.github.lsposed.lspd.ILSPManagerService;
import io.github.lsposed.manager.App;

public class LSPosedManagerServiceClient {

    private static IBinder binder = null;
    private static ILSPManagerService service = null;

    public static void testBinder() {
        if (service == null && binder != null) {
            service = ILSPManagerService.Stub.asInterface(binder);
        }
        if (service == null) {
            return;
        }
        int ver = -1;
        try {
            ver = service.getVersion();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        Log.i(App.TAG, "Got version " + ver);

        List<PackageInfo> ps = null;
        try {
             ps = service.getInstalledPackagesFromAllUsers(0).getList();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        Log.i(App.TAG, String.valueOf(ps));
    }
}
