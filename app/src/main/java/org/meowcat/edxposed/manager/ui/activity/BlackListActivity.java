package org.meowcat.edxposed.manager.ui.activity;

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

import org.meowcat.edxposed.manager.App;
import org.meowcat.edxposed.manager.R;
import org.meowcat.edxposed.manager.adapters.AppAdapter;
import org.meowcat.edxposed.manager.adapters.AppHelper;
import org.meowcat.edxposed.manager.adapters.BlackListAdapter;
import org.meowcat.edxposed.manager.databinding.ActivityBlackListBinding;
import org.meowcat.edxposed.manager.util.LinearLayoutManagerFix;

import me.zhanghai.android.fastscroll.FastScrollerBuilder;

public class BlackListActivity extends BaseActivity implements AppAdapter.Callback {
    private SearchView searchView;
    private AppAdapter appAdapter;

    private SearchView.OnQueryTextListener searchListener;
    private ActivityBlackListBinding binding;
    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            binding.swipeRefreshLayout.setRefreshing(true);
        }
    };
    private final Handler handler = new Handler();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        appAdapter = new BlackListAdapter(this, isWhiteListMode);
        appAdapter.setHasStableIds(true);
        binding.recyclerView.setAdapter(appAdapter);
        binding.recyclerView.setLayoutManager(new LinearLayoutManagerFix(this));
        FastScrollerBuilder fastScrollerBuilder = new FastScrollerBuilder(binding.recyclerView);
        if (!App.getPreferences().getBoolean("md2", false)) {
            DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this,
                    DividerItemDecoration.VERTICAL);
            binding.recyclerView.addItemDecoration(dividerItemDecoration);
        } else {
            fastScrollerBuilder.useMd2Style();
        }
        fastScrollerBuilder.build();
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
        changeTitle(isWhiteListMode());
    }


    private void changeTitle(boolean isWhiteListMode) {
        setTitle(isWhiteListMode ? R.string.title_white_list : R.string.title_black_list);

    }

    private boolean isWhiteListMode() {
        return AppHelper.isWhiteListMode();
    }

    @Override
    public void onDataReady() {
        handler.removeCallbacks(runnable);
        binding.swipeRefreshLayout.setRefreshing(false);
        String queryStr = searchView != null ? searchView.getQuery().toString() : "";
        runOnUiThread(() -> appAdapter.getFilter().filter(queryStr));
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
