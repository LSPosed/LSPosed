package org.meowcat.edxposed.manager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter;
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration;

import org.meowcat.edxposed.manager.adapters.CursorRecyclerViewAdapter;
import org.meowcat.edxposed.manager.databinding.ActivityDownloadBinding;
import org.meowcat.edxposed.manager.databinding.ItemDownloadBinding;
import org.meowcat.edxposed.manager.repo.RepoDb;
import org.meowcat.edxposed.manager.repo.RepoDbDefinitions;
import org.meowcat.edxposed.manager.util.ModuleUtil;
import org.meowcat.edxposed.manager.util.RepoLoader;

import java.text.DateFormat;
import java.util.Date;

public class DownloadActivity extends BaseActivity implements RepoLoader.RepoListener, ModuleUtil.ModuleListener, SharedPreferences.OnSharedPreferenceChangeListener {
    private DownloadsAdapter adapter;
    private String filterText;
    private RepoLoader repoLoader;
    private ModuleUtil moduleUtil;
    private int sortingOrder;
    private SearchView searchView;
    private SharedPreferences ignoredUpdatesPref;
    private boolean changed = false;
    private BroadcastReceiver connectionListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (repoLoader != null) {
                repoLoader.triggerReload(true);
            }
        }
    };
    private ActivityDownloadBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDownloadBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(view -> finish());
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }

        setupWindowInsets(binding.snackbar, binding.recyclerView);
        repoLoader = RepoLoader.getInstance();
        moduleUtil = ModuleUtil.getInstance();
        adapter = new DownloadsAdapter(this, RepoDb.queryModuleOverview(sortingOrder, filterText));
        /*adapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence constraint) {
                return RepoDb.queryModuleOverview(sortingOrder, constraint);
            }
        });*/

        sortingOrder = XposedApp.getPreferences().getInt("download_sorting_order", RepoDb.SORT_STATUS);

        ignoredUpdatesPref = getSharedPreferences("update_ignored", MODE_PRIVATE);
        binding.recyclerView.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            repoLoader.setSwipeRefreshLayout(binding.swipeRefreshLayout);
            repoLoader.triggerReload(true);
        });
        repoLoader.addListener(this, true);
        moduleUtil.addListener(this);
        binding.recyclerView.setAdapter(adapter);

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        StickyRecyclerHeadersDecoration headersDecor = new StickyRecyclerHeadersDecoration(adapter);
        binding.recyclerView.addItemDecoration(headersDecor);
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                headersDecor.invalidateHeaders();
            }
        });
        if (!XposedApp.getPreferences().getBoolean("md2", false)) {
            DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this,
                    DividerItemDecoration.VERTICAL);
            binding.recyclerView.addItemDecoration(dividerItemDecoration);
        }
    }


    @Override
    public void onResume() {
        super.onResume();

        ignoredUpdatesPref.registerOnSharedPreferenceChangeListener(this);
        if (changed) {
            reloadItems();
            changed = !changed;
        }

        registerReceiver(connectionListener, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    public void onPause() {
        super.onPause();

        unregisterReceiver(connectionListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        repoLoader.removeListener(this);
        moduleUtil.removeListener(this);
        ignoredUpdatesPref.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_download, menu);

        // Setup search button
        searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        searchView.setIconifiedByDefault(true);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                setFilter(query);
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                setFilter(newText);
                return true;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    private void setFilter(String filterText) {
        this.filterText = filterText;
        reloadItems();
    }

    private void reloadItems() {
        runOnUiThread(() -> {
            adapter.changeCursor(RepoDb.queryModuleOverview(sortingOrder, filterText));
            TransitionManager.beginDelayedTransition(binding.recyclerView);
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_sort) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.download_sorting_title)
                    .setSingleChoiceItems(R.array.download_sort_order, sortingOrder, (dialog, which) -> {
                        sortingOrder = which;
                        XposedApp.getPreferences().edit().putInt("download_sorting_order", sortingOrder).apply();
                        reloadItems();
                        dialog.dismiss();
                    })
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRepoReloaded(final RepoLoader loader) {
        reloadItems();
    }

    @Override
    public void onSingleInstalledModuleReloaded(ModuleUtil moduleUtil, String packageName, ModuleUtil.InstalledModule module) {
        reloadItems();
    }

    @Override
    public void onInstalledModulesReloaded(ModuleUtil moduleUtil) {
        reloadItems();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        changed = true;
    }

    @Override
    public void onBackPressed() {
        if (searchView.isIconified()) {
            super.onBackPressed();
        } else {
            searchView.setIconified(true);
        }
    }

    private class DownloadsAdapter extends CursorRecyclerViewAdapter<DownloadsAdapter.ViewHolder> implements StickyRecyclerHeadersAdapter {
        private final Context context;
        private final DateFormat dateFormatter = DateFormat.getDateInstance(DateFormat.SHORT);
        private final SharedPreferences prefs;
        private String[] sectionHeaders;

        DownloadsAdapter(Context context, Cursor cursor) {
            super(cursor);
            this.context = context;
            prefs = context.getSharedPreferences("update_ignored", MODE_PRIVATE);

            Resources res = context.getResources();
            sectionHeaders = new String[]{
                    res.getString(R.string.download_section_framework),
                    res.getString(R.string.download_section_update_available),
                    res.getString(R.string.download_section_installed),
                    res.getString(R.string.download_section_not_installed),
                    res.getString(R.string.download_section_24h),
                    res.getString(R.string.download_section_7d),
                    res.getString(R.string.download_section_30d),
                    res.getString(R.string.download_section_older)};
        }

        @Override
        public long getHeaderId(int position) {
            Cursor cursor = getCursor();
            cursor.moveToPosition(position);
            long created = cursor.getLong(RepoDbDefinitions.OverviewColumnsIndexes.CREATED);
            long updated = cursor.getLong(RepoDbDefinitions.OverviewColumnsIndexes.UPDATED);
            boolean isFramework = cursor.getInt(RepoDbDefinitions.OverviewColumnsIndexes.IS_FRAMEWORK) > 0;
            boolean isInstalled = cursor.getInt(RepoDbDefinitions.OverviewColumnsIndexes.IS_INSTALLED) > 0;
            boolean updateIgnored = prefs.getBoolean(cursor.getString(RepoDbDefinitions.OverviewColumnsIndexes.PKGNAME), false);
            boolean updateIgnorePreference = XposedApp.getPreferences().getBoolean("ignore_updates", false);
            boolean hasUpdate = cursor.getInt(RepoDbDefinitions.OverviewColumnsIndexes.HAS_UPDATE) > 0;

            if (hasUpdate && updateIgnored && updateIgnorePreference) {
                hasUpdate = false;
            }

            if (sortingOrder != RepoDb.SORT_STATUS) {
                long timestamp = (sortingOrder == RepoDb.SORT_UPDATED) ? updated : created;
                long age = System.currentTimeMillis() - timestamp;
                final long mSecsPerDay = 24 * 60 * 60 * 1000L;
                if (age < mSecsPerDay)
                    return 4;
                if (age < 7 * mSecsPerDay)
                    return 5;
                if (age < 30 * mSecsPerDay)
                    return 6;
                return 7;
            } else {
                if (isFramework)
                    return 0;

                if (hasUpdate)
                    return 1;
                else if (isInstalled)
                    return 2;
                else
                    return 3;
            }
        }

        @Override
        public RecyclerView.ViewHolder onCreateHeaderViewHolder(ViewGroup parent) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.sticky_header_download, parent, false);
            return new RecyclerView.ViewHolder(view) {
            };
        }

        @Override
        public void onBindHeaderViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
            long section = getHeaderId(position);
            TextView tv = viewHolder.itemView.findViewById(android.R.id.title);
            tv.setText(sectionHeaders[(int) section]);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemDownloadBinding binding = ItemDownloadBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, Cursor cursor) {
            String title = cursor.getString(RepoDbDefinitions.OverviewColumnsIndexes.TITLE);
            String summary = cursor.getString(RepoDbDefinitions.OverviewColumnsIndexes.SUMMARY);
            String installedVersion = cursor.getString(RepoDbDefinitions.OverviewColumnsIndexes.INSTALLED_VERSION);
            String latestVersion = cursor.getString(RepoDbDefinitions.OverviewColumnsIndexes.LATEST_VERSION);
            long created = cursor.getLong(RepoDbDefinitions.OverviewColumnsIndexes.CREATED);
            long updated = cursor.getLong(RepoDbDefinitions.OverviewColumnsIndexes.UPDATED);
            boolean isInstalled = cursor.getInt(RepoDbDefinitions.OverviewColumnsIndexes.IS_INSTALLED) > 0;
            boolean updateIgnored = prefs.getBoolean(cursor.getString(RepoDbDefinitions.OverviewColumnsIndexes.PKGNAME), false);
            boolean updateIgnorePreference = XposedApp.getPreferences().getBoolean("ignore_updates", false);
            boolean hasUpdate = cursor.getInt(RepoDbDefinitions.OverviewColumnsIndexes.HAS_UPDATE) > 0;

            if (hasUpdate && updateIgnored && updateIgnorePreference) {
                hasUpdate = false;
            }

            TextView txtTitle = holder.appName;
            txtTitle.setText(title);

            TextView txtSummary = holder.appDescription;
            txtSummary.setText(summary);

            TextView txtStatus = holder.downloadStatus;
            if (hasUpdate) {
                txtStatus.setText(context.getString(
                        R.string.download_status_update_available,
                        installedVersion, latestVersion));
                txtStatus.setTextColor(ContextCompat.getColor(DownloadActivity.this, R.color.download_status_update_available));
                txtStatus.setVisibility(View.VISIBLE);
            } else if (isInstalled) {
                txtStatus.setText(context.getString(
                        R.string.download_status_installed, installedVersion));
                txtStatus.setTextColor(ContextCompat.getColor(DownloadActivity.this, R.color.warning));
                txtStatus.setTextColor(getThemedColor(android.R.attr.textColorHighlight));
                txtStatus.setVisibility(View.VISIBLE);
            } else {
                txtStatus.setVisibility(View.GONE);
            }

            String creationDate = dateFormatter.format(new Date(created));
            String updateDate = dateFormatter.format(new Date(updated));
            holder.timestamps.setText(getString(R.string.download_timestamps, creationDate, updateDate));
            String packageName = cursor.getString(RepoDbDefinitions.OverviewColumnsIndexes.PKGNAME);
            holder.itemView.setOnClickListener(v -> {
                Intent detailsIntent = new Intent(DownloadActivity.this, DownloadDetailsActivity.class);
                detailsIntent.setData(Uri.fromParts("package", packageName, null));
                startActivity(detailsIntent);
            });
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView appName;
            TextView appDescription;
            TextView downloadStatus;
            TextView timestamps;

            ViewHolder(ItemDownloadBinding binding) {
                super(binding.getRoot());
                appName = binding.title;
                appDescription = binding.description;
                downloadStatus = binding.downloadStatus;
                timestamps = binding.timestamps;
            }
        }
    }
}

