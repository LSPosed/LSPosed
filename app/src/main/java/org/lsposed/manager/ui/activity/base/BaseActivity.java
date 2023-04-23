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

package org.lsposed.manager.ui.activity.base;

import android.app.ActivityManager;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.lsposed.manager.App;
import org.lsposed.manager.BuildConfig;
import org.lsposed.manager.ConfigManager;
import org.lsposed.manager.R;
import org.lsposed.manager.ui.dialog.BlurBehindDialogBuilder;
import org.lsposed.manager.ui.dialog.FlashDialogBuilder;
import org.lsposed.manager.util.NavUtil;
import org.lsposed.manager.util.Telemetry;
import org.lsposed.manager.util.ThemeUtil;
import org.lsposed.manager.util.UpdateUtil;

import rikka.material.app.MaterialActivity;

public class BaseActivity extends MaterialActivity {

    private static Bitmap icon = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        // make sure the versions are consistent
        if (BuildConfig.DEBUG) return;
        if (!ConfigManager.isBinderAlive()) return;
        var xposedVersionCode = ConfigManager.getXposedVersionCode();
        long managerVersionCode = BuildConfig.VERSION_CODE;
        if (!App.isParasitic)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    managerVersionCode = ConfigManager.getPackageInfo(BuildConfig.APPLICATION_ID, PackageManager.GET_META_DATA, 0).getLongVersionCode();
                } else
                    managerVersionCode = ConfigManager.getPackageInfo(BuildConfig.APPLICATION_ID, PackageManager.GET_META_DATA, 0).versionCode;
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        if (managerVersionCode == xposedVersionCode) return;
        new BlurBehindDialogBuilder(this)
                .setMessage(getString(R.string.version_mismatch, xposedVersionCode, managerVersionCode))
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    if (UpdateUtil.canInstall()) {
                        new FlashDialogBuilder(this, (d, i) -> finish()).show();
                    } else {
                        NavUtil.startURL(this, getString(R.string.install_url));
                        finish();
                    }
                })
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        for (var task : getSystemService(ActivityManager.class).getAppTasks()) {
            task.setExcludeFromRecents(false);
        }
        if (icon == null) {
            var drawable = getApplicationInfo().loadIcon(getPackageManager());
            if (drawable instanceof BitmapDrawable) {
                icon = ((BitmapDrawable) drawable).getBitmap();
            } else {
                icon = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                final Canvas canvas = new Canvas(icon);
                drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                drawable.draw(canvas);
            }
        }
        setTaskDescription(new ActivityManager.TaskDescription(getTitle().toString(), icon));
    }

    @Override
    protected void onStop() {
        super.onStop();
        Telemetry.trackEvent("BaseActivity stop", null);
    }

    @Override
    public void onApplyUserThemeResource(@NonNull Resources.Theme theme, boolean isDecorView) {
        if (!ThemeUtil.isSystemAccent()) {
            theme.applyStyle(ThemeUtil.getColorThemeStyleRes(), true);
        }
        theme.applyStyle(ThemeUtil.getNightThemeStyleRes(this), true);
        theme.applyStyle(rikka.material.preference.R.style.ThemeOverlay_Rikka_Material3_Preference, true);
    }

    @Override
    public String computeUserThemeKey() {
        return ThemeUtil.getColorTheme() + ThemeUtil.getNightTheme(this);
    }

    @Override
    public void onApplyTranslucentSystemBars() {
        super.onApplyTranslucentSystemBars();
        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
    }
}
