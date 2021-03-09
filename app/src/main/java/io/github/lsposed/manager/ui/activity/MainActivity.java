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

package io.github.lsposed.manager.ui.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;

import androidx.core.text.HtmlCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.util.Arrays;
import java.util.Locale;

import io.github.lsposed.manager.ConfigManager;
import io.github.lsposed.manager.R;
import io.github.lsposed.manager.databinding.ActivityMainBinding;
import io.github.lsposed.manager.databinding.DialogAboutBinding;
import io.github.lsposed.manager.ui.activity.base.BaseActivity;
import io.github.lsposed.manager.ui.dialog.BlurBehindDialogBuilder;
import io.github.lsposed.manager.ui.dialog.StatusDialogBuilder;
import io.github.lsposed.manager.util.GlideHelper;
import io.github.lsposed.manager.util.ModuleUtil;
import io.github.lsposed.manager.util.NavUtil;
import io.github.lsposed.manager.util.chrome.LinkTransformationMethod;
import name.mikanoshi.customiuizer.holidays.HolidayHelper;
import name.mikanoshi.customiuizer.utils.Helpers;
import rikka.core.res.ResourcesKt;

public class MainActivity extends BaseActivity {
    ActivityMainBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        HolidayHelper.setup(this);
        binding.status.setOnClickListener(v -> {
            if (ConfigManager.getXposedApiVersion() != -1) {
                new StatusDialogBuilder(this)
                        .setTitle(R.string.info)
                        .show();
            } else {
                NavUtil.startURL(this, getString(R.string.about_source));
            }
        });
        binding.modules.setOnClickListener(new StartActivityListener(ModulesActivity.class, true));
        binding.download.setOnClickListener(new StartActivityListener(RepoActivity.class, false));
        binding.logs.setOnClickListener(new StartActivityListener(LogsActivity.class, true));
        binding.settings.setOnClickListener(new StartActivityListener(SettingsActivity.class, false));
        binding.about.setOnClickListener(v -> {
            DialogAboutBinding binding = DialogAboutBinding.inflate(LayoutInflater.from(this), null, false);
            binding.sourceCode.setMovementMethod(LinkMovementMethod.getInstance());
            binding.sourceCode.setTransformationMethod(new LinkTransformationMethod(this));
            binding.sourceCode.setText(HtmlCompat.fromHtml(getString(
                    R.string.about_view_source_code,
                    "<b><a href=\"https://github.com/LSPosed/LSPosed\">GitHub</a></b>",
                    "<b><a href=\"https://t.me/LSPosed\">Telegram</a></b>"), HtmlCompat.FROM_HTML_MODE_LEGACY));
            new BlurBehindDialogBuilder(this)
                    .setView(binding.getRoot())
                    .show();
        });
        Glide.with(binding.appIcon)
                .load(GlideHelper.wrapApplicationInfoForIconLoader(getApplicationInfo()))
                .into(binding.appIcon);
        String installXposedVersion = ConfigManager.getXposedVersionName();
        int cardBackgroundColor;
        if (installXposedVersion != null) {
            binding.statusTitle.setText(getString(R.string.Activated, "YAHFA"));
            if (!ConfigManager.isPermissive()) {
                if (Helpers.currentHoliday == Helpers.Holidays.LUNARNEWYEAR) {
                    cardBackgroundColor = 0xfff05654;
                } else {
                    cardBackgroundColor = ResourcesKt.resolveColor(getTheme(), R.attr.colorNormal);
                }
                binding.statusIcon.setImageResource(R.drawable.ic_check_circle);
                binding.statusSummary.setText(String.format(Locale.US, "%s (%d)", installXposedVersion, ConfigManager.getXposedVersionCode()));
            } else {
                cardBackgroundColor = ResourcesKt.resolveColor(getTheme(), R.attr.colorError);
                binding.statusIcon.setImageResource(R.drawable.ic_warning);
                binding.statusSummary.setText(R.string.selinux_permissive_summary);
            }
        } else {
            cardBackgroundColor = ResourcesKt.resolveColor(getTheme(), R.attr.colorInstall);
            boolean isMagiskInstalled = Arrays.stream(System.getenv("PATH").split(File.pathSeparator))
                    .anyMatch(str -> new File(str, "magisk").exists());
            binding.statusTitle.setText(isMagiskInstalled ? R.string.Install : R.string.NotInstall);
            binding.statusSummary.setText(isMagiskInstalled ? R.string.InstallDetail : R.string.NotInstallDetail);
            if (!isMagiskInstalled) {
                binding.status.setOnClickListener(null);
                binding.download.setVisibility(View.GONE);
            }
            binding.statusIcon.setImageResource(R.drawable.ic_error);
            Snackbar.make(binding.snackbar, R.string.lsposed_not_active, Snackbar.LENGTH_LONG).show();
        }
        binding.status.setCardBackgroundColor(cardBackgroundColor);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            binding.status.setOutlineSpotShadowColor(cardBackgroundColor);
            binding.status.setOutlineAmbientShadowColor(cardBackgroundColor);
        }
    }

    private class StartActivityListener implements View.OnClickListener {
        boolean requireInstalled;
        Class<?> clazz;

        StartActivityListener(Class<?> clazz, boolean requireInstalled) {
            this.clazz = clazz;
            this.requireInstalled = requireInstalled;
        }

        @Override
        public void onClick(View v) {
            if (requireInstalled && ConfigManager.getXposedVersionName() == null) {
                Snackbar.make(binding.snackbar, R.string.lsposed_not_active, Snackbar.LENGTH_LONG).show();
            } else {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, clazz);
                startActivity(intent);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        int moduleCount = ModuleUtil.getInstance().getEnabledModules().size();
        binding.modulesSummary.setText(getResources().getQuantityString(R.plurals.modules_enabled_count, moduleCount, moduleCount));
        HolidayHelper.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        HolidayHelper.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        HolidayHelper.onPause();
    }
}
