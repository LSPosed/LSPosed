package org.lsposed.manager.util;

import android.app.Application;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.channel.AbstractChannelListener;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.crashes.Crashes;

import org.lsposed.manager.App;
import org.lsposed.manager.BuildConfig;

public class Telemetry {
    static Channel.Listener patchDeviceListener = new AbstractChannelListener() {
        @Override
        public void onPreparedLog(@NonNull com.microsoft.appcenter.ingestion.models.Log log, @NonNull String groupName, int flags) {
            var d = log.getDevice();
            d.setAppVersion(BuildConfig.VERSION_NAME);
            d.setAppBuild(String.valueOf(BuildConfig.VERSION_CODE));
            d.setAppNamespace(BuildConfig.APPLICATION_ID);
            log.setDevice(d);
        }
    };

    static void addPatchDeviceListener() {
        try {
            var channel = AppCenter.class.getDeclaredField("mChannel");
            channel.setAccessible(true);
            ((Channel) channel.get(AppCenter.getInstance())).addListener(patchDeviceListener);
        } catch (Throwable e) {
            Log.e(App.TAG, "add listener", e);
        }
    }

    static void patchDevice() throws Throwable {
        var handle = AppCenter.class.getDeclaredField("mHandler");
        handle.setAccessible(true);
        ((Handler) handle.get(AppCenter.getInstance())).post(Telemetry::addPatchDeviceListener);
    }

    public static void start(Application app) {
        AppCenter.start(app, "eb3c4175-e879-4312-a72e-b0e64bca142c", Crashes.class);
        try {
            patchDevice();
        } catch (Throwable e) {
            Log.w(App.TAG, "patch device", e);
        }
    }
}
