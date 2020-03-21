package org.meowcat.edxposed.manager;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.meowcat.edxposed.manager.adapters.AppAdapter;
import org.meowcat.edxposed.manager.adapters.AppHelper;
import org.meowcat.edxposed.manager.adapters.BlackListAdapter;
import org.meowcat.edxposed.manager.adapters.CompatListAdapter;
import org.meowcat.edxposed.manager.databinding.ActivityBlackListBinding;

public class BlackListActivity extends BaseActivity implements AppAdapter.Callback {
    private SearchView searchView;
    private AppAdapter appAdapter;

    private SearchView.OnQueryTextListener searchListener;
    private ActivityBlackListBinding binding;
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            binding.swipeRefreshLayout.setRefreshing(true);
        }
    };
    private Handler handler = new Handler();
    private boolean isCompat;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isCompat = getIntent().getBooleanExtra("compat_list", false);
        binding = ActivityBlackListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(view -> finish());
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }
        setupWindowInsets(binding.snackbar, binding.recyclerView);
        final boolean isWhiteListMode = isWhiteListMode();
        appAdapter = isCompat ? new CompatListAdapter(this, binding) : new BlackListAdapter(this, isWhiteListMode, binding);
        appAdapter.setHasStableIds(true);
        binding.recyclerView.setAdapter(appAdapter);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        if (!XposedApp.getPreferences().getBoolean("md2", false)) {
            DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this,
                    DividerItemDecoration.VERTICAL);
            binding.recyclerView.addItemDecoration(dividerItemDecoration);
        }
        appAdapter.setCallback(this);
        handler.postDelayed(runnable, 300);
        binding.swipeRefreshLayout.setOnRefreshListener(appAdapter::refresh);

        searchListener = new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                appAdapter.filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                appAdapter.filter(newText);
                return false;
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_app_list, menu);
        searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        searchView.setOnQueryTextListener(searchListener);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isCompat && !AppHelper.isBlackListMode()) {
            new MaterialAlertDialogBuilder(this)
                    .setMessage(R.string.warning_list_not_enabled)
                    .setPositiveButton(R.string.Settings, (dialog, which) -> {
                        Intent intent = new Intent();
                        intent.setClass(BlackListActivity.this, SettingsActivity.class);
                        startActivity(intent);
                    })
                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> finish())
                    .setCancelable(false)
                    .show();
        }
        changeTitle(isBlackListMode(), isWhiteListMode());
    }


    private void changeTitle(boolean isBlackListMode, boolean isWhiteListMode) {
        if (isCompat) {
            setTitle(R.string.nav_title_compat_list);
        } else if (isBlackListMode) {
            setTitle(isWhiteListMode ? R.string.title_white_list : R.string.title_black_list);
        } else {
            setTitle(R.string.nav_title_black_list);
        }
    }

    private boolean isWhiteListMode() {
        return AppHelper.isWhiteListMode();
    }

    private boolean isBlackListMode() {
        return AppHelper.isBlackListMode();
    }

    @Override
    public void onDataReady() {
        handler.removeCallbacks(runnable);
        binding.swipeRefreshLayout.setRefreshing(false);
        String queryStr = searchView != null ? searchView.getQuery().toString() : "";
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                appAdapter.getFilter().filter(queryStr);
            }
        });
    }

    @Override
    public void onItemClick(View v, ApplicationInfo info) {
        AppHelper.showMenu(this, getSupportFragmentManager(), v, info);
    }

    @Override
    public void onBackPressed() {
        if (searchView.isIconified()) {
            super.onBackPressed();
        } else {
            searchView.setIconified(true);
        }
    }
}
