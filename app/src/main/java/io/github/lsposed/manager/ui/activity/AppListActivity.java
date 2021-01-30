package io.github.lsposed.manager.ui.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.DividerItemDecoration;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import io.github.lsposed.manager.R;
import io.github.lsposed.manager.adapters.AppHelper;
import io.github.lsposed.manager.adapters.ScopeAdapter;
import io.github.lsposed.manager.databinding.ActivityAppListBinding;
import io.github.lsposed.manager.util.LinearLayoutManagerFix;
import io.github.lsposed.manager.util.ModuleUtil;
import me.zhanghai.android.fastscroll.FastScrollerBuilder;

public class AppListActivity extends BaseActivity {
    private SearchView searchView;
    private ScopeAdapter scopeAdapter;

    private SearchView.OnQueryTextListener searchListener;
    private ActivityAppListBinding binding;
    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            binding.swipeRefreshLayout.setRefreshing(true);
        }
    };
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String modulePackageName;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        modulePackageName = getIntent().getStringExtra("modulePackageName");
        String moduleName = getIntent().getStringExtra("moduleName");
        binding = ActivityAppListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(view -> onBackPressed());
        ActionBar bar = getSupportActionBar();
        assert bar != null;
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setTitle(moduleName);
        bar.setSubtitle(modulePackageName);
        scopeAdapter = new ScopeAdapter(this, modulePackageName, binding.masterSwitch);
        scopeAdapter.setHasStableIds(true);
        binding.recyclerView.setAdapter(scopeAdapter);
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
        handler.postDelayed(runnable, 300);
        binding.swipeRefreshLayout.setOnRefreshListener(scopeAdapter::refresh);

        searchListener = new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                scopeAdapter.filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                scopeAdapter.filter(newText);
                return false;
            }
        };
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (scopeAdapter.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        scopeAdapter.onCreateOptionsMenu(menu, getMenuInflater());
        searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        searchView.setOnQueryTextListener(searchListener);
        return super.onCreateOptionsMenu(menu);
    }

    public void onDataReady() {
        handler.removeCallbacks(runnable);
        binding.swipeRefreshLayout.setRefreshing(false);
        String queryStr = searchView != null ? searchView.getQuery().toString() : "";
        runOnUiThread(() -> scopeAdapter.getFilter().filter(queryStr));
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        if (scopeAdapter.onContextItemSelected(item)) {
            return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (searchView.isIconified()) {
            if (binding.masterSwitch.isChecked() && AppHelper.getScopeList(modulePackageName).isEmpty()) {
                new MaterialAlertDialogBuilder(this)
                        .setMessage(R.string.no_scope_selected)
                        .setPositiveButton(android.R.string.cancel, null)
                        .setNegativeButton(android.R.string.ok, (dialog, which) -> {
                            ModuleUtil.getInstance().setModuleEnabled(modulePackageName, false);
                            super.onBackPressed();
                        })
                        .show();
            } else {
                super.onBackPressed();
            }
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
