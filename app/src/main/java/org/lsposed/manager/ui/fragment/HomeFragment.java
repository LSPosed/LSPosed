/*
 * <!--This file is part of LSPosed.
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
 * Copyright (C) 2021 LSPosed Contributors-->
 */

package org.lsposed.manager.ui.fragment;

import android.os.Build;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;

import org.lsposed.manager.BuildConfig;
import org.lsposed.manager.ConfigManager;
import org.lsposed.manager.R;
import org.lsposed.manager.databinding.DialogAboutBinding;
import org.lsposed.manager.databinding.FragmentHomeBinding;
import org.lsposed.manager.databinding.FragmentMainBinding;
import org.lsposed.manager.ui.activity.base.BaseActivity;
import org.lsposed.manager.ui.dialog.BlurBehindDialogBuilder;
import org.lsposed.manager.ui.dialog.InfoDialogBuilder;
import org.lsposed.manager.util.GlideHelper;
import org.lsposed.manager.util.ModuleUtil;
import org.lsposed.manager.util.NavUtil;
import org.lsposed.manager.util.chrome.LinkTransformationMethod;

import java.util.Locale;

import rikka.core.res.ResourcesKt;

public class HomeFragment extends BaseFragment {

    private FragmentHomeBinding binding;
    private View snackbar;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentMainBinding mainBinding = FragmentMainBinding.inflate(inflater, container, false);
        snackbar = mainBinding.snackbar;
        binding = FragmentHomeBinding.bind(mainBinding.snackbar);
        return mainBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        BaseActivity activity = (BaseActivity) requireActivity();
        binding.status.setOnClickListener(v -> {
            if (ConfigManager.getXposedApiVersion() != -1) {
                new InfoDialogBuilder(activity)
                        .setTitle(R.string.info)
                        .show();
            } else {
                NavUtil.startURL(activity, getString(R.string.about_source));
            }
        });
        binding.modules.setOnClickListener(new StartFragmentListener(R.id.action_modules_fragment, true));
        binding.download.setOnClickListener(new StartFragmentListener(R.id.action_repo_fragment, false));
        binding.logs.setOnClickListener(new StartFragmentListener(R.id.action_logs_fragment, true));
        binding.settings.setOnClickListener(new StartFragmentListener(R.id.action_settings_fragment, false));
        binding.about.setOnClickListener(v -> {
            DialogAboutBinding binding = DialogAboutBinding.inflate(LayoutInflater.from(requireActivity()), null, false);
            binding.sourceCode.setMovementMethod(LinkMovementMethod.getInstance());
            binding.sourceCode.setTransformationMethod(new LinkTransformationMethod(activity));
            binding.sourceCode.setText(HtmlCompat.fromHtml(getString(
                    R.string.about_view_source_code,
                    "<b><a href=\"https://github.com/LSPosed/LSPosed\">GitHub</a></b>",
                    "<b><a href=\"https://t.me/LSPosed\">Telegram</a></b>"), HtmlCompat.FROM_HTML_MODE_LEGACY));
            binding.translators.setMovementMethod(LinkMovementMethod.getInstance());
            binding.translators.setTransformationMethod(new LinkTransformationMethod(activity));
            binding.translators.setText(HtmlCompat.fromHtml(getString(R.string.about_translators, getString(R.string.translators)), HtmlCompat.FROM_HTML_MODE_LEGACY));
            binding.version.setText(String.format(Locale.US, "%s (%s)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
            new BlurBehindDialogBuilder(activity)
                    .setView(binding.getRoot())
                    .show();
        });
        Glide.with(binding.appIcon)
                .load(GlideHelper.wrapApplicationInfoForIconLoader(activity.getApplicationInfo()))
                .into(binding.appIcon);
        String installXposedVersion = ConfigManager.getXposedVersionName();
        int cardBackgroundColor;
        if (installXposedVersion != null) {
            if (!ConfigManager.isSepolicyLoaded()) {
                binding.statusTitle.setText(R.string.partial_activated);
                cardBackgroundColor = ResourcesKt.resolveColor(activity.getTheme(), R.attr.colorWarning);
                binding.statusIcon.setImageResource(R.drawable.ic_warning);
                binding.statusSummary.setText(R.string.selinux_policy_not_loaded_summary);
            } else if (!ConfigManager.systemServerRequested()) {
                binding.statusTitle.setText(R.string.partial_activated);
                cardBackgroundColor = ResourcesKt.resolveColor(activity.getTheme(), R.attr.colorWarning);
                binding.statusIcon.setImageResource(R.drawable.ic_warning);
                binding.statusSummary.setText(R.string.system_inject_fail_summary);
            } else if (!ConfigManager.dex2oatFlagsLoaded()) {
                binding.statusTitle.setText(R.string.partial_activated);
                cardBackgroundColor = ResourcesKt.resolveColor(activity.getTheme(), R.attr.colorWarning);
                binding.statusIcon.setImageResource(R.drawable.ic_warning);
                binding.statusSummary.setText(R.string.system_prop_incorrect_summary);
            } else {
                binding.statusTitle.setText(R.string.activated);
                cardBackgroundColor = ResourcesKt.resolveColor(activity.getTheme(), R.attr.colorNormal);
                binding.statusIcon.setImageResource(R.drawable.ic_check_circle);
                binding.statusSummary.setText(String.format(Locale.US, "%s (%d)", installXposedVersion, ConfigManager.getXposedVersionCode()));
            }
        } else {
            cardBackgroundColor = ResourcesKt.resolveColor(activity.getTheme(), R.attr.colorInstall);
            boolean isMagiskInstalled = ConfigManager.isMagiskInstalled();
            binding.statusTitle.setText(isMagiskInstalled ? R.string.Install : R.string.NotInstall);
            binding.statusSummary.setText(isMagiskInstalled ? R.string.InstallDetail : R.string.NotInstallDetail);
            if (!isMagiskInstalled) {
                binding.status.setOnClickListener(null);
                binding.download.setVisibility(View.GONE);
            }
            binding.statusIcon.setImageResource(R.drawable.ic_error);
            Snackbar.make(snackbar, R.string.lsposed_not_active, Snackbar.LENGTH_INDEFINITE).show();
        }
        binding.status.setCardBackgroundColor(cardBackgroundColor);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            binding.status.setOutlineSpotShadowColor(cardBackgroundColor);
            binding.status.setOutlineAmbientShadowColor(cardBackgroundColor);
        }
    }

    private class StartFragmentListener implements View.OnClickListener {
        boolean requireInstalled;
        int fragment;

        StartFragmentListener(int fragment, boolean requireInstalled) {
            this.fragment = fragment;
            this.requireInstalled = requireInstalled;
        }

        @Override
        public void onClick(View v) {
            if (requireInstalled && ConfigManager.getXposedVersionName() == null) {
                Snackbar.make(snackbar, R.string.lsposed_not_active, Snackbar.LENGTH_LONG).show();
            } else {
                getNavController().navigate(fragment);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        int moduleCount = ModuleUtil.getInstance().getEnabledModulesCount();
        binding.modulesSummary.setText(getResources().getQuantityString(R.plurals.modules_enabled_count, moduleCount, moduleCount));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        binding = null;
    }
}
