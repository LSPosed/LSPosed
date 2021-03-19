package org.lsposed.lspd.service;

import android.annotation.SuppressLint;
import android.app.ActivityThread;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import java.lang.reflect.Method;

import org.lsposed.lspd.util.Utils;

public class BootReceiver {
    public static void register(BroadcastReceiver receiver) {
        ActivityThread activityThread = ActivityThread.currentActivityThread();
        if (activityThread == null) {
            Utils.logW("ActivityThread is null");
            return;
        }
        Context context = activityThread.getSystemContext();
        if (context == null) {
            Utils.logW("context is null");
            return;
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_LOCKED_BOOT_COMPLETED);

        HandlerThread thread = new HandlerThread("lspd-BootReceiver");
        thread.start();
        Handler handler = new Handler(thread.getLooper());

        try {
            @SuppressLint("DiscouragedPrivateApi")
            Method method = Context.class.getDeclaredMethod("registerReceiver", BroadcastReceiver.class, IntentFilter.class, String.class, Handler.class);
            method.invoke(context, receiver, intentFilter, null, handler);
            Utils.logI("registered package receiver");
        } catch (Throwable e) {
            Utils.logW("registerReceiver failed", e);
        }

    }
}
