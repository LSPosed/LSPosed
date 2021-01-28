package org.meowcat.edxposed.manager.ui.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.DividerItemDecoration;

import com.google.android.material.snackbar.Snackbar;

import org.meowcat.edxposed.manager.R;
import org.meowcat.edxposed.manager.adapters.AppAdapter;
import org.meowcat.edxposed.manager.adapters.AppHelper;
import org.meowcat.edxposed.manager.adapters.BlackListAdapter;
import org.meowcat.edxposed.manager.adapters.ScopeAdapter;
import org.meowcat.edxposed.manager.databinding.ActivityScopeListBinding;
import org.meowcat.edxposed.manager.util.LinearLayoutManagerFix;

import me.zhanghai.android.fastscroll.FastScrollerBuilder;

public class AppListActivity extends BaseActivity {
    private SearchView searchView;
    private AppAdapter appAdapter;

    private SearchView.OnQueryTextListener searchListener;
    private ActivityScopeListBinding binding;
    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            binding.swipeRefreshLayout.setRefreshing(true);
        }
    };
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String modulePackageName = getIntent().getStringExtra("modulePackageName");
        String moduleName = getIntent().getStringExtra("moduleName");
        binding = ActivityScopeListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(view -> finish());
        ActionBar bar = getSupportActionBar();
        bar.setDisplayHomeAsUpEnabled(true);
        if (!TextUtils.isEmpty(modulePackageName)) {
            bar.setTitle(R.string.menu_scope);
            bar.setSubtitle(moduleName);
            appAdapter = new ScopeAdapter(this, modulePackageName, binding.masterSwitch);
        } else {
            bar.setTitle(AppHelper.isWhiteListMode() ? R.string.title_white_list : R.string.title_black_list);
            binding.masterSwitch.setVisibility(View.GONE);
            appAdapter = new BlackListAdapter(this);
        }
        appAdapter.setHasStableIds(true);
        binding.recyclerView.setAdapter(appAdapter);
        binding.recyclerView.setLayoutManager(new LinearLayoutManagerFix(this));
        FastScrollerBuilder fastScrollerBuilder = new FastScrollerBuilder(binding.recyclerView);
        if (!preferences.getBoolean("md2", false)) {
            DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this,
                    DividerItemDecoration.VERTICAL);
            binding.recyclerView.addItemDecoration(dividerItemDecoration);
        } else {
            fastScrollerBuilder.useMd2Style();
        }
        fastScrollerBuilder.build();
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
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (!appAdapter.onOptionsItemSelected(item)) {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        appAdapter.onCreateOptionsMenu(menu, getMenuInflater());
        searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        searchView.setOnQueryTextListener(searchListener);
        return super.onCreateOptionsMenu(menu);
    }

    public void onDataReady() {
        handler.removeCallbacks(runnable);
        binding.swipeRefreshLayout.setRefreshing(false);
        String queryStr = searchView != null ? searchView.getQuery().toString() : "";
        runOnUiThread(() -> appAdapter.getFilter().filter(queryStr));
    }

    @Override
    public void onBackPressed() {
        if (searchView.isIconified()) {
            super.onBackPressed();
        } else {
            searchView.setIconified(true);
        }
    }

    public void makeSnackBar(@StringRes int text, @Snackbar.Duration int duration) {
        if (binding != null) {
            Snackbar.make(binding.snackbar, text, duration).show();
        } else {
            Toast.makeText(this, text, duration == Snackbar.LENGTH_LONG ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
        }
    }
}
