package org.meowcat.edxposed.manager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.FileUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.takisoft.preferencex.PreferenceFragmentCompat;
import com.topjohnwu.superuser.Shell;

import org.meowcat.edxposed.manager.databinding.ActivitySettingsBinding;
import org.meowcat.edxposed.manager.util.RepoLoader;
import org.meowcat.edxposed.manager.widget.IntegerListPreference;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class SettingsActivity extends BaseActivity {
    ActivitySettingsBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.appbar.toolbar);
        binding.appbar.toolbar.setNavigationOnClickListener(view -> finish());
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

    @SuppressWarnings({"ResultOfMethodCallIgnored", "deprecation"})
    public static class SettingsFragment extends PreferenceFragmentCompat {
        static final File disableResourcesFlag = new File(XposedApp.BASE_DIR + "conf/disable_resources");
        static final File dynamicModulesFlag = new File(XposedApp.BASE_DIR + "conf/dynamicmodules");
        static final File deoptBootFlag = new File(XposedApp.BASE_DIR + "conf/deoptbootimage");
        static final File whiteListModeFlag = new File(XposedApp.BASE_DIR + "conf/usewhitelist");
        static final File blackWhiteListModeFlag = new File(XposedApp.BASE_DIR + "conf/blackwhitelist");
        static final File disableVerboseLogsFlag = new File(XposedApp.BASE_DIR + "conf/disable_verbose_log");
        static final File disableModulesLogsFlag = new File(XposedApp.BASE_DIR + "conf/disable_modules_log");
        static final File verboseLogProcessID = new File(XposedApp.BASE_DIR + "log/all.pid");
        static final File modulesLogProcessID = new File(XposedApp.BASE_DIR + "log/error.pid");

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

        @SuppressLint({"ObsoleteSdkInt", "WorldReadableFiles"})
        @Override
        public void onCreatePreferencesFix(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.prefs);

            Preference stopVerboseLog = findPreference("stop_verbose_log");
            if (stopVerboseLog != null) {
                stopVerboseLog.setOnPreferenceClickListener(preference -> {
                    areYouSure(R.string.stop_verbose_log_summary, (dialog, which) -> Shell.su("kill $(cat " + verboseLogProcessID.getAbsolutePath() + ")").exec());
                    return true;
                });
            }
            Preference stopLog = findPreference("stop_log");
            if (stopLog != null) {
                stopLog.setOnPreferenceClickListener(preference -> {
                    areYouSure(R.string.stop_log_summary, (dialog, which) -> Shell.su("kill $(cat " + modulesLogProcessID.getAbsolutePath() + ")").exec());
                    return true;
                });
            }

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
                prefWhiteListMode.setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean enabled = (Boolean) newValue;
                    if (enabled) {
                        FileOutputStream fos = null;
                        try {
                            fos = new FileOutputStream(whiteListModeFlag.getPath());
                            setFilePermissionsFromMode(whiteListModeFlag.getPath(), MODE_WORLD_READABLE);
                        } catch (FileNotFoundException e) {
                            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        } finally {
                            if (fos != null) {
                                try {
                                    fos.close();
                                } catch (IOException e) {
                                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                    try {
                                        whiteListModeFlag.createNewFile();
                                    } catch (IOException e1) {
                                        Toast.makeText(getActivity(), e1.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        }
                    } else {
                        whiteListModeFlag.delete();
                    }
                    return (enabled == whiteListModeFlag.exists());
                });
            }

            SwitchPreferenceCompat prefVerboseLogs = findPreference("disable_verbose_log");
            if (prefVerboseLogs != null) {
                prefVerboseLogs.setChecked(disableVerboseLogsFlag.exists());
                prefVerboseLogs.setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean enabled = (Boolean) newValue;
                    if (enabled) {
                        FileOutputStream fos = null;
                        try {
                            fos = new FileOutputStream(disableVerboseLogsFlag.getPath());
                            setFilePermissionsFromMode(disableVerboseLogsFlag.getPath(), MODE_WORLD_READABLE);
                        } catch (FileNotFoundException e) {
                            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        } finally {
                            if (fos != null) {
                                try {
                                    fos.close();
                                } catch (IOException e) {
                                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                    try {
                                        disableVerboseLogsFlag.createNewFile();
                                    } catch (IOException e1) {
                                        Toast.makeText(getActivity(), e1.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        }
                    } else {
                        disableVerboseLogsFlag.delete();
                    }
                    return (enabled == disableVerboseLogsFlag.exists());
                });
            }

            SwitchPreferenceCompat prefModulesLogs = findPreference("disable_modules_log");
            if (prefModulesLogs != null) {
                prefModulesLogs.setChecked(disableModulesLogsFlag.exists());
                prefModulesLogs.setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean enabled = (Boolean) newValue;
                    if (enabled) {
                        FileOutputStream fos = null;
                        try {
                            fos = new FileOutputStream(disableModulesLogsFlag.getPath());
                            setFilePermissionsFromMode(disableModulesLogsFlag.getPath(), MODE_WORLD_READABLE);
                        } catch (FileNotFoundException e) {
                            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        } finally {
                            if (fos != null) {
                                try {
                                    fos.close();
                                } catch (IOException e) {
                                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                    try {
                                        disableModulesLogsFlag.createNewFile();
                                    } catch (IOException e1) {
                                        Toast.makeText(getActivity(), e1.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        }
                    } else {
                        disableModulesLogsFlag.delete();
                    }
                    return (enabled == disableModulesLogsFlag.exists());
                });
            }

            SwitchPreferenceCompat prefBlackWhiteListMode = findPreference("black_white_list_switch");
            if (prefBlackWhiteListMode != null) {
                prefBlackWhiteListMode.setChecked(blackWhiteListModeFlag.exists());
                prefBlackWhiteListMode.setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean enabled = (Boolean) newValue;
                    if (enabled) {
                        FileOutputStream fos = null;
                        try {
                            fos = new FileOutputStream(blackWhiteListModeFlag.getPath());
                            setFilePermissionsFromMode(blackWhiteListModeFlag.getPath(), MODE_WORLD_READABLE);
                        } catch (FileNotFoundException e) {
                            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        } finally {
                            if (fos != null) {
                                try {
                                    fos.close();
                                } catch (IOException e) {
                                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                    try {
                                        blackWhiteListModeFlag.createNewFile();
                                    } catch (IOException e1) {
                                        Toast.makeText(getActivity(), e1.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        }
                    } else {
                        blackWhiteListModeFlag.delete();
                    }
                    return (enabled == blackWhiteListModeFlag.exists());
                });
            }

            SwitchPreferenceCompat prefEnableDeopt = findPreference("enable_boot_image_deopt");
            if (prefEnableDeopt != null) {
                prefEnableDeopt.setChecked(deoptBootFlag.exists());
                prefEnableDeopt.setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean enabled = (Boolean) newValue;
                    if (enabled) {
                        FileOutputStream fos = null;
                        try {
                            fos = new FileOutputStream(deoptBootFlag.getPath());
                            setFilePermissionsFromMode(deoptBootFlag.getPath(), MODE_WORLD_READABLE);
                        } catch (FileNotFoundException e) {
                            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        } finally {
                            if (fos != null) {
                                try {
                                    fos.close();
                                } catch (IOException e) {
                                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                    try {
                                        deoptBootFlag.createNewFile();
                                    } catch (IOException e1) {
                                        Toast.makeText(getActivity(), e1.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        }
                    } else {
                        deoptBootFlag.delete();
                    }
                    return (enabled == deoptBootFlag.exists());
                });
            }

            SwitchPreferenceCompat prefDynamicResources = findPreference("is_dynamic_modules");
            if (prefDynamicResources != null) {
                prefDynamicResources.setChecked(dynamicModulesFlag.exists());
                prefDynamicResources.setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean enabled = (Boolean) newValue;
                    if (enabled) {
                        FileOutputStream fos = null;
                        try {
                            fos = new FileOutputStream(dynamicModulesFlag.getPath());
                            setFilePermissionsFromMode(dynamicModulesFlag.getPath(), MODE_WORLD_READABLE);
                        } catch (FileNotFoundException e) {
                            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        } finally {
                            if (fos != null) {
                                try {
                                    fos.close();
                                } catch (IOException e) {
                                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                    try {
                                        dynamicModulesFlag.createNewFile();
                                    } catch (IOException e1) {
                                        Toast.makeText(getActivity(), e1.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        }
                    } else {
                        dynamicModulesFlag.delete();
                    }
                    return (enabled == dynamicModulesFlag.exists());
                });
            }

            SwitchPreferenceCompat prefDisableResources = findPreference("disable_resources");
            if (prefDisableResources != null) {
                prefDisableResources.setChecked(disableResourcesFlag.exists());
                prefDisableResources.setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean enabled = (Boolean) newValue;
                    if (enabled) {
                        FileOutputStream fos = null;
                        try {
                            fos = new FileOutputStream(disableResourcesFlag.getPath());
                            setFilePermissionsFromMode(disableResourcesFlag.getPath(), MODE_WORLD_READABLE);
                        } catch (FileNotFoundException e) {
                            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        } finally {
                            if (fos != null) {
                                try {
                                    fos.close();
                                } catch (IOException e) {
                                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                    try {
                                        disableResourcesFlag.createNewFile();
                                    } catch (IOException e1) {
                                        Toast.makeText(getActivity(), e1.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        }
                    } else {
                        disableResourcesFlag.delete();
                    }
                    return (enabled == disableResourcesFlag.exists());
                });
            }

            SwitchPreferenceCompat transparent = findPreference("transparent_status_bar");
            if (transparent != null) {
                transparent.setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean enabled = (Boolean) newValue;
                    Activity activity = getActivity();
                    if (activity != null && !XposedApp.getPreferences().getBoolean("black_dark_theme", false)) {
                        if (enabled) {
                            activity.getWindow().setStatusBarColor(ContextCompat.getColor(activity, R.color.colorActionBar));
                        } else {
                            activity.getWindow().setStatusBarColor(ContextCompat.getColor(activity, R.color.colorPrimaryDark));
                        }
                    }
                    return true;
                });
            }

            Preference compat_mode = findPreference("compat_mode");
            if (compat_mode != null) {
                compat_mode.setOnPreferenceClickListener(preference -> {
                    Activity activity = getActivity();
                    if (activity != null) {
                        Intent intent = new Intent();
                        intent.setClass(activity, CompatListActivity.class);
                        activity.startActivity(intent);
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
                    Activity activity = getActivity();
                    if (activity != null) {
                        activity.recreate();
                    }
                    return true;
                });
            }
        }

        private void areYouSure(int contentTextId, DialogInterface.OnClickListener listener) {
            Activity activity = getActivity();
            if (activity != null) {
                new MaterialAlertDialogBuilder(activity)
                        .setTitle(R.string.areyousure)
                        .setMessage(contentTextId)
                        .setPositiveButton(android.R.string.yes, listener)
                        .setNegativeButton(android.R.string.no, null)
                        .show();
            }
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            ((LinearLayout) view).setClipToPadding(false);
            ((LinearLayout) view).setClipChildren(false);
            ((FrameLayout) getListView().getParent()).setClipChildren(false);
            ((FrameLayout) getListView().getParent()).setClipToPadding(false);
            ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
                getListView().setPadding(0, 0, 0, insets.getSystemWindowInsetBottom());
                return insets;
            });
        }
    }
}
