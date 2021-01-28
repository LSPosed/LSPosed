package org.meowcat.edxposed.manager.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import org.meowcat.edxposed.manager.util.NotificationUtil;

public class EdServiceReceiver extends BroadcastReceiver {

    private static String getPackageName(Intent intent) {
        Uri uri = intent.getData();
        return (uri != null) ? uri.getSchemeSpecificPart() : null;
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        String packageName = getPackageName(intent);
        if (packageName == null) {
            return;
        }

        String appName = "null";
        try {
            PackageManager pm = context.getPackageManager();
            pm.getApplicationInfo(packageName, 0);
            appName = (String) pm.getApplicationInfo(packageName, 0).loadLabel(pm);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (intent.getAction().equals("io.github.lsposed.action.MODULE_NOT_ACTIVATAED")) {
            NotificationUtil.showNotActivatedNotification(packageName, appName);
        } else if (intent.getAction().equals("io.github.lsposed.action.MODULE_UPDATED")) {
            NotificationUtil.showModulesUpdatedNotification(appName);
        }
    }
}
