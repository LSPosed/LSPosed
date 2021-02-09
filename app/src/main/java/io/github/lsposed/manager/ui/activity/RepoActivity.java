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
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import io.github.lsposed.manager.R;
import io.github.lsposed.manager.databinding.ActivityAppListBinding;
import io.github.lsposed.manager.repo.RepoLoader;
import io.github.lsposed.manager.repo.model.OnlineModule;
import io.github.lsposed.manager.util.LinearLayoutManagerFix;
import me.zhanghai.android.fastscroll.FastScrollerBuilder;

public class RepoActivity extends BaseActivity implements RepoLoader.Listener {
    private final RepoLoader repoLoader = RepoLoader.getInstance();
    private ActivityAppListBinding binding;
    private RepoAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAppListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(view -> onBackPressed());
        binding.masterSwitch.setVisibility(View.GONE);
        ActionBar bar = getSupportActionBar();
        assert bar != null;
        bar.setDisplayHomeAsUpEnabled(true);
        adapter = new RepoAdapter();
        binding.recyclerView.setAdapter(adapter);
        binding.recyclerView.setLayoutManager(new LinearLayoutManagerFix(this));
        setupRecyclerViewInsets(binding.recyclerView, binding.getRoot());
        FastScrollerBuilder fastScrollerBuilder = new FastScrollerBuilder(binding.recyclerView);
        if (!preferences.getBoolean("md2", true)) {
            DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this,
                    DividerItemDecoration.VERTICAL);
            binding.recyclerView.addItemDecoration(dividerItemDecoration);
        } else {
            fastScrollerBuilder.useMd2Style();
        }
        repoLoader.addListener(this);
        fastScrollerBuilder.build();
        binding.swipeRefreshLayout.setOnRefreshListener(repoLoader::loadRemoteData);
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.setData(repoLoader.getOnlineModules());
        binding.swipeRefreshLayout.setRefreshing(adapter.getItemCount() == 0);
    }

    @Override
    public void repoLoaded() {
        runOnUiThread(() -> {
            binding.swipeRefreshLayout.setRefreshing(false);
            adapter.setData(repoLoader.getOnlineModules());
        });
    }

    private class RepoAdapter extends RecyclerView.Adapter<RepoAdapter.ViewHolder> {
        private OnlineModule[] modules = new OnlineModule[0];

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_onlinemodule, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.appName.setText(modules[position].getDescription());
            String summary = modules[position].getSummary();
            if (summary != null) {
                holder.appDescription.setText(modules[position].getSummary());
            } else {
                holder.appDescription.setVisibility(View.GONE);
            }
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent();
                intent.setClass(RepoActivity.this, RepoItemActivity.class);
                intent.putExtra("module", (Parcelable) modules[position]);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return modules.length;
        }

        public void setData(OnlineModule[] modules) {
            this.modules = modules;
            notifyDataSetChanged();
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
    }
}
