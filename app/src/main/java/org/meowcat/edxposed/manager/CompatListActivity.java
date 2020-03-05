package org.meowcat.edxposed.manager;

import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.meowcat.edxposed.manager.adapters.AppAdapter;
import org.meowcat.edxposed.manager.adapters.AppHelper;
import org.meowcat.edxposed.manager.adapters.CompatListAdapter;

public class CompatListActivity extends BaseActivity implements AppAdapter.Callback {

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private SearchView mSearchView;
    private CompatListAdapter mAppAdapter;

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
        mAppAdapter = new CompatListAdapter(this);
        mRecyclerView.setAdapter(mAppAdapter);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mRecyclerView.getContext(),
                DividerItemDecoration.VERTICAL);
        mRecyclerView.addItemDecoration(dividerItemDecoration);
        mAppAdapter.setCallback(this);

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
        mSearchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        mSearchView.setOnQueryTextListener(mSearchListener);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onDataReady() {
        mSwipeRefreshLayout.setRefreshing(false);
        String queryStr = mSearchView != null ? mSearchView.getQuery().toString() : "";
        mAppAdapter.filter(queryStr);
    }

    @Override
    public void onItemClick(View v, ApplicationInfo info) {
        AppHelper.showMenu(this, getSupportFragmentManager(), v, info);

    }

    @Override
    public void onBackPressed() {
        if (mSearchView.isIconified()) {
            super.onBackPressed();
        } else {
            mSearchView.setIconified(true);
        }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}
