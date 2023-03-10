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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
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
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.Response;
import rikka.core.util.ResourceUtils;
import rikka.material.app.LocaleDelegate;
import rikka.recyclerview.RecyclerViewKt;
import rikka.widget.borderview.BorderView;

public class RepoItemFragment extends BaseFragment implements RepoLoader.RepoListener, MenuProvider {
    FragmentPagerBinding binding;
    OnlineModule module;
    private ReleaseAdapter releaseAdapter;
    private InformationAdapter informationAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPagerBinding.inflate(getLayoutInflater(), container, false);
        if (module == null) return binding.getRoot();
        String modulePackageName = module.getName();
        String moduleName = module.getDescription();
        binding.appBar.setLiftable(true);
        setupToolbar(binding.toolbar, binding.clickView, moduleName, R.menu.menu_repo_item);
        binding.clickView.setTooltipText(moduleName);
        binding.toolbar.setSubtitle(modulePackageName);
        binding.viewPager.setAdapter(new PagerAdapter(this));
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
        binding.toolbar.setOnClickListener(v -> binding.appBar.setExpanded(true, true));
        releaseAdapter = new ReleaseAdapter();
        informationAdapter = new InformationAdapter();
        RepoLoader.getInstance().addListener(this);
        return binding.getRoot();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        RepoLoader.getInstance().addListener(this);
        super.onCreate(savedInstanceState);

        String modulePackageName = getArguments() == null ? null : getArguments().getString("modulePackageName");
        module = RepoLoader.getInstance().getOnlineModule(modulePackageName);
        if (module == null) {
            if (!safeNavigate(R.id.action_repo_item_fragment_to_repo_fragment)) {
                safeNavigate(R.id.repo_nav);
            }
        }
    }

    private void renderGithubMarkdown(WebView view, String text) {
        try {
            view.setBackgroundColor(Color.TRANSPARENT);
            var setting = view.getSettings();
            setting.setOffscreenPreRaster(true);
            setting.setDomStorageEnabled(true);
            setting.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            setting.setAllowContentAccess(false);
            setting.setAllowFileAccessFromFileURLs(true);
            setting.setAllowFileAccess(false);
            setting.setGeolocationEnabled(false);
            setting.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            setting.setTextZoom(80);
            String body;
            String direction;
            if (getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                direction = "rtl";
            } else {
                direction = "ltr";
            }
            if (ResourceUtils.isNightMode(getResources().getConfiguration())) {
                body = App.HTML_TEMPLATE_DARK.get().replace("@dir@", direction).replace("@body@", text);
            } else {
                body = App.HTML_TEMPLATE.get().replace("@dir@", direction).replace("@body@", text);
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
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {

    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_open_in_browser) {
            NavUtil.startURL(requireActivity(), "https://modules.lsposed.org/module/" + module.getName());
            return true;
        }
        return false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        RepoLoader.getInstance().removeListener(this);
        binding = null;
    }

    @Override
    public void onModuleReleasesLoaded(OnlineModule module) {
        this.module = module;
        var repoLoader = RepoLoader.getInstance();
        if (releaseAdapter != null) {
            runAsync(releaseAdapter::loadItems);
        }
        if ((repoLoader.getReleases(module.getName()) != null ? repoLoader.getReleases(module.getName()).size() : 1) == 1) {
            showHint(R.string.module_release_no_more, true);
        }
    }

    @Override
    public void onThrowable(Throwable t) {
        if (releaseAdapter != null) {
            runAsync(releaseAdapter::loadItems);
        }
        showHint(getString(R.string.repo_load_failed, t.getLocalizedMessage()), true);
    }

    private class InformationAdapter extends SimpleStatefulAdaptor<InformationAdapter.ViewHolder> {

        private int rowCount = 0;
        private int homepageRow = -1;
        private int collaboratorsRow = -1;
        private int sourceUrlRow = -1;

        public InformationAdapter() {
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
            return new ViewHolder(ItemRepoTitleDescriptionBinding.inflate(getLayoutInflater(), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull InformationAdapter.ViewHolder holder, int position) {
            if (position == homepageRow) {
                holder.title.setText(R.string.module_information_homepage);
                holder.description.setText(module.getHomepageUrl());
            } else if (position == collaboratorsRow) {
                List<Collaborator> collaborators = module.getCollaborators();
                if (collaborators == null) return;
                holder.title.setText(R.string.module_information_collaborators);
                SpannableStringBuilder sb = new SpannableStringBuilder();
                ListIterator<Collaborator> iterator = collaborators.listIterator();
                while (iterator.hasNext()) {
                    Collaborator collaborator = iterator.next();
                    var collaboratorLogin = collaborator.getLogin();
                    if (collaboratorLogin == null) continue;
                    String name = collaborator.getName() == null ? collaboratorLogin : collaborator.getName();
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

    public static class DownloadDialog extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            var args = getArguments();
            if (args == null) throw new IllegalArgumentException();
            return new BlurBehindDialogBuilder(requireActivity(), R.style.ThemeOverlay_MaterialAlertDialog_Centered_FullWidthButtons)
                    .setTitle(R.string.module_release_view_assets)
                    .setPositiveButton(android.R.string.cancel, null)
                    .setAdapter(new ArrayAdapter<>(requireActivity(), R.layout.dialog_item, args.getCharSequenceArray("names")),
                            (dialog, which) -> NavUtil.startURL(requireActivity(), args.getStringArrayList("urls").get(which)))
                    .create();
        }

        static void create(Activity activity, FragmentManager fm, List<ReleaseAsset> assets) {
            var f = new DownloadDialog();
            var bundle = new Bundle();

            var displayNames = new CharSequence[assets.size()];
            for (int i = 0; i < assets.size(); i++) {
                var sb = new SpannableStringBuilder(assets.get(i).getName());
                var count = assets.get(i).getDownloadCount();
                var countStr = activity.getResources().getQuantityString(R.plurals.module_release_assets_download_count, count, count);
                var sizeStr = Formatter.formatShortFileSize(activity, assets.get(i).getSize());
                sb.append('\n').append(sizeStr).append('/').append(countStr);
                final ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(ResourceUtils.resolveColor(activity.getTheme(), android.R.attr.textColorSecondary));
                final RelativeSizeSpan relativeSizeSpan = new RelativeSizeSpan(0.8f);
                sb.setSpan(foregroundColorSpan, sb.length() - sizeStr.length() - countStr.length() - 1, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(relativeSizeSpan, sb.length() - sizeStr.length() - countStr.length() - 1, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                displayNames[i] = sb;
            }
            bundle.putCharSequenceArray("names", displayNames);
            bundle.putStringArrayList("urls", assets.stream().map(ReleaseAsset::getDownloadUrl).collect(Collectors.toCollection(ArrayList::new)));
            f.setArguments(bundle);
            f.show(fm, "download");
        }
    }

    private class ReleaseAdapter extends EmptyStateRecyclerView.EmptyStateAdapter<ReleaseAdapter.ViewHolder> {
        private List<Release> items = new ArrayList<>();
        private final Resources resources = App.getInstance().getResources();

        public ReleaseAdapter() {
            runAsync(this::loadItems);
        }

        @SuppressLint("NotifyDataSetChanged")
        public void loadItems() {
            var channels = resources.getStringArray(R.array.update_channel_values);
            var channel = App.getPreferences().getString("update_channel", channels[0]);
            var releases = RepoLoader.getInstance().getReleases(module.getName());
            if (releases == null) releases = module.getReleases();
            List<Release> tmpList;
            if (channel.equals(channels[0])) {
                tmpList = releases != null ? releases.parallelStream().filter(t -> {
                    if (Boolean.TRUE.equals(t.getIsPrerelease())) return false;
                    var name = t.getName() != null ? t.getName().toLowerCase(LocaleDelegate.getDefaultLocale()) : null;
                    return !(name != null && name.startsWith("snapshot")) && !(name != null && name.startsWith("nightly"));
                }).collect(Collectors.toList()) : null;
            } else if (channel.equals(channels[1])) {
                tmpList = releases != null ? releases.parallelStream().filter(t -> {
                    var name = t.getName() != null ? t.getName().toLowerCase(LocaleDelegate.getDefaultLocale()) : null;
                    return !(name != null && name.startsWith("snapshot")) && !(name != null && name.startsWith("nightly"));
                }).collect(Collectors.toList()) : null;
            } else tmpList = releases;
            runOnUiThread(() -> {
                items = tmpList;
                notifyDataSetChanged();
            });
        }

        @NonNull
        @Override
        public ReleaseAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == 0) {
                return new ReleaseViewHolder(ItemRepoReleaseBinding.inflate(getLayoutInflater(), parent, false));
            } else {
                return new LoadmoreViewHolder(ItemRepoLoadmoreBinding.inflate(getLayoutInflater(), parent, false));
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
                var instant = Instant.parse(release.getPublishedAt());
                var formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                        .withLocale(App.getLocale()).withZone(ZoneId.systemDefault());
                holder.publishedTime.setText(String.format(getString(R.string.module_repo_published_time), formatter.format(instant)));
                renderGithubMarkdown(holder.description, release.getDescriptionHTML());
                holder.openInBrowser.setOnClickListener(v -> NavUtil.startURL(requireActivity(), release.getUrl()));
                List<ReleaseAsset> assets = release.getReleaseAssets();
                if (assets != null && !assets.isEmpty()) {
                    holder.viewAssets.setOnClickListener(v -> DownloadDialog.create(requireActivity(), getParentFragmentManager(), assets));
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
            TextView publishedTime;
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
                publishedTime = binding.publishedTime;
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

    private static class PagerAdapter extends FragmentStateAdapter {

        public PagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            Bundle bundle = new Bundle();
            bundle.putInt("position", position);
            Fragment f;
            if (position == 0) {
                f = new ReadmeFragment();
            } else if (position == 1) {
                f = new RecyclerviewFragment();
            } else {
                f = new RecyclerviewFragment();
            }
            f.setArguments(bundle);
            return f;
        }

        @Override
        public int getItemCount() {
            return 3;
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? 0 : 1;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }
    }

    public static abstract class BorderFragment extends BaseFragment {
        BorderView borderView;

        void attachListeners() {
            var parent = getParentFragment();
            if (parent instanceof RepoItemFragment) {
                var repoItemFragment = (RepoItemFragment) parent;
                borderView.getBorderViewDelegate().setBorderVisibilityChangedListener((top, oldTop, bottom, oldBottom) -> repoItemFragment.binding.appBar.setLifted(!top));
                repoItemFragment.binding.appBar.setLifted(!borderView.getBorderViewDelegate().isShowingTopBorder());
                repoItemFragment.binding.toolbar.setOnClickListener(v -> {
                    repoItemFragment.binding.appBar.setExpanded(true, true);
                    scrollToTop();
                });
            }
        }

        abstract void scrollToTop();

        void detachListeners() {
            borderView.getBorderViewDelegate().setBorderVisibilityChangedListener(null);
        }

        @Override
        public void onResume() {
            super.onResume();
            attachListeners();
        }

        @Override
        public void onStart() {
            super.onStart();
            attachListeners();
        }

        @Override
        public void onStop() {
            super.onStop();
            detachListeners();
        }

        @Override
        public void onPause() {
            super.onPause();
            detachListeners();
        }
    }

    public static class ReadmeFragment extends BorderFragment {
        ItemRepoReadmeBinding binding;

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            var parent = getParentFragment();
            if (!(parent instanceof RepoItemFragment)) {
                if (!safeNavigate(R.id.action_repo_item_fragment_to_repo_fragment)) {
                    safeNavigate(R.id.repo_nav);
                }
                return null;
            }
            var repoItemFragment = (RepoItemFragment) parent;
            binding = ItemRepoReadmeBinding.inflate(getLayoutInflater(), container, false);
            repoItemFragment.renderGithubMarkdown(binding.readme, repoItemFragment.module.getReadmeHTML());
            borderView = binding.scrollView;
            return binding.getRoot();
        }

        @Override
        void scrollToTop() {
            binding.scrollView.fullScroll(ScrollView.FOCUS_UP);
        }
    }

    public static class RecyclerviewFragment extends BorderFragment {
        ItemRepoRecyclerviewBinding binding;
        RecyclerView.Adapter<?> adapter;

        @Override
        void scrollToTop() {
            binding.recyclerView.smoothScrollToPosition(0);
        }

        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            var arguments = getArguments();
            var parent = getParentFragment();
            if (arguments == null || !(parent instanceof RepoItemFragment)) {
                if (!safeNavigate(R.id.action_repo_item_fragment_to_repo_fragment)) {
                    safeNavigate(R.id.repo_nav);
                }
                return null;
            }
            var repoItemFragment = (RepoItemFragment) parent;
            var position = arguments.getInt("position", 0);
            if (position == 1)
                adapter = repoItemFragment.releaseAdapter;
            else if (position == 2)
                adapter = repoItemFragment.informationAdapter;
            else return null;
            binding = ItemRepoRecyclerviewBinding.inflate(getLayoutInflater(), container, false);
            binding.recyclerView.setAdapter(adapter);
            binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
            RecyclerViewKt.fixEdgeEffect(binding.recyclerView, false, true);
            borderView = binding.recyclerView;
            return binding.getRoot();
        }
    }
}
