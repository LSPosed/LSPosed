package org.lsposed.lspd.service;

import android.os.IBinder;
import android.os.IPowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import static org.lsposed.lspd.service.ServiceManager.TAG;

public class PowerService {
    private static IPowerManager pm = null;
    private static IBinder binder = null;

    public static IPowerManager getPowerManager() {
        if (binder == null && pm == null) {
            binder = ServiceManager.getService("power");
            if (binder == null) return null;
            try {
                binder.linkToDeath(new IBinder.DeathRecipient() {
                    @Override
                    public void binderDied() {
                        Log.w(TAG, "pm is dead");
                        binder.unlinkToDeath(this, 0);
                        binder = null;
                        pm = null;
                    }
                }, 0);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
            pm = IPowerManager.Stub.asInterface(binder);
        }
        return pm;
    }

    public static void reboot(boolean confirm, String reason, boolean wait) throws RemoteException {
        IPowerManager pm = getPowerManager();
        if (pm == null) return;
        pm.reboot(confirm, reason, wait);
    }
}
