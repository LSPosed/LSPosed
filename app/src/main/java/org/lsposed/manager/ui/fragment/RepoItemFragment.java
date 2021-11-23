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

import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.lsposed.manager.App;
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
import org.lsposed.manager.ui.dialog.BlurBehindDialogBuilder;
import org.lsposed.manager.ui.widget.EmptyStateRecyclerView;
import org.lsposed.manager.ui.widget.LinkifyTextView;
import org.lsposed.manager.util.NavUtil;
import org.lsposed.manager.util.SimpleStatefulAdaptor;
import org.lsposed.manager.util.chrome.CustomTabsURLSpan;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.Response;
import rikka.core.util.ResourceUtils;
import rikka.recyclerview.RecyclerViewKt;
import rikka.widget.borderview.BorderNestedScrollView;
import rikka.widget.borderview.BorderRecyclerView;

public class RepoItemFragment extends BaseFragment implements RepoLoader.Listener {
    FragmentPagerBinding binding;
    private OnlineModule module;
    private ReleaseAdapter releaseAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPagerBinding.inflate(getLayoutInflater(), container, false);
        if (module == null) return binding.getRoot();
        String modulePackageName = module.getName();
        String moduleName = module.getDescription();
        setupToolbar(binding.toolbar, moduleName, R.menu.menu_repo_item);
        binding.toolbar.setSubtitle(modulePackageName);
        binding.viewPager.setAdapter(new PagerAdapter());
        int[] titles = new int[]{R.string.module_readme, R.string.module_releases, R.string.module_information};
        new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> tab.setText(titles[position])).attach();

        binding.tabLayout.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            ViewGroup vg = (ViewGroup) binding.tabLayout.getChildAt(0);
            int tabLayoutWidth = IntStream.range(0, binding.tabLayout.getTabCount()).map(i -> vg.getChildAt(i).getWidth()).sum();
            if (tabLayoutWidth <= binding.getRoot().getWidth()) {
                binding.tabLayout.setTabMode(TabLayout.MODE_FIXED);
                binding.tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
            }
        });

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

        String modulePackageName = getArguments() == null ? null : getArguments().getString("modulePackageName");
        module = RepoLoader.getInstance().getOnlineModule(modulePackageName);
        if (module == null)
            getNavController().navigate(R.id.action_repo_item_fragment_to_repo_fragment);
    }

    private void renderGithubMarkdown(WebView view, String text) {
        try {
            view.setBackgroundColor(Color.TRANSPARENT);
            var setting = view.getSettings();
            setting.setOffscreenPreRaster(true);
            setting.setDomStorageEnabled(true);
            setting.setAppCacheEnabled(true);
            setting.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            setting.setAllowContentAccess(false);
            setting.setAllowFileAccessFromFileURLs(true);
            setting.setAllowFileAccess(false);
            setting.setGeolocationEnabled(false);
            setting.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            setting.setTextZoom(80);
            String body;
            if (ResourceUtils.isNightMode(getResources().getConfiguration())) {
                body = App.HTML_TEMPLATE_DARK.get().replace("@body@", text);
            } else {
                body = App.HTML_TEMPLATE.get().replace("@body@", text);
            }
            view.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    NavUtil.startURL(requireActivity(), request.getUrl());
                    return true;
                }

                @Nullable
                @Override
                public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                    if (!request.getUrl().getScheme().startsWith("http")) return null;
                    var client = App.getOkHttpClient();
                    var call = client.newCall(
                            new Request.Builder()
                                    .url(request.getUrl().toString())
                                    .method(request.getMethod(), null)
                                    .headers(Headers.of(request.getRequestHeaders()))
                                    .build());
                    try {
                        Response reply = call.execute();
                        var header = reply.header("content-type", "image/*;charset=utf-8");
                        String[] contentTypes = new String[0];
                        if (header != null) {
                            contentTypes = header.split(";\\s*");
                        }
                        var mimeType = contentTypes.length > 0 ? contentTypes[0] : "image/*";
                        var charset = contentTypes.length > 1 ? contentTypes[1].split("=\\s*")[1] : "utf-8";
                        var body = reply.body();
                        if (body == null) return null;
                        return new WebResourceResponse(
                                mimeType,
                                charset,
                                body.byteStream()
                        );
                    } catch (Throwable e) {
                        return new WebResourceResponse("text/html", "utf-8", new ByteArrayInputStream(Log.getStackTraceString(e).getBytes(StandardCharsets.UTF_8)));
                    }
                }
            });
            view.loadDataWithBaseURL("https://github.com", body, "text/html",
                    StandardCharsets.UTF_8.name(), null);
        } catch (Throwable e) {
            Log.e(App.TAG, "render readme", e);
        }
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
    public void moduleReleasesLoaded(OnlineModule module) {
        this.module = module;
        if (releaseAdapter != null) {
            runAsync(releaseAdapter::loadItems);
            if (isResumed() && module.getReleases().size() == 1) {
                Snackbar.make(binding.snackbar, R.string.module_release_no_more, Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onThrowable(Throwable t) {
        if (releaseAdapter != null) {
            runAsync(releaseAdapter::loadItems);
            if (isResumed()) {
                Snackbar.make(binding.snackbar, getString(R.string.repo_load_failed, t.getLocalizedMessage()), Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        RepoLoader.getInstance().removeListener(this);
        binding = null;
    }

    private class InformationAdapter extends SimpleStatefulAdaptor<InformationAdapter.ViewHolder> {
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

    private class ReleaseAdapter extends EmptyStateRecyclerView.EmptyStateAdapter<ReleaseAdapter.ViewHolder> {
        private List<Release> items = new ArrayList<>();
        private final Resources resources = App.getInstance().getResources();

        public ReleaseAdapter() {
            runAsync(this::loadItems);
        }

        public void loadItems() {
            var channels = resources.getStringArray(R.array.update_channel_values);
            var channel = App.getPreferences().getString("update_channel", channels[0]);
            var releases = module.getReleases();
            if (channel.equals(channels[0])) {
                this.items = releases.parallelStream().filter(t -> {
                    if (t.getIsPrerelease()) return false;
                    var name = t.getName().toLowerCase(Locale.ROOT);
                    return !name.startsWith("snapshot") && !name.startsWith("nightly");
                }).collect(Collectors.toList());
            } else if (channel.equals(channels[1])) {
                this.items = releases.parallelStream().filter(t -> {
                    var name = t.getName().toLowerCase(Locale.ROOT);
                    return !name.startsWith("snapshot") && !name.startsWith("nightly");
                }).collect(Collectors.toList());
            } else this.items = releases;
            runOnUiThread(this::notifyDataSetChanged);
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
            if (holder.getItemViewType() == 1) {
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
                renderGithubMarkdown(holder.description, release.getDescriptionHTML());
                holder.openInBrowser.setOnClickListener(v -> NavUtil.startURL(requireActivity(), release.getUrl()));
                List<ReleaseAsset> assets = release.getReleaseAssets();
                if (assets != null && !assets.isEmpty()) {
                    holder.viewAssets.setOnClickListener(v -> {
                        ArrayList<String> names = new ArrayList<>();
                        assets.forEach(releaseAsset -> names.add(releaseAsset.getName()));
                        new BlurBehindDialogBuilder(requireActivity())
                                .setItems(names.toArray(new String[0]), (dialog, which) -> NavUtil.startURL(requireActivity(), assets.get(which).getDownloadUrl()))
                                .show();
                    });
                } else {
                    holder.viewAssets.setVisibility(View.GONE);
                }
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

        @Override
        public boolean isLoaded() {
            return module.releasesLoaded;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView title;
            WebView description;
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

    private class PagerAdapter extends SimpleStatefulAdaptor<PagerAdapter.ViewHolder> {

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
                    if (module != null)
                        renderGithubMarkdown(holder.webView, module.getReadmeHTML());
                    break;
                case 1:
                case 2:
                    RecyclerView.Adapter adapter;
                    if (position == 1) {
                        adapter = releaseAdapter = new ReleaseAdapter();
                    } else {
                        adapter = new InformationAdapter(module);
                    }
                    holder.recyclerView.setAdapter(adapter);
                    holder.recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
                    RecyclerViewKt.fixEdgeEffect(holder.recyclerView, false, true);
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
            WebView webView;
            BorderNestedScrollView scrollView;
            BorderRecyclerView recyclerView;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }

        class ReadmeViewHolder extends PagerAdapter.ViewHolder {
            public ReadmeViewHolder(ItemRepoReadmeBinding binding) {
                super(binding.getRoot());
                webView = binding.readme;
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
