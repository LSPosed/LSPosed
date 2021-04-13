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

package org.lsposed.manager.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import org.lsposed.manager.R;
import org.lsposed.manager.ui.activity.AppListActivity;

public final class NotificationUtil {

    public static final int NOTIFICATION_MODULE_NOT_ACTIVATED_YET = 0;
    private static final int NOTIFICATION_MODULES_UPDATED = 1;
    private static final int PENDING_INTENT_OPEN_APP_LIST = 0;
    private static final String NOTIFICATION_MODULES_CHANNEL = "modules_channel_2";

    public static void showNotification(Context context, String modulePackageName, String moduleName, boolean enabled) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel channel = new NotificationChannel(NOTIFICATION_MODULES_CHANNEL,
                context.getString(R.string.Modules),
                NotificationManager.IMPORTANCE_HIGH);
        channel.setSound(null, null);
        channel.setVibrationPattern(null);
        notificationManager.createNotificationChannel(channel);

        String title = context.getString(enabled ? R.string.xposed_module_updated_notification_title : R.string.module_is_not_activated_yet);
        String content = context.getString(enabled ? R.string.xposed_module_updated_notification_content : R.string.module_is_not_activated_yet_detailed, moduleName);

        Intent intent = new Intent(context, AppListActivity.class)
                .putExtra("modulePackageName", modulePackageName)
                .putExtra("moduleName", moduleName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent contentIntent = PendingIntent.getActivity(context, PENDING_INTENT_OPEN_APP_LIST, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle();
        style.bigText(content);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_MODULES_CHANNEL)
                .setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_extension)
                .setColor(context.getColor(R.color.color_primary))
                .setContentIntent(contentIntent)
                .setStyle(style);

        notificationManager.notify(modulePackageName, enabled ? NOTIFICATION_MODULES_UPDATED : NOTIFICATION_MODULE_NOT_ACTIVATED_YET, builder.build());
    }
}
