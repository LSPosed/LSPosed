package org.meowcat.edxposed.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import com.takisoft.preferencex.PreferenceFragmentCompat;

import org.meowcat.edxposed.manager.repo.Module;
import org.meowcat.edxposed.manager.util.PrefixedSharedPreferences;
import org.meowcat.edxposed.manager.util.RepoLoader;

import java.util.Map;

public class DownloadDetailsSettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferencesFix(Bundle savedInstanceState, String rootKey) {
        DownloadDetailsActivity mActivity = (DownloadDetailsActivity) getActivity();
        if (mActivity == null) {
            return;
        }
        final Module module = mActivity.getModule();
        if (module == null) {
            return;
        }

        final String packageName = module.packageName;

        PreferenceManager prefManager = getPreferenceManager();
        prefManager.setSharedPreferencesName("module_settings");
        PrefixedSharedPreferences.injectToPreferenceManager(prefManager, module.packageName);
        addPreferencesFromResource(R.xml.module_prefs);

        SharedPreferences prefs = getActivity().getSharedPreferences("module_settings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        if (prefs.getBoolean("no_global", true)) {
            for (Map.Entry<String, ?> k : prefs.getAll().entrySet()) {
                if (("global").equals(prefs.getString(k.getKey(), ""))) {
                    editor.putString(k.getKey(), "").apply();
                }
            }

            editor.putBoolean("no_global", false).apply();
        }

        Preference releaseType = findPreference("release_type");
        if (releaseType != null) {
            releaseType.setOnPreferenceChangeListener((preference, newValue) -> {
                RepoLoader.getInstance().setReleaseTypeLocal(packageName, (String) newValue);
                return true;
            });
        }
    }

}