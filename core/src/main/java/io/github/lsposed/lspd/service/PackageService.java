package io.github.lsposed.lspd.service;

import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import io.github.lsposed.lspd.nativebridge.ConfigManager;

public class PackageService {
    private static IPackageManager pm = null;
    private static IBinder binder = null;

    public static IPackageManager getPackageManager() {
        if (binder == null && pm == null) {
            binder = ServiceManager.getService("package");
            pm = IPackageManager.Stub.asInterface(binder);
        }
        return pm;
    }

    public static PackageInfo getPackageInfo(String packageName, int flags, int uid) throws RemoteException {
        pm = getPackageManager();
        if (pm == null) return null;
        return pm.getPackageInfo(packageName, flags, uid);
    }

    public static String[] getPackagesForUid(int uid) throws RemoteException {
        pm = getPackageManager();
        if (pm == null) return new String[0];
        return pm.getPackagesForUid(uid);
    }

    public static boolean isInstaller(int uid) throws RemoteException {
        boolean res = false;
        String InstallerPackageName = ConfigManager.getInstallerPackageName();
        for (String pkg : getPackagesForUid(uid)) {
            res = res || InstallerPackageName.equals(pkg);
        }
        return res;
    }

    public static List<PackageInfo> getInstalledPackagesFromAllUsers(int flags) throws RemoteException {
        if (!isInstaller(Binder.getCallingUid())) {
            throw new RemoteException("Permission denied");
        }
        ArrayList<PackageInfo> res = new ArrayList<>();
        IPackageManager pm = getPackageManager();
        if (pm == null) return res;
        for (int uid : UserService.getUsers()) {
            Log.w("LSPosed", "uid: " + uid);
            res.addAll(pm.getInstalledPackages(flags, uid).getList());
        }
        return res;
    }
}
