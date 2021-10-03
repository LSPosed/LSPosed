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

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.snackbar.Snackbar;

import org.lsposed.manager.App;
import org.lsposed.manager.BuildConfig;
import org.lsposed.manager.ConfigManager;
import org.lsposed.manager.R;
import org.lsposed.manager.databinding.DialogAboutBinding;
import org.lsposed.manager.databinding.FragmentHomeBinding;
import org.lsposed.manager.receivers.LSPManagerServiceHolder;
import org.lsposed.manager.ui.dialog.BlurBehindDialogBuilder;
import org.lsposed.manager.ui.dialog.InfoDialogBuilder;
import org.lsposed.manager.ui.dialog.WarningDialogBuilder;
import org.lsposed.manager.util.ModuleUtil;
import org.lsposed.manager.util.NavUtil;
import org.lsposed.manager.util.chrome.LinkTransformationMethod;

import java.util.Locale;

import rikka.core.util.ResourceUtils;

public class HomeFragment extends BaseFragment {

    private FragmentHomeBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        setupToolbar(binding.toolbar, getString(R.string.app_name), R.menu.menu_home);
        binding.toolbar.setNavigationIcon(null);
        binding.appBar.setLiftable(true);
        binding.nestedScrollView.getBorderViewDelegate().setBorderVisibilityChangedListener((top, oldTop, bottom, oldBottom) -> binding.appBar.setLifted(!top));

        Activity activity = requireActivity();
        binding.status.setOnClickListener(v -> {
            if (ConfigManager.isBinderAlive() && !App.needUpdate()) {
                if (!ConfigManager.isSepolicyLoaded() || !ConfigManager.systemServerRequested() || !ConfigManager.dex2oatFlagsLoaded()) {
                    new WarningDialogBuilder(activity).show();
                } else {
                    new InfoDialogBuilder(activity).setTitle(R.string.info).show();
                }
            } else {
                NavUtil.startURL(activity, getString(R.string.about_source));
            }
        });
        binding.modules.setOnClickListener(new StartFragmentListener(R.id.action_modules_fragment, true));
        binding.download.setOnClickListener(new StartFragmentListener(R.id.action_repo_fragment, false));
        binding.logs.setOnClickListener(new StartFragmentListener(R.id.action_logs_fragment, true));
        binding.settings.setOnClickListener(new StartFragmentListener(R.id.action_settings_fragment, false));
        binding.issue.setOnClickListener(view -> NavUtil.startURL(activity, "https://github.com/LSPosed/LSPosed/issues"));

        if (!App.isParasitic() && !App.getPreferences().getBoolean("never_show_shortcut", false)) {
            new BlurBehindDialogBuilder(activity)
                    .setTitle(R.string.parasitic_recommend)
                    .setMessage(R.string.parasitic_recommend_summary)
                    .setNegativeButton(R.string.never_show, (dialog, which) -> App.getPreferences().edit().putBoolean("never_show_shortcut", true).apply())
                    .setNeutralButton(R.string.create_shortcut, (dialog, which) -> {
                        try {
                            LSPManagerServiceHolder.getService().createShortcut();
                        } catch (Throwable e) {
                            Snackbar.make(binding.snackbar, getString(R.string.failed_to_create_shortcut, e.getMessage()), Snackbar.LENGTH_LONG).show();
                        }
                    })
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }

        updateStates(requireActivity(), ConfigManager.isBinderAlive(), App.needUpdate());
        return binding.getRoot();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_refresh) {
            updateStates(requireActivity(), ConfigManager.isBinderAlive(), App.needUpdate());
        } else if (itemId == R.id.menu_info) {
            new InfoDialogBuilder(requireActivity()).setTitle(R.string.info).show();
        } else if (itemId == R.id.menu_about) {
            Activity activity = requireActivity();
            DialogAboutBinding binding = DialogAboutBinding.inflate(LayoutInflater.from(requireActivity()), null, false);
            binding.designAboutTitle.setText(R.string.app_name);
            binding.designAboutInfo.setMovementMethod(LinkMovementMethod.getInstance());
            binding.designAboutInfo.setTransformationMethod(new LinkTransformationMethod(activity));
            binding.designAboutInfo.setText(HtmlCompat.fromHtml(getString(
                    R.string.about_view_source_code,
                    "<b><a href=\"https://github.com/LSPosed/LSPosed\">GitHub</a></b>",
                    "<b><a href=\"https://t.me/LSPosed\">Telegram</a></b>"), HtmlCompat.FROM_HTML_MODE_LEGACY));
            binding.designAboutVersion.setText(String.format(Locale.US, "%s (%s)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
            new BlurBehindDialogBuilder(activity)
                    .setView(binding.getRoot())
                    .show();
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateStates(Activity activity, boolean binderAlive, boolean needUpdate) {
        int cardBackgroundColor;
        if (binderAlive) {
            StringBuilder sb = new StringBuilder(String.format(Locale.US, "%s (%d)",
                    ConfigManager.getXposedVersionName(), ConfigManager.getXposedVersionCode()));
            if (needUpdate) {
                cardBackgroundColor = ResourceUtils.resolveColor(activity.getTheme(), R.attr.colorInstall);
                binding.statusTitle.setText(R.string.need_update);
                binding.statusIcon.setImageResource(R.drawable.ic_round_update_24);
                sb.append("\n\n");
                sb.append(getString(R.string.please_update_summary));
            } else if (!ConfigManager.isSepolicyLoaded() || !ConfigManager.systemServerRequested() || !ConfigManager.dex2oatFlagsLoaded()) {
                cardBackgroundColor = ResourceUtils.resolveColor(activity.getTheme(), rikka.material.R.attr.colorWarning);
                binding.statusTitle.setText(R.string.partial_activated);
                binding.statusIcon.setImageResource(R.drawable.ic_round_warning_24);
                sb.append("\n");
                if (!ConfigManager.isSepolicyLoaded()) {
                    sb.append("\n");
                    sb.append(getString(R.string.selinux_policy_not_loaded_summary));
                }
                if (!ConfigManager.systemServerRequested()) {
                    sb.append("\n");
                    sb.append(getString(R.string.system_inject_fail_summary));
                }
                if (!ConfigManager.dex2oatFlagsLoaded()) {
                    sb.append("\n");
                    sb.append(getString(R.string.system_prop_incorrect_summary));
                }
            } else {
                cardBackgroundColor = ResourceUtils.resolveColor(activity.getTheme(), R.attr.colorNormal);
                binding.statusTitle.setText(R.string.activated);
                binding.statusIcon.setImageResource(R.drawable.ic_round_check_circle_24);
            }
            binding.statusSummary.setText(sb.toString());
        } else {
            cardBackgroundColor = ResourceUtils.resolveColor(activity.getTheme(), R.attr.colorInstall);
            boolean isMagiskInstalled = ConfigManager.isMagiskInstalled();
            binding.statusTitle.setText(isMagiskInstalled ? R.string.install : R.string.not_installed);
            binding.statusSummary.setText(isMagiskInstalled ? R.string.install_summary : R.string.not_install_summary);
            if (!isMagiskInstalled) {
                binding.status.setOnClickListener(null);
                binding.download.setVisibility(View.GONE);
            }
            binding.statusIcon.setImageResource(R.drawable.ic_round_error_outline_24);
            Snackbar.make(binding.snackbar, R.string.lsposed_not_active, Snackbar.LENGTH_INDEFINITE).show();
        }
        binding.status.setCardBackgroundColor(MaterialColors.harmonizeWithPrimary(activity, cardBackgroundColor));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            binding.status.setOutlineSpotShadowColor(MaterialColors.harmonizeWithPrimary(activity, cardBackgroundColor));
            binding.status.setOutlineAmbientShadowColor(MaterialColors.harmonizeWithPrimary(activity, cardBackgroundColor));
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
            if (requireInstalled && !ConfigManager.isBinderAlive()) {
                Snackbar.make(binding.snackbar, R.string.lsposed_not_active, Snackbar.LENGTH_LONG).show();
            } else {
                getNavController().navigate(fragment);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        int moduleCount;
        if (ConfigManager.isBinderAlive()) {
            moduleCount = ModuleUtil.getInstance().getEnabledModulesCount();
        } else {
            moduleCount = 0;
        }
        binding.modulesSummary.setText(getResources().getQuantityString(R.plurals.modules_enabled_count, moduleCount, moduleCount));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        binding = null;
    }
}
