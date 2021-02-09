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

import android.os.Build;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

import io.github.lsposed.manager.R;
import io.github.lsposed.manager.databinding.ActivityModuleDetailBinding;
import io.github.lsposed.manager.databinding.ItemRepoReadmeBinding;
import io.github.lsposed.manager.databinding.ItemRepoReleaseBinding;
import io.github.lsposed.manager.databinding.ItemRepoReleasesBinding;
import io.github.lsposed.manager.repo.model.OnlineModule;
import io.github.lsposed.manager.repo.model.Release;
import io.github.lsposed.manager.util.GlideApp;
import io.github.lsposed.manager.util.LinearLayoutManagerFix;
import io.github.lsposed.manager.util.NavUtil;
import io.github.lsposed.manager.util.chrome.LinkTransformationMethod;
import io.noties.markwon.Markwon;
import io.noties.markwon.image.glide.GlideImagesPlugin;
import io.noties.markwon.linkify.LinkifyPlugin;

public class RepoItemActivity extends BaseActivity {
    ActivityModuleDetailBinding binding;
    private Markwon markwon;
    private OnlineModule module;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityModuleDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        module = getIntent().getParcelableExtra("module");
        binding.toolbar.setNavigationOnClickListener(view -> onBackPressed());
        ActionBar bar = getSupportActionBar();
        assert bar != null;
        bar.setTitle(module.getDescription());
        bar.setSubtitle(module.getName());
        bar.setDisplayHomeAsUpEnabled(true);
        markwon = Markwon.builder(this)
                .usePlugin(GlideImagesPlugin.create(GlideApp.with(this)))
                .usePlugin(LinkifyPlugin.create(Linkify.WEB_URLS))
                .build();
        binding.viewPager.setAdapter(new PagerAdapter());
        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    binding.appBar.setLiftOnScrollTargetViewId(R.id.scrollView);
                } else {
                    binding.appBar.setLiftOnScrollTargetViewId(R.id.recyclerView);
                }
            }
        });
        int[] titles = new int[]{R.string.module_readme, R.string.module_releases};
        new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> tab.setText(titles[position])).attach();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
            ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
                Insets insets1 = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
                v.setPadding(insets1.left, insets1.top, insets1.right, 0);
                return insets;
            });
        }
    }

    private class ReleaseAdapter extends RecyclerView.Adapter<ReleaseAdapter.ViewHolder> {
        private final List<Release> items;

        public ReleaseAdapter(List<Release> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(ItemRepoReleaseBinding.inflate(getLayoutInflater(), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Release release = items.get(position);
            holder.title.setText(release.getName());
            holder.description.setText(release.getDescription());
            holder.itemView.setOnClickListener(v -> {
                ArrayList<String> names = new ArrayList<>();
                release.getReleaseAssets().forEach(releaseAsset -> names.add(releaseAsset.getName()));
                new MaterialAlertDialogBuilder(RepoItemActivity.this)
                        .setItems(names.toArray(new String[0]), (dialog, which) -> NavUtil.startURL(RepoItemActivity.this, release.getReleaseAssets().get(which).getDownloadUrl()))
                        .show();
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView title;
            TextView description;


            public ViewHolder(ItemRepoReleaseBinding binding) {
                super(binding.getRoot());
                title = binding.appName;
                description = binding.description;
            }
        }
    }

    private class PagerAdapter extends RecyclerView.Adapter<PagerAdapter.ViewHolder> {

        @NonNull
        @Override
        public PagerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == 0) {
                return new ViewHolder(ItemRepoReadmeBinding.inflate(getLayoutInflater(), parent, false).getRoot(), viewType);
            } else {
                return new ViewHolder(ItemRepoReleasesBinding.inflate(getLayoutInflater(), parent, false).getRoot(), viewType);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ViewCompat.setOnApplyWindowInsetsListener(holder.itemView, (v, insets) -> {
                    Insets insets1 = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
                    if (position == 0) {
                        v.setPadding(0, 0, 0, insets1.bottom);
                    } else {
                        holder.recyclerView.setPadding(0, 0, 0, insets1.bottom);
                    }
                    return WindowInsetsCompat.CONSUMED;
                });
                if (holder.itemView.isAttachedToWindow()) {
                    holder.itemView.requestApplyInsets();
                } else {
                    holder.itemView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                        @Override
                        public void onViewAttachedToWindow(View v) {
                            v.removeOnAttachStateChangeListener(this);
                            v.requestApplyInsets();
                        }

                        @Override
                        public void onViewDetachedFromWindow(View v) {

                        }
                    });
                }
            }
            if (position == 0) {
                holder.textView.setTransformationMethod(new LinkTransformationMethod(RepoItemActivity.this));
                markwon.setMarkdown(holder.textView, module.getReadme());
            } else {
                ReleaseAdapter adapter = new ReleaseAdapter(module.getReleases());
                holder.recyclerView.setAdapter(adapter);
                holder.recyclerView.setLayoutManager(new LinearLayoutManagerFix(RepoItemActivity.this));
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;
            RecyclerView recyclerView;

            public ViewHolder(@NonNull View itemView, int viewType) {
                super(itemView);
                if (viewType == 0) {
                    textView = itemView.findViewById(R.id.readme);
                } else {
                    recyclerView = itemView.findViewById(R.id.recyclerView);
                }
            }
        }
    }
}
