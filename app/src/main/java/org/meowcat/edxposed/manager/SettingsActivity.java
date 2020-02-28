package org.meowcat.edxposed.manager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.takisoft.preferencex.PreferenceFragmentCompat;
import com.topjohnwu.superuser.Shell;

import org.meowcat.edxposed.manager.util.FileUtils;
import org.meowcat.edxposed.manager.util.RepoLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

public class SettingsActivity extends BaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(view -> finish());
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }
        setupWindowInsets();
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, SettingsFragment.newInstance(findViewById(R.id.snackbar))).commit();
        }

    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "deprecation"})
    public static class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener, SharedPreferences.OnSharedPreferenceChangeListener {
        static final File mDisableResourcesFlag = new File(XposedApp.BASE_DIR + "conf/disable_resources");
        static final File mDynamicModulesFlag = new File(XposedApp.BASE_DIR + "conf/dynamicmodules");
        static final File mDeoptBootFlag = new File(XposedApp.BASE_DIR + "conf/deoptbootimage");
        static final File mWhiteListModeFlag = new File(XposedApp.BASE_DIR + "conf/usewhitelist");
        static final File mBlackWhiteListModeFlag = new File(XposedApp.BASE_DIR + "conf/blackwhitelist");
        static final File mDisableVerboseLogsFlag = new File(XposedApp.BASE_DIR + "conf/disable_verbose_log");
        static final File mDisableModulesLogsFlag = new File(XposedApp.BASE_DIR + "conf/disable_modules_log");
        static final File mVerboseLogProcessID = new File(XposedApp.BASE_DIR + "log/all.pid");
        static final File mModulesLogProcessID = new File(XposedApp.BASE_DIR + "log/error.pid");
        View rootView;
        private Preference stopVerboseLog;
        private Preference stopLog;

        static SettingsFragment newInstance(View rootView) {
            SettingsFragment fragment = new SettingsFragment();
            fragment.setRootView(rootView);
            return fragment;
        }

        @SuppressWarnings("SameParameterValue")
        @SuppressLint({"WorldReadableFiles", "WorldWriteableFiles"})
        static void setFilePermissionsFromMode(String name, int mode) {
            int perms = FileUtils.S_IRUSR | FileUtils.S_IWUSR
                    | FileUtils.S_IRGRP | FileUtils.S_IWGRP;
            if ((mode & MODE_WORLD_READABLE) != 0) {
                perms |= FileUtils.S_IROTH;
            }
            if ((mode & MODE_WORLD_WRITEABLE) != 0) {
                perms |= FileUtils.S_IWOTH;
            }
            FileUtils.setPermissions(name, perms, -1, -1);
        }

        void setRootView(View rootView) {
            this.rootView = rootView;
        }

        @SuppressLint({"ObsoleteSdkInt", "WorldReadableFiles"})
        @Override
        public void onCreatePreferencesFix(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.prefs);

            stopVerboseLog = findPreference("stop_verbose_log");
            stopLog = findPreference("stop_log");

            findPreference("release_type_global").setOnPreferenceChangeListener((preference, newValue) -> {
                RepoLoader.getInstance().setReleaseTypeGlobal((String) newValue);
                return true;
            });

            SwitchPreference prefWhiteListMode = findPreference("white_list_switch");
            Objects.requireNonNull(prefWhiteListMode).setChecked(mWhiteListModeFlag.exists());
            prefWhiteListMode.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enabled = (Boolean) newValue;
                if (enabled) {
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(mWhiteListModeFlag.getPath());
                        setFilePermissionsFromMode(mWhiteListModeFlag.getPath(), MODE_WORLD_READABLE);
                    } catch (FileNotFoundException e) {
                        Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    } finally {
                        if (fos != null) {
                            try {
                                fos.close();
                            } catch (IOException e) {
                                Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                try {
                                    mWhiteListModeFlag.createNewFile();
                                } catch (IOException e1) {
                                    Toast.makeText(getActivity(), e1.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
                } else {
                    mWhiteListModeFlag.delete();
                }
                return (enabled == mWhiteListModeFlag.exists());
            });

            SwitchPreference prefVerboseLogs = findPreference("disable_verbose_log");
            Objects.requireNonNull(prefVerboseLogs).setChecked(mDisableVerboseLogsFlag.exists());
            prefVerboseLogs.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enabled = (Boolean) newValue;
                if (enabled) {
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(mDisableVerboseLogsFlag.getPath());
                        setFilePermissionsFromMode(mDisableVerboseLogsFlag.getPath(), MODE_WORLD_READABLE);
                    } catch (FileNotFoundException e) {
                        Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    } finally {
                        if (fos != null) {
                            try {
                                fos.close();
                            } catch (IOException e) {
                                Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                try {
                                    mDisableVerboseLogsFlag.createNewFile();
                                } catch (IOException e1) {
                                    Toast.makeText(getActivity(), e1.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
                } else {
                    mDisableVerboseLogsFlag.delete();
                }
                return (enabled == mDisableVerboseLogsFlag.exists());
            });

            SwitchPreference prefModulesLogs = findPreference("disable_modules_log");
            Objects.requireNonNull(prefModulesLogs).setChecked(mDisableModulesLogsFlag.exists());
            prefModulesLogs.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enabled = (Boolean) newValue;
                if (enabled) {
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(mDisableModulesLogsFlag.getPath());
                        setFilePermissionsFromMode(mDisableModulesLogsFlag.getPath(), MODE_WORLD_READABLE);
                    } catch (FileNotFoundException e) {
                        Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    } finally {
                        if (fos != null) {
                            try {
                                fos.close();
                            } catch (IOException e) {
                                Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                try {
                                    mDisableModulesLogsFlag.createNewFile();
                                } catch (IOException e1) {
                                    Toast.makeText(getActivity(), e1.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
                } else {
                    mDisableModulesLogsFlag.delete();
                }
                return (enabled == mDisableModulesLogsFlag.exists());
            });

            SwitchPreference prefBlackWhiteListMode = findPreference("black_white_list_switch");
            Objects.requireNonNull(prefBlackWhiteListMode).setChecked(mBlackWhiteListModeFlag.exists());
            prefBlackWhiteListMode.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enabled = (Boolean) newValue;
                if (enabled) {
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(mBlackWhiteListModeFlag.getPath());
                        setFilePermissionsFromMode(mBlackWhiteListModeFlag.getPath(), MODE_WORLD_READABLE);
                    } catch (FileNotFoundException e) {
                        Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    } finally {
                        if (fos != null) {
                            try {
                                fos.close();
                            } catch (IOException e) {
                                Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                try {
                                    mBlackWhiteListModeFlag.createNewFile();
                                } catch (IOException e1) {
                                    Toast.makeText(getActivity(), e1.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
                } else {
                    mBlackWhiteListModeFlag.delete();
                }
                return (enabled == mBlackWhiteListModeFlag.exists());
            });

            SwitchPreference prefEnableDeopt = findPreference("enable_boot_image_deopt");
            Objects.requireNonNull(prefEnableDeopt).setChecked(mDeoptBootFlag.exists());
            prefEnableDeopt.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enabled = (Boolean) newValue;
                if (enabled) {
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(mDeoptBootFlag.getPath());
                        setFilePermissionsFromMode(mDeoptBootFlag.getPath(), MODE_WORLD_READABLE);
                    } catch (FileNotFoundException e) {
                        Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    } finally {
                        if (fos != null) {
                            try {
                                fos.close();
                            } catch (IOException e) {
                                Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                try {
                                    mDeoptBootFlag.createNewFile();
                                } catch (IOException e1) {
                                    Toast.makeText(getActivity(), e1.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
                } else {
                    mDeoptBootFlag.delete();
                }
                return (enabled == mDeoptBootFlag.exists());
            });

            SwitchPreference prefDynamicResources = findPreference("is_dynamic_modules");
            Objects.requireNonNull(prefDynamicResources).setChecked(mDynamicModulesFlag.exists());
            prefDynamicResources.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enabled = (Boolean) newValue;
                if (enabled) {
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(mDynamicModulesFlag.getPath());
                        setFilePermissionsFromMode(mDynamicModulesFlag.getPath(), MODE_WORLD_READABLE);
                    } catch (FileNotFoundException e) {
                        Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    } finally {
                        if (fos != null) {
                            try {
                                fos.close();
                            } catch (IOException e) {
                                Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                try {
                                    mDynamicModulesFlag.createNewFile();
                                } catch (IOException e1) {
                                    Toast.makeText(getActivity(), e1.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
                } else {
                    mDynamicModulesFlag.delete();
                }
                return (enabled == mDynamicModulesFlag.exists());
            });

            SwitchPreference prefDisableResources = findPreference("disable_resources");
            Objects.requireNonNull(prefDisableResources).setChecked(mDisableResourcesFlag.exists());
            prefDisableResources.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enabled = (Boolean) newValue;
                if (enabled) {
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(mDisableResourcesFlag.getPath());
                        setFilePermissionsFromMode(mDisableResourcesFlag.getPath(), MODE_WORLD_READABLE);
                    } catch (FileNotFoundException e) {
                        Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    } finally {
                        if (fos != null) {
                            try {
                                fos.close();
                            } catch (IOException e) {
                                Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                try {
                                    mDisableResourcesFlag.createNewFile();
                                } catch (IOException e1) {
                                    Toast.makeText(getActivity(), e1.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
                } else {
                    mDisableResourcesFlag.delete();
                }
                return (enabled == mDisableResourcesFlag.exists());
            });

            SwitchPreference transparent_status_bar = findPreference("transparent_status_bar");
            Objects.requireNonNull(transparent_status_bar).setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enabled = (Boolean) newValue;
                Activity activity = getActivity();
                if (activity != null && !XposedApp.getPreferences().getBoolean("black_dark_theme", false)) {
                    if (enabled) {
                        Objects.requireNonNull(getActivity()).getWindow().setStatusBarColor(ContextCompat.getColor(activity, R.color.colorActionBar));
                    } else {
                        Objects.requireNonNull(getActivity()).getWindow().setStatusBarColor(ContextCompat.getColor(activity, R.color.colorPrimaryDark));
                    }
                }
                return true;
            });

            Preference compat_mode = findPreference("compat_mode");
            if (compat_mode != null) {
                compat_mode.setOnPreferenceClickListener(preference -> {
                    Intent intent = new Intent();
                    intent.setClass(Objects.requireNonNull(getContext()), CompatListActivity.class);
                    Objects.requireNonNull(getActivity()).startActivity(intent);
                    return true;
                });
            }

        }

        @Override
        public void onResume() {
            super.onResume();

            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();

            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.contains("theme") || key.equals("ignore_chinese")) {
                AppCompatDelegate.setDefaultNightMode(XposedApp.getPreferences().getInt("theme", 0));
                Objects.requireNonNull(getActivity()).recreate();
            }
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            SettingsActivity act = (SettingsActivity) getActivity();
            if (act == null)
                return false;

            if (preference.getKey().equals(stopVerboseLog.getKey())) {
                new Runnable() {
                    @Override
                    public void run() {
                        areYouSure(R.string.stop_verbose_log_summary, (dialog, which) -> Shell.su("kill $(cat " + mVerboseLogProcessID.getAbsolutePath() + ")").exec());
                    }
                };
            } else if (preference.getKey().equals(stopLog.getKey())) {
                new Runnable() {
                    @Override
                    public void run() {
                        areYouSure(R.string.stop_log_summary, (dialog, which) -> Shell.su("kill $(cat " + mModulesLogProcessID.getAbsolutePath() + ")").exec());
                    }
                };
            }
            return true;
        }

        private void areYouSure(int contentTextId, DialogInterface.OnClickListener listener) {
            new MaterialAlertDialogBuilder(Objects.requireNonNull(getActivity())).setTitle(R.string.areyousure)
                    .setMessage(contentTextId)
                    .setPositiveButton(android.R.string.yes, listener)
                    .setNegativeButton(android.R.string.no, null)
                    .show();
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            if (rootView == null) {
                return;
            }
            //ActionBarShadowController.attachToRecyclerView((AppCompatActivity) getActivity(), getListView());
            ((LinearLayout) ((FrameLayout) rootView.findViewById(R.id.container)).getChildAt(0)).setClipToPadding(false);
            ((LinearLayout) ((FrameLayout) rootView.findViewById(R.id.container)).getChildAt(0)).setClipChildren(false);
            ((FrameLayout) ((LinearLayout) view).getChildAt(0)).setClipChildren(false);
            ((FrameLayout) ((LinearLayout) view).getChildAt(0)).setClipToPadding(false);
        }
    }
}
