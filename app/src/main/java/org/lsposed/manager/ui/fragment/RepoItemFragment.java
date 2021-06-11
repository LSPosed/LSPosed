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
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayoutMediator;

import org.lsposed.manager.R;
import org.lsposed.manager.databinding.FragmentPagerBinding;
import org.lsposed.manager.databinding.ItemRepoLoadmoreBinding;
import org.lsposed.manager.databinding.ItemRepoReadmeBinding;
import org.lsposed.manager.databinding.ItemRepoRecyclerviewBinding;
import org.lsposed.manager.databinding.ItemRepoReleaseBinding;
import org.lsposed.manager.databinding.ItemRepoTitleDescriptionBinding;
import org.lsposed.manager.repo.RepoLoader;
import org.lsposed.manager.repo.model.Collaborator;
import org.lsposed.manager.repo.model.OnlineModule;
import org.lsposed.manager.repo.model.Release;
import org.lsposed.manager.repo.model.ReleaseAsset;
import org.lsposed.manager.ui.widget.LinkifyTextView;
import org.lsposed.manager.util.GlideApp;
import org.lsposed.manager.util.LinearLayoutManagerFix;
import org.lsposed.manager.util.NavUtil;
import org.lsposed.manager.util.chrome.CustomTabsURLSpan;
import org.lsposed.manager.util.chrome.LinkTransformationMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

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
import rikka.widget.borderview.BorderView;

public class RepoItemFragment extends BaseFragment implements RepoLoader.Listener {
    FragmentPagerBinding binding;
    private Markwon markwon;
    private OnlineModule module;
    private ReleaseAdapter releaseAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPagerBinding.inflate(getLayoutInflater(), container, false);
        String modulePackageName = getArguments().getString("modulePackageName");
        String moduleName = getArguments().getString("moduleName");
        binding.getRoot().bringChildToFront(binding.appBar);
        setupToolbar(binding.toolbar, moduleName, R.menu.menu_repo_item);
        binding.toolbar.setSubtitle(modulePackageName);
        binding.viewPager.setAdapter(new PagerAdapter());
        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                BorderView borderView = binding.viewPager.findViewWithTag(position);

                if (borderView != null) {
                    binding.appBar.setRaised(!borderView.getBorderViewDelegate().isShowingTopBorder());
                }
            }
        });
        int[] titles = new int[]{R.string.module_readme, R.string.module_releases, R.string.module_information};
        new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> tab.setText(titles[position])).attach();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        RepoLoader.getInstance().addListener(this);
        super.onCreate(savedInstanceState);

        markwon = Markwon.builder(requireActivity())
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(TablePlugin.create(requireActivity()))
                .usePlugin(TaskListPlugin.create(requireActivity()))
                .usePlugin(HtmlPlugin.create())
                .usePlugin(GlideImagesPlugin.create(GlideApp.with(this)))
                .usePlugin(LinkifyPlugin.create(Linkify.WEB_URLS, true))
                .usePlugin(SoftBreakAddsNewLinePlugin.create())
                .build();
        String modulePackageName = getArguments().getString("modulePackageName");
        module = RepoLoader.getInstance().getOnlineModule(modulePackageName);

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_open_in_browser) {
            NavUtil.startURL(requireActivity(), "https://modules.lsposed.org/module/" + module.getName());
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void repoLoaded() {

    }

    @Override
    public void moduleReleasesLoaded(OnlineModule module) {
        this.module = module;
        if (releaseAdapter != null) {
            requireActivity().runOnUiThread(() -> releaseAdapter.loadItems());
            if (module.getReleases().size() == 1) {
                Snackbar.make(binding.snackbar, R.string.module_release_no_more, Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onThrowable(Throwable t) {
        if (releaseAdapter != null) {
            requireActivity().runOnUiThread(() -> releaseAdapter.loadItems());
        }
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
            Snackbar.make(binding.snackbar, getString(R.string.repo_load_failed, t.getLocalizedMessage()), Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RepoLoader.getInstance().removeListener(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        binding = null;
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
                    CustomTabsURLSpan span = new CustomTabsURLSpan(requireActivity(), String.format("https://github.com/%s", collaborator.getLogin()));
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
                    NavUtil.startURL(requireActivity(), module.getHomepageUrl());
                } else if (position == collaboratorsRow) {
                    ClickableSpan span = holder.description.getCurrentSpan();
                    holder.description.clearCurrentSpan();

                    if (span instanceof CustomTabsURLSpan) {
                        span.onClick(v);
                    }
                } else if (position == sourceUrlRow) {
                    NavUtil.startURL(requireActivity(), module.getSourceUrl());
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
        private List<Release> items;

        public ReleaseAdapter() {
            loadItems();
        }

        public void loadItems() {
            this.items = module.getReleases();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ReleaseAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == 0) {
                return new ReleaseAdapter.ReleaseViewHolder(ItemRepoReleaseBinding.inflate(getLayoutInflater(), parent, false));
            } else {
                return new ReleaseAdapter.LoadmoreViewHolder(ItemRepoLoadmoreBinding.inflate(getLayoutInflater(), parent, false));
            }
        }

        @Override
        public void onBindViewHolder(@NonNull ReleaseAdapter.ViewHolder holder, int position) {
            if (position == items.size()) {
                holder.progress.setVisibility(View.GONE);
                holder.title.setVisibility(View.VISIBLE);
                holder.itemView.setOnClickListener(v -> {
                    if (holder.progress.getVisibility() == View.GONE) {
                        holder.title.setVisibility(View.GONE);
                        holder.progress.show();
                        RepoLoader.getInstance().loadRemoteReleases(module.getName());
                    }
                });
            } else {
                Release release = items.get(position);
                holder.title.setText(release.getName());
                holder.description.setTransformationMethod(new LinkTransformationMethod(requireActivity()));
                holder.description.setSpannableFactory(NoCopySpannableFactory.getInstance());
                markwon.setMarkdown(holder.description, release.getDescription());
                holder.description.setMovementMethod(null);
                holder.openInBrowser.setOnClickListener(v -> NavUtil.startURL(requireActivity(), release.getUrl()));
                List<ReleaseAsset> assets = release.getReleaseAssets();
                if (assets != null && !assets.isEmpty()) {
                    holder.viewAssets.setOnClickListener(v -> {
                        ArrayList<String> names = new ArrayList<>();
                        assets.forEach(releaseAsset -> names.add(releaseAsset.getName()));
                        new AlertDialog.Builder(requireActivity())
                                .setItems(names.toArray(new String[0]), (dialog, which) -> NavUtil.startURL(requireActivity(), assets.get(which).getDownloadUrl()))
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
        }

        @Override
        public int getItemCount() {
            return items.size() + (module.releasesLoaded ? 0 : 1);
        }

        @Override
        public int getItemViewType(int position) {
            return !module.releasesLoaded && position == getItemCount() - 1 ? 1 : 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView title;
            LinkifyTextView description;
            MaterialButton openInBrowser;
            MaterialButton viewAssets;
            CircularProgressIndicator progress;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }

        class ReleaseViewHolder extends ReleaseAdapter.ViewHolder {
            public ReleaseViewHolder(ItemRepoReleaseBinding binding) {
                super(binding.getRoot());
                title = binding.title;
                description = binding.description;
                openInBrowser = binding.openInBrowser;
                viewAssets = binding.viewAssets;
            }
        }

        class LoadmoreViewHolder extends ReleaseAdapter.ViewHolder {
            public LoadmoreViewHolder(ItemRepoLoadmoreBinding binding) {
                super(binding.getRoot());
                title = binding.title;
                progress = binding.progress;
            }
        }
    }

    private class PagerAdapter extends RecyclerView.Adapter<PagerAdapter.ViewHolder> {

        @NonNull
        @Override
        public PagerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == 0) {
                return new PagerAdapter.ReadmeViewHolder(ItemRepoReadmeBinding.inflate(getLayoutInflater(), parent, false));
            } else {
                return new PagerAdapter.RecyclerviewBinding(ItemRepoRecyclerviewBinding.inflate(getLayoutInflater(), parent, false));
            }
        }

        @Override
        public void onBindViewHolder(@NonNull PagerAdapter.ViewHolder holder, int position) {
            switch (position) {
                case 0:
                    holder.textView.setTransformationMethod(new LinkTransformationMethod(requireActivity()));
                    holder.scrollView.getBorderViewDelegate().setBorderVisibilityChangedListener((top, oldTop, bottom, oldBottom) -> binding.appBar.setRaised(!top));
                    holder.scrollView.setTag(position);
                    markwon.setMarkdown(holder.textView, module.getReadme());
                    break;
                case 1:
                case 2:
                    if (position == 1) {
                        holder.recyclerView.setAdapter(releaseAdapter = new ReleaseAdapter());
                    } else {
                        holder.recyclerView.setAdapter(new InformationAdapter(module));
                    }
                    holder.recyclerView.setTag(position);
                    holder.recyclerView.setLayoutManager(new LinearLayoutManagerFix(requireActivity()));
                    holder.recyclerView.getBorderViewDelegate().setBorderVisibilityChangedListener((top, oldTop, bottom, oldBottom) -> binding.appBar.setRaised(!top));
                    var insets = requireActivity().getWindow().getDecorView().getRootWindowInsets();
                    if (insets != null)
                        holder.recyclerView.onApplyWindowInsets(insets);
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

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }

        class ReadmeViewHolder extends PagerAdapter.ViewHolder {
            public ReadmeViewHolder(ItemRepoReadmeBinding binding) {
                super(binding.getRoot());
                textView = binding.readme;
                scrollView = binding.scrollView;
            }
        }

        class RecyclerviewBinding extends PagerAdapter.ViewHolder {
            public RecyclerviewBinding(ItemRepoRecyclerviewBinding binding) {
                super(binding.getRoot());
                recyclerView = binding.recyclerView;
            }
        }
    }
}
