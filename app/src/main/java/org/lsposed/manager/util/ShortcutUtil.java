package org.lsposed.manager.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.graphics.drawable.LayerDrawable;

import org.lsposed.manager.App;
import org.lsposed.manager.R;

public class ShortcutUtil {
    private static final String SHORTCUT_ID = "org.lsposed.manager.shortcut";

    private static Bitmap getBitmap(Context context, int id) {
        var r = context.getResources();
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

    public static void requestPinLaunchShortcut() {
        if (!App.isParasitic()) throw new RuntimeException();
        var context = App.getInstance();
        var info = new ShortcutInfo.Builder(context, SHORTCUT_ID)
                .setShortLabel(context.getString(R.string.app_name))
                .setIntent(getLaunchIntent(context))
                .setActivity(new ComponentName(context.getPackageName(), "android.__dummy__"))
                .setIcon(Icon.createWithAdaptiveBitmap(getBitmap(context, R.drawable.ic_launcher)))
                .build();
        var sm = context.getSystemService(ShortcutManager.class);
        sm.requestPinShortcut(info, null);
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
