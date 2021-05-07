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

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.takisoft.preferencex.PreferenceCategory;
import com.takisoft.preferencex.PreferenceFragmentCompat;

import org.lsposed.manager.BuildConfig;
import org.lsposed.manager.ConfigManager;
import org.lsposed.manager.R;
import org.lsposed.manager.databinding.ActivitySettingsBinding;
import org.lsposed.manager.ui.activity.base.BaseActivity;
import org.lsposed.manager.util.BackupUtils;
import org.lsposed.manager.util.theme.ThemeUtil;

import java.util.Calendar;
import java.util.Locale;

import rikka.core.util.ResourceUtils;
import rikka.material.app.DayNightDelegate;
import rikka.recyclerview.RecyclerViewKt;
import rikka.widget.borderview.BorderRecyclerView;

public class SettingsActivity extends BaseActivity {
    private static final String KEY_PREFIX = SettingsActivity.class.getName() + '.';
    private static final String EXTRA_SAVED_INSTANCE_STATE = KEY_PREFIX + "SAVED_INSTANCE_STATE";
    ActivitySettingsBinding binding;
    private boolean restarting;

    @NonNull
    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, SettingsActivity.class);
    }

    @NonNull
    private static Intent newIntent(@NonNull Bundle savedInstanceState, @NonNull Context context) {
        return newIntent(context)
                .putExtra(EXTRA_SAVED_INSTANCE_STATE, savedInstanceState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            savedInstanceState = getIntent().getBundleExtra(EXTRA_SAVED_INSTANCE_STATE);
        }
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setAppBar(binding.appBar, binding.toolbar);
        binding.getRoot().bringChildToFront(binding.appBar);
        binding.toolbar.setNavigationOnClickListener(view -> finish());
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new SettingsFragment()).commit();
        }
        if (ConfigManager.getXposedVersionName() == null) {
            Snackbar.make(binding.snackbar, R.string.lsposed_not_active, Snackbar.LENGTH_LONG).show();
        }
    }

    private void restart() {
        Bundle savedInstanceState = new Bundle();
        onSaveInstanceState(savedInstanceState);
        finish();
        startActivity(newIntent(savedInstanceState, this));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        restarting = true;
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        return restarting || super.dispatchKeyEvent(event);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean dispatchKeyShortcutEvent(@NonNull KeyEvent event) {
        return restarting || super.dispatchKeyShortcutEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(@NonNull MotionEvent event) {
        return restarting || super.dispatchTouchEvent(event);
    }

    @Override
    public boolean dispatchTrackballEvent(@NonNull MotionEvent event) {
        return restarting || super.dispatchTrackballEvent(event);
    }

    @Override
    public boolean dispatchGenericMotionEvent(@NonNull MotionEvent event) {
        return restarting || super.dispatchGenericMotionEvent(event);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
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
                                SettingsActivity activity = (SettingsActivity) requireActivity();
                                activity.runOnUiThread(() -> {
                                    alertDialog.dismiss();
                                    activity.makeSnackBar(success ? R.string.settings_backup_success : R.string.settings_backup_failed, Snackbar.LENGTH_SHORT);
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
                                SettingsActivity activity = (SettingsActivity) requireActivity();
                                activity.runOnUiThread(() -> {
                                    alertDialog.dismiss();
                                    activity.makeSnackBar(success ? R.string.settings_restore_success : R.string.settings_restore_failed, Snackbar.LENGTH_SHORT);
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

            boolean installed = ConfigManager.getXposedVersionName() != null;
            SwitchPreference prefVerboseLogs = findPreference("disable_verbose_log");
            if (prefVerboseLogs != null) {
                if (requireActivity().getApplicationInfo().uid / 100000 != 0) {
                    prefVerboseLogs.setVisible(false);
                } else {
                    prefVerboseLogs.setEnabled(installed);
                    prefVerboseLogs.setChecked(!ConfigManager.isVerboseLogEnabled());
                    prefVerboseLogs.setOnPreferenceChangeListener((preference, newValue) -> {
                        boolean result = ConfigManager.setVerboseLogEnabled(!(boolean) newValue);
                        SettingsActivity activity = (SettingsActivity) getActivity();
                        if (result && activity != null) {
                            Snackbar.make(activity.binding.snackbar, R.string.reboot_required, Snackbar.LENGTH_SHORT)
                                    .setAction(R.string.reboot, v -> ConfigManager.reboot(false, null, false))
                                    .show();
                        }
                        return result;
                    });
                }
            }

            SwitchPreference prefEnableResources = findPreference("enable_resources");
            if (prefEnableResources != null) {
                prefEnableResources.setEnabled(installed);
                prefEnableResources.setChecked(ConfigManager.isResourceHookEnabled());
                prefEnableResources.setOnPreferenceChangeListener((preference, newValue) -> ConfigManager.setResourceHookEnabled((boolean) newValue));
            }

            Preference backup = findPreference("backup");
            if (backup != null) {
                backup.setEnabled(installed);
                backup.setOnPreferenceClickListener(preference -> {
                    Calendar now = Calendar.getInstance();
                    backupLauncher.launch(String.format(Locale.US,
                            "LSPosed_%04d%02d%02d_%02d%02d%02d.lsp",
                            now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1,
                            now.get(Calendar.DAY_OF_MONTH), now.get(Calendar.HOUR_OF_DAY),
                            now.get(Calendar.MINUTE), now.get(Calendar.SECOND)));
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
                    if (!preferences.getString("dark_theme", ThemeUtil.MODE_NIGHT_FOLLOW_SYSTEM).equals(newValue)) {
                        DayNightDelegate.setDefaultNightMode(ThemeUtil.getDarkTheme((String) newValue));
                        SettingsActivity activity = (SettingsActivity) getActivity();
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
                    SettingsActivity activity = (SettingsActivity) getActivity();
                    if (activity != null && ResourceUtils.isNightMode(getResources().getConfiguration())) {
                        activity.restart();
                    }
                    return true;
                });
            }

            Preference primary_color = findPreference("theme_color");
            if (primary_color != null) {
                primary_color.setOnPreferenceChangeListener((preference, newValue) -> {
                    SettingsActivity activity = (SettingsActivity) getActivity();
                    if (activity != null) {
                        activity.restart();
                    }
                    return true;
                });
            }

            PreferenceCategory prefGroupSystem = findPreference("settings_group_system");
            SwitchPreference prefShowHiddenIcons = findPreference("show_hidden_icon_apps_enabled");
            if (prefGroupSystem != null && prefShowHiddenIcons != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    && requireActivity().checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED) {
                prefGroupSystem.setVisible(true);
                prefShowHiddenIcons.setVisible(true);
                prefShowHiddenIcons.setChecked(Settings.Global.getInt(
                        requireActivity().getContentResolver(), "show_hidden_icon_apps_enabled", 1) != 0);
                prefShowHiddenIcons.setOnPreferenceChangeListener((preference, newValue) -> Settings.Global.putInt(requireActivity().getContentResolver(),
                        "show_hidden_icon_apps_enabled", (boolean) newValue ? 1 : 0));
            }
        }

        @Override
        public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
            BorderRecyclerView recyclerView = (BorderRecyclerView) super.onCreateRecyclerView(inflater, parent, savedInstanceState);
            RecyclerViewKt.fixEdgeEffect(recyclerView, false, true);
            recyclerView.getBorderViewDelegate().setBorderVisibilityChangedListener((top, oldTop, bottom, oldBottom) -> {
                SettingsActivity activity = (SettingsActivity) getActivity();
                if (activity != null) {
                    activity.binding.appBar.setRaised(!top);
                }
            });
            return recyclerView;
        }
    }

    public void makeSnackBar(@StringRes int text, @Snackbar.Duration int duration) {
        Snackbar.make(binding.snackbar, text, duration).show();
    }
}
