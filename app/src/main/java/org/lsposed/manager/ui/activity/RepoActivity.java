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
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import org.lsposed.manager.ConfigManager;
import org.lsposed.manager.R;
import org.lsposed.manager.repo.RepoLoader;
import org.lsposed.manager.repo.model.OnlineModule;
import org.lsposed.manager.ui.activity.base.ListActivity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import rikka.core.util.LabelComparator;

public class RepoActivity extends ListActivity implements RepoLoader.Listener {
    private final RepoLoader repoLoader = RepoLoader.getInstance();
    private RepoAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        repoLoader.addListener(this);
        super.onCreate(savedInstanceState);
        if (ConfigManager.getXposedVersionName() == null && !ConfigManager.isMagiskInstalled()) {
            Toast.makeText(this, R.string.lsposed_not_active, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected BaseAdapter<?> createAdapter() {
        return adapter = new RepoAdapter();
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
    public void moduleReleasesLoaded(OnlineModule module) {

    }

    @Override
    public void onThrowable(Throwable t) {
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
            Snackbar.make(binding.snackbar, getString(R.string.repo_load_failed, t.getLocalizedMessage()), Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_refresh) {
            binding.progress.show();
            repoLoader.loadRemoteData();
        } else if (itemId == R.id.item_sort_by_name) {
            item.setChecked(true);
            preferences.edit().putInt("repo_sort", 0).apply();
            adapter.setData(repoLoader.getOnlineModules());
        } else if (itemId == R.id.item_sort_by_update_time) {
            item.setChecked(true);
            preferences.edit().putInt("repo_sort", 1).apply();
            adapter.setData(repoLoader.getOnlineModules());
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_repo, menu);
        int sort = preferences.getInt("repo_sort", 0);
        if (sort == 0) {
            menu.findItem(R.id.item_sort_by_name).setChecked(true);
        } else if (sort == 1) {
            menu.findItem(R.id.item_sort_by_update_time).setChecked(true);
        }
        return super.onCreateOptionsMenu(menu);
    }

    private class RepoAdapter extends BaseAdapter<RepoAdapter.ViewHolder> {
        private List<OnlineModule> fullList, showList;
        private final LabelComparator labelComparator = new LabelComparator();

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
            fullList = fullList.stream().filter((onlineModule -> !onlineModule.isHide() && !onlineModule.getReleases().isEmpty())).collect(Collectors.toList());
            int sort = preferences.getInt("repo_sort", 0);
            if (sort == 0) {
                fullList.sort((o1, o2) -> labelComparator.compare(o1.getDescription(), o2.getDescription()));
            } else if (sort == 1) {
                fullList.sort(Collections.reverseOrder(Comparator.comparing(o -> Instant.parse(o.getReleases().get(0).getUpdatedAt()))));
            }
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
