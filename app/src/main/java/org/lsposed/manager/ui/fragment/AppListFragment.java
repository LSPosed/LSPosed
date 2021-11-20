/*
 * <!--This file is part of LSPosed.
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
 * Copyright (C) 2021 LSPosed Contributors-->
 */

package org.lsposed.manager.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import org.lsposed.manager.App;
import org.lsposed.manager.R;
import org.lsposed.manager.adapters.ScopeAdapter;
import org.lsposed.manager.databinding.FragmentAppListBinding;
import org.lsposed.manager.util.BackupUtils;
import org.lsposed.manager.util.ModuleUtil;

import java.util.Locale;

import rikka.recyclerview.RecyclerViewKt;

public class AppListFragment extends BaseFragment {

    public SearchView searchView;
    private ScopeAdapter scopeAdapter;
    private ModuleUtil.InstalledModule module;

    private SearchView.OnQueryTextListener searchListener;
    public FragmentAppListBinding binding;
    public ActivityResultLauncher<String> backupLauncher;
    public ActivityResultLauncher<String[]> restoreLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAppListBinding.inflate(getLayoutInflater(), container, false);
        if (module == null) {
            return binding.getRoot();
        }
        binding.appBar.setLiftable(true);
        binding.appBar.setLifted(true);
        String title;
        if (module.userId != 0) {
            title = String.format(Locale.ROOT, "%s (%d)", module.getAppName(), module.userId);
        } else {
            title = module.getAppName();
        }
        binding.toolbar.setSubtitle(module.packageName);

        scopeAdapter = new ScopeAdapter(this, module);
        scopeAdapter.setHasStableIds(true);
        scopeAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                if (binding != null && scopeAdapter != null) {
                    binding.progress.setVisibility(scopeAdapter.isLoaded() ? View.GONE : View.VISIBLE);
                    binding.swipeRefreshLayout.setRefreshing(!scopeAdapter.isLoaded());
                }
            }
        });
        binding.recyclerView.setAdapter(scopeAdapter);
        binding.recyclerView.setHasFixedSize(true);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
        RecyclerViewKt.fixEdgeEffect(binding.recyclerView, false, true);
        binding.swipeRefreshLayout.setOnRefreshListener(() -> scopeAdapter.refresh());

        searchListener = scopeAdapter.getSearchListener();

        setupToolbar(binding.toolbar, title, R.menu.menu_app_list, view -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (module == null) {
            getNavController().navigate(R.id.action_app_list_fragment_to_modules_fragment);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppListFragmentArgs args = AppListFragmentArgs.fromBundle(getArguments());
        String modulePackageName = args.getModulePackageName();
        int moduleUserId = args.getModuleUserId();

        module = ModuleUtil.getInstance().getModule(modulePackageName, moduleUserId);

        backupLauncher = registerForActivityResult(new ActivityResultContracts.CreateDocument(),
                uri -> {
                    if (uri == null) return;
                    runAsync(() -> {
                        try {
                            BackupUtils.backup(uri, modulePackageName);
                        } catch (Exception e) {
                            var text = App.getInstance().getString(R.string.settings_backup_failed2, e.getMessage());
                            if (binding != null && isResumed()) {
                                Snackbar.make(binding.snackbar, text, Snackbar.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(App.getInstance(), text, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                });
        restoreLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri == null) return;
                    runAsync(() -> {
                        try {
                            BackupUtils.restore(uri, modulePackageName);
                        } catch (Exception e) {
                            var text = App.getInstance().getString(R.string.settings_restore_failed2, e.getMessage());
                            if (binding != null && isResumed()) {
                                Snackbar.make(binding.snackbar, text, Snackbar.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(App.getInstance(), text, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                });

        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                scopeAdapter.onBackPressed();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (scopeAdapter != null) scopeAdapter.refresh();
    }

    @Override
    public void onDestroy() {
        if (scopeAdapter != null) scopeAdapter.onDestroy();

        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        binding = null;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (scopeAdapter.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        searchView.setOnQueryTextListener(searchListener);
        scopeAdapter.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        if (scopeAdapter.onContextItemSelected(item)) {
            return true;
        }
        return super.onContextItemSelected(item);
    }
}
