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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.text.HtmlCompat;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.snackbar.Snackbar;

import org.lsposed.manager.App;
import org.lsposed.manager.BuildConfig;
import org.lsposed.manager.ConfigManager;
import org.lsposed.manager.R;
import org.lsposed.manager.databinding.DialogAboutBinding;
import org.lsposed.manager.databinding.FragmentHomeBinding;
import org.lsposed.manager.repo.RepoLoader;
import org.lsposed.manager.ui.dialog.BlurBehindDialogBuilder;
import org.lsposed.manager.ui.dialog.FlashDialogBuilder;
import org.lsposed.manager.ui.dialog.InfoDialogBuilder;
import org.lsposed.manager.ui.dialog.ShortcutDialogBuilder;
import org.lsposed.manager.ui.dialog.WarningDialogBuilder;
import org.lsposed.manager.util.ModuleUtil;
import org.lsposed.manager.util.NavUtil;
import org.lsposed.manager.util.UpdateUtil;
import org.lsposed.manager.util.chrome.LinkTransformationMethod;

import java.util.HashSet;
import java.util.Locale;

import rikka.core.util.ResourceUtils;

public class HomeFragment extends BaseFragment implements RepoLoader.Listener {

    private FragmentHomeBinding binding;

    private static final RepoLoader repoLoader = RepoLoader.getInstance();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ShortcutDialogBuilder.showIfNeed(requireContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        setupToolbar(binding.toolbar, getString(R.string.app_name), R.menu.menu_home);
        binding.toolbar.setNavigationIcon(null);
        binding.appBar.setLiftable(true);
        binding.nestedScrollView.getBorderViewDelegate().setBorderVisibilityChangedListener((top, oldTop, bottom, oldBottom) -> binding.appBar.setLifted(!top));

        Activity activity = requireActivity();
        binding.status.setOnClickListener(v -> {
            if (ConfigManager.isBinderAlive() && !UpdateUtil.needUpdate()) {
                if (!ConfigManager.isSepolicyLoaded() || !ConfigManager.systemServerRequested() || !ConfigManager.dex2oatFlagsLoaded()) {
                    new WarningDialogBuilder(activity).show();
                } else {
                    new InfoDialogBuilder(activity).show();
                }
            } else {
                if (UpdateUtil.canInstall()) {
                    new FlashDialogBuilder(activity, null).show();
                    return;
                }
                NavUtil.startURL(activity, getString(R.string.about_source));
            }
        });
        binding.status.setOnLongClickListener(v -> {
            if (UpdateUtil.canInstall()) {
                new FlashDialogBuilder(activity, null).show();
                return true;
            }
            return false;
        });
        binding.modules.setOnClickListener(new StartFragmentListener(R.id.action_modules_fragment, true));
        binding.download.setOnClickListener(new StartFragmentListener(R.id.action_repo_fragment, false));
        binding.logs.setOnClickListener(new StartFragmentListener(R.id.action_logs_fragment, true));
        binding.settings.setOnClickListener(new StartFragmentListener(R.id.action_settings_fragment, false));
        binding.issue.setOnClickListener(view -> NavUtil.startURL(activity, "https://github.com/LSPosed/LSPosed/issues"));

        updateStates(requireActivity(), ConfigManager.isBinderAlive(), UpdateUtil.needUpdate());

        repoLoader.addListener(this);
        if (repoLoader.isRepoLoaded()) {
            repoLoaded();
        }
        return binding.getRoot();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_refresh) {
            updateStates(requireActivity(), ConfigManager.isBinderAlive(), UpdateUtil.needUpdate());
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
            binding.designAboutVersion.setText(String.format(Locale.ROOT, "%s (%s)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
            new BlurBehindDialogBuilder(activity)
                    .setView(binding.getRoot())
                    .show();
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateStates(Activity activity, boolean binderAlive, boolean needUpdate) {
        int cardBackgroundColor;
        if (binderAlive) {
            StringBuilder sb = new StringBuilder(String.format(Locale.ROOT, "%s (%d) - %s",
                    ConfigManager.getXposedVersionName(), ConfigManager.getXposedVersionCode(), ConfigManager.getApi()));
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
        cardBackgroundColor = MaterialColors.harmonizeWithPrimary(activity, cardBackgroundColor);
        binding.status.setCardBackgroundColor(cardBackgroundColor);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            binding.status.setOutlineSpotShadowColor(cardBackgroundColor);
            binding.status.setOutlineAmbientShadowColor(cardBackgroundColor);
        }
    }

    @Override
    public void repoLoaded() {
        final int[] count = new int[]{0};
        HashSet<String> processedModules = new HashSet<>();
        ModuleUtil.getInstance().getModules().forEach((k, v) -> {
                    if (!processedModules.contains(k.first)) {
                        var ver = repoLoader.getModuleLatestVersion(k.first);
                        if (ver != null && ver.upgradable(v.versionCode, v.versionName)) {
                            ++count[0];
                        }
                        processedModules.add(k.first);
                    }
                }
        );
        runOnUiThread(() -> {
            if (count[0] > 0) {
                binding.downloadSummary.setText(getResources().getQuantityString(R.plurals.module_repo_upgradable, count[0], count[0]));
            } else {
                onThrowable(null);
            }
        });
    }

    @Override
    public void onThrowable(Throwable t) {
        runOnUiThread(() -> binding.downloadSummary.setText(getResources().getString(R.string.module_repo_up_to_date)));
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
        if (ConfigManager.isBinderAlive()) {
            var t = new Thread(() -> {
                try {
                    var moduleCount = ModuleUtil.getInstance().getEnabledModulesCount();
                    runOnUiThread(() -> binding.modulesSummary.setText(getResources().getQuantityString(R.plurals.modules_enabled_count, moduleCount, moduleCount)));
                } catch (InterruptedException e) {
                    Log.e(App.TAG, "getEnabledModulesCount: ", e);
                }
            });
            t.start();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        repoLoader.removeListener(this);
        binding = null;
    }
}
