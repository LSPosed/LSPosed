package org.meowcat.edxposed.manager.ui.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.takisoft.preferencex.PreferenceFragmentCompat;

import org.meowcat.edxposed.manager.Constants;
import org.meowcat.edxposed.manager.R;
import org.meowcat.edxposed.manager.databinding.ActivitySettingsBinding;
import org.meowcat.edxposed.manager.ui.widget.IntegerListPreference;
import org.meowcat.edxposed.manager.util.RepoLoader;

import java.io.File;

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
        setupWindowInsets(binding.snackbar, null);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new SettingsFragment()).commit();
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

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    public static class SettingsFragment extends PreferenceFragmentCompat {
        private static final File enableResourcesFlag = new File(Constants.getBaseDir() + "conf/enable_resources");
        private static final File deoptBootFlag = new File(Constants.getBaseDir() + "conf/deoptbootimage");
        private static final File whiteListModeFlag = new File(Constants.getBaseDir() + "conf/usewhitelist");
        private static final File disableVerboseLogsFlag = new File(Constants.getBaseDir() + "conf/disable_verbose_log");
        private static final File disableModulesLogsFlag = new File(Constants.getBaseDir() + "conf/disable_modules_log");

        @SuppressLint({"ObsoleteSdkInt", "WorldReadableFiles"})
        @Override
        public void onCreatePreferencesFix(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.prefs);

            Preference releaseType = findPreference("release_type_global");
            if (releaseType != null) {
                releaseType.setOnPreferenceChangeListener((preference, newValue) -> {
                    RepoLoader.getInstance().setReleaseTypeGlobal((String) newValue);
                    return true;
                });
            }

            SwitchPreferenceCompat prefWhiteListMode = findPreference("white_list_switch");
            if (prefWhiteListMode != null) {
                prefWhiteListMode.setChecked(whiteListModeFlag.exists());
                prefWhiteListMode.setOnPreferenceChangeListener(new OnFlagChangeListener(whiteListModeFlag));
            }

            SwitchPreferenceCompat prefVerboseLogs = findPreference("disable_verbose_log");
            if (prefVerboseLogs != null) {
                prefVerboseLogs.setChecked(disableVerboseLogsFlag.exists());
                prefVerboseLogs.setOnPreferenceChangeListener(new OnFlagChangeListener(disableVerboseLogsFlag));
            }

            SwitchPreferenceCompat prefModulesLogs = findPreference("disable_modules_log");
            if (prefModulesLogs != null) {
                prefModulesLogs.setChecked(disableModulesLogsFlag.exists());
                prefModulesLogs.setOnPreferenceChangeListener(new OnFlagChangeListener(disableModulesLogsFlag));
            }

            SwitchPreferenceCompat prefEnableDeopt = findPreference("enable_boot_image_deopt");
            if (prefEnableDeopt != null) {
                prefEnableDeopt.setChecked(deoptBootFlag.exists());
                prefEnableDeopt.setOnPreferenceChangeListener(new OnFlagChangeListener(deoptBootFlag));
            }

            SwitchPreferenceCompat prefDisableResources = findPreference("disable_resources");
            if (prefDisableResources != null) {
                prefDisableResources.setChecked(!enableResourcesFlag.exists());
                prefDisableResources.setOnPreferenceChangeListener(new OnFlagChangeListener(enableResourcesFlag));
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
            Preference black_dark_theme = findPreference("black_dark_theme");
            if (black_dark_theme != null) {
                black_dark_theme.setVisible(show);
            }
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
            private final File flag;

            OnFlagChangeListener(File flag) {
                this.flag = flag;
            }

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean enabled = (Boolean) newValue;
                if (enabled) {
                    try {
                        flag.createNewFile();
                    } catch (Exception e) {
                        Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    flag.delete();
                }
                return (enabled == flag.exists());
            }
        }
    }
}
