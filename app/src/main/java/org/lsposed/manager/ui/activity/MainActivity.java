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

package org.lsposed.manager.ui.activity;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;

import androidx.core.text.HtmlCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;

import org.lsposed.manager.BuildConfig;
import org.lsposed.manager.ConfigManager;
import org.lsposed.manager.R;
import org.lsposed.manager.databinding.ActivityMainBinding;
import org.lsposed.manager.databinding.DialogAboutBinding;
import org.lsposed.manager.ui.activity.base.BaseActivity;
import org.lsposed.manager.ui.dialog.BlurBehindDialogBuilder;
import org.lsposed.manager.ui.dialog.InfoDialogBuilder;
import org.lsposed.manager.util.GlideHelper;
import org.lsposed.manager.util.ModuleUtil;
import org.lsposed.manager.util.NavUtil;
import org.lsposed.manager.util.chrome.LinkTransformationMethod;
import org.lsposed.manager.util.holiday.HolidayHelper;

import java.util.Locale;

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
                new InfoDialogBuilder(this)
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
            binding.translators.setMovementMethod(LinkMovementMethod.getInstance());
            binding.translators.setTransformationMethod(new LinkTransformationMethod(this));
            binding.translators.setText(HtmlCompat.fromHtml(getString(R.string.about_translators, getString(R.string.translators)), HtmlCompat.FROM_HTML_MODE_LEGACY));
            binding.version.setText(String.format(Locale.US, "%s (%s)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
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
            if (ConfigManager.isPermissive()) {
                cardBackgroundColor = ResourcesKt.resolveColor(getTheme(), R.attr.colorError);
                binding.statusTitle.setText(R.string.activated);
                binding.statusIcon.setImageResource(R.drawable.ic_warning);
                binding.statusSummary.setText(R.string.selinux_permissive_summary);
            } else if (!ConfigManager.isSepolicyLoaded()) {
                binding.statusTitle.setText(R.string.partial_activated);
                cardBackgroundColor = ResourcesKt.resolveColor(getTheme(), R.attr.colorWarning);
                binding.statusIcon.setImageResource(R.drawable.ic_warning);
                binding.statusSummary.setText(R.string.selinux_policy_not_loaded_summary);
            } else {
                binding.statusTitle.setText(R.string.activated);
                HolidayHelper.CardColors cardColors = HolidayHelper.getHolidayColors();
                if (cardColors.textColor != 0) {
                    binding.statusIcon.setImageTintList(ColorStateList.valueOf(cardColors.textColor));
                    binding.statusTitle.setTextColor(ColorStateList.valueOf(cardColors.textColor));
                    binding.statusSummary.setTextColor(ColorStateList.valueOf(cardColors.textColor));
                }
                if (cardColors.backgroundColor != 0) {
                    cardBackgroundColor = cardColors.backgroundColor;
                } else {
                    cardBackgroundColor = ResourcesKt.resolveColor(getTheme(), R.attr.colorNormal);
                }
                binding.statusIcon.setImageResource(R.drawable.ic_check_circle);
                binding.statusSummary.setText(String.format(Locale.US, "%s (%d)", installXposedVersion, ConfigManager.getXposedVersionCode()));
            }
        } else {
            cardBackgroundColor = ResourcesKt.resolveColor(getTheme(), R.attr.colorInstall);
            boolean isMagiskInstalled = ConfigManager.isMagiskInstalled();
            binding.statusTitle.setText(isMagiskInstalled ? R.string.Install : R.string.NotInstall);
            binding.statusSummary.setText(isMagiskInstalled ? R.string.InstallDetail : R.string.NotInstallDetail);
            if (!isMagiskInstalled) {
                binding.status.setOnClickListener(null);
                binding.download.setVisibility(View.GONE);
            }
            binding.statusIcon.setImageResource(R.drawable.ic_error);
            Snackbar.make(binding.snackbar, R.string.lsposed_not_active, Snackbar.LENGTH_INDEFINITE).show();
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
        int moduleCount = ModuleUtil.getInstance().getEnabledModulesCount();
        binding.modulesSummary.setText(getResources().getQuantityString(R.plurals.modules_enabled_count, moduleCount, moduleCount));
    }
}
