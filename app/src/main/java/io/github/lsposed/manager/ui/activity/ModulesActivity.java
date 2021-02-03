package io.github.lsposed.manager.ui.activity;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;

import io.github.lsposed.manager.Constants;
import io.github.lsposed.manager.R;
import io.github.lsposed.manager.adapters.AppHelper;
import io.github.lsposed.manager.adapters.ScopeAdapter;
import io.github.lsposed.manager.databinding.ActivityAppListBinding;
import io.github.lsposed.manager.util.GlideApp;
import io.github.lsposed.manager.util.LinearLayoutManagerFix;
import io.github.lsposed.manager.util.ModuleUtil;
import me.zhanghai.android.fastscroll.FastScrollerBuilder;

import static android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS;

public class ModulesActivity extends BaseActivity implements ModuleUtil.ModuleListener {

    ActivityAppListBinding binding;
    private ApplicationFilter filter;
    private SearchView searchView;
    private SearchView.OnQueryTextListener mSearchListener;
    private PackageManager pm;
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
                    if (lowercaseContains(ScopeAdapter.getAppLabel(info.app, pm), filter)
                            || lowercaseContains(info.packageName, filter)) {
                        showList.add(info);
                    }
                }
            }
            Comparator<PackageInfo> cmp = AppHelper.getAppListComparator(0, pm);
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
            moduleUtil.updateModulesList();
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
        binding = ActivityAppListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(view -> onBackPressed());
        binding.masterSwitch.setVisibility(View.GONE);
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }
        filter = new ApplicationFilter();
        moduleUtil = ModuleUtil.getInstance();
        pm = getPackageManager();
        adapter = new ModuleAdapter();
        adapter.setHasStableIds(true);
        moduleUtil.addListener(this);
        binding.recyclerView.setAdapter(adapter);
        binding.recyclerView.setLayoutManager(new LinearLayoutManagerFix(this));
        setupRecyclerViewInsets(binding.recyclerView, binding.getRoot());
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
    public void onDestroy() {
        super.onDestroy();
        moduleUtil.removeListener(this);
        binding.recyclerView.setAdapter(null);
        adapter = null;
    }

    @Override
    public void onSingleInstalledModuleReloaded(ModuleUtil moduleUtil, String packageName, ModuleUtil.InstalledModule module) {
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
            Intent intent = AppHelper.getSettingsIntent(packageName, pm);
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
        }
        return super.onContextItemSelected(item);
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
            boolean enabled = moduleUtil.isModuleEnabled(item.packageName);
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(ModulesActivity.this, AppListActivity.class);
                intent.putExtra("modulePackageName", item.packageName);
                intent.putExtra("moduleName", item.getAppName());
                startActivity(intent);
            });

            holder.itemView.setOnLongClickListener(v -> {
                selectedPackageName = item.packageName;
                return false;
            });

            holder.root.setAlpha(enabled ? 1.0f : .5f);

            holder.itemView.setOnCreateContextMenuListener((menu, v, menuInfo) -> {
                getMenuInflater().inflate(R.menu.context_menu_modules, menu);
                Intent intent = AppHelper.getSettingsIntent(item.packageName, pm);
                if (intent == null) {
                    menu.removeItem(R.id.menu_launch);
                }
            });
            holder.appName.setText(item.getAppName());

            TextView version = holder.appVersion;
            version.setText(Objects.requireNonNull(item).versionName);
            version.setSelected(true);

            GlideApp.with(holder.appIcon)
                    .load(item.getPackageInfo())
                    .into(holder.appIcon);

            SpannableStringBuilder sb = new SpannableStringBuilder();
            if (!item.getDescription().isEmpty()) {
                sb.append(item.getDescription());
            } else {
                sb.append(getString(R.string.module_empty_description));
            }

            int installedXposedVersion = Constants.getXposedApiVersion();
            String warningText = null;
            if (item.minVersion == 0) {
                warningText = getString(R.string.no_min_version_specified);
            } else if (installedXposedVersion > 0 && item.minVersion > installedXposedVersion) {
                warningText = String.format(getString(R.string.warning_xposed_min_version), item.minVersion);
            } else if (item.minVersion < ModuleUtil.MIN_MODULE_VERSION) {
                warningText = String.format(getString(R.string.warning_min_version_too_low), item.minVersion, ModuleUtil.MIN_MODULE_VERSION);
            } else if (item.isInstalledOnExternalStorage()) {
                warningText = getString(R.string.warning_installed_on_external_storage);
            }
            if (warningText != null) {
                sb.append("\n");
                sb.append(warningText);
                final ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(ContextCompat.getColor(ModulesActivity.this, R.color.material_red_500));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    final TypefaceSpan typefaceSpan = new TypefaceSpan(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                    sb.setSpan(typefaceSpan, sb.length() - warningText.length(), sb.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                } else {
                    final StyleSpan styleSpan = new StyleSpan(Typeface.BOLD);
                    sb.setSpan(styleSpan, sb.length() - warningText.length(), sb.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                }
                sb.setSpan(foregroundColorSpan, sb.length() - warningText.length(), sb.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            }
            holder.appDescription.setText(sb);
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
            View root;
            ImageView appIcon;
            TextView appName;
            TextView appDescription;
            TextView appVersion;
            TextView warningText;

            ViewHolder(View itemView) {
                super(itemView);
                root = itemView.findViewById(R.id.item_root);
                appIcon = itemView.findViewById(R.id.app_icon);
                appName = itemView.findViewById(R.id.app_name);
                appDescription = itemView.findViewById(R.id.description);
                appVersion = itemView.findViewById(R.id.version_name);
                appVersion.setVisibility(View.VISIBLE);
                warningText = itemView.findViewById(R.id.warning);
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
