package io.github.lsposed.manager.ui.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.takisoft.preferencex.PreferenceFragmentCompat;
import com.takisoft.preferencex.SimpleMenuPreference;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Locale;

import io.github.lsposed.manager.BuildConfig;
import io.github.lsposed.manager.Constants;
import io.github.lsposed.manager.R;
import io.github.lsposed.manager.databinding.ActivitySettingsBinding;
import io.github.lsposed.manager.ui.fragment.StatusDialogBuilder;
import io.github.lsposed.manager.ui.widget.IntegerListPreference;
import io.github.lsposed.manager.ui.widget.RecyclerViewBugFixed;
import io.github.lsposed.manager.util.BackupUtils;
import io.github.lsposed.manager.util.ModuleUtil;

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
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(view -> finish());
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new SettingsFragment()).commit();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
            ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
                Insets insets1 = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
                binding.getRoot().setPadding(insets1.left, insets1.top, insets1.right, 0);
                return insets;
            });
        }
        if (Constants.getXposedVersion() == null) {
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
        private static final Path enableResourcesFlag = Paths.get(Constants.getBaseDir(), "conf/enable_resources");
        private static final Path disableVerboseLogsFlag = Paths.get(Constants.getBaseDir(), "conf/disable_verbose_log");
        private static final Path disableModulesLogsFlag = Paths.get(Constants.getBaseDir() + "conf/disable_modules_log");
        private static final Path variantFlag = Paths.get(Constants.getBaseDir()).getParent().resolve("variant");
        ActivityResultLauncher<String> backupLauncher = registerForActivityResult(new ActivityResultContracts.CreateDocument(),
                uri -> {
                    if (uri != null) {
                        try {
                            // grantUriPermission might throw RemoteException on MIUI
                            requireActivity().grantUriPermission(BuildConfig.APPLICATION_ID, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        AlertDialog alertDialog = new MaterialAlertDialogBuilder(requireActivity())
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
                        AlertDialog alertDialog = new MaterialAlertDialogBuilder(requireActivity())
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
                                    ModuleUtil.getInstance().reloadEnabledModules();
                                    ModuleUtil.getInstance().updateModulesList();
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

            boolean installed = Constants.getXposedVersion() != null;
            SwitchPreferenceCompat prefVerboseLogs = findPreference("disable_verbose_log");
            if (prefVerboseLogs != null) {
                prefVerboseLogs.setEnabled(installed);
                prefVerboseLogs.setChecked(Files.exists(disableVerboseLogsFlag));
                prefVerboseLogs.setOnPreferenceChangeListener(new OnFlagChangeListener(disableVerboseLogsFlag));
            }

            SwitchPreferenceCompat prefModulesLogs = findPreference("disable_modules_log");
            if (prefModulesLogs != null) {
                prefModulesLogs.setEnabled(installed);
                prefModulesLogs.setChecked(Files.exists(disableModulesLogsFlag));
                prefModulesLogs.setOnPreferenceChangeListener(new OnFlagChangeListener(disableModulesLogsFlag));
            }

            SwitchPreferenceCompat prefEnableResources = findPreference("enable_resources");
            if (prefEnableResources != null) {
                prefEnableResources.setEnabled(installed);
                prefEnableResources.setChecked(Files.exists(enableResourcesFlag));
                prefEnableResources.setOnPreferenceChangeListener(new OnFlagChangeListener(enableResourcesFlag));
            }

            SimpleMenuPreference prefVariant = findPreference("variant");
            if (prefVariant != null) {
                if (StatusDialogBuilder.getArch().contains("x86") || requireActivity().getApplicationInfo().uid / 100000 != 0) {
                    prefVariant.setVisible(false);
                } else {
                    prefVariant.setEnabled(installed);
                    try {
                        prefVariant.setValue(new String(Files.readAllBytes(variantFlag)).trim());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    prefVariant.setOnPreferenceChangeListener((preference, newValue) -> {
                        try {
                            Files.write(variantFlag, ((String) newValue).getBytes());
                            return true;
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                            return false;
                        }
                    });
                }
            }

            Preference backup = findPreference("backup");
            if (backup != null) {
                backup.setEnabled(installed);
                backup.setOnPreferenceClickListener(preference -> {
                    Calendar now = Calendar.getInstance();
                    backupLauncher.launch(String.format(Locale.US,
                            "LSPosed_Backup_%04d%02d%02d_%02d%02d%02d.zip",
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

            SwitchPreferenceCompat transparent = findPreference("transparent_status_bar");
            if (transparent != null) {
                transparent.setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean enabled = (Boolean) newValue;
                    SettingsActivity activity = (SettingsActivity) getActivity();
                    if (activity != null) {
                        if (enabled) {
                            activity.getWindow().setStatusBarColor(activity.getThemedColor(R.attr.colorActionBar));
                        } else {
                            activity.getWindow().setStatusBarColor(activity.getThemedColor(R.attr.colorPrimaryDark));
                        }
                    }
                    return true;
                });
            }

            IntegerListPreference theme = findPreference("theme");
            if (theme != null) {
                theme.setOnPreferenceChangeListener((preference, newValue) -> {
                    AppCompatDelegate.setDefaultNightMode(Integer.parseInt((String) newValue));
                    return true;
                });
            }

            SwitchPreferenceCompat black_dark_theme = findPreference("black_dark_theme");
            if (black_dark_theme != null) {
                black_dark_theme.setOnPreferenceChangeListener((preference, newValue) -> {
                    SettingsActivity activity = (SettingsActivity) getActivity();
                    if (activity != null && isNightMode(getResources().getConfiguration())) {
                        activity.restart();
                    }
                    return true;
                });
            }

            Preference primary_color = findPreference("primary_color");
            if (primary_color != null) {
                primary_color.setOnPreferenceChangeListener((preference, newValue) -> {
                    SettingsActivity activity = (SettingsActivity) getActivity();
                    if (activity != null) {
                        activity.restart();
                    }
                    return true;
                });
            }

            Preference accent_color = findPreference("accent_color");
            if (accent_color != null) {
                accent_color.setOnPreferenceChangeListener((preference, newValue) -> {
                    SettingsActivity activity = (SettingsActivity) getActivity();
                    if (activity != null) {
                        activity.restart();
                    }
                    return true;
                });
            }

            Preference colorized_action_bar = findPreference("colorized_action_bar");
            if (colorized_action_bar != null) {
                colorized_action_bar.setOnPreferenceChangeListener((preference, newValue) -> {
                    SettingsActivity activity = (SettingsActivity) getActivity();
                    if (activity != null && !(isBlackNightTheme() && isNightMode(getResources().getConfiguration()))) {
                        activity.restart();
                    }
                    return true;
                });
            }

            SwitchPreferenceCompat md2 = findPreference("md2");
            if (md2 != null) {
                md2.setOnPreferenceChangeListener((preference, newValue) -> {
                    SettingsActivity activity = (SettingsActivity) getActivity();
                    if (activity != null) {
                        updatePreference(!md2.isChecked());
                        activity.restart();
                    }
                    return true;
                });
                updatePreference(!md2.isChecked());
            }
        }

        private void updatePreference(boolean show) {
            Preference transparent_status_bar = findPreference("transparent_status_bar");
            if (transparent_status_bar != null) {
                transparent_status_bar.setVisible(show);
            }
            Preference colorized_action_bar = findPreference("colorized_action_bar");
            if (colorized_action_bar != null) {
                colorized_action_bar.setVisible(show);
            }
        }

        private class OnFlagChangeListener implements Preference.OnPreferenceChangeListener {
            private final Path flag;

            OnFlagChangeListener(Path flag) {
                this.flag = flag;
            }

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean enabled = (Boolean) newValue;
                try {
                    if (enabled) {
                        Files.createFile(flag);
                    } else {
                        Files.delete(flag);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                return (enabled == Files.exists(flag));
            }
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                RecyclerView recyclerView = getListView();
                recyclerView.setClipToPadding(false);
                recyclerView.setEdgeEffectFactory(new RecyclerViewBugFixed.AlwaysClipToPaddingEdgeEffectFactory());
                ViewCompat.setOnApplyWindowInsetsListener(recyclerView, (v, insets) -> {
                    Insets insets1 = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
                    v.setPadding(0, 0, 0, insets1.bottom);
                    return WindowInsetsCompat.CONSUMED;
                });
            }
        }
    }

    public void makeSnackBar(@StringRes int text, @Snackbar.Duration int duration) {
        Snackbar.make(binding.snackbar, text, duration).show();
    }
}
