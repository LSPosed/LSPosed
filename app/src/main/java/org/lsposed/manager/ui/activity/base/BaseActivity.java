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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.lsposed.manager.App;
import org.lsposed.manager.BuildConfig;
import org.lsposed.manager.ConfigManager;
import org.lsposed.manager.R;
import org.lsposed.manager.ui.dialog.FlashDialogBuilder;
import org.lsposed.manager.util.NavUtil;
import org.lsposed.manager.util.ThemeUtil;
import org.lsposed.manager.util.UpdateUtil;

import rikka.core.util.ResourceUtils;
import rikka.material.app.MaterialActivity;

public class BaseActivity extends MaterialActivity {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (ThemeUtil.isSystemAccent()) {
            DynamicColors.applyIfAvailable(this);
        }
        // make sure the versions are consistent
        if (BuildConfig.DEBUG) return;
        if (!ConfigManager.isBinderAlive()) return;
        var version = ConfigManager.getXposedVersionCode();
        if (BuildConfig.VERSION_CODE == version) return;
        new MaterialAlertDialogBuilder(this)
                .setMessage(getString(R.string.version_mismatch, version, BuildConfig.VERSION_CODE))
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    if (UpdateUtil.canInstall()) {
                        new FlashDialogBuilder(this, (d, i) -> finish()).show();
                    } else {
                        NavUtil.startURL(this, getString(R.string.about_source));
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
        Bitmap icon = ((BitmapDrawable) getApplicationInfo().loadIcon(getPackageManager())).getBitmap();
        setTaskDescription(new ActivityManager.TaskDescription(getTitle().toString(), icon));
        icon.recycle();
    }

    @Override
    public void onApplyUserThemeResource(@NonNull Resources.Theme theme, boolean isDecorView) {
        theme.applyStyle(ThemeUtil.getNightThemeStyleRes(this), true);
        if (!ThemeUtil.isSystemAccent()) {
            theme.applyStyle(ThemeUtil.getColorThemeStyleRes(), true);
        }
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

        window.getDecorView().post(() -> {
            var rootWindowInsets = window.getDecorView().getRootWindowInsets();
            if (rootWindowInsets != null &&
                    rootWindowInsets.getSystemWindowInsetBottom() >= Resources.getSystem().getDisplayMetrics().density * 40) {
                window.setNavigationBarColor(ResourceUtils.resolveColor(getTheme(), android.R.attr.navigationBarColor) & 0x00ffffff | -0x20000000);
            } else {
                window.setNavigationBarColor(Color.TRANSPARENT);
            }
        });
    }
}
