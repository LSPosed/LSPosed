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

import static android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.lsposed.lspd.models.UserInfo;
import org.lsposed.manager.App;
import org.lsposed.manager.ConfigManager;
import org.lsposed.manager.R;
import org.lsposed.manager.adapters.AppHelper;
import org.lsposed.manager.databinding.FragmentPagerBinding;
import org.lsposed.manager.databinding.ItemModuleBinding;
import org.lsposed.manager.databinding.SwiperefreshRecyclerviewBinding;
import org.lsposed.manager.repo.RepoLoader;
import org.lsposed.manager.ui.dialog.BlurBehindDialogBuilder;
import org.lsposed.manager.ui.widget.EmptyStateRecyclerView;
import org.lsposed.manager.util.GlideApp;
import org.lsposed.manager.util.ModuleUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import rikka.core.util.ResourceUtils;
import rikka.material.app.LocaleDelegate;
import rikka.recyclerview.RecyclerViewKt;

public class ModulesFragment extends BaseFragment implements ModuleUtil.ModuleListener, RepoLoader.RepoListener {
    private static final PackageManager pm = App.getInstance().getPackageManager();
    private static final ModuleUtil moduleUtil = ModuleUtil.getInstance();
    private static final RepoLoader repoLoader = RepoLoader.getInstance();
    protected FragmentPagerBinding binding;
    protected SearchView searchView;
    private SearchView.OnQueryTextListener searchListener;

    SparseArray<ModuleAdapter> adapters = new SparseArray<>();
    PagerAdapter pagerAdapter = null;

    private ModuleUtil.InstalledModule selectedModule;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        searchListener = new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                forEachAdaptor(adapter -> adapter.getFilter().filter(query));
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                forEachAdaptor(adapter -> adapter.getFilter().filter(query));
                return false;
            }
        };
    }

    private void forEachAdaptor(Consumer<? super ModuleAdapter> action) {
        var snapshot = adapters;
        for (var i = 0; i < snapshot.size(); ++i) {
            action.accept(snapshot.valueAt(i));
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPagerBinding.inflate(inflater, container, false);
        setupToolbar(R.string.Modules, R.menu.menu_modules);
        activityMainBinding.appBar.setLiftable(true);
        activityMainBinding.toolbar.setNavigationIcon(null);
        pagerAdapter = new PagerAdapter(this);
        binding.viewPager.setAdapter(pagerAdapter);
        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                showFabAndBottomNav();
            }
        });

        new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> {
            if (position < adapters.size()) {
                tab.setText(adapters.valueAt(position).getUser().name);
            }
        }).attach();

        binding.tabLayout.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            ViewGroup vg = (ViewGroup) binding.tabLayout.getChildAt(0);
            int tabLayoutWidth = IntStream.range(0, binding.tabLayout.getTabCount()).map(i -> vg.getChildAt(i).getWidth()).sum();
            if (tabLayoutWidth <= binding.getRoot().getWidth()) {
                binding.tabLayout.setTabMode(TabLayout.MODE_FIXED);
                binding.tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
            }
        });

        activityMainBinding.fab.setOnClickListener(v -> {
            var bundle = new Bundle();
            var user = adapters.valueAt(binding.viewPager.getCurrentItem()).getUser();
            bundle.putParcelable("userInfo", user);
            var f = new RecyclerViewDialogFragment();
            f.setArguments(bundle);
            f.show(getChildFragmentManager(), "install_to_user" + user.id);
        });

        moduleUtil.addListener(this);
        repoLoader.addListener(this);
        onModulesReloaded();

        return binding.getRoot();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        searchView.setOnQueryTextListener(searchListener);
        searchView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View arg0) {
                activityMainBinding.appBar.setExpanded(false, true);
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
            }
        });
        searchView.findViewById(androidx.appcompat.R.id.search_edit_frame).setLayoutDirection(View.LAYOUT_DIRECTION_INHERIT);
    }

    @Override
    public void onResume() {
        super.onResume();
        forEachAdaptor(ModuleAdapter::refresh);
    }

    @Override
    public void onSingleModuleReloaded(ModuleUtil.InstalledModule module) {
        forEachAdaptor(ModuleAdapter::refresh);
    }

    @Override
    public void onModulesReloaded() {
        var users = moduleUtil.getUsers();
        if (users == null) return;

        if (users.size() != 1) {
            binding.viewPager.setUserInputEnabled(true);
            binding.tabLayout.setVisibility(View.VISIBLE);
            showFab = R.drawable.ic_baseline_add_24;
        } else {
            binding.viewPager.setUserInputEnabled(false);
            binding.tabLayout.setVisibility(View.GONE);
            showFab = 0;
        }

        var tmp = new SparseArray<ModuleAdapter>(users.size());
        var snapshot = adapters;
        for (var user : users) {
            if (snapshot.indexOfKey(user.id) >= 0) {
                tmp.put(user.id, snapshot.get(user.id));
            } else {
                var adapter = new ModuleAdapter(user);
                adapter.setHasStableIds(true);
                tmp.put(user.id, adapter);
            }
        }
        adapters = tmp;
        forEachAdaptor(ModuleAdapter::refresh);
        runOnUiThread(pagerAdapter::notifyDataSetChanged);
        updateModuleSummary();
    }

    @Override
    public void onRepoLoaded() {
        forEachAdaptor(ModuleAdapter::refresh);
    }

    private void updateModuleSummary() {
        var moduleCount = moduleUtil.getEnabledModulesCount();
        runOnUiThread(() -> {
            if (binding != null) {
                activityMainBinding.toolbarLayout.setSubtitle(moduleCount == -1 ? getString(R.string.loading) : getResources().getQuantityString(R.plurals.modules_enabled_count, moduleCount, moduleCount));
            }
        });
    }

    void installModuleToUser(ModuleUtil.InstalledModule module, UserInfo user) {
        new BlurBehindDialogBuilder(requireActivity(), R.style.ThemeOverlay_MaterialAlertDialog_Centered_FullWidthButtons)
                .setTitle(getString(R.string.install_to_user, user.name))
                .setMessage(getString(R.string.install_to_user_message, module.getAppName(), user.name))
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                        runAsync(() -> {
                            var success = ConfigManager.installExistingPackageAsUser(module.packageName, user.id);
                            String text = success ?
                                    getString(R.string.module_installed, module.getAppName(), user.name) :
                                    getString(R.string.module_install_failed);
                            showHint(text, false);
                            if (success)
                                moduleUtil.reloadSingleModule(module.packageName, user.id);
                        }))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @SuppressLint("WrongConstant")
    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        if (selectedModule == null) {
            return false;
        }
        int itemId = item.getItemId();
        if (itemId == R.id.menu_launch) {
            String packageName = selectedModule.packageName;
            if (packageName == null) {
                return false;
            }
            Intent intent = AppHelper.getSettingsIntent(packageName, selectedModule.userId);
            if (intent != null) {
                ConfigManager.startActivityAsUserWithFeature(intent, selectedModule.userId);
            }
            return true;
        } else if (itemId == R.id.menu_other_app) {
            var intent = new Intent(Intent.ACTION_SHOW_APP_INFO);
            intent.putExtra(Intent.EXTRA_PACKAGE_NAME, selectedModule.packageName);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ConfigManager.startActivityAsUserWithFeature(intent, selectedModule.userId);
            return true;
        } else if (itemId == R.id.menu_app_info) {
            ConfigManager.startActivityAsUserWithFeature(new Intent(ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", selectedModule.packageName, null)), selectedModule.userId);
            return true;
        } else if (itemId == R.id.menu_uninstall) {
            new BlurBehindDialogBuilder(requireActivity(), R.style.ThemeOverlay_MaterialAlertDialog_FullWidthButtons)
                    .setIcon(selectedModule.app.loadIcon(pm))
                    .setTitle(selectedModule.getAppName())
                    .setMessage(R.string.module_uninstall_message)
                    .setPositiveButton(android.R.string.ok, (dialog, which) ->
                            runAsync(() -> {
                                boolean success = ConfigManager.uninstallPackage(selectedModule.packageName, selectedModule.userId);
                                String text = success ? getString(R.string.module_uninstalled, selectedModule.getAppName()) : getString(R.string.module_uninstall_failed);
                                showHint(text, false);
                                if (success)
                                    moduleUtil.reloadSingleModule(selectedModule.packageName, selectedModule.userId);
                            }))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return true;
        } else if (itemId == R.id.menu_repo) {
            var navController = getNavController();
            navController.navigate(
                    new Uri.Builder().scheme("lsposed").authority("repo").appendQueryParameter("modulePackageName", selectedModule.packageName).build(),
                    new NavOptions.Builder().setEnterAnim(R.anim.fragment_enter).setExitAnim(R.anim.fragment_exit).setPopEnterAnim(R.anim.fragment_enter_pop).setPopExitAnim(R.anim.fragment_exit_pop).setLaunchSingleTop(true).setPopUpTo(getNavController().getGraph().getStartDestinationId(), false, true).build());
            return true;
        } else if (itemId == R.id.menu_compile_speed) {
            CompileDialogFragment.speed(getChildFragmentManager(), selectedModule.pkg.applicationInfo);
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        moduleUtil.removeListener(this);
        repoLoader.removeListener(this);
        binding = null;
    }

    public static class ModuleListFragment extends Fragment {
        public SwiperefreshRecyclerviewBinding binding;
        private ModuleAdapter adapter;
        private final RecyclerView.AdapterDataObserver observer = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                binding.swipeRefreshLayout.setRefreshing(!adapter.isLoaded());
            }
        };

        private final View.OnAttachStateChangeListener searchViewLocker = new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                binding.recyclerView.setNestedScrollingEnabled(false);
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                binding.recyclerView.setNestedScrollingEnabled(true);
            }
        };

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            ModulesFragment fragment = (ModulesFragment) getParentFragment();
            Bundle arguments = getArguments();
            if (fragment == null || arguments == null) {
                return null;
            }
            int userId = arguments.getInt("user_id");
            binding = SwiperefreshRecyclerviewBinding.inflate(getLayoutInflater(), container, false);
            adapter = fragment.adapters.get(userId);
            binding.recyclerView.setAdapter(adapter);
            binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
            binding.swipeRefreshLayout.setOnRefreshListener(adapter::fullRefresh);
            binding.swipeRefreshLayout.setProgressViewEndTarget(true, binding.swipeRefreshLayout.getProgressViewEndOffset());
            RecyclerViewKt.fixEdgeEffect(binding.recyclerView, false, true);
            adapter.registerAdapterDataObserver(observer);
            return binding.getRoot();
        }

        void attachListeners() {
            var parent = getParentFragment();
            if (parent instanceof ModulesFragment) {
                var moduleFragment = (ModulesFragment) parent;
                binding.recyclerView.getBorderViewDelegate().setBorderVisibilityChangedListener((top, oldTop, bottom, oldBottom) -> moduleFragment.activityMainBinding.appBar.setLifted(!top));
                moduleFragment.activityMainBinding.appBar.setLifted(!binding.recyclerView.getBorderViewDelegate().isShowingTopBorder());
                moduleFragment.searchView.addOnAttachStateChangeListener(searchViewLocker);
                binding.recyclerView.setNestedScrollingEnabled(moduleFragment.searchView.isIconified());
                View.OnClickListener l = v -> {
                    if (moduleFragment.searchView.isIconified()) {
                        binding.recyclerView.smoothScrollToPosition(0);
                        moduleFragment.activityMainBinding.appBar.setExpanded(true, true);
                    }
                };
                moduleFragment.activityMainBinding.clickView.setOnClickListener(l);
                moduleFragment.activityMainBinding.toolbar.setOnClickListener(l);
            }
        }

        void detachListeners() {
            binding.recyclerView.getBorderViewDelegate().setBorderVisibilityChangedListener(null);
            var parent = getParentFragment();
            if (parent instanceof ModulesFragment) {
                var moduleFragment = (ModulesFragment) parent;
                moduleFragment.searchView.removeOnAttachStateChangeListener(searchViewLocker);
                binding.recyclerView.setNestedScrollingEnabled(true);
            }
        }

        @Override
        public void onStart() {
            super.onStart();
            attachListeners();
        }

        @Override
        public void onResume() {
            super.onResume();
            attachListeners();
        }

        @Override
        public void onDestroyView() {
            adapter.unregisterAdapterDataObserver(observer);
            super.onDestroyView();
        }

        @Override
        public void onPause() {
            super.onPause();
            detachListeners();
        }

        @Override
        public void onStop() {
            super.onStop();
            detachListeners();
        }
    }

    private class PagerAdapter extends FragmentStateAdapter {

        public PagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            Bundle bundle = new Bundle();
            bundle.putInt("user_id", adapters.keyAt(position));
            Fragment fragment = new ModuleListFragment();
            fragment.setArguments(bundle);
            return fragment;
        }

        @Override
        public int getItemCount() {
            return adapters.size();
        }

        @Override
        public long getItemId(int position) {
            return adapters.keyAt(position);
        }

        @Override
        public boolean containsItem(long itemId) {
            return adapters.indexOfKey((int) itemId) >= 0;
        }
    }

    ModuleAdapter createPickModuleAdapter(UserInfo userInfo) {
        return new ModuleAdapter(userInfo, true);
    }

    class ModuleAdapter extends EmptyStateRecyclerView.EmptyStateAdapter<ModuleAdapter.ViewHolder> implements Filterable {
        private List<ModuleUtil.InstalledModule> searchList = new ArrayList<>();
        private List<ModuleUtil.InstalledModule> showList = new ArrayList<>();
        private final UserInfo user;
        private final boolean isPick;
        private boolean isLoaded;
        private View.OnClickListener onPickListener;

        ModuleAdapter(UserInfo user) {
            this(user, false);
        }

        ModuleAdapter(UserInfo user, boolean isPick) {
            this.user = user;
            this.isPick = isPick;
        }

        public UserInfo getUser() {
            return user;
        }

        @NonNull
        @Override
        public ModuleAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ModuleAdapter.ViewHolder(ItemModuleBinding.inflate(getLayoutInflater(), parent, false));
        }

        public boolean isPick() {
            return isPick;
        }

        @Override
        public void onBindViewHolder(@NonNull ModuleAdapter.ViewHolder holder, int position) {
            ModuleUtil.InstalledModule item = showList.get(position);
            String appName;
            if (item.userId != 0) {
                appName = String.format(LocaleDelegate.getDefaultLocale(), "%s (%d)", item.getAppName(), item.userId);
            } else {
                appName = item.getAppName();
            }
            holder.appName.setText(appName);
            GlideApp.with(holder.appIcon)
                    .load(item.getPackageInfo())
                    .into(new CustomTarget<Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                            holder.appIcon.setImageDrawable(resource);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }
                    });
            SpannableStringBuilder sb = new SpannableStringBuilder();
            if (!item.getDescription().isEmpty()) {
                sb.append(item.getDescription());
            } else {
                sb.append(getString(R.string.module_empty_description));
            }
            holder.appDescription.setText(sb);
            holder.appDescription.setVisibility(View.VISIBLE);
            sb = new SpannableStringBuilder();

            int installXposedVersion = ConfigManager.getXposedApiVersion();
            String warningText = null;
            if (item.minVersion == 0) {
                warningText = getString(R.string.no_min_version_specified);
            } else if (installXposedVersion > 0 && item.minVersion > installXposedVersion) {
                warningText = String.format(getString(R.string.warning_xposed_min_version), item.minVersion);
            } else if (item.minVersion < ModuleUtil.MIN_MODULE_VERSION) {
                warningText = String.format(getString(R.string.warning_min_version_too_low), item.minVersion, ModuleUtil.MIN_MODULE_VERSION);
            } else if (item.isInstalledOnExternalStorage()) {
                warningText = getString(R.string.warning_installed_on_external_storage);
            }
            if (warningText != null) {
                sb.append(warningText);
                final ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(ResourceUtils.resolveColor(requireActivity().getTheme(), com.google.android.material.R.attr.colorError));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    final TypefaceSpan typefaceSpan = new TypefaceSpan(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                    sb.setSpan(typefaceSpan, sb.length() - warningText.length(), sb.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                } else {
                    final StyleSpan styleSpan = new StyleSpan(Typeface.BOLD);
                    sb.setSpan(styleSpan, sb.length() - warningText.length(), sb.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                }
                sb.setSpan(foregroundColorSpan, sb.length() - warningText.length(), sb.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            }
            var ver = repoLoader.getModuleLatestVersion(item.packageName);
            if (ver != null && ver.upgradable(item.versionCode, item.versionName)) {
                if (warningText != null) sb.append("\n");
                String recommended = getString(R.string.update_available, ver.versionName);
                sb.append(recommended);
                final ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(ResourceUtils.resolveColor(requireActivity().getTheme(), androidx.appcompat.R.attr.colorPrimary));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    final TypefaceSpan typefaceSpan = new TypefaceSpan(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                    sb.setSpan(typefaceSpan, sb.length() - recommended.length(), sb.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                } else {
                    final StyleSpan styleSpan = new StyleSpan(Typeface.BOLD);
                    sb.setSpan(styleSpan, sb.length() - recommended.length(), sb.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                }
                sb.setSpan(foregroundColorSpan, sb.length() - recommended.length(), sb.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            }
            if (sb.length() == 0) {
                holder.hint.setVisibility(View.GONE);
            } else {
                holder.hint.setVisibility(View.VISIBLE);
                holder.hint.setText(sb);
            }

            if (!isPick) {
                holder.root.setAlpha(moduleUtil.isModuleEnabled(item.packageName) ? 1.0f : .5f);
                holder.itemView.setOnClickListener(v -> {
                    searchView.clearFocus();
                    safeNavigate(ModulesFragmentDirections.actionModulesFragmentToAppListFragment(item.packageName, item.userId));
                });
                holder.itemView.setOnLongClickListener(v -> {
                    searchView.clearFocus();
                    selectedModule = item;
                    return false;
                });
                holder.itemView.setOnCreateContextMenuListener((menu, v, menuInfo) -> {
                    requireActivity().getMenuInflater().inflate(R.menu.context_menu_modules, menu);
                    menu.setHeaderTitle(item.getAppName());
                    Intent intent = AppHelper.getSettingsIntent(item.packageName, item.userId);
                    if (intent == null) {
                        menu.removeItem(R.id.menu_launch);
                    }
                    if (repoLoader.getOnlineModule(item.packageName) == null) {
                        menu.removeItem(R.id.menu_repo);
                    }
                    if (item.userId == 0) {
                        var users = ConfigManager.getUsers();
                        if (users != null) {
                            for (var user : users) {
                                if (moduleUtil.getModule(item.packageName, user.id) == null) {
                                    menu.add(1, user.id, 0, getString(R.string.install_to_user, user.name)).setOnMenuItemClickListener(i -> {
                                        installModuleToUser(selectedModule, user);
                                        return true;
                                    });
                                }
                            }
                        }
                    }
                });
                holder.appVersion.setVisibility(View.VISIBLE);
                holder.appVersion.setText(item.versionName);
                holder.appVersion.setSelected(true);
            } else {
                holder.itemView.setTag(item);
                holder.itemView.setOnClickListener(v -> {
                    if (onPickListener != null) onPickListener.onClick(v);
                });
            }
        }

        @Override
        public void onViewRecycled(@NonNull ViewHolder holder) {
            holder.itemView.setTag(null);
            super.onViewRecycled(holder);
        }

        @Override
        public int getItemCount() {
            return showList.size();
        }

        @Override
        public long getItemId(int position) {
            var module = showList.get(position);
            return (module.packageName + "!" + module.userId).hashCode();
        }

        @Override
        public Filter getFilter() {
            return new ModuleAdapter.ApplicationFilter();
        }

        public void setOnPickListener(View.OnClickListener onPickListener) {
            this.onPickListener = onPickListener;
        }

        public void refresh() {
            runAsync(reloadModules);
        }

        public void fullRefresh() {
            runAsync(() -> {
                setLoaded(null, false);
                moduleUtil.reloadInstalledModules();
                refresh();
            });
        }

        private final Runnable reloadModules = () -> {
            var modules = moduleUtil.getModules();
            if (modules == null) return;
            Comparator<PackageInfo> cmp = AppHelper.getAppListComparator(0, pm);
            setLoaded(null, false);
            var tmpList = new ArrayList<ModuleUtil.InstalledModule>();
            modules.values().parallelStream()
                    .sorted((a, b) -> {
                        boolean aChecked = moduleUtil.isModuleEnabled(a.packageName);
                        boolean bChecked = moduleUtil.isModuleEnabled(b.packageName);
                        if (aChecked == bChecked) {
                            var c = cmp.compare(a.pkg, b.pkg);
                            if (c == 0) {
                                if (a.userId == getUser().id) return -1;
                                if (b.userId == getUser().id) return 1;
                                else return Integer.compare(a.userId, b.userId);
                            }
                            return c;
                        } else if (aChecked) {
                            return -1;
                        } else {
                            return 1;
                        }
                    }).forEachOrdered(new Consumer<>() {
                private final HashSet<String> uniquer = new HashSet<>();

                @Override
                public void accept(ModuleUtil.InstalledModule module) {
                    if (isPick()) {
                        if (!uniquer.contains(module.packageName)) {
                            uniquer.add(module.packageName);
                            if (module.userId != getUser().id)
                                tmpList.add(module);
                        }
                    } else if (module.userId == getUser().id) {
                        tmpList.add(module);
                    }
                }
            });
            String queryStr = searchView != null ? searchView.getQuery().toString() : "";
            searchList = tmpList;
            runOnUiThread(() -> getFilter().filter(queryStr));
        };

        @SuppressLint("NotifyDataSetChanged")
        private void setLoaded(List<ModuleUtil.InstalledModule> list, boolean loaded) {
            runOnUiThread(() -> {
                if (list != null) showList = list;
                isLoaded = loaded;
                notifyDataSetChanged();
            });
        }

        @Override
        public boolean isLoaded() {
            return isLoaded && moduleUtil.isModulesLoaded();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ConstraintLayout root;
            ImageView appIcon;
            TextView appName;
            TextView appDescription;
            TextView appVersion;
            TextView hint;
            MaterialCheckBox checkBox;

            ViewHolder(ItemModuleBinding binding) {
                super(binding.getRoot());
                root = binding.itemRoot;
                appIcon = binding.appIcon;
                appName = binding.appName;
                appDescription = binding.description;
                appVersion = binding.versionName;
                hint = binding.hint;
                checkBox = binding.checkbox;
            }
        }

        class ApplicationFilter extends Filter {

            private boolean lowercaseContains(String s, String filter) {
                return !TextUtils.isEmpty(s) && s.toLowerCase().contains(filter);
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                List<ModuleUtil.InstalledModule> filtered = new ArrayList<>();
                String filter = constraint.toString().toLowerCase();
                for (ModuleUtil.InstalledModule info : searchList) {
                    if (lowercaseContains(info.getAppName(), filter) ||
                            lowercaseContains(info.packageName, filter) ||
                            lowercaseContains(info.getDescription(), filter)) {
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
                setLoaded((List<ModuleUtil.InstalledModule>) results.values, true);
            }
        }
    }
}
