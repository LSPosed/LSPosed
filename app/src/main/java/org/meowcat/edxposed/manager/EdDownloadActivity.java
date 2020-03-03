package org.meowcat.edxposed.manager;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.annimon.stream.Stream;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;

import org.meowcat.edxposed.manager.util.json.JSONUtils;
import org.meowcat.edxposed.manager.util.json.XposedTab;

import java.util.ArrayList;
import java.util.List;

public class EdDownloadActivity extends BaseActivity {

    private TabsAdapter tabsAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ed_download);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(view -> finish());
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }
        ViewPager mPager = findViewById(R.id.pager);
        TabLayout mTabLayout = findViewById(R.id.tab_layout);

        tabsAdapter = new TabsAdapter(getSupportFragmentManager());
        tabsAdapter.notifyDataSetChanged();
        mPager.setAdapter(tabsAdapter);
        mTabLayout.setupWithViewPager(mPager);
        new JSONParser().execute();

        if (!XposedApp.getPreferences().getBoolean("hide_install_warning", false)) {
            @SuppressLint("InflateParams") final View dontShowAgainView = getLayoutInflater().inflate(R.layout.dialog_install_warning, null);

            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.install_warning_title)
                    .setView(dontShowAgainView)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        CheckBox checkBox = dontShowAgainView.findViewById(android.R.id.checkbox);
                        if (checkBox.isChecked())
                            XposedApp.getPreferences().edit().putBoolean("hide_install_warning", true).apply();
                    })
                    .setCancelable(false)
                    .show();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_installer, menu);
        if (Build.VERSION.SDK_INT < 26) {
            menu.findItem(R.id.dexopt_all).setVisible(false);
            menu.findItem(R.id.speed_all).setVisible(false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @SuppressLint("StaticFieldLeak")
    private class JSONParser extends AsyncTask<Void, Void, String> {

        private String newApkVersion = null;
        private String newApkLink = null;
        private String newApkChangelog = null;

        @Override
        protected String doInBackground(Void... params) {
            try {
                return JSONUtils.getFileContent(JSONUtils.JSON_LINK);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(XposedApp.TAG, "AdvancedInstallerFragment -> " + e.getMessage());
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

                List<XposedTab> tabs = Stream.of(xposedJson.tabs)
                        .filter(value -> value.sdks.contains(Build.VERSION.SDK_INT)).toList();

                for (XposedTab tab : tabs) {
                    tabsAdapter.addFragment(tab.name, BaseAdvancedInstaller.newInstance(tab));
                    tabsAdapter.notifyDataSetChanged();
                }

                newApkVersion = xposedJson.apk.version;
                newApkLink = xposedJson.apk.link;
                newApkChangelog = xposedJson.apk.changelog;

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
