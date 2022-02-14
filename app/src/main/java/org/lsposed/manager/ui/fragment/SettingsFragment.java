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

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.text.HtmlCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import rikka.material.preference.MaterialSwitchPreference;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.DynamicColors;

import org.lsposed.manager.App;
import org.lsposed.manager.BuildConfig;
import org.lsposed.manager.ConfigManager;
import org.lsposed.manager.R;
import org.lsposed.manager.databinding.FragmentSettingsBinding;
import org.lsposed.manager.receivers.LSPManagerServiceHolder;
import org.lsposed.manager.ui.activity.MainActivity;
import org.lsposed.manager.util.BackupUtils;
import org.lsposed.manager.util.LangList;
import org.lsposed.manager.util.NavUtil;
import org.lsposed.manager.util.ThemeUtil;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Locale;

import rikka.core.util.ResourceUtils;
import rikka.material.app.DayNightDelegate;
import rikka.material.app.LocaleDelegate;
import rikka.preference.SimpleMenuPreference;
import rikka.recyclerview.RecyclerViewKt;
import rikka.widget.borderview.BorderRecyclerView;

public class SettingsFragment extends BaseFragment {
    FragmentSettingsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        setupToolbar(R.string.Settings);
        activityMainBinding.appBar.setLiftable(true);
        activityMainBinding.toolbar.setNavigationIcon(null);
        if (savedInstanceState == null) {
            getChildFragmentManager().beginTransaction()
                    .add(R.id.setting_container, new PreferenceFragment()).commitNow();
        }
        if (ConfigManager.isBinderAlive()) {
            activityMainBinding.toolbarLayout.setSubtitle(String.format(LocaleDelegate.getDefaultLocale(), "%s (%d) - %s",
                    ConfigManager.getXposedVersionName(), ConfigManager.getXposedVersionCode(), ConfigManager.getApi()));
        } else {
            activityMainBinding.toolbarLayout.setSubtitle(String.format(LocaleDelegate.getDefaultLocale(), "%s (%d) - %s",
                    BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, getString(R.string.not_installed)));
        }
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        binding = null;
    }

    public static class PreferenceFragment extends PreferenceFragmentCompat {
        private SettingsFragment parentFragment;

        ActivityResultLauncher<String> backupLauncher = registerForActivityResult(new ActivityResultContracts.CreateDocument("application/gzip"),
                uri -> {
                    if (uri == null || parentFragment == null) return;
                    parentFragment.runAsync(() -> {
                        try {
                            BackupUtils.backup(uri);
                        } catch (Exception e) {
                            var text = App.getInstance().getString(R.string.settings_backup_failed2, e.getMessage());
                            parentFragment.showHint(text, false);
                        }
                    });
                });
        ActivityResultLauncher<String[]> restoreLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri == null || parentFragment == null) return;
                    parentFragment.runAsync(() -> {
                        try {
                            BackupUtils.restore(uri);
                        } catch (Exception e) {
                            var text = App.getInstance().getString(R.string.settings_restore_failed2, e.getMessage());
                            parentFragment.showHint(text, false);
                        }
                    });
                });

        @Override
        public void onAttach(@NonNull Context context) {
            super.onAttach(context);

            parentFragment = (SettingsFragment) requireParentFragment();
        }

        @Override
        public void onDetach() {
            super.onDetach();

            parentFragment = null;
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            final String SYSTEM = "SYSTEM";

            addPreferencesFromResource(R.xml.prefs);

            boolean installed = ConfigManager.isBinderAlive();
            MaterialSwitchPreference prefVerboseLogs = findPreference("disable_verbose_log");
            if (prefVerboseLogs != null) {
                prefVerboseLogs.setEnabled(!BuildConfig.DEBUG && installed);
                prefVerboseLogs.setChecked(!installed || !ConfigManager.isVerboseLogEnabled());
                prefVerboseLogs.setOnPreferenceChangeListener((preference, newValue) ->
                        ConfigManager.setVerboseLogEnabled(!(boolean) newValue));
            }

            MaterialSwitchPreference prefDexObfuscate = findPreference("enable_dex_obfuscate");
            if (prefDexObfuscate != null) {
                prefDexObfuscate.setEnabled(installed);
                prefDexObfuscate.setChecked(!installed || ConfigManager.isDexObfuscateEnabled());
                prefDexObfuscate.setOnPreferenceChangeListener((preference, newValue) -> {
                    parentFragment.showHint(R.string.reboot_required, true, R.string.reboot,
                            v -> ConfigManager.reboot(false));
                    return ConfigManager.setDexObfuscateEnabled((boolean) newValue);
                });
            }

            MaterialSwitchPreference prefEnableShortcut = findPreference("enable_auto_add_shortcut");
            if (prefEnableShortcut != null) {
                prefEnableShortcut.setEnabled(installed);
                prefEnableShortcut.setVisible(!App.isParasitic());
                prefEnableShortcut.setChecked(installed && ConfigManager.isAddShortcut());
                prefEnableShortcut.setOnPreferenceChangeListener((preference, newValue) -> ConfigManager.setAddShortcut((boolean) newValue));
            }

            Preference shortcut = findPreference("add_shortcut");
            if (shortcut != null) {
                shortcut.setEnabled(installed);
                shortcut.setOnPreferenceClickListener(preference -> {
                    try {
                        LSPManagerServiceHolder.getService().createShortcut();
                    } catch (Throwable ignored) {
                    }
                    return true;
                });
            }

            Preference backup = findPreference("backup");
            if (backup != null) {
                backup.setEnabled(installed);
                backup.setOnPreferenceClickListener(preference -> {
                    LocalDateTime now = LocalDateTime.now();
                    try {
                        backupLauncher.launch(String.format(LocaleDelegate.getDefaultLocale(),
                                "LSPosed_%s.lsp", now.toString()));
                        return true;
                    } catch (ActivityNotFoundException e) {
                        parentFragment.showHint(R.string.enable_documentui, true);
                        return false;
                    }
                });
            }

            Preference restore = findPreference("restore");
            if (restore != null) {
                restore.setEnabled(installed);
                restore.setOnPreferenceClickListener(preference -> {
                    try {
                        restoreLauncher.launch(new String[]{"*/*"});
                        return true;
                    } catch (ActivityNotFoundException e) {
                        parentFragment.showHint(R.string.enable_documentui, true);
                        return false;
                    }
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

            MaterialSwitchPreference prefShowHiddenIcons = findPreference("show_hidden_icon_apps_enabled");
            if (prefShowHiddenIcons != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (ConfigManager.isBinderAlive()) {
                    prefShowHiddenIcons.setEnabled(true);
                    prefShowHiddenIcons.setOnPreferenceChangeListener((preference, newValue) ->
                            ConfigManager.setHiddenIcon(!(boolean) newValue));
                }
                prefShowHiddenIcons.setChecked(Settings.Global.getInt(
                        requireActivity().getContentResolver(), "show_hidden_icon_apps_enabled", 1) != 0);
            }

            MaterialSwitchPreference prefFollowSystemAccent = findPreference("follow_system_accent");
            if (prefFollowSystemAccent != null && DynamicColors.isDynamicColorAvailable()) {
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

            SimpleMenuPreference language = findPreference("language");
            if (language != null) {
                var tag = language.getValue();
                var userLocale = App.getLocale();
                var entries = new ArrayList<CharSequence>();
                var lstLang = LangList.LOCALES;
                for (var lang : lstLang) {
                    if (lang.equals(SYSTEM)) {
                        entries.add(getString(rikka.core.R.string.follow_system));
                        continue;
                    }
                    var locale = Locale.forLanguageTag(lang);
                    entries.add(HtmlCompat.fromHtml(locale.getDisplayName(locale), HtmlCompat.FROM_HTML_MODE_LEGACY));
                }
                language.setEntries(entries.toArray(new CharSequence[0]));
                language.setEntryValues(lstLang);
                if (TextUtils.isEmpty(tag) || SYSTEM.equals(tag)) {
                    language.setSummary(getString(rikka.core.R.string.follow_system));
                } else {
                    var locale = Locale.forLanguageTag(tag);
                    language.setSummary(!TextUtils.isEmpty(locale.getScript()) ? locale.getDisplayScript(userLocale) : locale.getDisplayName(userLocale));
                }
                language.setOnPreferenceChangeListener((preference, newValue) -> {
                    var app = App.getInstance();
                    var locale = App.getLocale((String)newValue);
                    var res = app.getResources();
                    var config = res.getConfiguration();
                    config.setLocale(locale);
                    LocaleDelegate.setDefaultLocale(locale);
                    //noinspection deprecation
                    res.updateConfiguration(config, res.getDisplayMetrics());
                    MainActivity activity = (MainActivity) getActivity();
                    if (activity != null) {
                        activity.restart();
                    }
                    return true;
                });
            }

            Preference translation = findPreference("translation");
            if (translation != null) {
                translation.setOnPreferenceClickListener(preference -> {
                    NavUtil.startURL(requireActivity(), "https://lsposed.crowdin.com/lsposed");
                    return true;
                });
                translation.setSummary(getString(R.string.settings_translation_summary, getString(R.string.app_name)));
            }

            Preference translation_contributors = findPreference("translation_contributors");
            if (translation_contributors != null) {
                var translators = HtmlCompat.fromHtml(getString(R.string.translators), HtmlCompat.FROM_HTML_MODE_LEGACY);
                if (translators.toString().equals("null")) {
                    translation_contributors.setVisible(false);
                } else {
                    translation_contributors.setSummary(translators);
                }
            }
        }

        @NonNull
        @Override
        public RecyclerView onCreateRecyclerView(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, Bundle savedInstanceState) {
            BorderRecyclerView recyclerView = (BorderRecyclerView) super.onCreateRecyclerView(inflater, parent, savedInstanceState);
            RecyclerViewKt.fixEdgeEffect(recyclerView, false, true);
            recyclerView.getBorderViewDelegate().setBorderVisibilityChangedListener((top, oldTop, bottom, oldBottom) -> parentFragment.activityMainBinding.appBar.setLifted(!top));
            var fragment = getParentFragment();
            if (fragment instanceof SettingsFragment) {
                var settingsFragment = (SettingsFragment) fragment;
                View.OnClickListener l = v -> {
                    settingsFragment.activityMainBinding.appBar.setExpanded(true, true);
                    recyclerView.smoothScrollToPosition(0);
                    settingsFragment.showFabAndBottomNav();
                };
                settingsFragment.activityMainBinding.toolbar.setOnClickListener(l);
                settingsFragment.activityMainBinding.clickView.setOnClickListener(l);
            }
            return recyclerView;
        }
    }
}
