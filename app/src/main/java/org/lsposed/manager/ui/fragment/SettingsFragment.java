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

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.text.HtmlCompat;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.snackbar.Snackbar;
import com.takisoft.preferencex.PreferenceFragmentCompat;

import org.lsposed.manager.App;
import org.lsposed.manager.BuildConfig;
import org.lsposed.manager.ConfigManager;
import org.lsposed.manager.R;
import org.lsposed.manager.databinding.FragmentSettingsBinding;
import org.lsposed.manager.ui.activity.MainActivity;
import org.lsposed.manager.util.BackupUtils;
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
        binding.getRoot().bringChildToFront(binding.appBar);
        binding.appBar.setLiftable(true);
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
        private SettingsFragment parentFragment;

        ActivityResultLauncher<String> backupLauncher = registerForActivityResult(new ActivityResultContracts.CreateDocument(),
                uri -> {
                    if (uri == null || parentFragment == null) return;
                    parentFragment.runAsync(() -> {
                        try {
                            BackupUtils.backup(uri);
                        } catch (Exception e) {
                            var text = App.getInstance().getString(R.string.settings_backup_failed2, e.getMessage());
                            if (parentFragment != null && parentFragment.binding != null && isResumed()) {
                                Snackbar.make(parentFragment.binding.snackbar, text, Snackbar.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(App.getInstance(), text, Toast.LENGTH_LONG).show();
                            }
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
                            if (parentFragment != null && parentFragment.binding != null && isResumed()) {
                                Snackbar.make(parentFragment.binding.snackbar, text, Snackbar.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(App.getInstance(), text, Toast.LENGTH_LONG).show();
                            }
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
        public void onCreatePreferencesFix(Bundle savedInstanceState, String rootKey) {
            final String SYSTEM = "SYSTEM";

            addPreferencesFromResource(R.xml.prefs);

            boolean installed = ConfigManager.isBinderAlive();
            SwitchPreference prefVerboseLogs = findPreference("disable_verbose_log");
            if (prefVerboseLogs != null) {
                prefVerboseLogs.setEnabled(!BuildConfig.DEBUG && installed);
                prefVerboseLogs.setChecked(!installed || !ConfigManager.isVerboseLogEnabled());
                prefVerboseLogs.setOnPreferenceChangeListener((preference, newValue) ->
                        ConfigManager.setVerboseLogEnabled(!(boolean) newValue));
            }

            SwitchPreference prefEnableResources = findPreference("enable_resources");
            if (prefEnableResources != null) {
                prefEnableResources.setEnabled(installed);
                prefEnableResources.setChecked(installed && ConfigManager.isResourceHookEnabled());
                prefEnableResources.setOnPreferenceChangeListener((preference, newValue) -> ConfigManager.setResourceHookEnabled((boolean) newValue));
            }

            SwitchPreference prefEnableShortcut = findPreference("enable_auto_add_shortcut");
            if (prefEnableShortcut != null) {
                prefEnableShortcut.setEnabled(!App.isParasitic());
                prefEnableShortcut.setChecked(installed && ConfigManager.isAddShortcut());
                prefEnableShortcut.setOnPreferenceChangeListener((preference, newValue) -> ConfigManager.setAddShortcut((boolean) newValue));
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
                language.setOnPreferenceChangeListener((preference, newValue) -> {
                    var locale = SYSTEM.equals(newValue) ? LocaleDelegate.getSystemLocale() : Locale.forLanguageTag((String) newValue);
                    LocaleDelegate.setDefaultLocale(locale);
                    MainActivity activity = (MainActivity) getActivity();
                    if (activity != null) {
                        activity.restart();
                    }
                    return true;
                });
                var tag = language.getValue();
                var userLocale = App.getLocale();
                var entries = new ArrayList<CharSequence>();
                entries.add(language.getEntries()[0]);
                var lstLang = getAppLanguages(requireContext(), R.string.Settings);
                for (var lang : lstLang) {
                    var locale = Locale.forLanguageTag(lang);
                    entries.add(HtmlCompat.fromHtml(String.format("%s - %s",
                            !TextUtils.isEmpty(locale.getScript()) ? locale.getDisplayScript(locale) : locale.getDisplayName(locale),
                            !TextUtils.isEmpty(locale.getScript()) ? locale.getDisplayScript(userLocale) : locale.getDisplayName(userLocale)
                    ), HtmlCompat.FROM_HTML_MODE_LEGACY));
                }
                language.setEntries(entries.toArray(new CharSequence[0]));
                lstLang.add(0, SYSTEM);
                language.setEntryValues(lstLang.toArray(new CharSequence[0]));
                if (TextUtils.isEmpty(tag) || SYSTEM.equals(tag)) {
                    language.setSummary(getString(rikka.material.R.string.follow_system));
                } else {
                    var locale = Locale.forLanguageTag(tag);
                    language.setSummary(!TextUtils.isEmpty(locale.getScript()) ? locale.getDisplayScript(userLocale) : locale.getDisplayName(userLocale));
                }
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

        @Override
        public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
            BorderRecyclerView recyclerView = (BorderRecyclerView) super.onCreateRecyclerView(inflater, parent, savedInstanceState);
            RecyclerViewKt.fixEdgeEffect(recyclerView, false, true);
            recyclerView.getBorderViewDelegate().setBorderVisibilityChangedListener((top, oldTop, bottom, oldBottom) -> {
                SettingsFragment fragment = (SettingsFragment) getParentFragment();
                if (fragment != null) {
                    fragment.binding.appBar.setLifted(!top);
                }
            });
            return recyclerView;
        }

        private ArrayList<String> getAppLanguages(Context ctx, int id) {
            Configuration conf = ctx.getResources().getConfiguration();
            Locale originalLocale = conf.getLocales().get(0);
            conf.setLocale(Locale.ENGLISH);
            final String reference = ctx.createConfigurationContext(conf).getString(id);

            var lstLang = new ArrayList<String>();
            lstLang.add(Locale.ENGLISH.getLanguage());

            for (String loc : ctx.getAssets().getLocales()) {
                if (loc.isEmpty()) {
                    continue;
                }
                Locale locale = Locale.forLanguageTag(loc);
                conf.setLocale(locale);
                if (!lstLang.contains(loc) && !reference.equals(ctx.createConfigurationContext(conf).getString(id))) {
                    lstLang.add(loc);
                }
            }
            conf.setLocale(originalLocale);
            return lstLang;
        }
    }
}
