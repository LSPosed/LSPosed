package io.github.lsposed.lspd.service;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Binder;
import android.os.RemoteException;
import android.util.Log;

import io.github.lsposed.lspd.Application;

import static io.github.lsposed.lspd.service.ServiceManager.TAG;

public class LSPosedService extends ILSPosedService.Stub {
    @Override
    public ILSPApplicationService requestApplicationService(int uid, int pid, String processName) {
        if (Binder.getCallingUid() != 1000) {
            Log.w(TAG, "Someone else got my binder!?");
            return null;
        }
        if (uid == 1000 && processName.equals("android")) {
            if (ConfigManager.shouldSkipSystemServer())
                return null;
            else
                return ServiceManager.getApplicationService();
        }
        if (ConfigManager.getInstance().shouldSkipProcess(new ConfigManager.ProcessScope(processName, uid))) {
            Log.d(TAG, "Skipped " + processName + "/" + uid);
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
            return;
        }
        int uid = intent.getIntExtra(Intent.EXTRA_UID, -1);
        int userId = intent.getIntExtra(Intent.EXTRA_USER, -1);
        boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
        if (intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED) && uid > 0 && !replacing) {
            ConfigManager.getInstance().removeModule(packageName);
            Application app = new Application();
            app.packageName = packageName;
            app.userId = userId;
            ConfigManager.getInstance().removeApp(app);
        }
        PackageInfo pkgInfo = PackageService.getPackageInfo(packageName, PackageManager.GET_META_DATA, 0);
        boolean isXposedModule = pkgInfo != null && pkgInfo.applicationInfo != null &&
                pkgInfo.applicationInfo.enabled && pkgInfo.applicationInfo.metaData != null &&
                pkgInfo.applicationInfo.metaData.containsKey("xposedmodule");
        if (isXposedModule) {
            ConfigManager.getInstance().updateModuleApkPath(packageName, pkgInfo.applicationInfo.sourceDir);
        }
        if (!intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED) && uid > 0 && ConfigManager.getInstance().isManager(packageName)) {
            try {
                ConfigManager.grantManagerPermission();
            } catch (Throwable e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }
    }

}
