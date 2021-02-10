package hidden;

//import android.annotation.NonNull;
//import android.app.ActivityThread;
//import android.content.pm.ApplicationInfo;
//import android.content.pm.PackageInfo;
//import android.content.pm.PackageManager;
//import android.os.UserHandle;
import android.annotation.NonNull;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.content.res.AssetManager;
import android.os.UserManager;

import java.util.List;

public class HiddenApiBridge {
    public static int AssetManager_addAssetPath(AssetManager am, String path) {
        return am.addAssetPath(path);
    }
    public static List<UserInfo> UserManager_getUsers(UserManager um) {
        return um.getUsers();
    }
//    public static ApplicationInfo PackageManager_getApplicationInfoAsUser(PackageManager packageManager, @NonNull String packageName, int flags, int userId) throws android.content.pm.PackageManager.NameNotFoundException {
//        return packageManager.getApplicationInfoAsUser(packageName, flags, userId);
//    }
//
//    public static PackageInfo PackageManager_getPackageInfoAsUser(PackageManager packageManager, @NonNull String packageName, int flags, int userId) throws android.content.pm.PackageManager.NameNotFoundException {
//        return packageManager.getPackageInfoAsUser(packageName, flags, userId);
//    }
//
//    public static List<ApplicationInfo> PackageManager_getInstalledApplicationsAsUser(PackageManager packageManager, int flags, int userId) {
//        return packageManager.getInstalledApplicationsAsUser(flags, userId);
//    }
//
    public static List<PackageInfo> PackageManager_getInstalledPackagesAsUser(PackageManager packageManager, int flags, int userId) {
        return packageManager.getInstalledPackagesAsUser(flags, userId);
    }
//
//    public static UserHandle createUserHandle(int userId) {
//        return new UserHandle(userId);
//    }
//
//    public static ActivityThread ActivityThread_systemMain() {
//        return ActivityThread.systemMain();
//    }
//
//    public static String PackageInfo_overlayTarget(PackageInfo packageInfo) {
//        return packageInfo.overlayTarget;
//    }
}
