package org.meowcat.edxposed.manager;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuItemCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.meowcat.edxposed.manager.adapters.AppAdapter;
import org.meowcat.edxposed.manager.adapters.AppHelper;
import org.meowcat.edxposed.manager.adapters.BlackListAdapter;

public class BlackListActivity extends BaseActivity implements AppAdapter.Callback {
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private SearchView mSearchView;
    private BlackListAdapter mAppAdapter;

    private SearchView.OnQueryTextListener mSearchListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_black_list);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(view -> finish());
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }
        setupWindowInsets();
        mSwipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        RecyclerView mRecyclerView = findViewById(R.id.recyclerView);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        final boolean isWhiteListMode = isWhiteListMode();
        mAppAdapter = new BlackListAdapter(this, isWhiteListMode);
        mRecyclerView.setAdapter(mAppAdapter);
        mAppAdapter.setCallback(this);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mRecyclerView.getContext(),
                DividerItemDecoration.VERTICAL);
        mRecyclerView.addItemDecoration(dividerItemDecoration);
        mSwipeRefreshLayout.setRefreshing(true);
        mSwipeRefreshLayout.setOnRefreshListener(mAppAdapter::refresh);
        mSearchListener = new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mAppAdapter.filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mAppAdapter.filter(newText);
                return false;
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_app_list, menu);
        mSearchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.menu_search));
        mSearchView.setOnQueryTextListener(mSearchListener);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!AppHelper.isBlackListMode()) {
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
        if (isBlackListMode) {
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
        mSwipeRefreshLayout.setRefreshing(false);
        String queryStr = mSearchView != null ? mSearchView.getQuery().toString() : "";
        mAppAdapter.filter(queryStr);
    }

    @Override
    public void onItemClick(View v, ApplicationInfo info) {
        getSupportFragmentManager();
        AppHelper.showMenu(this, getSupportFragmentManager(), v, info);
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public void onBackPressed() {
        if (mSearchView.isIconified()) {
            super.onBackPressed();
        } else {
            mSearchView.setIconified(true);
        }
    }
}
