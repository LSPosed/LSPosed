package io.github.lsposed.manager.receivers;

import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import io.github.lsposed.manager.App;
import io.github.lsposed.lspd.service.ILSPosedService;

public class LSPosedServiceClient {
    public static IBinder binder = null;
    public static ILSPosedService service = null;

    private static final IBinder.DeathRecipient DEATH_RECIPIENT = () -> {
        binder = null;
        service = null;
    };

    public static IBinder getBinder() {
        try {
            Log.e(App.TAG, "Cannot get binder");
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static void testBinder() {
        if (binder == null && service == null) {
            binder = getBinder();
        }
        if (binder == null) {
            return;
        }

        try {
            binder.linkToDeath(DEATH_RECIPIENT, 0);
        } catch (Throwable e) {
            e.printStackTrace();
        }

        service = ILSPosedService.Stub.asInterface(binder);
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
    }
}
