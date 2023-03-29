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
 * Copyright (C) 2022 LSPosed Contributors
 */

package org.lsposed.manager.util;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;

import androidx.annotation.NonNull;

import org.lsposed.manager.App;
import org.lsposed.manager.R;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShortcutUtil {
    private static final String SHORTCUT_ID = "org.lsposed.manager.shortcut";
    static int iconBitmapSize = pxFromDp(48, App.getInstance().getResources().getDisplayMetrics());

    public static int pxFromDp(float size, DisplayMetrics metrics) {
        return pxFromDp(size, metrics, 1f);
    }

    public static int roundPxValueFromFloat(float value) {
        float fraction = (float) (value - Math.floor(value));
        if (Math.abs(0.5f - fraction) < 0.0001f) {
            // Note: we add for negative values as well, as Math.round brings -.5 to the next
            // "highest" value, e.g. Math.round(-2.5) == -2 [i.e. (int)Math.floor(a + 0.5d)]
            value += 0.0001f;
        }
        return Math.round(value);
    }

    public static int pxFromDp(float size, DisplayMetrics metrics, float scale) {
        float value = scale * TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size, metrics);
        return size < 0 ? -1 : roundPxValueFromFloat(value);
    }

    private static void drawIconBitmap(@NonNull Canvas canvas, @NonNull final Drawable icon,
                                       final float scale) {
        final int size = iconBitmapSize;
        var mOldBounds = new Rect();
        mOldBounds.set(icon.getBounds());

        if (icon instanceof AdaptiveIconDrawable) {
            int offset = Math.max((int) Math.ceil(1.68f / 48 * size),
                    Math.round(size * (1 - scale) / 2));
            // b/211896569: AdaptiveIconDrawable do not work properly for non top-left bounds
            icon.setBounds(0, 0, size - offset - offset, size - offset - offset);
            int count = canvas.save();
            canvas.translate(offset, offset);

            icon.draw(canvas);

            canvas.restoreToCount(count);
        } else {
            if (icon instanceof BitmapDrawable) {
                BitmapDrawable bitmapDrawable = (BitmapDrawable) icon;
                Bitmap b = bitmapDrawable.getBitmap();
                if (b != null && b.getDensity() == Bitmap.DENSITY_NONE) {
                    bitmapDrawable.setTargetDensity(App.getInstance().getResources().getDisplayMetrics());
                }
            }
            int width = size;
            int height = size;

            int intrinsicWidth = icon.getIntrinsicWidth();
            int intrinsicHeight = icon.getIntrinsicHeight();
            if (intrinsicWidth > 0 && intrinsicHeight > 0) {
                // Scale the icon proportionally to the icon dimensions
                final float ratio = (float) intrinsicWidth / intrinsicHeight;
                if (intrinsicWidth > intrinsicHeight) {
                    height = (int) (width / ratio);
                } else if (intrinsicHeight > intrinsicWidth) {
                    width = (int) (height * ratio);
                }
            }
            final int left = (size - width) / 2;
            final int top = (size - height) / 2;
            icon.setBounds(left, top, left + width, top + height);

            canvas.save();
            canvas.scale(scale, scale, size / 2, size / 2);
            icon.draw(canvas);
            canvas.restore();
        }
        icon.setBounds(mOldBounds);
    }

    private static Bitmap getBitmap(Context context, int id) {
        var r = context.getResources();
        var res = r.getDrawable(id, context.getTheme());
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && res instanceof BitmapDrawable) {
            return ((BitmapDrawable) res).getBitmap();
        } else {
            var themed = App.getPreferences().getBoolean("use_themed_icon", false);
            if (res instanceof AdaptiveIconDrawable) {
                Drawable[] layers;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && themed) {
                    var monochromeIconFactory = new MonochromeIconFactory(iconBitmapSize);
                    res = monochromeIconFactory.wrap(res);
                } else {
                    layers = new Drawable[]{((AdaptiveIconDrawable) res).getBackground(),
                            ((AdaptiveIconDrawable) res).getForeground()};
                    res = new LayerDrawable(layers);
                }
            }
            Bitmap bitmap;
            if (themed)
                bitmap = Bitmap.createBitmap(iconBitmapSize, iconBitmapSize, Bitmap.Config.ALPHA_8);
            else
                bitmap = Bitmap.createBitmap(res.getIntrinsicWidth(), res.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            var canvas = new Canvas(bitmap);
            res.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            res.draw(canvas);
//            drawIconBitmap(canvas, res, 1f);
            canvas.setBitmap(null);
            return bitmap;
        }
    }

    private static Intent getLaunchIntent(Context context) {
        var pm = context.getPackageManager();
        var pkg = context.getPackageName();
        var intent = pm.getLaunchIntentForPackage(pkg);
        if (intent == null) {
            try {
                var pkgInfo = pm.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES);
                if (pkgInfo.activities != null) {
                    for (var activityInfo : pkgInfo.activities) {
                        if (activityInfo.processName.equals(activityInfo.packageName)) {
                            intent = new Intent(Intent.ACTION_MAIN);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.setComponent(new ComponentName(pkg, activityInfo.name));
                            break;
                        }
                    }
                }
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        if (intent != null) {
            var categories = intent.getCategories();
            if (categories != null) {
                categories.clear();
            }
            intent.addCategory("org.lsposed.manager.LAUNCH_MANAGER");
            intent.setPackage(pkg);
        }
        return intent;
    }

    @SuppressLint("InlinedApi")
    private static IntentSender registerReceiver(Context context, Runnable task) {
        if (task == null) return null;

        var uuid = UUID.randomUUID().toString();
        var filter = new IntentFilter(uuid);
        var permission = "android.permission.CREATE_USERS";
        var receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                if (!uuid.equals(intent.getAction())) return;
                context.unregisterReceiver(this);
                task.run();
            }
        };
        context.registerReceiver(receiver, filter, permission,
                null/* main thread */, Context.RECEIVER_NOT_EXPORTED);

        App.getMainHandler().postDelayed(() -> {
            if (isLaunchShortcutPinned()) {
                task.run();
            }
        }, 1000);

        var intent = new Intent(uuid);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(context, 0, intent, flags).getIntentSender();
    }

    private static ShortcutInfo.Builder getShortcutBuilder(Context context) {
        var builder = new ShortcutInfo.Builder(context, SHORTCUT_ID)
                .setShortLabel(context.getString(R.string.app_name))
                .setIntent(getLaunchIntent(context))
                .setIcon(Icon.createWithAdaptiveBitmap(getBitmap(context,
                        R.drawable.ic_launcher)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var activity = new ComponentName(context.getPackageName(),
                    "android.app.AppDetailsActivity");
            builder.setActivity(activity);
        }
        return builder;
    }

    public static boolean isRequestPinShortcutSupported(Context context) {
        var sm = context.getSystemService(ShortcutManager.class);
        return sm.isRequestPinShortcutSupported();
    }

    public static boolean requestPinLaunchShortcut(Runnable afterPinned) {
        if (!App.isParasitic) throw new RuntimeException();
        var context = App.getInstance();
        var sm = context.getSystemService(ShortcutManager.class);
        if (!sm.isRequestPinShortcutSupported()) return false;
        return sm.requestPinShortcut(getShortcutBuilder(context).build(),
                registerReceiver(context, afterPinned));
    }

    public static boolean updateShortcut() {
        if (!isLaunchShortcutPinned()) return false;
        Log.d(App.TAG, "update shortcut");
        var context = App.getInstance();
        var sm = context.getSystemService(ShortcutManager.class);
        List<ShortcutInfo> shortcutInfoList = new ArrayList<>();
        shortcutInfoList.add(getShortcutBuilder(context).build());
        return sm.updateShortcuts(shortcutInfoList);
    }

    public static boolean isLaunchShortcutPinned() {
        var context = App.getInstance();
        var sm = context.getSystemService(ShortcutManager.class);
        boolean pinned = false;
        for (var info : sm.getPinnedShortcuts()) {
            if (SHORTCUT_ID.equals(info.getId())) {
                pinned = true;
                break;
            }
        }
        return pinned;
    }
}
