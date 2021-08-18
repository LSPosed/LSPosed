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

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.takisoft.preferencex.PreferenceFragmentCompat;

import org.lsposed.manager.App;
import org.lsposed.manager.BuildConfig;
import org.lsposed.manager.ConfigManager;
import org.lsposed.manager.R;
import org.lsposed.manager.databinding.FragmentSettingsBinding;
import org.lsposed.manager.ui.activity.MainActivity;
import org.lsposed.manager.util.BackupUtils;
import org.lsposed.manager.util.theme.ThemeUtil;

import java.time.LocalDateTime;
import java.util.Locale;

import rikka.core.util.ResourceUtils;
import rikka.material.app.DayNightDelegate;
import rikka.recyclerview.RecyclerViewKt;
import rikka.widget.borderview.BorderRecyclerView;

public class SettingsFragment extends BaseFragment {
    FragmentSettingsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        binding.getRoot().bringChildToFront(binding.appBar);
        setupToolbar(binding.toolbar, R.string.Settings);
        if (savedInstanceState == null) {
            getChildFragmentManager().beginTransaction()
                    .add(R.id.container, new PreferenceFragment()).commit();
        }
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        binding = null;
    }

    public static class PreferenceFragment extends PreferenceFragmentCompat {
        ActivityResultLauncher<String> backupLauncher = registerForActivityResult(new ActivityResultContracts.CreateDocument(),
                uri -> {
                    if (uri != null) {
                        try {
                            // grantUriPermission might throw RemoteException on MIUI
                            requireActivity().grantUriPermission(BuildConfig.APPLICATION_ID, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        AlertDialog alertDialog = new AlertDialog.Builder(requireActivity())
                                .setCancelable(false)
                                .setMessage(R.string.settings_backuping)
                                .show();
                        AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
                            boolean success = BackupUtils.backup(requireContext(), uri);
                            try {
                                SettingsFragment fragment = (SettingsFragment) getParentFragment();
                                requireActivity().runOnUiThread(() -> {
                                    alertDialog.dismiss();
                                    fragment.makeSnackBar(success ? R.string.settings_backup_success : R.string.settings_backup_failed, Snackbar.LENGTH_SHORT);
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    }
                });
        ActivityResultLauncher<String[]> restoreLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        try {
                            // grantUriPermission might throw RemoteException on MIUI
                            requireActivity().grantUriPermission(BuildConfig.APPLICATION_ID, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        AlertDialog alertDialog = new AlertDialog.Builder(requireActivity())
                                .setCancelable(false)
                                .setMessage(R.string.settings_restoring)
                                .show();
                        AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
                            boolean success = BackupUtils.restore(requireContext(), uri);
                            try {
                                SettingsFragment fragment = (SettingsFragment) getParentFragment();
                                requireActivity().runOnUiThread(() -> {
                                    alertDialog.dismiss();
                                    fragment.makeSnackBar(success ? R.string.settings_restore_success : R.string.settings_restore_failed, Snackbar.LENGTH_SHORT);
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    }
                });

        @Override
        public void onCreatePreferencesFix(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.prefs);

            boolean installed = ConfigManager.isBinderAlive();
            SwitchPreference prefVerboseLogs = findPreference("disable_verbose_log");
            if (prefVerboseLogs != null) {
                if (requireActivity().getApplicationInfo().uid / 100000 != 0) {
                    prefVerboseLogs.setVisible(false);
                } else {
                    prefVerboseLogs.setEnabled(installed);
                    prefVerboseLogs.setChecked(!installed || !ConfigManager.isVerboseLogEnabled());
                    prefVerboseLogs.setOnPreferenceChangeListener((preference, newValue) -> {
                        boolean result = ConfigManager.setVerboseLogEnabled(!(boolean) newValue);
                        SettingsFragment fragment = (SettingsFragment) getParentFragment();
                        if (result && fragment != null) {
                            Snackbar.make(fragment.binding.snackbar, R.string.reboot_required, Snackbar.LENGTH_SHORT)
                                    .setAction(R.string.reboot, v -> ConfigManager.reboot(false))
                                    .show();
                        }
                        return result;
                    });
                }
            }

            SwitchPreference prefEnableResources = findPreference("enable_resources");
            if (prefEnableResources != null) {
                prefEnableResources.setEnabled(installed);
                prefEnableResources.setChecked(installed && ConfigManager.isResourceHookEnabled());
                prefEnableResources.setOnPreferenceChangeListener((preference, newValue) -> ConfigManager.setResourceHookEnabled((boolean) newValue));
            }

            Preference backup = findPreference("backup");
            if (backup != null) {
                backup.setEnabled(installed);
                backup.setOnPreferenceClickListener(preference -> {
                    LocalDateTime now = LocalDateTime.now();
                    backupLauncher.launch(String.format(Locale.ROOT,
                            "LSPosed_%s.lsp", now.toString()));
                    return true;
                });
            }

            Preference restore = findPreference("restore");
            if (restore != null) {
                restore.setEnabled(installed);
                restore.setOnPreferenceClickListener(preference -> {
                    restoreLauncher.launch(new String[]{"*/*"});
                    return true;
                });
            }

            Preference theme = findPreference("dark_theme");
            if (theme != null) {
                theme.setOnPreferenceChangeListener((preference, newValue) -> {
                    if (!App.getPreferences().getString("dark_theme", ThemeUtil.MODE_NIGHT_FOLLOW_SYSTEM).equals(newValue)) {
                        DayNightDelegate.setDefaultNightMode(ThemeUtil.getDarkTheme((String) newValue));
                        MainActivity activity = (MainActivity) getActivity();
                        if (activity != null) {
                            activity.restart();
                        }
                    }
                    return true;
                });
            }

            Preference black_dark_theme = findPreference("black_dark_theme");
            if (black_dark_theme != null) {
                black_dark_theme.setOnPreferenceChangeListener((preference, newValue) -> {
                    MainActivity activity = (MainActivity) getActivity();
                    if (activity != null && ResourceUtils.isNightMode(getResources().getConfiguration())) {
                        activity.restart();
                    }
                    return true;
                });
            }

            Preference primary_color = findPreference("theme_color");
            if (primary_color != null) {
                primary_color.setOnPreferenceChangeListener((preference, newValue) -> {
                    MainActivity activity = (MainActivity) getActivity();
                    if (activity != null) {
                        activity.restart();
                    }
                    return true;
                });
            }

            SwitchPreference prefShowHiddenIcons = findPreference("show_hidden_icon_apps_enabled");
            if (prefShowHiddenIcons != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (ConfigManager.isBinderAlive()) {
                    prefShowHiddenIcons.setEnabled(true);
                    prefShowHiddenIcons.setOnPreferenceChangeListener((preference, newValue) ->
                            ConfigManager.setHiddenIcon(!(boolean) newValue));
                }
                prefShowHiddenIcons.setChecked(Settings.Global.getInt(
                        requireActivity().getContentResolver(), "show_hidden_icon_apps_enabled", 1) != 0);
            }

            SwitchPreference prefFollowSystemAccent = findPreference("follow_system_accent");
            if (prefFollowSystemAccent != null && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S || Build.VERSION.SDK_INT == Build.VERSION_CODES.R && Build.VERSION.PREVIEW_SDK_INT != 0)) {
                if (primary_color != null) {
                    primary_color.setVisible(!prefFollowSystemAccent.isChecked());
                }
                prefFollowSystemAccent.setVisible(true);
                prefFollowSystemAccent.setOnPreferenceChangeListener((preference, newValue) -> {
                    MainActivity activity = (MainActivity) getActivity();
                    if (activity != null) {
                        activity.restart();
                    }
                    return true;
                });
            }
        }

        @Override
        public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
            BorderRecyclerView recyclerView = (BorderRecyclerView) super.onCreateRecyclerView(inflater, parent, savedInstanceState);
            RecyclerViewKt.fixEdgeEffect(recyclerView, false, true);
            recyclerView.getBorderViewDelegate().setBorderVisibilityChangedListener((top, oldTop, bottom, oldBottom) -> {
                SettingsFragment fragment = (SettingsFragment) getParentFragment();
                if (fragment != null) {
                    fragment.binding.appBar.setRaised(!top);
                }
            });
            return recyclerView;
        }
    }

    public void makeSnackBar(@StringRes int text, @Snackbar.Duration int duration) {
        Snackbar.make(binding.snackbar, text, duration).show();
    }
}
