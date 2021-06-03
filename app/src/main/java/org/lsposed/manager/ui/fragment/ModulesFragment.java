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
import android.os.Handler;
import android.os.HandlerThread;
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
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayoutMediator;

import org.lsposed.lspd.models.UserInfo;
import org.lsposed.manager.App;
import org.lsposed.manager.ConfigManager;
import org.lsposed.manager.R;
import org.lsposed.manager.adapters.AppHelper;
import org.lsposed.manager.databinding.DialogRecyclerviewBinding;
import org.lsposed.manager.databinding.FragmentPagerBinding;
import org.lsposed.manager.databinding.ItemModuleBinding;
import org.lsposed.manager.databinding.ItemRepoRecyclerviewBinding;
import org.lsposed.manager.repo.RepoLoader;
import org.lsposed.manager.ui.widget.EmptyStateRecyclerView;
import org.lsposed.manager.util.GlideApp;
import org.lsposed.manager.util.LinearLayoutManagerFix;
import org.lsposed.manager.util.ModuleUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import rikka.core.res.ResourcesKt;
import rikka.insets.WindowInsetsHelperKt;
import rikka.recyclerview.RecyclerViewKt;
import rikka.widget.borderview.BorderRecyclerView;

public class ModulesFragment extends BaseFragment implements ModuleUtil.ModuleListener {

    private static final Handler workHandler;
    private static final PackageManager pm = App.getInstance().getPackageManager();
    private static final ModuleUtil moduleUtil = ModuleUtil.getInstance();

    protected FragmentPagerBinding binding;
    protected SearchView searchView;
    private SearchView.OnQueryTextListener searchListener;
    private final ArrayList<ModuleAdapter> adapters = new ArrayList<>();
    private final ArrayList<String> tabTitles = new ArrayList<>();

    private ModuleUtil.InstalledModule selectedModule;

    static {
        HandlerThread workThread = new HandlerThread("ModulesActivity WorkHandler");
        workThread.start();
        workHandler = new Handler(workThread.getLooper());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        moduleUtil.addListener(this);
        searchListener = new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapters.forEach(adapter -> adapter.getFilter().filter(query));
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapters.forEach(adapter -> adapter.getFilter().filter(newText));
                return false;
            }
        };
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPagerBinding.inflate(inflater, container, false);

        binding.getRoot().bringChildToFront(binding.appBar);
        setupToolbar(binding.toolbar, R.string.Modules, R.menu.menu_modules);
        binding.viewPager.setAdapter(new PagerAdapter(this));
        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                BorderRecyclerView recyclerView = binding.viewPager.findViewWithTag(position);

                if (recyclerView != null) {
                    binding.appBar.setRaised(!recyclerView.getBorderViewDelegate().isShowingTopBorder());
                }

                if (position > 0) {
                    binding.fab.show();
                } else {
                    binding.fab.hide();
                }
            }
        });

        var users = ConfigManager.getUsers();
        if (users != null) {
            adapters.clear();
            if (users.size() != 1) {
                tabTitles.clear();
                for (var user : users) {
                    var adapter = new ModuleAdapter(user);
                    adapter.setHasStableIds(true);
                    adapters.add(adapter);
                    tabTitles.add(user.name);
                }
                new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> {
                    if (position < tabTitles.size()) {
                        tab.setText(tabTitles.get(position));
                    }
                }).attach();
                binding.viewPager.setUserInputEnabled(true);
                binding.tabLayout.setVisibility(View.VISIBLE);
            } else {
                var adapter = new ModuleAdapter(null);
                adapter.setHasStableIds(true);
                adapters.add(adapter);
                binding.viewPager.setUserInputEnabled(false);
                binding.tabLayout.setVisibility(View.GONE);
            }
        }

        binding.fab.setOnClickListener(v -> {
            var pickAdaptor = new ModuleAdapter(null, true);
            var position = binding.viewPager.getCurrentItem();
            var snapshot = adapters.get(position).snapshot().stream().map(m -> m.packageName).collect(Collectors.toSet());
            var user = adapters.get(position).getUser();
            pickAdaptor.setFilter(m -> !snapshot.contains(m.packageName));
            pickAdaptor.refresh();
            var rv = DialogRecyclerviewBinding.inflate(getLayoutInflater()).getRoot();
            rv.setAdapter(pickAdaptor);
            rv.setLayoutManager(new LinearLayoutManagerFix(requireActivity()));
            var dialog = new AlertDialog.Builder(requireActivity())
                    .setTitle(getString(R.string.install_to_user, user.name))
                    .setView(rv)
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            pickAdaptor.setOnPickListener(picked -> {
                var module = (ModuleUtil.InstalledModule) picked.getTag();
                installModuleToUser(module, user);
                dialog.dismiss();
            });
        });

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (ConfigManager.getXposedVersionName() == null) {
            Toast.makeText(requireContext(), R.string.lsposed_not_active, Toast.LENGTH_LONG).show();
            getNavController().navigateUp();
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        searchView.setOnQueryTextListener(searchListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        adapters.forEach(ModuleAdapter::refresh);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        moduleUtil.removeListener(this);
    }

    @Override
    public void onSingleInstalledModuleReloaded() {
        adapters.forEach(adapter -> adapter.refresh(true));
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_refresh) {
            adapters.forEach(adapter -> adapter.refresh(true));
        }
        return super.onOptionsItemSelected(item);
    }

    private void installModuleToUser(ModuleUtil.InstalledModule module, UserInfo user) {
        new AlertDialog.Builder(requireActivity())
                .setTitle(getString(R.string.install_to_user, user.name))
                .setMessage(getString(R.string.install_to_user_message, module.getAppName(), user.name))
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                        workHandler.post(() -> {
                            var success = ConfigManager.installExistingPackageAsUser(module.packageName, user.id);
                            requireActivity().runOnUiThread(() -> {
                                String text = success ? getString(R.string.module_installed, module.getAppName(), user.name) : getString(R.string.module_install_failed);
                                if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                                    Snackbar.make(binding.snackbar, text, Snackbar.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(requireActivity(), text, Toast.LENGTH_SHORT).show();
                                }
                            });
                            if (success)
                                moduleUtil.reloadSingleModule(module.packageName, user.id);
                        }))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

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
            } else {
                Snackbar.make(binding.snackbar, R.string.module_no_ui, Snackbar.LENGTH_LONG).show();
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
            new AlertDialog.Builder(requireActivity())
                    .setTitle(selectedModule.getAppName())
                    .setMessage(R.string.module_uninstall_message)
                    .setPositiveButton(android.R.string.ok, (dialog, which) ->
                            workHandler.post(() -> {
                                boolean success = ConfigManager.uninstallPackage(selectedModule.packageName, selectedModule.userId);
                                requireActivity().runOnUiThread(() -> {
                                    String text = success ? getString(R.string.module_uninstalled, selectedModule.getAppName()) : getString(R.string.module_uninstall_failed);
                                    if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                                        Snackbar.make(binding.snackbar, text, Snackbar.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(requireActivity(), text, Toast.LENGTH_SHORT).show();
                                    }
                                });
                                if (success)
                                    moduleUtil.reloadSingleModule(selectedModule.packageName, selectedModule.userId);
                            }))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return true;
        } else if (itemId == R.id.menu_repo) {
            getNavController().navigate(ModulesFragmentDirections.actionModulesFragmentToRepoItemFragment(selectedModule.packageName, selectedModule.getAppName()));
            return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        binding = null;
    }

    public static class ModuleListFragment extends Fragment {

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            ModulesFragment fragment = (ModulesFragment) getParentFragment();
            Bundle arguments = getArguments();
            if (fragment == null || arguments == null) {
                return null;
            }
            int position = arguments.getInt("position");
            ItemRepoRecyclerviewBinding binding = ItemRepoRecyclerviewBinding.inflate(getLayoutInflater(), container, false);
            if (fragment.adapters.size() == 1) {
                WindowInsetsHelperKt.setInitialPadding(binding.recyclerView, 0, ResourcesKt.resolveDimensionPixelOffset(requireActivity().getTheme(), R.attr.actionBarSize, 0), 0, 0);
            }
            binding.recyclerView.setTag(position);
            binding.recyclerView.setAdapter(fragment.adapters.get(position));
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManagerFix(requireActivity());
            binding.recyclerView.setLayoutManager(layoutManager);
            binding.recyclerView.getBorderViewDelegate().setBorderVisibilityChangedListener((top, oldTop, bottom, oldBottom) -> fragment.binding.appBar.setRaised(!top));
            binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE && position > 0) {
                        fragment.binding.fab.show();
                    } else {
                        fragment.binding.fab.hide();
                    }
                }
            });
            RecyclerViewKt.fixEdgeEffect(binding.recyclerView, false, true);
            RecyclerViewKt.addFastScroller(binding.recyclerView, binding.recyclerView);
            return binding.getRoot();
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
            bundle.putInt("position", position);
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
            return position;
        }
    }

    private class ModuleAdapter extends EmptyStateRecyclerView.EmptyStateAdapter<ModuleAdapter.ViewHolder> implements Filterable {
        private final ConcurrentLinkedQueue<ModuleUtil.InstalledModule> searchList = new ConcurrentLinkedQueue<>();
        private final List<ModuleUtil.InstalledModule> showList = new ArrayList<>();
        private final UserInfo user;
        private final boolean isPick;
        private boolean isLoaded;
        private View.OnClickListener onPickListener;

        private Predicate<ModuleUtil.InstalledModule> customFilter = m -> true;

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

        @Override
        public void onBindViewHolder(@NonNull ModuleAdapter.ViewHolder holder, int position) {
            ModuleUtil.InstalledModule item = showList.get(position);
            String appName;
            if (item.userId != 0) {
                appName = String.format("%s (%s)", item.getAppName(), item.userId);
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
                sb.append("\n");
                sb.append(warningText);
                final ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(ContextCompat.getColor(requireActivity(), R.color.material_red_500));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    final TypefaceSpan typefaceSpan = new TypefaceSpan(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                    sb.setSpan(typefaceSpan, sb.length() - warningText.length(), sb.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                } else {
                    final StyleSpan styleSpan = new StyleSpan(Typeface.BOLD);
                    sb.setSpan(styleSpan, sb.length() - warningText.length(), sb.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                }
                sb.setSpan(foregroundColorSpan, sb.length() - warningText.length(), sb.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            }
            holder.appDescription.setText(sb);

            if (!isPick) {
                holder.root.setAlpha(moduleUtil.isModuleEnabled(item.packageName) ? 1.0f : .5f);
                holder.itemView.setOnClickListener(v -> getNavController().navigate(ModulesFragmentDirections.actionModulesFragmentToAppListFragment(item.packageName, item.userId)));
                holder.itemView.setOnLongClickListener(v -> {
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
                    if (RepoLoader.getInstance().getOnlineModule(item.packageName) == null) {
                        menu.removeItem(R.id.menu_repo);
                    }
                    if (item.userId == 0) {
                        var users = ConfigManager.getUsers();
                        if (users != null) {
                            for (var user : users) {
                                if (ModuleUtil.getInstance().getModule(item.packageName, user.id) == null) {
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

        public void setFilter(@NonNull Predicate<ModuleUtil.InstalledModule> filter) {
            this.customFilter = filter;
        }

        public void setOnPickListener(View.OnClickListener onPickListener) {
            this.onPickListener = onPickListener;
        }

        public List<ModuleUtil.InstalledModule> snapshot() {
            return new ArrayList<>(searchList);
        }

        public void refresh() {
            refresh(false);
        }

        public void refresh(boolean force) {
            if (force) moduleUtil.reloadInstalledModules();
            requireActivity().runOnUiThread(reloadModules);
        }

        private final Runnable reloadModules = new Runnable() {
            public void run() {
                var tmpList = moduleUtil.getModules().values().stream().filter(module -> user == null ? module.userId == 0 : module.userId == user.id).filter(customFilter).collect(Collectors.toCollection(ArrayList::new));
                Comparator<PackageInfo> cmp = AppHelper.getAppListComparator(0, pm);
                tmpList.sort((a, b) -> {
                    boolean aChecked = moduleUtil.isModuleEnabled(a.packageName);
                    boolean bChecked = moduleUtil.isModuleEnabled(b.packageName);
                    if (aChecked == bChecked) {
                        return cmp.compare(a.pkg, b.pkg);
                    } else if (aChecked) {
                        return -1;
                    } else {
                        return 1;
                    }
                });
                searchList.clear();
                searchList.addAll(tmpList);
                String queryStr = searchView != null ? searchView.getQuery().toString() : "";
                requireActivity().runOnUiThread(() -> getFilter().filter(queryStr));
            }
        };

        @Override
        public boolean isLoaded() {
            return isLoaded;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ConstraintLayout root;
            ImageView appIcon;
            TextView appName;
            TextView appDescription;
            TextView appVersion;
            MaterialCheckBox checkBox;

            ViewHolder(ItemModuleBinding binding) {
                super(binding.getRoot());
                root = binding.itemRoot;
                appIcon = binding.appIcon;
                appName = binding.appName;
                appDescription = binding.description;
                appVersion = binding.versionName;
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
                if (constraint.toString().isEmpty()) {
                    filtered.addAll(searchList);
                } else {
                    String filter = constraint.toString().toLowerCase();
                    for (ModuleUtil.InstalledModule info : searchList) {
                        if (lowercaseContains(info.getAppName(), filter) ||
                                lowercaseContains(info.packageName, filter) ||
                                lowercaseContains(info.getDescription(), filter)) {
                            filtered.add(info);
                        }
                    }
                }
                filterResults.values = filtered;
                filterResults.count = filtered.size();
                return filterResults;
            }

            @SuppressLint("NotifyDataSetChanged")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                showList.clear();
                //noinspection unchecked
                showList.addAll((List<ModuleUtil.InstalledModule>) results.values);
                isLoaded = true;
                notifyDataSetChanged();
            }
        }
    }
}
