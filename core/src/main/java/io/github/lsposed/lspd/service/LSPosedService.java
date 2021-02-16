package io.github.lsposed.lspd.service;

import android.os.Binder;
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
            Log.w(TAG, "Someone else got my binder!?");
            return null;
        }
        if (ConfigManager.getInstance().shouldSkipUid(uid)) {
            Log.d(TAG, "Skipped uid " + uid);
            return null;
        }
        if (ServiceManager.getApplicationService().hasRegister(uid, pid)) {
            Log.d(TAG, "Skipped duplicated request for uid " + uid + " pid " + pid);
            return null;
        }
        Log.d(TAG, "returned service");
        return ServiceManager.getApplicationService();
    }

}
