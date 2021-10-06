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

import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.lsposed.manager.BuildConfig;
import org.lsposed.manager.ConfigManager;
import org.lsposed.manager.R;
import org.lsposed.manager.util.NavUtil;
import org.lsposed.manager.util.theme.ThemeUtil;

import rikka.core.util.ResourceUtils;
import rikka.material.app.MaterialActivity;

public class BaseActivity extends MaterialActivity {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // make sure the versions are consistent
        if (BuildConfig.DEBUG) return;
        if (!ConfigManager.isBinderAlive()) return;
        var version = ConfigManager.getXposedVersionName();
        if (BuildConfig.VERSION_NAME.equals(version)) return;
        new MaterialAlertDialogBuilder(this)
                .setMessage(BuildConfig.VERSION_NAME.compareTo(version) > 0 ?
                        R.string.outdated_core : R.string.outdated_manager)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    NavUtil.startURL(this, getString(R.string.about_source));
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onApplyUserThemeResource(@NonNull Resources.Theme theme, boolean isDecorView) {
        theme.applyStyle(ThemeUtil.getNightThemeStyleRes(this), true);
        theme.applyStyle(ThemeUtil.getColorThemeStyleRes(), true);
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
            if (window.getDecorView().getRootWindowInsets() != null &&
                    window.getDecorView().getRootWindowInsets().getSystemWindowInsetBottom() >= Resources.getSystem().getDisplayMetrics().density * 40) {
                window.setNavigationBarColor(ResourceUtils.resolveColor(getTheme(), android.R.attr.navigationBarColor) & 0x00ffffff | -0x20000000);
            } else {
                window.setNavigationBarColor(Color.TRANSPARENT);
            }
        });

    }
}
