package org.meowcat.edxposed.manager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
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
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter;
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration;

import org.meowcat.edxposed.manager.adapters.CursorRecyclerViewAdapter;
import org.meowcat.edxposed.manager.repo.RepoDb;
import org.meowcat.edxposed.manager.repo.RepoDbDefinitions;
import org.meowcat.edxposed.manager.util.ModuleUtil;
import org.meowcat.edxposed.manager.util.RepoLoader;

import java.text.DateFormat;
import java.util.Date;

public class DownloadActivity extends BaseActivity implements RepoLoader.RepoListener, ModuleUtil.ModuleListener, SharedPreferences.OnSharedPreferenceChangeListener {
    private SharedPreferences mPref;
    private DownloadsAdapter mAdapter;
    private String mFilterText;
    private RepoLoader mRepoLoader;
    private ModuleUtil mModuleUtil;
    private int mSortingOrder;
    private SearchView mSearchView;
    private SharedPreferences mIgnoredUpdatesPref;
    private boolean changed = false;
    private BroadcastReceiver connectionListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mRepoLoader != null) {
                mRepoLoader.triggerReload(true);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_download);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(view -> finish());
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }

        setupWindowInsets();
        mPref = XposedApp.getPreferences();
        mRepoLoader = RepoLoader.getInstance();
        mModuleUtil = ModuleUtil.getInstance();
        mAdapter = new DownloadsAdapter(this, RepoDb.queryModuleOverview(mSortingOrder, mFilterText));
        /*mAdapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence constraint) {
                return RepoDb.queryModuleOverview(mSortingOrder, constraint);
            }
        });*/

        mSortingOrder = mPref.getInt("download_sorting_order",
                RepoDb.SORT_STATUS);

        mIgnoredUpdatesPref = getSharedPreferences("update_ignored", MODE_PRIVATE);
        RecyclerView mListView = findViewById(R.id.recyclerView);
        if (Build.VERSION.SDK_INT >= 26) {
            mListView.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
        }
        final SwipeRefreshLayout refreshLayout = findViewById(R.id.swipeRefreshLayout);
        refreshLayout.setOnRefreshListener(() -> {
            mRepoLoader.setSwipeRefreshLayout(refreshLayout);
            mRepoLoader.triggerReload(true);
        });
        mRepoLoader.addListener(this, true);
        mModuleUtil.addListener(this);
        mListView.setAdapter(mAdapter);

        mListView.setLayoutManager(new LinearLayoutManager(this));
        StickyRecyclerHeadersDecoration headersDecor = new StickyRecyclerHeadersDecoration(mAdapter);
        mListView.addItemDecoration(headersDecor);
        mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                headersDecor.invalidateHeaders();
            }
        });

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mListView.getContext(),
                DividerItemDecoration.VERTICAL);
        mListView.addItemDecoration(dividerItemDecoration);
    }


    @Override
    public void onResume() {
        super.onResume();

        mIgnoredUpdatesPref.registerOnSharedPreferenceChangeListener(this);
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

        mRepoLoader.removeListener(this);
        mModuleUtil.removeListener(this);
        mIgnoredUpdatesPref.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_download, menu);

        // Setup search button
        mSearchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        mSearchView.setIconifiedByDefault(true);
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                setFilter(query);
                mSearchView.clearFocus();
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
        mFilterText = filterText;
        reloadItems();
    }

    private void reloadItems() {
        runOnUiThread(() -> {
            mAdapter.swapCursor(RepoDb.queryModuleOverview(mSortingOrder, mFilterText));
            mAdapter.notifyDataSetChanged();
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_sort) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.download_sorting_title)
                    .setSingleChoiceItems(R.array.download_sort_order, mSortingOrder, (dialog, which) -> {
                        mSortingOrder = which;
                        mPref.edit().putInt("download_sorting_order", mSortingOrder).apply();
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
        if (mSearchView.isIconified()) {
            super.onBackPressed();
        } else {
            mSearchView.setIconified(true);
        }
    }

    private class DownloadsAdapter extends CursorRecyclerViewAdapter<DownloadsAdapter.ViewHolder> implements StickyRecyclerHeadersAdapter {
        private final Context mContext;
        private final DateFormat mDateFormatter = DateFormat.getDateInstance(DateFormat.SHORT);
        private final SharedPreferences mPrefs;
        private String[] sectionHeaders;

        DownloadsAdapter(Context context, Cursor cursor) {
            super(context, cursor);
            mContext = context;
            mPrefs = context.getSharedPreferences("update_ignored", MODE_PRIVATE);

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
            boolean updateIgnored = mPrefs.getBoolean(cursor.getString(RepoDbDefinitions.OverviewColumnsIndexes.PKGNAME), false);
            boolean updateIgnorePreference = XposedApp.getPreferences().getBoolean("ignore_updates", false);
            boolean hasUpdate = cursor.getInt(RepoDbDefinitions.OverviewColumnsIndexes.HAS_UPDATE) > 0;

            if (hasUpdate && updateIgnored && updateIgnorePreference) {
                hasUpdate = false;
            }

            if (mSortingOrder != RepoDb.SORT_STATUS) {
                long timestamp = (mSortingOrder == RepoDb.SORT_UPDATED) ? updated : created;
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
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_download, parent, false);
            return new ViewHolder(v);
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
            boolean updateIgnored = mPrefs.getBoolean(cursor.getString(RepoDbDefinitions.OverviewColumnsIndexes.PKGNAME), false);
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
                txtStatus.setText(mContext.getString(
                        R.string.download_status_update_available,
                        installedVersion, latestVersion));
                txtStatus.setTextColor(getResources().getColor(R.color.download_status_update_available));
                txtStatus.setVisibility(View.VISIBLE);
            } else if (isInstalled) {
                txtStatus.setText(mContext.getString(
                        R.string.download_status_installed, installedVersion));
                //txtStatus.setTextColor(ThemeUtil.getThemeColor(mContext, R.attr.download_status_installed));
                TypedArray typedArray = DownloadActivity.this.getTheme().obtainStyledAttributes(new int[]{android.R.attr.textColorSecondary});
                int textColor = typedArray.getColor(0, 0);
                typedArray.recycle();
                txtStatus.setTextColor(textColor);
                txtStatus.setVisibility(View.VISIBLE);
            } else {
                txtStatus.setVisibility(View.GONE);
            }

            String creationDate = mDateFormatter.format(new Date(created));
            String updateDate = mDateFormatter.format(new Date(updated));
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

            ViewHolder(View itemView) {
                super(itemView);
                appName = itemView.findViewById(R.id.title);
                appDescription = itemView.findViewById(R.id.description);
                downloadStatus = itemView.findViewById(R.id.downloadStatus);
                timestamps = itemView.findViewById(R.id.timestamps);
            }
        }
    }
}

