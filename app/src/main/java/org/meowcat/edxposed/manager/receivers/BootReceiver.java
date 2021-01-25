package org.meowcat.edxposed.manager.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.json.JSONObject;
import org.meowcat.edxposed.manager.BuildConfig;
import org.meowcat.edxposed.manager.util.NotificationUtil;
import org.meowcat.edxposed.manager.util.TaskRunner;
import org.meowcat.edxposed.manager.util.json.JSONUtils;

import java.util.concurrent.Callable;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        //new TaskRunner().executeAsync(new LongRunningTask());
    }

    private static class LongRunningTask implements Callable<Void> {

        @Override
        public Void call() {
            try {
                Thread.sleep(60 * 60 * 1000);
                String jsonString = JSONUtils.getFileContent(JSONUtils.JSON_LINK).replace("%XPOSED_ZIP%", "");

                String newApkVersion = new JSONObject(jsonString).getJSONObject("apk").getString("version");

                Integer a = BuildConfig.VERSION_CODE;
                Integer b = Integer.valueOf(newApkVersion);

                if (a.compareTo(b) < 0) {
                    NotificationUtil.showInstallerUpdateNotification();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
