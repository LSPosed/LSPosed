package com.swift.sandhook.xposedcompat.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by swift_gan on 2017/11/23.
 */

public class ProcessUtils {

    private static volatile String processName = null;

    public static String getProcessName(Context context) {
        if (!TextUtils.isEmpty(processName))
            return processName;
        processName = doGetProcessName(context);
        return processName;
    }

    private static String doGetProcessName(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningApps = am.getRunningAppProcesses();
        if (runningApps == null) {
            return null;
        }
        for (ActivityManager.RunningAppProcessInfo proInfo : runningApps) {
            if (proInfo.pid == android.os.Process.myPid()) {
                if (proInfo.processName != null) {
                    return proInfo.processName;
                }
            }
        }
        return context.getPackageName();
    }

    public static boolean isMainProcess(Context context) {
        String processName = getProcessName(context);
        String pkgName = context.getPackageName();
        if (!TextUtils.isEmpty(processName) && !TextUtils.equals(processName, pkgName)) {
            return false;
        } else {
            return true;
        }
    }

    public static List<ResolveInfo> findActivitiesForPackage(Context context, String packageName) {
        final PackageManager packageManager = context.getPackageManager();

        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        mainIntent.setPackage(packageName);

        final List<ResolveInfo> apps = packageManager.queryIntentActivities(mainIntent, 0);
        return apps != null ? apps : new ArrayList<ResolveInfo>();
    }
}
