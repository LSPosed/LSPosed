package io.github.lsposed.manager.ui.activity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.DividerItemDecoration;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import io.github.lsposed.manager.BuildConfig;
import io.github.lsposed.manager.R;
import io.github.lsposed.manager.adapters.ScopeAdapter;
import io.github.lsposed.manager.databinding.ActivityAppListBinding;
import io.github.lsposed.manager.util.BackupUtils;
import io.github.lsposed.manager.util.LinearLayoutManagerFix;
import rikka.recyclerview.RecyclerViewKt;

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
    public ActivityResultLauncher<String> backupLauncher;
    public ActivityResultLauncher<String[]> restoreLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String modulePackageName = getIntent().getStringExtra("modulePackageName");
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
        scopeAdapter = new ScopeAdapter(this, moduleName, modulePackageName, binding.masterSwitch);
        scopeAdapter.setHasStableIds(true);
        binding.recyclerView.setAdapter(scopeAdapter);
        binding.recyclerView.setLayoutManager(new LinearLayoutManagerFix(this));
        RecyclerViewKt.addFastScroller(binding.recyclerView, binding.swipeRefreshLayout);
        RecyclerViewKt.fixEdgeEffect(binding.recyclerView, false, true);
        if (!preferences.getBoolean("md2", true)) {
            DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this,
                    DividerItemDecoration.VERTICAL);
            binding.recyclerView.addItemDecoration(dividerItemDecoration);
        }
        handler.postDelayed(runnable, 300);
        binding.swipeRefreshLayout.setOnRefreshListener(scopeAdapter::refresh);

        searchListener = new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                scopeAdapter.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                scopeAdapter.getFilter().filter(newText);
                return false;
            }
        };

        backupLauncher = registerForActivityResult(new ActivityResultContracts.CreateDocument(),
                uri -> {
                    if (uri != null) {
                        try {
                            // grantUriPermission might throw RemoteException on MIUI
                            grantUriPermission(BuildConfig.APPLICATION_ID, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        AlertDialog alertDialog = new MaterialAlertDialogBuilder(this)
                                .setCancelable(false)
                                .setMessage(R.string.settings_backuping)
                                .show();
                        AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
                            boolean success = BackupUtils.backup(this, uri, modulePackageName);
                            try {
                                runOnUiThread(() -> {
                                    alertDialog.dismiss();
                                    makeSnackBar(success ? R.string.settings_backup_success : R.string.settings_backup_failed, Snackbar.LENGTH_SHORT);
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    }
                });
        restoreLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        try {
                            // grantUriPermission might throw RemoteException on MIUI
                            grantUriPermission(BuildConfig.APPLICATION_ID, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        AlertDialog alertDialog = new MaterialAlertDialogBuilder(this)
                                .setCancelable(false)
                                .setMessage(R.string.settings_restoring)
                                .show();
                        AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
                            boolean success = BackupUtils.restore(this, uri, modulePackageName);
                            try {
                                runOnUiThread(() -> {
                                    alertDialog.dismiss();
                                    makeSnackBar(success ? R.string.settings_restore_success : R.string.settings_restore_failed, Snackbar.LENGTH_SHORT);
                                    scopeAdapter.refresh();
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    }
                });
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
            if (scopeAdapter.onBackPressed()) {
                super.onBackPressed();
            }
        } else {
            searchView.setIconified(true);
        }
    }

    public void makeSnackBar(String text, @Snackbar.Duration int duration) {
        Snackbar.make(binding.snackbar, text, duration).show();
    }

    public void makeSnackBar(@StringRes int text, @Snackbar.Duration int duration) {
        Snackbar.make(binding.snackbar, text, duration).show();
    }
}
