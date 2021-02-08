package io.github.lsposed.manager.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import io.github.lsposed.manager.util.ModuleUtil;
import io.github.lsposed.manager.util.NotificationUtil;

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

        ModuleUtil.InstalledModule module = ModuleUtil.getInstance().reloadSingleModule(packageName);
        if (module == null) {
            return;
        }

        if (intent.getAction().equals("io.github.lsposed.action.MODULE_NOT_ACTIVATAED")) {
            NotificationUtil.showNotification(context, packageName, module.getAppName(), false);
        } else if (intent.getAction().equals("io.github.lsposed.action.MODULE_UPDATED")) {
            NotificationUtil.showNotification(context, packageName, module.getAppName(), true);
        }
    }
}
