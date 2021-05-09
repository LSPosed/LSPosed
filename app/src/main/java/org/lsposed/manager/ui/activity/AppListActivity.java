/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2020 EdXposed Contributors
 * Copyright (C) 2021 LSPosed Contributors
 */

package org.lsposed.manager.ui.activity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
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

import com.google.android.material.snackbar.Snackbar;

import org.lsposed.manager.BuildConfig;
import org.lsposed.manager.R;
import org.lsposed.manager.adapters.ScopeAdapter;
import org.lsposed.manager.databinding.ActivityAppListBinding;
import org.lsposed.manager.ui.activity.base.BaseActivity;
import org.lsposed.manager.util.BackupUtils;
import org.lsposed.manager.util.LinearLayoutManagerFix;
import org.lsposed.manager.util.ModuleUtil;

import rikka.recyclerview.RecyclerViewKt;

public class AppListActivity extends BaseActivity {
    public SearchView searchView;
    private ScopeAdapter scopeAdapter;

    private SearchView.OnQueryTextListener searchListener;
    public ActivityAppListBinding binding;
    public ActivityResultLauncher<String> backupLauncher;
    public ActivityResultLauncher<String[]> restoreLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String modulePackageName = getIntent().getStringExtra("modulePackageName");
        binding = ActivityAppListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setAppBar(binding.appBar, binding.toolbar);
        binding.appBar.setRaised(true);
        binding.toolbar.setNavigationOnClickListener(view -> onBackPressed());
        ModuleUtil.InstalledModule module = ModuleUtil.getInstance().getModule(modulePackageName);
        if (module == null) {
            finish();
            return;
        }
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setTitle(module.getAppName());
            bar.setSubtitle(module.packageName);
        }
        scopeAdapter = new ScopeAdapter(this, module);
        scopeAdapter.setHasStableIds(true);
        binding.recyclerView.setAdapter(scopeAdapter);
        binding.recyclerView.setHasFixedSize(true);
        binding.recyclerView.setLayoutManager(new LinearLayoutManagerFix(this));
        RecyclerViewKt.addFastScroller(binding.recyclerView, binding.recyclerView);
        RecyclerViewKt.fixEdgeEffect(binding.recyclerView, false, true);
        binding.swipeRefreshLayout.setOnRefreshListener(() -> scopeAdapter.refresh(true));

        searchListener = scopeAdapter.getSearchListener();

        backupLauncher = registerForActivityResult(new ActivityResultContracts.CreateDocument(),
                uri -> {
                    if (uri != null) {
                        try {
                            // grantUriPermission might throw RemoteException on MIUI
                            grantUriPermission(BuildConfig.APPLICATION_ID, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        AlertDialog alertDialog = new AlertDialog.Builder(this)
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
                        AlertDialog alertDialog = new AlertDialog.Builder(this)
                                .setCancelable(false)
                                .setMessage(R.string.settings_restoring)
                                .show();
                        AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
                            boolean success = BackupUtils.restore(this, uri, modulePackageName);
                            try {
                                runOnUiThread(() -> {
                                    alertDialog.dismiss();
                                    makeSnackBar(success ? R.string.settings_restore_success : R.string.settings_restore_failed, Snackbar.LENGTH_SHORT);
                                    scopeAdapter.refresh(false);
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        scopeAdapter.refresh(false);
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
