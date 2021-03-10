/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2020 EdXposed Contributors
 * Copyright (C) 2021 LSPosed Contributors
 */

package org.lsposed.lspd.service;

import android.annotation.SuppressLint;
import android.app.ActivityThread;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.UserHandle;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.lsposed.lspd.util.Utils;

public class PackageReceiver {
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

        UserHandle userHandleAll;
        try {
            //noinspection JavaReflectionMemberAccess
            Field field = UserHandle.class.getDeclaredField("ALL");
            userHandleAll = (UserHandle) field.get(null);
        } catch (Throwable e) {
            Utils.logW("UserHandle.ALL", e);
            return;
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED);
        intentFilter.addDataScheme("package");

        HandlerThread thread = new HandlerThread("lspd-PackageReceiver");
        thread.start();
        Handler handler = new Handler(thread.getLooper());

        try {
            @SuppressLint("DiscouragedPrivateApi")
            Method method = Context.class.getDeclaredMethod("registerReceiverAsUser", BroadcastReceiver.class, UserHandle.class, IntentFilter.class, String.class, Handler.class);
            method.invoke(context, receiver, userHandleAll, intentFilter, null, handler);
            Utils.logI("registered package receiver");
        } catch (Throwable e) {
            Utils.logW("registerReceiver failed", e);
        }

    }
}
