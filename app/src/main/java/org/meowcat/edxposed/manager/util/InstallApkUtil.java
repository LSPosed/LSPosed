package org.meowcat.edxposed.manager.util;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;

public class InstallApkUtil {

    public static String getAppLabel(ApplicationInfo info, PackageManager pm) {
        try {
            if (info.labelRes > 0) {
                Resources res = pm.getResourcesForApplication(info);
                return res.getString(info.labelRes);
            }
        } catch (Exception ignored) {
        }
        return info.loadLabel(pm).toString();
    }
}
