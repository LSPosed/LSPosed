package org.lsposed.manager.util;

import android.app.Application;
import android.os.Handler;

import androidx.annotation.NonNull;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.channel.AbstractChannelListener;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.crashes.Crashes;
import com.microsoft.appcenter.ingestion.models.Log;

import org.lsposed.manager.App;
import org.lsposed.manager.BuildConfig;

import java.util.Map;

public class Telemetry {
    private static final Channel.Listener patchDeviceListener = new AbstractChannelListener() {
        @Override
        public void onPreparedLog(@NonNull Log log, @NonNull String groupName, int flags) {
            var device = log.getDevice();
            device.setAppVersion(BuildConfig.VERSION_NAME);
            device.setAppBuild(String.valueOf(BuildConfig.VERSION_CODE));
        }
    };

    private static void addPatchDeviceListener() {
        try {
            var channelField = AppCenter.class.getDeclaredField("mChannel");
            channelField.setAccessible(true);
            var channel = (Channel) channelField.get(AppCenter.getInstance());
            assert channel != null;
            channel.addListener(patchDeviceListener);
        } catch (ReflectiveOperationException e) {
            android.util.Log.e(App.TAG, "add listener", e);
        }
    }

    private static void patchDevice() {
        try {
            var handlerField = AppCenter.class.getDeclaredField("mHandler");
            handlerField.setAccessible(true);
            var handler = ((Handler) handlerField.get(AppCenter.getInstance()));
            assert handler != null;
            handler.post(Telemetry::addPatchDeviceListener);
        } catch (ReflectiveOperationException e) {
            android.util.Log.e(App.TAG, "patch device", e);
        }
    }

    public static void start(Application app) {
        AppCenter.start(app, "eb3c4175-e879-4312-a72e-b0e64bca142c",
                Analytics.class, Crashes.class);
        patchDevice();
    }

    public static void trackEvent(String name, Map<String, String> properties) {
        Analytics.trackEvent(name, properties);
    }

    public static void trackError(Throwable throwable, Map<String, String> properties) {
        Crashes.trackError(throwable, properties, null);
    }
}
