package io.github.lsposed.lspd.service;

import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import static io.github.lsposed.lspd.service.ServiceManager.TAG;

public class LSPosedService extends ILSPosedService.Stub {
    LSPosedService() {
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
    public ILSPApplicationService requestApplicationService(int uid, int pid) {
        if (Binder.getCallingUid() != 1000) {
            return null;
        }
        if (ConfigManager.getInstance().shouldSkipUid(uid)) {
            return null;
        }
        if (ServiceManager.getApplicationService().hasRegister(uid, pid)) {
            return null;
        }
        return ServiceManager.getApplicationService();
    }

}
