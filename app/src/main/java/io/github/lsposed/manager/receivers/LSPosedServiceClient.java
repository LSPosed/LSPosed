package io.github.lsposed.manager.receivers;

import android.content.pm.PackageInfo;
import android.os.RemoteException;
import android.util.Log;

import java.util.List;

import io.github.lsposed.manager.App;
import io.github.xposed.xposedservice.XposedService;

public class LSPosedServiceClient {

    public static void testBinder() {
        XposedService service = XposedService.getService();
        if (service == null) {
            Log.e(App.TAG, "Version fail");
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
