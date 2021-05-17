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
import android.os.UserHandle;
import android.os.UserManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
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
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayoutMediator;

import org.lsposed.manager.ConfigManager;
import org.lsposed.manager.R;
import org.lsposed.manager.adapters.AppHelper;
import org.lsposed.manager.databinding.ActivityModuleDetailBinding;
import org.lsposed.manager.databinding.DialogRecyclerviewBinding;
import org.lsposed.manager.databinding.ItemModuleBinding;
import org.lsposed.manager.databinding.ItemRepoRecyclerviewBinding;
import org.lsposed.manager.repo.RepoLoader;
import org.lsposed.manager.ui.activity.base.BaseActivity;
import org.lsposed.manager.ui.widget.EmptyStateRecyclerView;
import org.lsposed.manager.util.GlideApp;
import org.lsposed.manager.util.LinearLayoutManagerFix;
import org.lsposed.manager.util.ModuleUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import rikka.core.res.ResourcesKt;
import rikka.insets.WindowInsetsHelperKt;
import rikka.recyclerview.RecyclerViewKt;
import rikka.widget.borderview.BorderRecyclerView;

public class ModulesActivity extends BaseActivity implements ModuleUtil.ModuleListener {

    protected ActivityModuleDetailBinding binding;
    protected SearchView searchView;
    private SearchView.OnQueryTextListener mSearchListener;
    private final PagerAdapter pagerAdapter = new PagerAdapter();
    private final ArrayList<ModuleAdapter> adapters = new ArrayList<>();

    private Handler workHandler;
    private PackageManager pm;
    private ModuleUtil moduleUtil;
    private ModuleUtil.InstalledModule selectedModule;
    private UserHandle selectedModuleUser;
    private UserManager userManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        HandlerThread workThread = new HandlerThread("ModulesActivity WorkHandler");
        workThread.start();
        workHandler = new Handler(workThread.getLooper());
        moduleUtil = ModuleUtil.getInstance();
        pm = getPackageManager();
        moduleUtil.addListener(this);
        userManager = getSystemService(UserManager.class);
        super.onCreate(savedInstanceState);
        binding = ActivityModuleDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setAppBar(binding.appBar, binding.toolbar);
        binding.getRoot().bringChildToFront(binding.appBar);
        binding.toolbar.setNavigationOnClickListener(view -> onBackPressed());
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }
        binding.viewPager.setAdapter(new PagerAdapter());
        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                BorderRecyclerView recyclerView = binding.viewPager.findViewWithTag(position);

                if (recyclerView != null) {
                    binding.appBar.setRaised(!recyclerView.getBorderViewDelegate().isShowingTopBorder());
                }
                if (position > 0) binding.fab.show();
                else binding.fab.hide();
            }
        });

        binding.fab.setOnClickListener(view -> {
            var pickAdaptor = new ModuleAdapter(0, null, true);
            var position = binding.viewPager.getCurrentItem();
            var snapshot = adapters.get(position).snapshot().stream().map(m -> m.packageName).collect(Collectors.toSet());
            var userId = adapters.get(position).getUserId();
            pickAdaptor.setFilter(m -> !snapshot.contains(m.packageName));
            pickAdaptor.refresh();
            var v = DialogRecyclerviewBinding.inflate(getLayoutInflater()).getRoot();
            v.setAdapter(pickAdaptor);
            v.setLayoutManager(new LinearLayoutManagerFix(ModulesActivity.this));
            var dialog = new AlertDialog.Builder(ModulesActivity.this)
                    .setTitle(getString(R.string.install_to_user, userId))
                    .setView(v)
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            pickAdaptor.setOnPickListener(picked -> {
                var module = (ModuleUtil.InstalledModule) picked.getTag();
                installModuleToUser(module, userId);
                dialog.dismiss();
            });
        });

        mSearchListener = new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapters.forEach(adapter -> {
                    adapter.getFilter().filter(query);
                });
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapters.forEach(adapter -> {
                    adapter.getFilter().filter(newText);
                });
                return false;
            }
        };
        if (ConfigManager.getXposedVersionName() == null) {
            Toast.makeText(this, R.string.lsposed_not_active, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        searchView.setOnQueryTextListener(mSearchListener);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        if (searchView.isIconified()) {
            super.onBackPressed();
        } else {
            searchView.setIconified(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        int[] userIds = ConfigManager.getUsers();
        if (userIds != null) {
            List<UserHandle> users = userManager.getUserProfiles();
            HashMap<Integer, UserHandle> handles = new HashMap<>();
            for (UserHandle handle : users) {
                handles.put(handle.hashCode(), handle);
            }
            if (userIds.length != adapters.size()) {
                adapters.clear();
                if (users.size() != 1) {
                    binding.viewPager.setUserInputEnabled(true);
                    ArrayList<String> titles = new ArrayList<>();
                    for (int userId : userIds) {
                        var adapter = new ModuleAdapter(userId, handles.get(userId));
                        adapter.setHasStableIds(true);
                        adapters.add(adapter);
                        titles.add(getString(R.string.user_title, userId));
                    }
                    new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> tab.setText(titles.get(position))).attach();
                    binding.tabLayout.setVisibility(View.VISIBLE);
                } else {
                    binding.viewPager.setUserInputEnabled(false);
                    var adapter = new ModuleAdapter(0, users.get(0));
                    adapter.setHasStableIds(true);
                    adapters.add(adapter);
                    binding.tabLayout.setVisibility(View.GONE);
                }
                pagerAdapter.notifyDataSetChanged();
            }
        }
        adapters.forEach(ModuleAdapter::refresh);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_modules, menu);
        return super.onCreateOptionsMenu(menu);
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
            adapters.forEach(adapter -> {
                adapter.refresh(true);
            });
        }
        return super.onOptionsItemSelected(item);
    }

    private void installModuleToUser(ModuleUtil.InstalledModule module, int userId) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.install_to_user, userId))
                .setMessage(getString(R.string.install_to_user_message, module.getAppName(), userId))
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                        workHandler.post(() -> {
                            var success = ConfigManager.installExistingPackageAsUser(module.packageName, userId);
                            runOnUiThread(() -> {
                                String text = success ? getString(R.string.module_installed, module.getAppName(), userId) : getString(R.string.module_install_failed);
                                if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                                    Snackbar.make(binding.snackbar, text, Snackbar.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(ModulesActivity.this, text, Toast.LENGTH_SHORT).show();
                                }
                            });
                            if (success)
                                moduleUtil.reloadSingleModule(module.packageName, userId);
                        }))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        ModuleUtil.InstalledModule module = ModuleUtil.getInstance().getModule(selectedModule.packageName, selectedModule.userId);
        if (module == null) {
            return false;
        }
        int itemId = item.getItemId();
        if (itemId == R.id.menu_launch) {
            String packageName = module.packageName;
            if (packageName == null) {
                return false;
            }
            Intent intent = AppHelper.getSettingsIntent(packageName, module.userId, pm);
            if (intent != null) {
                AppHelper.startActivityAsUser(this, intent, selectedModuleUser);
            } else {
                Snackbar.make(binding.snackbar, R.string.module_no_ui, Snackbar.LENGTH_LONG).show();
            }
            return true;
        } else if (itemId == R.id.menu_other_app) {
            var intent = new Intent(Intent.ACTION_SHOW_APP_INFO);
            intent.putExtra(Intent.EXTRA_PACKAGE_NAME, module.packageName);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        } else if (itemId == R.id.menu_app_info) {
            AppHelper.startActivityAsUser(this, (new Intent(ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", module.packageName, null))), selectedModuleUser);
            return true;
        } else if (itemId == R.id.menu_uninstall) {
            new AlertDialog.Builder(this)
                    .setTitle(module.getAppName())
                    .setMessage(R.string.module_uninstall_message)
                    .setPositiveButton(android.R.string.ok, (dialog, which) ->
                            workHandler.post(() -> {
                                boolean success = ConfigManager.uninstallPackage(module.packageName, module.userId);
                                runOnUiThread(() -> {
                                    String text = success ? getString(R.string.module_uninstalled, module.getAppName()) : getString(R.string.module_uninstall_failed);
                                    if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                                        Snackbar.make(binding.snackbar, text, Snackbar.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(ModulesActivity.this, text, Toast.LENGTH_SHORT).show();
                                    }
                                });
                                if (success)
                                    moduleUtil.reloadSingleModule(module.packageName, module.userId);
                            }))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return true;
        } else if (itemId == R.id.menu_repo) {
            Intent intent = new Intent();
            intent.setClass(this, RepoItemActivity.class);
            intent.putExtra("modulePackageName", module.packageName);
            intent.putExtra("moduleName", module.getAppName());
            startActivity(intent);
            return true;
        } else if (item.getGroupId() == 1) {
            installModuleToUser(module, itemId);
            return true;
        }
        return super.onContextItemSelected(item);
    }

    private class PagerAdapter extends RecyclerView.Adapter<PagerAdapter.ViewHolder> {

        @NonNull
        @Override
        public PagerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new PagerAdapter.ViewHolder(ItemRepoRecyclerviewBinding.inflate(getLayoutInflater(), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull PagerAdapter.ViewHolder holder, int position) {
            if (getItemCount() == 1) {
                WindowInsetsHelperKt.setInitialPadding(holder.recyclerView, 0, ResourcesKt.resolveDimensionPixelOffset(getTheme(), R.attr.actionBarSize, 0), 0, 0);
            }
            holder.recyclerView.setTag(position);
            holder.recyclerView.setAdapter(adapters.get(position));
            holder.recyclerView.setLayoutManager(new LinearLayoutManagerFix(ModulesActivity.this));
            holder.recyclerView.getBorderViewDelegate().setBorderVisibilityChangedListener((top, oldTop, bottom, oldBottom) -> binding.appBar.setRaised(!top));
            holder.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE && holder.getBindingAdapterPosition() > 0)
                        binding.fab.show();
                    else binding.fab.hide();
                    super.onScrollStateChanged(recyclerView, newState);
                }
            });
            RecyclerViewKt.fixEdgeEffect(holder.recyclerView, false, true);
            RecyclerViewKt.addFastScroller(holder.recyclerView, holder.itemView);
        }

        @Override
        public int getItemCount() {
            return adapters.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            BorderRecyclerView recyclerView;

            public ViewHolder(@NonNull ItemRepoRecyclerviewBinding binding) {
                super(binding.getRoot());
                recyclerView = binding.recyclerView;
            }
        }
    }

    private class ModuleAdapter extends EmptyStateRecyclerView.EmptyStateAdapter<ModuleAdapter.ViewHolder> implements Filterable {
        private final List<ModuleUtil.InstalledModule> searchList = new ArrayList<>();
        private final List<ModuleUtil.InstalledModule> showList = new ArrayList<>();
        private final int userId;
        private final UserHandle userHandle;
        private final boolean isPick;
        private boolean isLoaded;
        private View.OnClickListener onPickListener;

        private Predicate<ModuleUtil.InstalledModule> customFilter = m -> true;

        ModuleAdapter(int userId, UserHandle userHandle) {
            this(userId, userHandle, false);
        }

        ModuleAdapter(int userId, UserHandle userHandle, boolean isPick) {
            this.userId = userId;
            this.userHandle = userHandle;
            this.isPick = isPick;
        }

        public int getUserId() {
            return userId;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(ItemModuleBinding.inflate(getLayoutInflater(), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
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
                final ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(ContextCompat.getColor(ModulesActivity.this, R.color.material_red_500));
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

            holder.itemView.setOnCreateContextMenuListener((menu, v, menuInfo) -> {
                getMenuInflater().inflate(R.menu.context_menu_modules, menu);
                menu.setHeaderTitle(item.getAppName());
                Intent intent = AppHelper.getSettingsIntent(item.packageName, item.userId, pm);
                if (intent == null) {
                    menu.removeItem(R.id.menu_launch);
                }
                if (RepoLoader.getInstance().getOnlineModule(item.packageName) == null) {
                    menu.removeItem(R.id.menu_repo);
                }
                if (userHandle == null) {
                    menu.removeItem(R.id.menu_app_info);
                }
                if (item.userId == 0) {
                    for (int profile : ConfigManager.getUsers()) {
                        if (ModuleUtil.getInstance().getModule(item.packageName, profile) == null) {
                            menu.add(1, profile, 0, getString(R.string.install_to_user, profile));
                        }
                    }
                }
            });

            if (!isPick) {
                holder.root.setAlpha(moduleUtil.isModuleEnabled(item.packageName) ? 1.0f : .5f);
                holder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(ModulesActivity.this, AppListActivity.class);
                    intent.putExtra("modulePackageName", item.packageName);
                    intent.putExtra("moduleUserId", item.userId);
                    intent.putExtra("userHandle", userHandle);
                    startActivity(intent);
                });

                holder.itemView.setOnLongClickListener(v -> {
                    selectedModule = item;
                    selectedModuleUser = userHandle;
                    return false;
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
            return new ApplicationFilter();
        }

        public void setFilter(@NonNull Predicate<ModuleUtil.InstalledModule> filter) {
            this.customFilter = filter;
        }

        public void setOnPickListener(View.OnClickListener onPickListener) {
            this.onPickListener = onPickListener;
        }

        public List<ModuleUtil.InstalledModule> snapshot() {
            List<ModuleUtil.InstalledModule> list = new ArrayList<>();
            list.addAll(searchList);
            return list;
        }

        public void refresh() {
            refresh(false);
        }

        public void refresh(boolean force) {
            if (force) moduleUtil.reloadInstalledModules();
            runOnUiThread(reloadModules);
        }

        private final Runnable reloadModules = new Runnable() {
            public void run() {
                searchList.clear();
                searchList.addAll(moduleUtil.getModules().values().stream().filter(module -> module.userId == userId).filter(customFilter).collect(Collectors.toList()));
                Comparator<PackageInfo> cmp = AppHelper.getAppListComparator(0, pm);
                searchList.sort((a, b) -> {
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
                String queryStr = searchView != null ? searchView.getQuery().toString() : "";
                runOnUiThread(() -> getFilter().filter(queryStr));
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
