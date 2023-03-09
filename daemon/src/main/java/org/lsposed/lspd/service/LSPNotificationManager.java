package org.lsposed.lspd.service;

import static org.lsposed.lspd.service.ServiceManager.TAG;

import android.app.INotificationManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ParceledListSlice;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import org.lsposed.daemon.R;
import org.lsposed.lspd.util.FakeContext;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.github.libxposed.service.IXposedScopeCallback;

public class LSPNotificationManager {
    static final String UPDATED_CHANNEL_ID = "lsposed_module_updated";
    static final String SCOPE_CHANNEL_ID = "lsposed_module_scope";
    private static final String STATUS_CHANNEL_ID = "lsposed_status";
    private static final int STATUS_NOTIFICATION_ID = 2000;
    private static final String opPkg = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ?
            "android" : "com.android.settings";

    private static final Map<String, Integer> notificationIds = new ConcurrentHashMap<>();
    private static int previousNotificationId = STATUS_NOTIFICATION_ID;

    static final String openManagerAction = UUID.randomUUID().toString();
    static final String moduleScope = UUID.randomUUID().toString();

    private static INotificationManager notificationManager = null;
    private static IBinder binder = null;

    private static final IBinder.DeathRecipient recipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            Log.w(TAG, "nm is dead");
            binder.unlinkToDeath(this, 0);
            binder = null;
            notificationManager = null;
        }
    };

    private static INotificationManager getNotificationManager() throws RemoteException {
        if (binder == null || notificationManager == null) {
            binder = android.os.ServiceManager.getService(Context.NOTIFICATION_SERVICE);
            binder.linkToDeath(recipient, 0);
            notificationManager = INotificationManager.Stub.asInterface(binder);
        }
        return notificationManager;
    }

    private static Bitmap getBitmap(int id) {
        var r = ConfigFileManager.getResources();
        var res = r.getDrawable(id, r.newTheme());
        if (res instanceof BitmapDrawable) {
            return ((BitmapDrawable) res).getBitmap();
        } else {
            if (res instanceof AdaptiveIconDrawable) {
                var layers = new Drawable[]{((AdaptiveIconDrawable) res).getBackground(),
                        ((AdaptiveIconDrawable) res).getForeground()};
                res = new LayerDrawable(layers);
            }
            var bitmap = Bitmap.createBitmap(res.getIntrinsicWidth(),
                    res.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            var canvas = new Canvas(bitmap);
            res.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            res.draw(canvas);
            return bitmap;
        }
    }

    private static Icon getNotificationIcon() {
        return Icon.createWithBitmap(getBitmap(R.drawable.ic_notification));
    }

    private static boolean hasNotificationChannelForSystem(
            INotificationManager nm, String channelId) throws RemoteException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return nm.getNotificationChannelForPackage("android", 1000, channelId, null, false) != null;
        } else {
            return nm.getNotificationChannelForPackage("android", 1000, channelId, false) != null;
        }
    }

    private static void createNotificationChannel(INotificationManager nm) throws RemoteException {
        var context = new FakeContext();
        var list = new ArrayList<NotificationChannel>();

        var updated = new NotificationChannel(UPDATED_CHANNEL_ID,
                context.getString(R.string.module_updated_channel_name),
                NotificationManager.IMPORTANCE_HIGH);
        updated.setShowBadge(false);
        if (hasNotificationChannelForSystem(nm, UPDATED_CHANNEL_ID)) {
            nm.updateNotificationChannelForPackage("android", 1000, updated);
        } else {
            list.add(updated);
        }

        var status = new NotificationChannel(STATUS_CHANNEL_ID,
                context.getString(R.string.status_channel_name),
                NotificationManager.IMPORTANCE_MIN);
        status.setShowBadge(false);
        if (hasNotificationChannelForSystem(nm, STATUS_CHANNEL_ID)) {
            nm.updateNotificationChannelForPackage("android", 1000, status);
        } else {
            list.add(status);
        }

        var scope = new NotificationChannel(SCOPE_CHANNEL_ID,
                context.getString(R.string.scope_channel_name),
                NotificationManager.IMPORTANCE_HIGH);
        scope.setShowBadge(false);
        if (hasNotificationChannelForSystem(nm, SCOPE_CHANNEL_ID)) {
            nm.updateNotificationChannelForPackage("android", 1000, scope);
        } else {
            list.add(scope);
        }

        nm.createNotificationChannelsForPackage("android", 1000, new ParceledListSlice<>(list));
    }

    static void notifyStatusNotification() {
        var intent = new Intent(openManagerAction);
        intent.setPackage("android");
        var context = new FakeContext();
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        var notification = new Notification.Builder(context, STATUS_CHANNEL_ID)
                .setContentTitle(context.getString(R.string.lsposed_running_notification_title))
                .setContentText(context.getString(R.string.lsposed_running_notification_content))
                .setSmallIcon(getNotificationIcon())
                .setContentIntent(PendingIntent.getBroadcast(context, 1, intent, flags))
                .setVisibility(Notification.VISIBILITY_SECRET)
                .setColor(0xFFF48FB1)
                .setOngoing(true)
                .setAutoCancel(false)
                .build();
        notification.extras.putString("android.substName", "LSPosed");
        try {
            var nm = getNotificationManager();
            createNotificationChannel(nm);
            nm.enqueueNotificationWithTag("android", opPkg, null,
                    STATUS_NOTIFICATION_ID, notification, 0);
        } catch (RemoteException e) {
            Log.e(TAG, "notifyStatusNotification: ", e);
        }
    }

    static void cancelStatusNotification() {
        try {
            var nm = getNotificationManager();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                nm.cancelNotificationWithTag("android", "android", null, STATUS_NOTIFICATION_ID, 0);
            } else {
                nm.cancelNotificationWithTag("android", null, STATUS_NOTIFICATION_ID, 0);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "cancelStatusNotification: ", e);
        }
    }

    private static PendingIntent getModuleIntent(String modulePackageName, int moduleUserId) {
        var intent = new Intent(openManagerAction);
        intent.setPackage("android");
        intent.setData(new Uri.Builder().scheme("module").encodedAuthority(modulePackageName + ":" + moduleUserId).build());
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(new FakeContext(), 3, intent, flags);
    }

    private static PendingIntent getModuleScopeIntent(String modulePackageName, int moduleUserId, String scopePackageName, String action, IXposedScopeCallback callback) {
        var intent = new Intent(moduleScope);
        intent.setPackage("android");
        intent.setData(new Uri.Builder().scheme("module").encodedAuthority(modulePackageName + ":" + moduleUserId).encodedPath(scopePackageName).appendQueryParameter("action", action).build());
        var extras = new Bundle();
        extras.putBinder("callback", callback.asBinder());
        intent.putExtras(extras);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(new FakeContext(), 4, intent, flags);
    }

    private static String getNotificationIdKey(String channel, String modulePackageName, int moduleUserId) {
        return channel + "/" + modulePackageName + ":" + moduleUserId;
    }

    private static int pushAndGetNotificationId(String channel, String modulePackageName, int moduleUserId) {
        var idKey = getNotificationIdKey(channel, modulePackageName, moduleUserId);
        // previousNotificationId start with 2001
        // https://android.googlesource.com/platform/frameworks/base/+/master/proto/src/system_messages.proto
        // https://android.googlesource.com/platform/system/core/+/master/libcutils/include/private/android_filesystem_config.h
        // (AID_APP_END - AID_APP_START) x10 = 100000 < NOTE_NETWORK_AVAILABLE
        return notificationIds.computeIfAbsent(idKey, key -> previousNotificationId++);
    }

    static void notifyModuleUpdated(String modulePackageName,
                                    int moduleUserId,
                                    boolean enabled,
                                    boolean systemModule) {
        try {
            var context = new FakeContext();
            var userInfo = UserService.getUserInfo(moduleUserId);
            String userName = userInfo != null ? userInfo.name : String.valueOf(moduleUserId);
            String title = context.getString(enabled ? systemModule ?
                    R.string.xposed_module_updated_notification_title_system :
                    R.string.xposed_module_updated_notification_title :
                    R.string.module_is_not_activated_yet);
            String content = context.getString(enabled ? systemModule ?
                    R.string.xposed_module_updated_notification_content_system :
                    R.string.xposed_module_updated_notification_content :
                    (moduleUserId == 0 ?
                            R.string.module_is_not_activated_yet_main_user_detailed :
                            R.string.module_is_not_activated_yet_multi_user_detailed), modulePackageName, userName);

            var style = new Notification.BigTextStyle();
            style.bigText(content);

            var notification = new Notification.Builder(context, UPDATED_CHANNEL_ID)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setSmallIcon(getNotificationIcon())
                    .setContentIntent(getModuleIntent(modulePackageName, moduleUserId))
                    .setVisibility(Notification.VISIBILITY_SECRET)
                    .setColor(0xFFF48FB1)
                    .setAutoCancel(true)
                    .setStyle(style)
                    .build();
            notification.extras.putString("android.substName", "LSPosed");
            var nm = getNotificationManager();
            createNotificationChannel(nm);
            nm.enqueueNotificationWithTag("android", opPkg, modulePackageName,
                    pushAndGetNotificationId(UPDATED_CHANNEL_ID, modulePackageName, moduleUserId),
                    notification, 0);
        } catch (Throwable e) {
            Log.e(TAG, "notify module updated", e);
        }
    }

    static void requestModuleScope(String modulePackageName, int moduleUserId, String scopePackageName, IXposedScopeCallback callback) {
        try {
            var context = new FakeContext();
            var userInfo = UserService.getUserInfo(moduleUserId);
            String userName = userInfo != null ? userInfo.name : String.valueOf(moduleUserId);
            String title = context.getString(R.string.xposed_module_request_scope_title);
            String content = context.getString(R.string.xposed_module_request_scope_content, modulePackageName, userName, scopePackageName);

            var style = new Notification.BigTextStyle();
            style.bigText(content);

            var notification = new Notification.Builder(context, SCOPE_CHANNEL_ID)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setSmallIcon(getNotificationIcon())
                    .setVisibility(Notification.VISIBILITY_SECRET)
                    .setColor(0xFFF48FB1)
                    .setAutoCancel(true)
                    .setTimeoutAfter(1000 * 60 * 60)
                    .setStyle(style)
                    .setDeleteIntent(getModuleScopeIntent(modulePackageName, moduleUserId, scopePackageName, "delete", callback))
                    .setActions(new Notification.Action.Builder(
                                    Icon.createWithResource(context, R.drawable.ic_baseline_check_24),
                                    context.getString(R.string.scope_approve),
                                    getModuleScopeIntent(modulePackageName, moduleUserId, scopePackageName, "approve", callback))
                                    .build(),
                            new Notification.Action.Builder(
                                    Icon.createWithResource(context, R.drawable.ic_baseline_close_24),
                                    context.getString(R.string.scope_deny),
                                    getModuleScopeIntent(modulePackageName, moduleUserId, scopePackageName, "deny", callback))
                                    .build(),
                            new Notification.Action.Builder(
                                    Icon.createWithResource(context, R.drawable.ic_baseline_block_24),
                                    context.getString(R.string.nerver_ask_again),
                                    getModuleScopeIntent(modulePackageName, moduleUserId, scopePackageName, "block", callback))
                                    .build()
                    ).build();
            notification.extras.putString("android.substName", "LSPosed");
            var nm = getNotificationManager();
            createNotificationChannel(nm);
            nm.enqueueNotificationWithTag("android", opPkg, modulePackageName,
                    pushAndGetNotificationId(SCOPE_CHANNEL_ID, modulePackageName, moduleUserId),
                    notification, 0);
        } catch (Throwable e) {
            try {
                callback.onScopeRequestFailed(scopePackageName, e.getMessage());
            } catch (RemoteException ignored) {
            }
            Log.e(TAG, "request module scope", e);
        }
    }

    static void cancelNotification(String channel, String modulePackageName, int moduleUserId) {
        try {
            var idKey = getNotificationIdKey(channel, modulePackageName, moduleUserId);
            var idValue = notificationIds.get(idKey);
            if (idValue == null) return;
            var nm = getNotificationManager();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                nm.cancelNotificationWithTag("android", "android", modulePackageName, idValue, 0);
            } else {
                nm.cancelNotificationWithTag("android", modulePackageName, idValue, 0);
            }
            notificationIds.remove(idKey);
        } catch (RemoteException e) {
            Log.e(TAG, "cancel notification", e);
        }
    }
}
