package org.lsposed.manager.util;

import android.app.Application;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.crashes.Crashes;


public class Telemetry {
    public static void start(Application context) {
        AppCenter.start(context, "eb3c4175-e879-4312-a72e-b0e64bca142c",
                Analytics.class, Crashes.class);
    }
}
