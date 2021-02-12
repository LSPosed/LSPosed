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

import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import io.github.lsposed.manager.R;
import io.github.lsposed.manager.databinding.ActivityModuleDetailBinding;
import io.github.lsposed.manager.databinding.ItemRepoReadmeBinding;
import io.github.lsposed.manager.databinding.ItemRepoRecyclerviewBinding;
import io.github.lsposed.manager.databinding.ItemRepoReleaseBinding;
import io.github.lsposed.manager.databinding.ItemRepoTitleDescriptionBinding;
import io.github.lsposed.manager.repo.RepoLoader;
import io.github.lsposed.manager.repo.model.Collaborator;
import io.github.lsposed.manager.repo.model.OnlineModule;
import io.github.lsposed.manager.repo.model.Release;
import io.github.lsposed.manager.repo.model.ReleaseAsset;
import io.github.lsposed.manager.ui.widget.LinkifyTextView;
import io.github.lsposed.manager.util.GlideApp;
import io.github.lsposed.manager.util.LinearLayoutManagerFix;
import io.github.lsposed.manager.util.NavUtil;
import io.github.lsposed.manager.util.chrome.CustomTabsURLSpan;
import io.github.lsposed.manager.util.chrome.LinkTransformationMethod;
import io.noties.markwon.Markwon;
import io.noties.markwon.SoftBreakAddsNewLinePlugin;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.ext.tasklist.TaskListPlugin;
import io.noties.markwon.html.HtmlPlugin;
import io.noties.markwon.image.glide.GlideImagesPlugin;
import io.noties.markwon.linkify.LinkifyPlugin;
import io.noties.markwon.utils.NoCopySpannableFactory;
import rikka.recyclerview.RecyclerViewKt;
import rikka.widget.borderview.BorderNestedScrollView;
import rikka.widget.borderview.BorderRecyclerView;
import rikka.widget.borderview.BorderViewDelegate;

public class RepoItemActivity extends BaseActivity {
    ActivityModuleDetailBinding binding;
    private Markwon markwon;
    private OnlineModule module;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityModuleDetailBinding.inflate(getLayoutInflater());
        String modulePackageName = getIntent().getStringExtra("modulePackageName");
        String moduleName = getIntent().getStringExtra("moduleName");
        setContentView(binding.getRoot());
        setAppBar(binding.appBar, binding.toolbar);
        binding.getRoot().bringChildToFront(binding.appBar);
        binding.toolbar.setNavigationOnClickListener(view -> onBackPressed());
        ActionBar bar = getSupportActionBar();
        assert bar != null;
        bar.setTitle(moduleName);
        bar.setSubtitle(modulePackageName);
        bar.setDisplayHomeAsUpEnabled(true);
        markwon = Markwon.builder(this)
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(TablePlugin.create(this))
                .usePlugin(TaskListPlugin.create(this))
                .usePlugin(HtmlPlugin.create())
                .usePlugin(GlideImagesPlugin.create(GlideApp.with(this)))
                .usePlugin(LinkifyPlugin.create(Linkify.WEB_URLS, true))
                .usePlugin(SoftBreakAddsNewLinePlugin.create())
                .build();
        module = RepoLoader.getInstance().getOnlineModule(modulePackageName);
        binding.viewPager.setAdapter(new PagerAdapter());
        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                BorderViewDelegate delegate;
                if (position == 0) {
                    BorderNestedScrollView borderNestedScrollView = findViewById(R.id.scrollView);
                    delegate = borderNestedScrollView.getBorderViewDelegate();
                } else {
                    BorderRecyclerView borderRecyclerView = findViewById(R.id.recyclerView);
                    delegate = borderRecyclerView.getBorderViewDelegate();
                }
                binding.appBar.setRaised(!delegate.isShowingTopBorder());
            }
        });
        int[] titles = new int[]{R.string.module_readme, R.string.module_releases, R.string.module_information};
        new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> tab.setText(titles[position])).attach();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_repo_item, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_open_in_browser) {
            NavUtil.startURL(this, module.getUrl());
            // TODO: replace with web version
        }
        return super.onOptionsItemSelected(item);
    }

    private class InformationAdapter extends RecyclerView.Adapter<InformationAdapter.ViewHolder> {
        private final OnlineModule module;

        private int rowCount = 0;
        private int homepageRow = -1;
        private int collaboratorsRow = -1;
        private int sourceUrlRow = -1;

        public InformationAdapter(OnlineModule module) {
            this.module = module;
            if (!TextUtils.isEmpty(module.getHomepageUrl())) {
                homepageRow = rowCount++;
            }
            if (module.getCollaborators() != null && !module.getCollaborators().isEmpty()) {
                collaboratorsRow = rowCount++;
            }
            if (!TextUtils.isEmpty(module.getSourceUrl())) {
                sourceUrlRow = rowCount++;
            }
        }

        @NonNull
        @Override
        public InformationAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new InformationAdapter.ViewHolder(ItemRepoTitleDescriptionBinding.inflate(getLayoutInflater(), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull InformationAdapter.ViewHolder holder, int position) {
            if (position == homepageRow) {
                holder.title.setText(R.string.module_information_homepage);
                holder.description.setText(module.getHomepageUrl());
            } else if (position == collaboratorsRow) {
                holder.title.setText(R.string.module_information_collaborators);
                List<Collaborator> collaborators = module.getCollaborators();
                SpannableStringBuilder sb = new SpannableStringBuilder();
                ListIterator<Collaborator> iterator = collaborators.listIterator();
                while (iterator.hasNext()) {
                    Collaborator collaborator = iterator.next();
                    String name = collaborator.getName() == null ? collaborator.getLogin() : collaborator.getName();
                    sb.append(name);
                    CustomTabsURLSpan span = new CustomTabsURLSpan(RepoItemActivity.this, String.format("https://github.com/%s", collaborator.getLogin()));
                    sb.setSpan(span, sb.length() - name.length(), sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    if (iterator.hasNext()) {
                        sb.append(", ");
                    }
                }
                holder.description.setText(sb);
            } else if (position == sourceUrlRow) {
                holder.title.setText(R.string.module_information_source_url);
                holder.description.setText(module.getSourceUrl());
            }
            holder.itemView.setOnClickListener(v -> {
                if (position == homepageRow) {
                    NavUtil.startURL(RepoItemActivity.this, module.getHomepageUrl());
                } else if (position == collaboratorsRow) {
                    ClickableSpan span = holder.description.getCurrentSpan();
                    holder.description.clearCurrentSpan();

                    if (span instanceof CustomTabsURLSpan) {
                        span.onClick(v);
                    }
                } else if (position == sourceUrlRow) {
                    NavUtil.startURL(RepoItemActivity.this, module.getSourceUrl());
                }
            });

        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView title;
            LinkifyTextView description;

            public ViewHolder(ItemRepoTitleDescriptionBinding binding) {
                super(binding.getRoot());
                title = binding.title;
                description = binding.description;
            }
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
            holder.description.setTransformationMethod(new LinkTransformationMethod(RepoItemActivity.this));
            holder.description.setSpannableFactory(NoCopySpannableFactory.getInstance());
            markwon.setMarkdown(holder.description, release.getDescription());
            holder.description.setMovementMethod(null);
            holder.openInBrowser.setOnClickListener(v -> NavUtil.startURL(RepoItemActivity.this, release.getUrl()));
            List<ReleaseAsset> assets = release.getReleaseAssets();
            if (assets != null && !assets.isEmpty()) {
                holder.viewAssets.setOnClickListener(v -> {
                    ArrayList<String> names = new ArrayList<>();
                    assets.forEach(releaseAsset -> names.add(releaseAsset.getName()));
                    new MaterialAlertDialogBuilder(RepoItemActivity.this)
                            .setItems(names.toArray(new String[0]), (dialog, which) -> NavUtil.startURL(RepoItemActivity.this, assets.get(which).getDownloadUrl()))
                            .show();
                });
            } else {
                holder.viewAssets.setVisibility(View.GONE);
            }
            holder.itemView.setOnClickListener(v -> {
                ClickableSpan span = holder.description.getCurrentSpan();
                holder.description.clearCurrentSpan();

                if (span instanceof CustomTabsURLSpan) {
                    span.onClick(v);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView title;
            LinkifyTextView description;
            View openInBrowser;
            View viewAssets;

            public ViewHolder(ItemRepoReleaseBinding binding) {
                super(binding.getRoot());
                title = binding.title;
                description = binding.description;
                openInBrowser = binding.openInBrowser;
                viewAssets = binding.viewAssets;
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
                return new ViewHolder(ItemRepoRecyclerviewBinding.inflate(getLayoutInflater(), parent, false).getRoot(), viewType);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            switch (position) {
                case 0:
                    holder.textView.setTransformationMethod(new LinkTransformationMethod(RepoItemActivity.this));
                    holder.scrollView.getBorderViewDelegate().setBorderVisibilityChangedListener((top, oldTop, bottom, oldBottom) -> binding.appBar.setRaised(!top));
                    markwon.setMarkdown(holder.textView, module.getReadme());
                    break;
                case 1:
                case 2:
                    holder.recyclerView.setAdapter(position == 1 ? new ReleaseAdapter(module.getReleases()) : new InformationAdapter(module));
                    holder.recyclerView.setLayoutManager(new LinearLayoutManagerFix(RepoItemActivity.this));
                    if (!preferences.getBoolean("md2", true)) {
                        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(RepoItemActivity.this, DividerItemDecoration.VERTICAL);
                        holder.recyclerView.addItemDecoration(dividerItemDecoration);
                    }
                    holder.recyclerView.getBorderViewDelegate().setBorderVisibilityChangedListener((top, oldTop, bottom, oldBottom) -> binding.appBar.setRaised(!top));
                    RecyclerViewKt.fixEdgeEffect(holder.recyclerView, false, true);
                    RecyclerViewKt.addFastScroller(holder.recyclerView, holder.itemView);
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? 0 : 1;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;
            BorderNestedScrollView scrollView;
            BorderRecyclerView recyclerView;

            public ViewHolder(@NonNull View itemView, int viewType) {
                super(itemView);
                if (viewType == 0) {
                    textView = itemView.findViewById(R.id.readme);
                    scrollView = itemView.findViewById(R.id.scrollView);
                } else {
                    recyclerView = itemView.findViewById(R.id.recyclerView);
                }
            }
        }
    }
}
