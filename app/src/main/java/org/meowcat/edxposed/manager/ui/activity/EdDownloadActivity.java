package org.meowcat.edxposed.manager.ui.activity;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.transition.TransitionManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;

import org.meowcat.edxposed.manager.App;
import org.meowcat.edxposed.manager.BuildConfig;
import org.meowcat.edxposed.manager.R;
import org.meowcat.edxposed.manager.databinding.ActivityEdDownloadBinding;
import org.meowcat.edxposed.manager.databinding.DialogInstallWarningBinding;
import org.meowcat.edxposed.manager.ui.fragment.BaseAdvancedInstaller;
import org.meowcat.edxposed.manager.ui.fragment.StatusInstallerFragment;
import org.meowcat.edxposed.manager.util.json.JSONUtils;
import org.meowcat.edxposed.manager.util.json.XposedTab;

import java.util.ArrayList;

public class EdDownloadActivity extends BaseActivity {
    ActivityEdDownloadBinding binding;
    private TabsAdapter tabsAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEdDownloadBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(view -> finish());
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }

        tabsAdapter = new TabsAdapter(getSupportFragmentManager());
        tabsAdapter.notifyDataSetChanged();
        binding.pager.setAdapter(tabsAdapter);
        binding.tabLayout.setupWithViewPager(binding.pager);
        //new JSONParser().execute();

        if (!App.getPreferences().getBoolean("hide_install_warning", false)) {
            DialogInstallWarningBinding binding = DialogInstallWarningBinding.inflate(getLayoutInflater());
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.install_warning_title)
                    .setMessage(R.string.install_warning)
                    .setView(binding.getRoot())
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        if (binding.checkbox.isChecked())
                            App.getPreferences().edit().putBoolean("hide_install_warning", true).apply();
                    })
                    .setCancelable(false)
                    .show();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_installer, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("StaticFieldLeak")
    private class JSONParser extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            try {
                return JSONUtils.getFileContent(JSONUtils.JSON_LINK);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(App.TAG, "AdvancedInstallerFragment -> " + e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result == null) {
                return;
            }
            try {
                final JSONUtils.XposedJson xposedJson = new Gson().fromJson(result, JSONUtils.XposedJson.class);

                TransitionManager.beginDelayedTransition(binding.tabLayout);
                for (XposedTab tab : xposedJson.tabs) {
                    if (tab.installers.size() > 0 && tab.sdks.contains(Build.VERSION.SDK_INT)) {
                        tabsAdapter.addFragment(tab.name, BaseAdvancedInstaller.newInstance(tab));
                        tabsAdapter.notifyDataSetChanged();
                    }
                }

                String newApkVersion = xposedJson.apk.version;
                String newApkLink = xposedJson.apk.link;
                String newApkChangelog = xposedJson.apk.changelog;

                if (newApkVersion == null) {
                    return;
                }

                SharedPreferences prefs;
                try {
                    prefs = EdDownloadActivity.this.getSharedPreferences(EdDownloadActivity.this.getPackageName() + "_preferences", MODE_PRIVATE);

                    prefs.edit().putString("changelog", newApkChangelog).apply();
                } catch (NullPointerException ignored) {
                }

                Integer a = BuildConfig.VERSION_CODE;
                Integer b = Integer.valueOf(newApkVersion);

                if (a.compareTo(b) < 0) {
                    StatusInstallerFragment.setUpdate(newApkLink, newApkChangelog, EdDownloadActivity.this);
                }

            } catch (Exception ignored) {
            }

        }
    }

    private class TabsAdapter extends FragmentPagerAdapter {

        private final ArrayList<String> titles = new ArrayList<>();
        private final ArrayList<Fragment> listFragment = new ArrayList<>();

        @SuppressWarnings("deprecation")
        TabsAdapter(FragmentManager mgr) {
            super(mgr);
            addFragment(getString(R.string.tabInstall), new StatusInstallerFragment());
        }

        void addFragment(String title, Fragment fragment) {
            titles.add(title);
            listFragment.add(fragment);
        }

        @Override
        public int getCount() {
            return listFragment.size();
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return listFragment.get(position);
        }

        @Override
        public String getPageTitle(int position) {
            return titles.get(position);
        }
    }
}
