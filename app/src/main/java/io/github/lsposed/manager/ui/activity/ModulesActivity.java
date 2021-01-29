package io.github.lsposed.manager.ui.activity;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.github.lsposed.manager.App;
import io.github.lsposed.manager.BuildConfig;
import io.github.lsposed.manager.Constants;
import io.github.lsposed.manager.R;
import io.github.lsposed.manager.adapters.AppAdapter;
import io.github.lsposed.manager.adapters.AppHelper;
import io.github.lsposed.manager.databinding.ActivityModulesBinding;
import io.github.lsposed.manager.util.GlideApp;
import io.github.lsposed.manager.util.LinearLayoutManagerFix;
import io.github.lsposed.manager.util.ModuleUtil;
import io.github.lsposed.manager.util.NavUtil;
import me.zhanghai.android.fastscroll.FastScrollerBuilder;

import static android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS;

public class ModulesActivity extends BaseActivity implements ModuleUtil.ModuleListener {

    public static final String SETTINGS_CATEGORY = "de.robv.android.xposed.category.MODULE_SETTINGS";
    ActivityModulesBinding binding;
    private int installedXposedVersion;
    private ApplicationFilter filter;
    private SearchView searchView;
    private SearchView.OnQueryTextListener mSearchListener;
    private PackageManager pm;
    private final DateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private ModuleUtil moduleUtil;
    private ModuleAdapter adapter = null;
    private final Runnable reloadModules = new Runnable() {
        public void run() {
            String queryStr = searchView != null ? searchView.getQuery().toString() : "";
            ArrayList<ModuleUtil.InstalledModule> showList;
            ArrayList<ModuleUtil.InstalledModule> fullList = new ArrayList<>(moduleUtil.getModules().values());
            if (queryStr.length() == 0) {
                showList = fullList;
            } else {
                showList = new ArrayList<>();
                String filter = queryStr.toLowerCase();
                for (ModuleUtil.InstalledModule info : fullList) {
                    if (lowercaseContains(AppAdapter.getAppLabel(info.app, pm), filter)
                            || lowercaseContains(info.packageName, filter)) {
                        showList.add(info);
                    }
                }
            }
            Comparator<PackageInfo> cmp = AppHelper.getAppListComparator(preferences.getInt("list_sort", 0), pm);
            fullList.sort((a, b) -> {
                boolean aChecked = moduleUtil.isModuleEnabled(a.packageName);
                boolean bChecked = moduleUtil.isModuleEnabled(b.packageName);
                if (aChecked == bChecked) {
                    return cmp.compare(a.pkg, b.pkg);
                } else if (aChecked) {
                    return -1;
                } else {
                    return 1;
                }
            });
            adapter.addAll(showList);
            adapter.notifyDataSetChanged();
            moduleUtil.updateModulesList(false);
            binding.swipeRefreshLayout.setRefreshing(false);
        }
    };
    private String selectedPackageName;

    private void filter(String constraint) {
        filter.filter(constraint);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityModulesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(view -> finish());
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }
        filter = new ApplicationFilter();
        moduleUtil = ModuleUtil.getInstance();
        pm = getPackageManager();
        installedXposedVersion = Constants.getXposedApiVersion();
        if (installedXposedVersion <= 0) {
            Snackbar.make(binding.snackbar, R.string.xposed_not_active, Snackbar.LENGTH_LONG).setAction(R.string.Settings, v -> {
                Intent intent = new Intent();
                intent.setClass(ModulesActivity.this, SettingsActivity.class);
                startActivity(intent);
            }).show();
        }
        adapter = new ModuleAdapter();
        adapter.setHasStableIds(true);
        moduleUtil.addListener(this);
        binding.recyclerView.setAdapter(adapter);
        binding.recyclerView.setLayoutManager(new LinearLayoutManagerFix(this));
        FastScrollerBuilder fastScrollerBuilder = new FastScrollerBuilder(binding.recyclerView);
        if (!preferences.getBoolean("md2", true)) {
            DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this,
                    DividerItemDecoration.VERTICAL);
            binding.recyclerView.addItemDecoration(dividerItemDecoration);
        } else {
            fastScrollerBuilder.useMd2Style();
        }
        fastScrollerBuilder.build();
        binding.swipeRefreshLayout.setOnRefreshListener(reloadModules::run);
        mSearchListener = new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filter(newText);
                return false;
            }
        };

    }

    @Override
    protected void onResume() {
        super.onResume();
        reloadModules.run();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_modules, menu);
        searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        searchView.setOnQueryTextListener(mSearchListener);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        if (requestCode == 42) {
            File listModules = new File(Constants.getEnabledModulesListFile());
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    try {
                        OutputStream os = getContentResolver().openOutputStream(uri);
                        if (os != null) {
                            FileInputStream in = new FileInputStream(listModules);
                            byte[] buffer = new byte[1024];
                            int len;
                            while ((len = in.read(buffer)) > 0) {
                                os.write(buffer, 0, len);
                            }
                            os.close();
                        }
                    } catch (Exception e) {
                        Snackbar.make(binding.snackbar, getResources().getString(R.string.logs_save_failed) + "\n" + e.getMessage(), Snackbar.LENGTH_LONG).show();
                    }
                }
            }
        } else if (requestCode == 43) {
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    try {
                        OutputStream os = getContentResolver().openOutputStream(uri);
                        if (os != null) {
                            PrintWriter fileOut = new PrintWriter(os);

                            Set<String> keys = ModuleUtil.getInstance().getModules().keySet();
                            for (String key1 : keys) {
                                fileOut.println(key1);
                            }
                            fileOut.close();
                            os.close();
                        }
                    } catch (Exception e) {
                        Snackbar.make(binding.snackbar, getResources().getString(R.string.logs_save_failed) + "\n" + e.getMessage(), Snackbar.LENGTH_LONG).show();
                    }
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Intent intent;
        int itemId = item.getItemId();
        if (itemId == R.id.export_enabled_modules) {
            if (ModuleUtil.getInstance().getEnabledModules().isEmpty()) {
                Snackbar.make(binding.snackbar, R.string.no_enabled_modules, Snackbar.LENGTH_SHORT).show();
                return false;
            }
            intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/*");
            intent.putExtra(Intent.EXTRA_TITLE, "enabled_modules.list");
            startActivityForResult(intent, 42);
            return true;
        } else if (itemId == R.id.export_installed_modules) {
            Map<String, ModuleUtil.InstalledModule> installedModules = ModuleUtil.getInstance().getModules();

            if (installedModules.isEmpty()) {
                Snackbar.make(binding.snackbar, R.string.no_installed_modules, Snackbar.LENGTH_SHORT).show();
                return false;
            }
            intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/*");
            intent.putExtra(Intent.EXTRA_TITLE, "installed_modules.list");
            startActivityForResult(intent, 43);
            return true;
        }
        if (AppHelper.onOptionsItemSelected(item, preferences)) {
            moduleUtil.updateModulesList(false, null);
            reloadModules.run();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        moduleUtil.removeListener(this);
        binding.recyclerView.setAdapter(null);
        adapter = null;
    }

    @Override
    public void onSingleInstalledModuleReloaded(ModuleUtil moduleUtil, String packageName, ModuleUtil.InstalledModule module) {
        moduleUtil.updateModulesList(false);
        runOnUiThread(reloadModules);
    }

    @Override
    public void onInstalledModulesReloaded(ModuleUtil moduleUtil) {
        moduleUtil.updateModulesList(false);
        runOnUiThread(reloadModules);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        ModuleUtil.InstalledModule module = ModuleUtil.getInstance().getModule(selectedPackageName);
        if (module == null) {
            return false;
        }
        int itemId = item.getItemId();
        if (itemId == R.id.menu_launch) {
            String packageName = module.packageName;
            if (packageName == null) {
                return false;
            }
            Intent intent = getSettingsIntent(packageName);
            if (intent != null) {
                startActivity(intent);
            } else {
                Snackbar.make(binding.snackbar, R.string.module_no_ui, Snackbar.LENGTH_LONG).show();
            }
            return true;
        } else if (itemId == R.id.menu_app_store) {
            Uri uri = Uri.parse("market://details?id=" + module.packageName);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        } else if (itemId == R.id.menu_app_info) {
            startActivity(new Intent(ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", module.packageName, null)));
            return true;
        } else if (itemId == R.id.menu_uninstall) {
            startActivity(new Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.fromParts("package", module.packageName, null)));
            return true;
        } else if (itemId == R.id.menu_scope) {
            if (App.supportScope()) {
                Intent intent = new Intent(this, AppListActivity.class);
                intent.putExtra("modulePackageName", module.packageName);
                intent.putExtra("moduleName", module.getAppName());
                startActivity(intent);
            } else {
                new MaterialAlertDialogBuilder(this)
                        .setMessage(R.string.scope_not_supported)
                        .setPositiveButton(R.string.download_view_download, (dialog, which) -> NavUtil.startURL(this, getString(R.string.about_source)))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
            return true;
        }
        return super.onContextItemSelected(item);
    }

    private Intent getSettingsIntent(String packageName) {
        // taken from
        // ApplicationPackageManager.getLaunchIntentForPackage(String)
        // first looks for an Xposed-specific category, falls back to
        // getLaunchIntentForPackage
        PackageManager pm = getPackageManager();

        Intent intentToResolve = new Intent(Intent.ACTION_MAIN);
        intentToResolve.addCategory(SETTINGS_CATEGORY);
        intentToResolve.setPackage(packageName);
        List<ResolveInfo> ris = pm.queryIntentActivities(intentToResolve, 0);

        if (ris.size() <= 0) {
            return pm.getLaunchIntentForPackage(packageName);
        }

        Intent intent = new Intent(intentToResolve);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClassName(ris.get(0).activityInfo.packageName, ris.get(0).activityInfo.name);
        return intent;
    }

    private boolean lowercaseContains(String s, CharSequence filter) {
        return !TextUtils.isEmpty(s) && s.toLowerCase().contains(filter);
    }

    @Override
    public void onBackPressed() {
        if (searchView.isIconified()) {
            super.onBackPressed();
        } else {
            searchView.setIconified(true);
        }
    }

    private class ModuleAdapter extends RecyclerView.Adapter<ModuleAdapter.ViewHolder> {
        ArrayList<ModuleUtil.InstalledModule> items = new ArrayList<>();

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_module, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ModuleUtil.InstalledModule item = items.get(position);
            holder.itemView.setOnClickListener(v -> {
                String packageName = item.packageName;
                if (packageName == null || packageName.equals(BuildConfig.APPLICATION_ID)) {
                    return;
                }
                Intent launchIntent = getSettingsIntent(packageName);
                if (launchIntent != null) {
                    startActivity(launchIntent);
                } else {
                    Snackbar.make(findViewById(R.id.snackbar), R.string.module_no_ui, Snackbar.LENGTH_LONG).show();
                }
            });

            holder.itemView.setOnLongClickListener(v -> {
                selectedPackageName = item.packageName;
                return false;
            });

            holder.itemView.setOnCreateContextMenuListener((menu, v, menuInfo) -> getMenuInflater().inflate(R.menu.context_menu_modules, menu));
            holder.appName.setText(item.getAppName());

            TextView version = holder.appVersion;
            version.setText(Objects.requireNonNull(item).versionName);
            version.setSelected(true);

            TextView packageTv = holder.appPackage;
            packageTv.setText(item.packageName);

            String creationDate = dateformat.format(new Date(item.installTime));
            String updateDate = dateformat.format(new Date(item.updateTime));
            holder.timestamps.setText(getString(R.string.install_timestamps, creationDate, updateDate));

            GlideApp.with(holder.appIcon)
                    .load(item.getPackageInfo())
                    .into(holder.appIcon);

            TextView descriptionText = holder.appDescription;
            descriptionText.setVisibility(View.VISIBLE);
            if (!item.getDescription().isEmpty()) {
                descriptionText.setText(item.getDescription());
            } else {
                descriptionText.setText(getString(R.string.module_empty_description));
                descriptionText.setTextColor(ContextCompat.getColor(ModulesActivity.this, R.color.warning));
            }

            SwitchCompat mSwitch = holder.mSwitch;
            mSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                String packageName = item.packageName;
                boolean changed = moduleUtil.isModuleEnabled(packageName) ^ isChecked;
                if (changed) {
                    if (isChecked && AppHelper.getScopeList(packageName).isEmpty()) {
                        moduleUtil.setModuleEnabled(packageName, true);
                        Intent intent = new Intent(ModulesActivity.this, AppListActivity.class);
                        intent.putExtra("modulePackageName", packageName);
                        intent.putExtra("moduleName", item.getAppName());
                        startActivity(intent);
                        return;
                    }
                    moduleUtil.setModuleEnabled(packageName, isChecked);
                    moduleUtil.updateModulesList(true, binding.snackbar);
                }
            });
            mSwitch.setChecked(moduleUtil.isModuleEnabled(item.packageName));
            TextView warningText = holder.warningText;

            if (item.minVersion == 0) {
                if (!preferences.getBoolean("skip_xposedminversion_check", false)) {
                    mSwitch.setEnabled(false);
                }
                warningText.setText(getString(R.string.no_min_version_specified));
                warningText.setVisibility(View.VISIBLE);
            } else if (installedXposedVersion > 0 && item.minVersion > installedXposedVersion) {
                if (!preferences.getBoolean("skip_xposedminversion_check", false)) {
                    mSwitch.setEnabled(false);
                }
                warningText.setText(String.format(getString(R.string.warning_xposed_min_version), item.minVersion));
                warningText.setVisibility(View.VISIBLE);
            } else if (item.minVersion < ModuleUtil.MIN_MODULE_VERSION) {
                if (!preferences.getBoolean("skip_xposedminversion_check", false)) {
                    mSwitch.setEnabled(false);
                }
                warningText.setText(String.format(getString(R.string.warning_min_version_too_low), item.minVersion, ModuleUtil.MIN_MODULE_VERSION));
                warningText.setVisibility(View.VISIBLE);
            } else if (item.isInstalledOnExternalStorage()) {
                if (!preferences.getBoolean("skip_xposedminversion_check", false)) {
                    mSwitch.setEnabled(false);
                }
                warningText.setText(getString(R.string.warning_installed_on_external_storage));
                warningText.setVisibility(View.VISIBLE);
            } else if (installedXposedVersion == 0 || (installedXposedVersion == -1)) {
                if (!preferences.getBoolean("skip_xposedminversion_check", false)) {
                    mSwitch.setEnabled(false);
                }
                warningText.setText(getString(R.string.not_installed_no_lollipop));
                warningText.setVisibility(View.VISIBLE);
            } else {
                mSwitch.setEnabled(true);
                warningText.setVisibility(View.GONE);
            }
        }

        void addAll(ArrayList<ModuleUtil.InstalledModule> items) {
            this.items = items;
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            if (items != null) {
                return items.size();
            } else {
                return 0;
            }
        }

        @Override
        public long getItemId(int position) {
            return items.get(position).hashCode();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView appIcon;
            TextView appName;
            TextView appPackage;
            TextView appDescription;
            TextView appVersion;
            TextView timestamps;
            TextView warningText;
            SwitchCompat mSwitch;

            ViewHolder(View itemView) {
                super(itemView);
                appIcon = itemView.findViewById(R.id.app_icon);
                appName = itemView.findViewById(R.id.app_name);
                appDescription = itemView.findViewById(R.id.description);
                appPackage = itemView.findViewById(R.id.package_name);
                appVersion = itemView.findViewById(R.id.version_name);
                timestamps = itemView.findViewById(R.id.timestamps);
                warningText = itemView.findViewById(R.id.warning);
                mSwitch = itemView.findViewById(R.id.checkbox);
            }
        }
    }

    class ApplicationFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            runOnUiThread(reloadModules);
            return null;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            runOnUiThread(reloadModules);
        }
    }
}
