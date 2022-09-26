package android.content.pm;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

import androidx.annotation.RequiresApi;

import java.util.List;

public interface IPackageManager extends IInterface {

    boolean isPackageAvailable(String packageName, int userId) throws RemoteException;

    boolean getApplicationHiddenSettingAsUser(String packageName, int userId) throws RemoteException;

    ApplicationInfo getApplicationInfo(String packageName, int flags, int userId)
            throws RemoteException;

    @RequiresApi(33)
    ApplicationInfo getApplicationInfo(String packageName, long flags, int userId)
            throws RemoteException;

    PackageInfo getPackageInfo(String packageName, int flags, int userId)
            throws RemoteException;

    @RequiresApi(33)
    PackageInfo getPackageInfo(String packageName, long flags, int userId)
            throws RemoteException;

    int getPackageUid(String packageName, int flags, int userId) throws RemoteException;

    @RequiresApi(33)
    int getPackageUid(String packageName, long flags, int userId) throws RemoteException;

    String[] getPackagesForUid(int uid)
            throws RemoteException;

    ParceledListSlice<PackageInfo> getInstalledPackages(int flags, int userId)
            throws RemoteException;

    @RequiresApi(33)
    ParceledListSlice<PackageInfo> getInstalledPackages(long flags, int userId)
            throws RemoteException;

    ParceledListSlice<ApplicationInfo> getInstalledApplications(int flags, int userId)
            throws RemoteException;

    @RequiresApi(33)
    ParceledListSlice<ApplicationInfo> getInstalledApplications(long flags, int userId)
            throws RemoteException;

    int getUidForSharedUser(String sharedUserName)
            throws RemoteException;

    void grantRuntimePermission(String packageName, String permissionName, int userId)
            throws RemoteException;

    void revokeRuntimePermission(String packageName, String permissionName, int userId)
            throws RemoteException;

    int getPermissionFlags(String permissionName, String packageName, int userId)
            throws RemoteException;

    void updatePermissionFlags(String permissionName, String packageName, int flagMask, int flagValues, int userId)
            throws RemoteException;

    int checkPermission(String permName, String pkgName, int userId)
            throws RemoteException;

    int checkUidPermission(String permName, int uid)
            throws RemoteException;

    IPackageInstaller getPackageInstaller() throws RemoteException;

    int installExistingPackageAsUser(String packageName, int userId, int installFlags,
                                     int installReason) throws RemoteException;

    @RequiresApi(29)
    int installExistingPackageAsUser(String packageName, int userId, int installFlags,
                                     int installReason, List<String> whiteListedPermissions) throws RemoteException;

    ParceledListSlice<ResolveInfo> queryIntentActivities(Intent intent,
                                                         String resolvedType, int flags, int userId) throws RemoteException;

    @RequiresApi(33)
    ParceledListSlice<ResolveInfo> queryIntentActivities(Intent intent,
                                            String resolvedType, long flags, int userId) throws RemoteException;

    boolean performDexOptMode(String packageName, boolean checkProfiles,
                              String targetCompilerFilter, boolean force, boolean bootComplete, String splitName)
            throws RemoteException;

    void clearApplicationProfileData(String packageName) throws RemoteException;

    abstract class Stub extends Binder implements IPackageManager {

        public static IPackageManager asInterface(IBinder obj) {
            throw new UnsupportedOperationException();
        }
    }
}
