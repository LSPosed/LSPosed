package io.github.lsposed.lspd.service;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Binder;
import android.os.RemoteException;
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


    @Override
    public void dispatchPackageChanged(Intent intent) throws RemoteException {
        if (Binder.getCallingUid() != 1000 || intent == null) return;
        Uri uri = intent.getData();
        String packageName = (uri != null) ? uri.getSchemeSpecificPart() : null;
        if (packageName == null) {
            Log.e(TAG, "Package name is null");
        }
        int uid = intent.getIntExtra(Intent.EXTRA_UID, -1);
        boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
        if (intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED) && uid > 0 && !replacing) {
            ConfigManager.getInstance().removeModule(packageName);
            ConfigManager.getInstance().removeApp(uid);
        }
        PackageInfo pkgInfo = PackageService.getPackageInfo(packageName, PackageManager.GET_META_DATA, 0);
        boolean isXposedModule = pkgInfo != null && pkgInfo.applicationInfo != null &&
                pkgInfo.applicationInfo.enabled && pkgInfo.applicationInfo.metaData != null &&
                pkgInfo.applicationInfo.metaData.containsKey("xposedmodule");
        if (isXposedModule) {
            ConfigManager.getInstance().updateModuleApkPath(packageName, pkgInfo.applicationInfo.sourceDir);
        }
    }

}
