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

package io.github.lsposed.manager.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import io.github.lsposed.manager.R;
import io.github.lsposed.manager.databinding.ActivityListBinding;
import io.github.lsposed.manager.repo.RepoLoader;
import io.github.lsposed.manager.repo.model.OnlineModule;
import io.github.lsposed.manager.util.LinearLayoutManagerFix;
import rikka.recyclerview.RecyclerViewKt;

public class RepoActivity extends BaseActivity implements RepoLoader.Listener {
    private final RepoLoader repoLoader = RepoLoader.getInstance();
    private SearchView searchView;
    private SearchView.OnQueryTextListener searchListener;
    private ActivityListBinding binding;
    private RepoAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setAppBar(binding.appBar, binding.toolbar);
        binding.getRoot().bringChildToFront(binding.appBar);
        binding.toolbar.setNavigationOnClickListener(view -> onBackPressed());
        binding.recyclerView.getBorderViewDelegate().setBorderVisibilityChangedListener((top, oldTop, bottom, oldBottom) -> binding.appBar.setRaised(!top));
        ActionBar bar = getSupportActionBar();
        assert bar != null;
        bar.setDisplayHomeAsUpEnabled(true);
        adapter = new RepoAdapter();
        adapter.setHasStableIds(true);
        binding.recyclerView.setAdapter(adapter);
        binding.recyclerView.setHasFixedSize(true);
        binding.recyclerView.setLayoutManager(new LinearLayoutManagerFix(this));
        binding.progress.setVisibilityAfterHide(View.GONE);
        RecyclerViewKt.addFastScroller(binding.recyclerView, binding.recyclerView);
        RecyclerViewKt.fixEdgeEffect(binding.recyclerView, false, true);
        repoLoader.addListener(this);
        searchListener = new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return false;
            }
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        repoLoader.removeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.initData();
    }

    @Override
    public void repoLoaded() {
        runOnUiThread(() -> {
            binding.progress.hide();
            adapter.setData(repoLoader.getOnlineModules());
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_refresh) {
            binding.progress.show();
            repoLoader.loadRemoteData();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_repo, menu);
        searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        searchView.setOnQueryTextListener(searchListener);
        return super.onCreateOptionsMenu(menu);
    }

    private class RepoAdapter extends RecyclerView.Adapter<RepoAdapter.ViewHolder> implements Filterable {
        private List<OnlineModule> fullList, showList;

        RepoAdapter() {
            fullList = showList = Collections.emptyList();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_onlinemodule, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            OnlineModule module = showList.get(position);
            holder.appName.setText(module.getDescription());
            SpannableStringBuilder sb = new SpannableStringBuilder(module.getName());
            String summary = module.getSummary();
            if (summary != null) {
                sb.append("\n");
                sb.append(summary);
            }
            holder.appDescription.setText(sb);
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent();
                intent.setClass(RepoActivity.this, RepoItemActivity.class);
                intent.putExtra("modulePackageName", module.getName());
                intent.putExtra("moduleName", module.getDescription());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return showList.size();
        }

        public void setData(Collection<OnlineModule> modules) {
            fullList = new ArrayList<>(modules);
            fullList = fullList.stream().filter((onlineModule -> !onlineModule.isHide())).collect(Collectors.toList());
            fullList.sort((o1, o2) -> o1.getDescription().compareToIgnoreCase(o2.getDescription()));
            String queryStr = searchView != null ? searchView.getQuery().toString() : "";
            runOnUiThread(() -> getFilter().filter(queryStr));
        }

        public void initData() {
            Collection<OnlineModule> modules = repoLoader.getOnlineModules();
            if (!repoLoader.isRepoLoaded()) {
                binding.progress.show();
                repoLoader.loadRemoteData();
            } else {
                adapter.setData(modules);
            }
        }

        @Override
        public long getItemId(int position) {
            return showList.get(position).getName().hashCode();
        }

        @Override
        public Filter getFilter() {
            return new ModuleFilter();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            View root;
            TextView appName;
            TextView appDescription;

            ViewHolder(View itemView) {
                super(itemView);
                root = itemView.findViewById(R.id.item_root);
                appName = itemView.findViewById(R.id.app_name);
                appDescription = itemView.findViewById(R.id.description);
            }
        }

        class ModuleFilter extends Filter {

            private boolean lowercaseContains(String s, String filter) {
                return !TextUtils.isEmpty(s) && s.toLowerCase().contains(filter);
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                if (constraint.toString().isEmpty()) {
                    showList = fullList;
                } else {
                    ArrayList<OnlineModule> filtered = new ArrayList<>();
                    String filter = constraint.toString().toLowerCase();
                    for (OnlineModule info : fullList) {
                        if (lowercaseContains(info.getDescription(), filter) ||
                                lowercaseContains(info.getName(), filter) ||
                                lowercaseContains(info.getSummary(), filter)) {
                            filtered.add(info);
                        }
                    }
                    showList = filtered;
                }
                return null;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                notifyDataSetChanged();
            }
        }
    }
}
