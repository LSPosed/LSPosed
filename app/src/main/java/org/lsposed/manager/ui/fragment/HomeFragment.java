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
import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.text.HtmlCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.DialogFragment;

import org.lsposed.lspd.ILSPManagerService;
import org.lsposed.manager.BuildConfig;
import org.lsposed.manager.ConfigManager;
import org.lsposed.manager.R;
import org.lsposed.manager.databinding.DialogAboutBinding;
import org.lsposed.manager.databinding.FragmentHomeBinding;
import org.lsposed.manager.ui.dialog.BlurBehindDialogBuilder;
import org.lsposed.manager.ui.dialog.FlashDialogBuilder;
import org.lsposed.manager.ui.dialog.WelcomeDialog;
import org.lsposed.manager.util.NavUtil;
import org.lsposed.manager.util.Telemetry;
import org.lsposed.manager.util.UpdateUtil;
import org.lsposed.manager.util.chrome.LinkTransformationMethod;

import java.util.Arrays;
import java.util.HashMap;

import rikka.core.util.ClipboardUtils;
import rikka.material.app.LocaleDelegate;

public class HomeFragment extends BaseFragment implements MenuProvider {
    private FragmentHomeBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WelcomeDialog.showIfNeed(getChildFragmentManager());
    }

    @Override
    public void onPrepareMenu(Menu menu) {
        menu.findItem(R.id.menu_about).setOnMenuItemClickListener(v -> {
            showAbout();
            return true;
        });
        menu.findItem(R.id.menu_issue).setOnMenuItemClickListener(v -> {
            NavUtil.startURL(requireActivity(), "https://github.com/LSPosed/LSPosed/issues/new/choose");
            return true;
        });
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {

    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        return false;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        setupToolbar(binding.toolbar, binding.clickView, R.string.app_name, R.menu.menu_home);
        binding.toolbar.setNavigationIcon(null);
        binding.toolbar.setOnClickListener(v -> showAbout());
        binding.clickView.setOnClickListener(v -> showAbout());
        binding.appBar.setLiftable(true);
        binding.nestedScrollView.getBorderViewDelegate().setBorderVisibilityChangedListener((top, oldTop, bottom, oldBottom) -> binding.appBar.setLifted(!top));

        updateStates(requireActivity(), ConfigManager.isBinderAlive(), UpdateUtil.needUpdate());

        return binding.getRoot();
    }

    private void updateStates(Activity activity, boolean binderAlive, boolean needUpdate) {
        if (binderAlive) {
            if (needUpdate) {
                binding.updateTitle.setText(R.string.need_update);
                binding.updateSummary.setText(getString(R.string.please_update_summary));
                binding.statusIcon.setImageResource(R.drawable.ic_round_update_24);
                binding.updateBtn.setOnClickListener(v -> {
                    if (UpdateUtil.canInstall()) {
                        new FlashDialogBuilder(activity, null).show();
                    } else {
                        NavUtil.startURL(activity, getString(R.string.latest_url));
                    }
                });
                binding.updateCard.setVisibility(View.VISIBLE);
            } else {
                binding.updateCard.setVisibility(View.GONE);
            }
            boolean dex2oatAbnormal = ConfigManager.getDex2OatWrapperCompatibility() != ILSPManagerService.DEX2OAT_OK && !ConfigManager.dex2oatFlagsLoaded();
            var sepolicyAbnormal = !ConfigManager.isSepolicyLoaded();
            var systemServerAbnormal = !ConfigManager.systemServerRequested();
            if (sepolicyAbnormal || systemServerAbnormal || dex2oatAbnormal) {
                binding.statusTitle.setText(R.string.partial_activated);
                binding.statusIcon.setImageResource(R.drawable.ic_round_warning_24);
                binding.warningCard.setVisibility(View.VISIBLE);
                if (sepolicyAbnormal) {
                    binding.warningTitle.setText(R.string.selinux_policy_not_loaded_summary);
                    binding.warningSummary.setText(HtmlCompat.fromHtml(getString(R.string.selinux_policy_not_loaded), HtmlCompat.FROM_HTML_MODE_LEGACY));
                }
                if (systemServerAbnormal) {
                    binding.warningTitle.setText(R.string.system_inject_fail_summary);
                    binding.warningSummary.setText(HtmlCompat.fromHtml(getString(R.string.system_inject_fail), HtmlCompat.FROM_HTML_MODE_LEGACY));
                }
                if (dex2oatAbnormal) {
                    binding.warningTitle.setText(R.string.system_prop_incorrect_summary);
                    binding.warningSummary.setText(HtmlCompat.fromHtml(getString(R.string.system_prop_incorrect), HtmlCompat.FROM_HTML_MODE_LEGACY));
                }
            } else {
                binding.warningCard.setVisibility(View.GONE);
                binding.statusTitle.setText(R.string.activated);
                binding.statusIcon.setImageResource(R.drawable.ic_round_check_circle_24);
            }
            binding.statusSummary.setText(String.format(LocaleDelegate.getDefaultLocale(), "%s (%d) - %s",
                    ConfigManager.getXposedVersionName(), ConfigManager.getXposedVersionCode(), ConfigManager.getApi()));
        } else {
            boolean isMagiskInstalled = ConfigManager.isMagiskInstalled();
            if (isMagiskInstalled) {
                binding.updateTitle.setText(R.string.install);
                binding.updateSummary.setText(R.string.install_summary);
                binding.statusIcon.setImageResource(R.drawable.ic_round_error_outline_24);
                binding.updateBtn.setOnClickListener(v -> {
                    if (UpdateUtil.canInstall()) {
                        new FlashDialogBuilder(activity, null).show();
                    } else {
                        NavUtil.startURL(activity, getString(R.string.install_url));
                    }
                });
                binding.updateCard.setVisibility(View.VISIBLE);
            } else {
                binding.updateCard.setVisibility(View.GONE);
            }
            binding.warningCard.setVisibility(View.GONE);
            binding.statusTitle.setText(R.string.not_installed);
            binding.statusSummary.setText(R.string.not_install_summary);
        }

        if (ConfigManager.isBinderAlive()) {
            binding.apiVersion.setText(String.valueOf(ConfigManager.getXposedApiVersion()));
            binding.api.setText(ConfigManager.isDexObfuscateEnabled() ? R.string.enabled : R.string.not_enabled);
            binding.frameworkVersion.setText(String.format(LocaleDelegate.getDefaultLocale(), "%1$s (%2$d)", ConfigManager.getXposedVersionName(), ConfigManager.getXposedVersionCode()));
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                binding.dex2oatWrapper.setText(String.format(LocaleDelegate.getDefaultLocale(), "%s (%s)", getString(R.string.unsupported), getString(R.string.android_version_unsatisfied)));
            } else switch (ConfigManager.getDex2OatWrapperCompatibility()) {
                case ILSPManagerService.DEX2OAT_OK:
                    binding.dex2oatWrapper.setText(R.string.supported);
                    break;
                case ILSPManagerService.DEX2OAT_CRASHED:
                    binding.dex2oatWrapper.setText(String.format(LocaleDelegate.getDefaultLocale(), "%s (%s)", getString(R.string.unsupported), getString(R.string.crashed)));
                    break;
                case ILSPManagerService.DEX2OAT_MOUNT_FAILED:
                    binding.dex2oatWrapper.setText(String.format(LocaleDelegate.getDefaultLocale(), "%s (%s)", getString(R.string.unsupported), getString(R.string.mount_failed)));
                    break;
                case ILSPManagerService.DEX2OAT_SELINUX_PERMISSIVE:
                    binding.dex2oatWrapper.setText(String.format(LocaleDelegate.getDefaultLocale(), "%s (%s)", getString(R.string.unsupported), getString(R.string.selinux_permissive)));
                    break;
                case ILSPManagerService.DEX2OAT_SEPOLICY_INCORRECT:
                    binding.dex2oatWrapper.setText(String.format(LocaleDelegate.getDefaultLocale(), "%s (%s)", getString(R.string.unsupported), getString(R.string.sepolicy_incorrect)));
                    break;
            }
        } else {
            binding.apiVersion.setText(R.string.not_installed);
            binding.api.setText(R.string.not_installed);
            binding.frameworkVersion.setText(R.string.not_installed);
        }
        binding.managerVersion.setText(String.format(LocaleDelegate.getDefaultLocale(), "%1$s (%2$d)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));

        if (Build.VERSION.PREVIEW_SDK_INT != 0) {
            binding.systemVersion.setText(String.format(LocaleDelegate.getDefaultLocale(), "%1$s Preview (API %2$d)", Build.VERSION.CODENAME, Build.VERSION.SDK_INT));
        } else {
            binding.systemVersion.setText(String.format(LocaleDelegate.getDefaultLocale(), "%1$s (API %2$d)", Build.VERSION.RELEASE, Build.VERSION.SDK_INT));
        }

        binding.device.setText(getDevice());
        binding.systemAbi.setText(Build.SUPPORTED_ABIS[0]);
        String info = activity.getString(R.string.info_api_version) +
                "\n" +
                binding.apiVersion.getText() +
                "\n\n" +
                activity.getString(R.string.settings_xposed_api_call_protection) +
                "\n" +
                binding.api.getText() +
                "\n\n" +
                activity.getString(R.string.info_dex2oat_wrapper) +
                "\n" +
                binding.dex2oatWrapper.getText() +
                "\n\n" +
                activity.getString(R.string.info_framework_version) +
                "\n" +
                binding.frameworkVersion.getText() +
                "\n\n" +
                activity.getString(R.string.info_manager_version) +
                "\n" +
                binding.managerVersion.getText() +
                "\n\n" +
                activity.getString(R.string.info_system_version) +
                "\n" +
                binding.systemVersion.getText() +
                "\n\n" +
                activity.getString(R.string.info_device) +
                "\n" +
                binding.device.getText() +
                "\n\n" +
                activity.getString(R.string.info_system_abi) +
                "\n" +
                binding.systemAbi.getText();
        var map = new HashMap<String, String>();
        map.put("apiVersion", binding.apiVersion.getText().toString());
        map.put("api", binding.api.getText().toString());
        map.put("frameworkVersion", binding.frameworkVersion.getText().toString());
        map.put("systemAbi", Arrays.toString(Build.SUPPORTED_ABIS));
        Telemetry.trackEvent("HomeFragment", map);
        binding.copyInfo.setOnClickListener(v -> {
            ClipboardUtils.put(activity, info);
            showHint(R.string.info_copied, false);
        });
    }

    private String getDevice() {
        String manufacturer = Character.toUpperCase(Build.MANUFACTURER.charAt(0)) + Build.MANUFACTURER.substring(1);
        if (!Build.BRAND.equals(Build.MANUFACTURER)) {
            manufacturer += " " + Character.toUpperCase(Build.BRAND.charAt(0)) + Build.BRAND.substring(1);
        }
        manufacturer += " " + Build.MODEL + " ";
        return manufacturer;
    }

    public static class AboutDialog extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            DialogAboutBinding binding = DialogAboutBinding.inflate(getLayoutInflater(), null, false);
            binding.designAboutTitle.setText(R.string.app_name);
            binding.designAboutInfo.setMovementMethod(LinkMovementMethod.getInstance());
            binding.designAboutInfo.setTransformationMethod(new LinkTransformationMethod(requireActivity()));
            binding.designAboutInfo.setText(HtmlCompat.fromHtml(getString(
                    R.string.about_view_source_code,
                    "<b><a href=\"https://github.com/LSPosed/LSPosed\">GitHub</a></b>",
                    "<b><a href=\"https://t.me/LSPosed\">Telegram</a></b>"), HtmlCompat.FROM_HTML_MODE_LEGACY));
            binding.designAboutVersion.setText(String.format(LocaleDelegate.getDefaultLocale(), "%s (%d)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
            return new BlurBehindDialogBuilder(requireContext())
                    .setView(binding.getRoot()).create();
        }
    }

    private void showAbout() {
        new AboutDialog().show(getChildFragmentManager(), "about");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
