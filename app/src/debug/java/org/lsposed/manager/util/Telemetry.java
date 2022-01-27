package org.lsposed.manager.util;

import android.app.Application;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.crashes.AbstractCrashesListener;
import com.microsoft.appcenter.crashes.Crashes;
import com.microsoft.appcenter.crashes.model.ErrorReport;

import org.lsposed.manager.BuildConfig;

import java.util.Locale;

public class Telemetry {
    public static void start(Application app) {
        Crashes.setListener(new AbstractCrashesListener() {
            @Override
            public void onBeforeSending(ErrorReport report) {
                var d = report.getDevice();
                d.setAppVersion(String.format(Locale.ROOT, "%s (%d)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
                d.setAppBuild(String.valueOf(BuildConfig.VERSION_CODE));
                d.setAppNamespace(BuildConfig.APPLICATION_ID);
                report.setDevice(d);
            }
        });
        AppCenter.start(app, "eb3c4175-e879-4312-a72e-b0e64bca142c", Crashes.class);
    }
}
