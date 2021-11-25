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

import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import org.lsposed.manager.App;
import org.lsposed.manager.R;
import org.lsposed.manager.databinding.FragmentRepoBinding;
import org.lsposed.manager.databinding.ItemOnlinemoduleBinding;
import org.lsposed.manager.repo.RepoLoader;
import org.lsposed.manager.repo.model.OnlineModule;
import org.lsposed.manager.ui.widget.EmptyStateRecyclerView;
import org.lsposed.manager.util.ModuleUtil;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import rikka.core.util.LabelComparator;
import rikka.core.util.ResourceUtils;
import rikka.recyclerview.RecyclerViewKt;

public class RepoFragment extends BaseFragment implements RepoLoader.RepoListener, ModuleUtil.ModuleListener {
    protected FragmentRepoBinding binding;
    protected SearchView searchView;
    private SearchView.OnQueryTextListener mSearchListener;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean preLoadWebview = true;

    private final RepoLoader repoLoader = RepoLoader.getInstance();
    private final ModuleUtil moduleUtil = ModuleUtil.getInstance();
    private RepoAdapter adapter;
    private final RecyclerView.AdapterDataObserver observer = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            binding.swipeRefreshLayout.setRefreshing(!adapter.isLoaded());
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        mSearchListener = new SearchView.OnQueryTextListener() {
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
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRepoBinding.inflate(getLayoutInflater(), container, false);
        binding.appBar.setLiftable(true);
        binding.recyclerView.getBorderViewDelegate().setBorderVisibilityChangedListener((top, oldTop, bottom, oldBottom) -> binding.appBar.setLifted(!top));
        setupToolbar(binding.toolbar, R.string.module_repo, R.menu.menu_repo);
        adapter = new RepoAdapter();
        adapter.setHasStableIds(true);
        adapter.registerAdapterDataObserver(observer);
        binding.recyclerView.setAdapter(adapter);
        binding.recyclerView.setHasFixedSize(true);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
        RecyclerViewKt.fixEdgeEffect(binding.recyclerView, false, true);
        binding.swipeRefreshLayout.setOnRefreshListener(adapter::fullRefresh);
        binding.swipeRefreshLayout.setProgressViewEndTarget(true, binding.swipeRefreshLayout.getProgressViewEndOffset());
        binding.toolbar.setOnClickListener(v -> {
            if (searchView.isIconified()) {
                binding.recyclerView.smoothScrollToPosition(0);
                binding.appBar.setExpanded(true, true);
            }
        });
        repoLoader.addListener(this);
        moduleUtil.addListener(this);
        updateRepoSummary();
        return binding.getRoot();
    }

    private void updateRepoSummary() {
        final int[] count = new int[]{0};
        HashSet<String> processedModules = new HashSet<>();
        var modules = moduleUtil.getModules();
        if (modules != null && repoLoader.isRepoLoaded()) {
            modules.forEach((k, v) -> {
                        if (!processedModules.contains(k.first)) {
                            var ver = repoLoader.getModuleLatestVersion(k.first);
                            if (ver != null && ver.upgradable(v.versionCode, v.versionName)) {
                                ++count[0];
                            }
                            processedModules.add(k.first);
                        }
                    }
            );
        } else {
            count[0] = -1;
        }
        runOnUiThread(() -> {
            if (count[0] > 0) {
                binding.toolbar.setSubtitle(getResources().getQuantityString(R.plurals.module_repo_upgradable, count[0], count[0]));
            } else if (count[0] == 0) {
                binding.toolbar.setSubtitle(getResources().getString(R.string.module_repo_up_to_date));
            } else {
                binding.toolbar.setSubtitle(getResources().getString(R.string.loading));
            }
            binding.toolbarLayout.setSubtitle(binding.toolbar.getSubtitle());
        });
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        searchView.setOnQueryTextListener(mSearchListener);
        searchView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View arg0) {
                binding.appBar.setExpanded(false, true);
                binding.recyclerView.setNestedScrollingEnabled(false);
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                binding.recyclerView.setNestedScrollingEnabled(true);
            }
        });
        int sort = App.getPreferences().getInt("repo_sort", 0);
        if (sort == 0) {
            menu.findItem(R.id.item_sort_by_name).setChecked(true);
        } else if (sort == 1) {
            menu.findItem(R.id.item_sort_by_update_time).setChecked(true);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mHandler.removeCallbacksAndMessages(null);
        repoLoader.removeListener(this);
        moduleUtil.removeListener(this);
        adapter.unregisterAdapterDataObserver(observer);
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter.refresh();
        if (preLoadWebview) {
            mHandler.postDelayed(() -> new WebView(requireContext()), 500);
            preLoadWebview = false;
        }
    }

    @Override
    public void onRepoLoaded() {
        adapter.refresh();
        updateRepoSummary();
    }

    @Override
    public void onThrowable(Throwable t) {
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
            Snackbar.make(binding.snackbar, getString(R.string.repo_load_failed, t.getLocalizedMessage()), Snackbar.LENGTH_SHORT).show();
        }
        updateRepoSummary();
    }

    @Override
    public void onModulesReloaded() {
        updateRepoSummary();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.item_sort_by_name) {
            item.setChecked(true);
            App.getPreferences().edit().putInt("repo_sort", 0).apply();
            adapter.refresh();
        } else if (itemId == R.id.item_sort_by_update_time) {
            item.setChecked(true);
            App.getPreferences().edit().putInt("repo_sort", 1).apply();
            adapter.refresh();
        }
        return super.onOptionsItemSelected(item);
    }

    private class RepoAdapter extends EmptyStateRecyclerView.EmptyStateAdapter<RepoAdapter.ViewHolder> implements Filterable {
        private List<OnlineModule> fullList, showList;
        private final LabelComparator labelComparator = new LabelComparator();
        private boolean isLoaded = false;

        RepoAdapter() {
            fullList = showList = Collections.emptyList();
        }

        @NonNull
        @Override
        public RepoAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RepoAdapter.ViewHolder(ItemOnlinemoduleBinding.inflate(getLayoutInflater(), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RepoAdapter.ViewHolder holder, int position) {
            OnlineModule module = showList.get(position);
            holder.appName.setText(module.getDescription());

            SpannableStringBuilder sb = new SpannableStringBuilder(module.getName());

            String summary = module.getSummary();
            if (summary != null) {
                sb.append("\n");
                sb.append(summary);
            }
            holder.appDescription.setVisibility(View.VISIBLE);
            holder.appDescription.setText(sb);
            sb = new SpannableStringBuilder();
            ModuleUtil.InstalledModule installedModule = moduleUtil.getModule(module.getName());
            if (installedModule != null) {
                var ver = repoLoader.getModuleLatestVersion(installedModule.packageName);
                if (ver != null && ver.upgradable(installedModule.versionCode, installedModule.versionName)) {
                    String hint = getString(R.string.update_available, ver.versionName);
                    sb.append(hint);
                    final ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(ResourceUtils.resolveColor(requireActivity().getTheme(), androidx.appcompat.R.attr.colorAccent));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        final TypefaceSpan typefaceSpan = new TypefaceSpan(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                        sb.setSpan(typefaceSpan, sb.length() - hint.length(), sb.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                    } else {
                        final StyleSpan styleSpan = new StyleSpan(Typeface.BOLD);
                        sb.setSpan(styleSpan, sb.length() - hint.length(), sb.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                    }
                    sb.setSpan(foregroundColorSpan, sb.length() - hint.length(), sb.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                }
            }
            if (sb.length() > 0) {
                holder.hint.setVisibility(View.VISIBLE);
                holder.hint.setText(sb);
            } else {
                holder.hint.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(v -> {
                searchView.clearFocus();
                getNavController().navigate(RepoFragmentDirections.actionRepoFragmentToRepoItemFragment(module.getName(), module.getDescription()));
            });
            holder.itemView.setTooltipText(module.getDescription());
        }

        @Override
        public int getItemCount() {
            return showList.size();
        }

        private void setLoaded(boolean isLoaded) {
            this.isLoaded = isLoaded;
            runOnUiThread(this::notifyDataSetChanged);
        }

        public void setData(Collection<OnlineModule> modules) {
            if (modules == null) return;
            setLoaded(false);
            fullList = new ArrayList<>(modules);
            fullList = fullList.stream().filter((onlineModule -> !onlineModule.isHide() && !onlineModule.getReleases().isEmpty())).collect(Collectors.toList());
            int sort = App.getPreferences().getInt("repo_sort", 0);
            if (sort == 0) {
                fullList.sort((o1, o2) -> labelComparator.compare(o1.getDescription(), o2.getDescription()));
            } else if (sort == 1) {
                fullList.sort(Collections.reverseOrder(Comparator.comparing(o -> Instant.parse(o.getReleases().get(0).getUpdatedAt()))));
            }
            String queryStr = searchView != null ? searchView.getQuery().toString() : "";
            runOnUiThread(() -> getFilter().filter(queryStr));
        }

        public void fullRefresh() {
            runAsync(() -> {
                setLoaded(false);
                repoLoader.loadRemoteData();
                refresh();
            });
        }

        public void refresh() {
            runAsync(() -> adapter.setData(repoLoader.getOnlineModules()));
        }

        @Override
        public long getItemId(int position) {
            return showList.get(position).getName().hashCode();
        }

        @Override
        public Filter getFilter() {
            return new RepoAdapter.ModuleFilter();
        }

        @Override
        public boolean isLoaded() {
            return isLoaded && repoLoader.isRepoLoaded();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ConstraintLayout root;
            TextView appName;
            TextView appDescription;
            TextView hint;

            ViewHolder(ItemOnlinemoduleBinding binding) {
                super(binding.getRoot());
                root = binding.itemRoot;
                appName = binding.appName;
                appDescription = binding.description;
                hint = binding.hint;
            }
        }

        class ModuleFilter extends Filter {

            private boolean lowercaseContains(String s, String filter) {
                return !TextUtils.isEmpty(s) && s.toLowerCase().contains(filter);
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                ArrayList<OnlineModule> filtered = new ArrayList<>();
                String filter = constraint.toString().toLowerCase();
                for (OnlineModule info : fullList) {
                    if (lowercaseContains(info.getDescription(), filter) ||
                            lowercaseContains(info.getName(), filter) ||
                            lowercaseContains(info.getSummary(), filter)) {
                        filtered.add(info);
                    }
                }
                filterResults.values = filtered;
                filterResults.count = filtered.size();
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                //noinspection unchecked
                showList = (List<OnlineModule>) results.values;
                setLoaded(true);
            }
        }
    }
}
