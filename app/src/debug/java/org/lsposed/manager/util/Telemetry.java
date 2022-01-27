package org.lsposed.manager.util;

import android.app.Application;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.channel.AbstractChannelListener;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.crashes.AbstractCrashesListener;
import com.microsoft.appcenter.crashes.Crashes;
import com.microsoft.appcenter.crashes.model.ErrorReport;

import org.lsposed.manager.App;
import org.lsposed.manager.BuildConfig;

public class Telemetry {
    public static void start(Application app) {
        AppCenter.start(app, "eb3c4175-e879-4312-a72e-b0e64bca142c", Crashes.class);
        try {
            var handle = AppCenter.class.getDeclaredField("mHandler");
            handle.setAccessible(true);
            ((Handler) handle.get(AppCenter.getInstance())).post(() -> {
                try {
                    var channel = AppCenter.class.getDeclaredField("mChannel");
                    channel.setAccessible(true);
                    ((Channel) channel.get(AppCenter.getInstance())).addListener(new AbstractChannelListener() {
                        @Override
                        public void onPreparedLog(@NonNull com.microsoft.appcenter.ingestion.models.Log log, @NonNull String groupName, int flags) {
                            var d = log.getDevice();
                            d.setAppVersion(BuildConfig.VERSION_NAME);
                            d.setAppBuild(String.valueOf(BuildConfig.VERSION_CODE));
                            d.setAppNamespace(BuildConfig.APPLICATION_ID);
                            log.setDevice(d);
                        }
                    });
                } catch (Throwable e) {
                    Log.e(App.TAG, "add listener", e);
                }
            });
        } catch (Throwable e) {
            Log.w(App.TAG, "get handler", e);
        }
    }
}
