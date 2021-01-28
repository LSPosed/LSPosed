package io.github.lsposed.manager.util;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;

public class GlideHelper {
    public static PackageInfo wrapApplicationInfoForIconLoader(ApplicationInfo applicationInfo) {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.applicationInfo = applicationInfo;
        return packageInfo;
    }
}
