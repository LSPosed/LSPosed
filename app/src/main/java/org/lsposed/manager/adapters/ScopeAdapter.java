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

package org.lsposed.manager.adapters;

import static android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
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
import android.widget.CompoundButton;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.checkbox.MaterialCheckBox;

import org.lsposed.lspd.models.Application;
import org.lsposed.manager.App;
import org.lsposed.manager.BuildConfig;
import org.lsposed.manager.ConfigManager;
import org.lsposed.manager.R;
import org.lsposed.manager.databinding.ItemMasterSwitchBinding;
import org.lsposed.manager.databinding.ItemModuleBinding;
import org.lsposed.manager.ui.dialog.BlurBehindDialogBuilder;
import org.lsposed.manager.ui.fragment.AppListFragment;
import org.lsposed.manager.ui.fragment.CompileDialogFragment;
import org.lsposed.manager.ui.widget.EmptyStateRecyclerView;
import org.lsposed.manager.util.GlideApp;
import org.lsposed.manager.util.ModuleUtil;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import rikka.core.util.ResourceUtils;
import rikka.material.app.LocaleDelegate;
import rikka.widget.mainswitchbar.MainSwitchBar;
import rikka.widget.mainswitchbar.OnMainSwitchChangeListener;

public class ScopeAdapter extends EmptyStateRecyclerView.EmptyStateAdapter<ScopeAdapter.ViewHolder> implements Filterable {

    private final Activity activity;
    private final AppListFragment fragment;
    private final PackageManager pm;
    private final SharedPreferences preferences;
    private final ModuleUtil moduleUtil;

    private final ModuleUtil.InstalledModule module;

    private Set<ApplicationWithEquals> recommendedList = new HashSet<>();
    private Set<ApplicationWithEquals> checkedList = new HashSet<>();
    private List<AppInfo> searchList = new ArrayList<>();
    private List<AppInfo> showList = new ArrayList<>();
    private List<String> denyList = new ArrayList<>();

    public RecyclerView.Adapter<RecyclerView.ViewHolder> switchAdaptor = new RecyclerView.Adapter<>() {
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(ItemMasterSwitchBinding.inflate(activity.getLayoutInflater(), parent, false).masterSwitch) {
            };
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            var mainSwitchBar = (MainSwitchBar) holder.itemView;
            mainSwitchBar.setChecked(enabled);
            mainSwitchBar.addOnSwitchChangeListener(switchBarOnCheckedChangeListener);
        }

        @Override
        public int getItemCount() {
            return 1;
        }
    };

    private final OnMainSwitchChangeListener switchBarOnCheckedChangeListener = new OnMainSwitchChangeListener() {
        @Override
        public void onSwitchChanged(Switch view, boolean isChecked) {
            enabled = isChecked;
            if (!moduleUtil.setModuleEnabled(module.packageName, isChecked)) {
                view.setChecked(!isChecked);
                enabled = !isChecked;
            }
            var tmpChkList = new HashSet<>(checkedList);
            if (isChecked && !tmpChkList.isEmpty() && !ConfigManager.setModuleScope(module.packageName, module.legacy, tmpChkList)) {
                view.setChecked(false);
                enabled = false;
            }
            fragment.runOnUiThread(ScopeAdapter.this::notifyDataSetChanged);
        }
    };

    private ApplicationInfo selectedApplicationInfo;
    private boolean isLoaded = false;
    private boolean enabled = true;

    public ScopeAdapter(AppListFragment fragment, ModuleUtil.InstalledModule module) {
        this.fragment = fragment;
        this.activity = fragment.requireActivity();
        this.module = module;
        moduleUtil = ModuleUtil.getInstance();
        preferences = App.getPreferences();
        pm = activity.getPackageManager();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemModuleBinding.inflate(activity.getLayoutInflater(), parent, false));
    }

    private boolean shouldHideApp(PackageInfo info, ApplicationWithEquals app, HashSet<ApplicationWithEquals> tmpChkList) {
        if (info.packageName.equals("system")) {
            return false;
        }
        if (tmpChkList.contains(app)) {
            return false;
        }
        if (preferences.getBoolean("filter_denylist", false)) {
            if (denyList.contains(info.packageName)) {
                return true;
            }
        }
        if (preferences.getBoolean("filter_modules", true)) {
            if (ModuleUtil.getInstance().getModule(info.packageName, info.applicationInfo.uid / App.PER_USER_RANGE) != null) {
                return true;
            }
        }
        if (preferences.getBoolean("filter_games", true)) {
            if (info.applicationInfo.category == ApplicationInfo.CATEGORY_GAME) {
                return true;
            }
            //noinspection deprecation
            if ((info.applicationInfo.flags & ApplicationInfo.FLAG_IS_GAME) != 0) {
                return true;
            }
        }
        return preferences.getBoolean("filter_system_apps", true) && (info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }

    private int sortApps(AppInfo x, AppInfo y) {
        Comparator<PackageInfo> comparator = AppHelper.getAppListComparator(preferences.getInt("list_sort", 0), pm);
        Comparator<AppInfo> frameworkComparator = (a, b) -> {
            if (a.packageName.equals("system") == b.packageName.equals("system")) {
                return comparator.compare(a.packageInfo, b.packageInfo);
            } else if (a.packageName.equals("system")) {
                return -1;
            } else {
                return 1;
            }
        };
        Comparator<AppInfo> recommendedComparator = (a, b) -> {
            boolean aRecommended = !recommendedList.isEmpty() && recommendedList.contains(a.application);
            boolean bRecommended = !recommendedList.isEmpty() && recommendedList.contains(b.application);
            if (aRecommended == bRecommended) {
                return frameworkComparator.compare(a, b);
            } else if (aRecommended) {
                return -1;
            } else {
                return 1;
            }
        };
        boolean aChecked = checkedList.contains(x.application);
        boolean bChecked = checkedList.contains(y.application);
        if (aChecked == bChecked) {
            return recommendedComparator.compare(x, y);
        } else if (aChecked) {
            return -1;
        } else {
            return 1;
        }
    }

    private void checkRecommended() {
        if (!enabled) {
            fragment.showHint(R.string.module_is_not_activated_yet, false);
            return;
        }
        fragment.runAsync(() -> {
            var tmpChkList = new HashSet<>(checkedList);
            tmpChkList.removeIf(i -> i.userId == module.userId);
            tmpChkList.addAll(recommendedList);
            ConfigManager.setModuleScope(module.packageName, module.legacy, tmpChkList);
            checkedList = tmpChkList;
            fragment.runOnUiThread(this::notifyDataSetChanged);
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void setLoaded(List<AppInfo> list, boolean loaded) {
        fragment.runOnUiThread(() -> {
            if (list != null) showList = list;
            isLoaded = loaded;
            notifyDataSetChanged();
        });
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.use_recommended) {
            if (!checkedList.isEmpty()) {
                new BlurBehindDialogBuilder(activity, R.style.ThemeOverlay_MaterialAlertDialog_Centered_FullWidthButtons)
                        .setMessage(R.string.use_recommended_message)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> checkRecommended())
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            } else {
                checkRecommended();
            }
            return true;
        } else if (itemId == R.id.item_filter_system) {
            item.setChecked(!item.isChecked());
            preferences.edit().putBoolean("filter_system_apps", item.isChecked()).apply();
        } else if (itemId == R.id.item_filter_games) {
            item.setChecked(!item.isChecked());
            preferences.edit().putBoolean("filter_games", item.isChecked()).apply();
        } else if (itemId == R.id.item_filter_modules) {
            item.setChecked(!item.isChecked());
            preferences.edit().putBoolean("filter_modules", item.isChecked()).apply();
        } else if (itemId == R.id.item_filter_denylist) {
            item.setChecked(!item.isChecked());
            preferences.edit().putBoolean("filter_denylist", item.isChecked()).apply();
        } else if (itemId == R.id.backup) {
            LocalDateTime now = LocalDateTime.now();
            try {
                fragment.backupLauncher.launch(String.format(LocaleDelegate.getDefaultLocale(),
                        "%s_%s.lsp", module.getAppName(), now.toString()));
                return true;
            } catch (ActivityNotFoundException e) {
                fragment.showHint(R.string.enable_documentui, true);
                return false;
            }
        } else if (itemId == R.id.restore) {
            try {
                fragment.restoreLauncher.launch(new String[]{"*/*"});
                return true;
            } catch (ActivityNotFoundException e) {
                fragment.showHint(R.string.enable_documentui, true);
                return false;
            }
        } else if (!AppHelper.onOptionsItemSelected(item, preferences)) {
            return false;
        }
        refresh();
        return true;
    }

    public boolean onContextItemSelected(@NonNull MenuItem item) {
        var info = selectedApplicationInfo;
        if (info == null) {
            return false;
        }
        int itemId = item.getItemId();
        if (itemId == R.id.menu_launch) {
            Intent launchIntent = AppHelper.getLaunchIntentForPackage(info.packageName, info.uid / App.PER_USER_RANGE);
            if (launchIntent != null) {
                ConfigManager.startActivityAsUserWithFeature(launchIntent, module.userId);
            }
        } else if (itemId == R.id.menu_compile_speed) {
            CompileDialogFragment.speed(fragment.getChildFragmentManager(), info);
        } else if (itemId == R.id.menu_other_app) {
            var intent = new Intent(Intent.ACTION_SHOW_APP_INFO);
            intent.putExtra(Intent.EXTRA_PACKAGE_NAME, module.packageName);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ConfigManager.startActivityAsUserWithFeature(intent, module.userId);
        } else if (itemId == R.id.menu_app_info) {
            ConfigManager.startActivityAsUserWithFeature(new Intent(ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", info.packageName, null)), module.userId);
        } else if (itemId == R.id.menu_force_stop) {
            if (info.packageName.equals("system")) {
                new BlurBehindDialogBuilder(activity, R.style.ThemeOverlay_MaterialAlertDialog_Centered_FullWidthButtons)
                        .setTitle(R.string.reboot)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> ConfigManager.reboot())
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            } else {
                new BlurBehindDialogBuilder(activity, R.style.ThemeOverlay_MaterialAlertDialog_Centered_FullWidthButtons)
                        .setTitle(R.string.force_stop_dlg_title)
                        .setMessage(R.string.force_stop_dlg_text)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> ConfigManager.forceStopPackage(info.packageName, info.uid / 100000))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        } else {
            return false;
        }
        return true;
    }

    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        List<String> scopeList = module.getScopeList();
        if (scopeList == null || scopeList.isEmpty()) {
            menu.removeItem(R.id.use_recommended);
        }
        menu.findItem(R.id.item_filter_system).setChecked(preferences.getBoolean("filter_system_apps", true));
        menu.findItem(R.id.item_filter_games).setChecked(preferences.getBoolean("filter_games", true));
        menu.findItem(R.id.item_filter_modules).setChecked(preferences.getBoolean("filter_modules", true));
        menu.findItem(R.id.item_filter_denylist).setChecked(preferences.getBoolean("filter_denylist", false));
        switch (preferences.getInt("list_sort", 0)) {
            case 7 -> {
                menu.findItem(R.id.item_sort_by_update_time).setChecked(true);
                menu.findItem(R.id.reverse).setChecked(true);
            }
            case 6 -> menu.findItem(R.id.item_sort_by_update_time).setChecked(true);
            case 5 -> {
                menu.findItem(R.id.item_sort_by_install_time).setChecked(true);
                menu.findItem(R.id.reverse).setChecked(true);
            }
            case 4 -> menu.findItem(R.id.item_sort_by_install_time).setChecked(true);
            case 3 -> {
                menu.findItem(R.id.item_sort_by_package_name).setChecked(true);
                menu.findItem(R.id.reverse).setChecked(true);
            }
            case 2 -> menu.findItem(R.id.item_sort_by_package_name).setChecked(true);
            case 1 -> {
                menu.findItem(R.id.item_sort_by_name).setChecked(true);
                menu.findItem(R.id.reverse).setChecked(true);
            }
            case 0 -> menu.findItem(R.id.item_sort_by_name).setChecked(true);
        }
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        if (holder.checkbox != null) {
            holder.checkbox.setOnCheckedChangeListener(null);
        }
        super.onViewRecycled(holder);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppInfo appInfo = showList.get(position);
        boolean deny = denyList.contains(appInfo.packageName);
        holder.root.setAlpha(!deny && enabled ? 1.0f : .5f);
        boolean system = appInfo.packageName.equals("system");
        CharSequence appName;
        int userId = appInfo.applicationInfo.uid / App.PER_USER_RANGE;
        appName = system ? activity.getString(R.string.android_framework) : appInfo.label;
        holder.appName.setText(appName);
        GlideApp.with(holder.appIcon).load(appInfo.packageInfo).into(new CustomTarget<Drawable>() {
            @Override
            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                holder.appIcon.setImageDrawable(resource);
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {

            }

            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                holder.appIcon.setImageDrawable(pm.getDefaultActivityIcon());
            }
        });
        if (system) {
            //noinspection SetTextI18n
            holder.appPackageName.setText("system");
            holder.appVersionName.setVisibility(View.GONE);
        } else {
            holder.appVersionName.setVisibility(View.VISIBLE);
            holder.appPackageName.setText(appInfo.packageName);
        }
        holder.appPackageName.setVisibility(View.VISIBLE);
        holder.appVersionName.setText(activity.getString(R.string.app_version, appInfo.packageInfo.versionName));
        var sb = new SpannableStringBuilder();
        if (!recommendedList.isEmpty() && recommendedList.contains(appInfo.application)) {
            String recommended = activity.getString(R.string.requested_by_module);
            sb.append(recommended);
            final ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(ResourceUtils.resolveColor(activity.getTheme(), com.google.android.material.R.attr.colorPrimary));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                final TypefaceSpan typefaceSpan = new TypefaceSpan(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                sb.setSpan(typefaceSpan, sb.length() - recommended.length(), sb.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            } else {
                final StyleSpan styleSpan = new StyleSpan(Typeface.BOLD);
                sb.setSpan(styleSpan, sb.length() - recommended.length(), sb.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            }
            sb.setSpan(foregroundColorSpan, sb.length() - recommended.length(), sb.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }
        if (deny) {
            if (sb.length() != 0) sb.append("\n");
            String denylist = activity.getString(R.string.deny_list_info);
            sb.append(denylist);
            final ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(ResourceUtils.resolveColor(activity.getTheme(), com.google.android.material.R.attr.colorError));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                final TypefaceSpan typefaceSpan = new TypefaceSpan(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                sb.setSpan(typefaceSpan, sb.length() - denylist.length(), sb.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            } else {
                final StyleSpan styleSpan = new StyleSpan(Typeface.BOLD);
                sb.setSpan(styleSpan, sb.length() - denylist.length(), sb.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            }
            sb.setSpan(foregroundColorSpan, sb.length() - denylist.length(), sb.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }
        if (sb.length() == 0) {
            holder.hint.setVisibility(View.GONE);
        } else {
            holder.hint.setText(sb);
            holder.hint.setVisibility(View.VISIBLE);
        }

        holder.itemView.setOnCreateContextMenuListener((menu, v, menuInfo) -> {
            activity.getMenuInflater().inflate(R.menu.menu_app_item, menu);
            menu.setHeaderTitle(appName);
            Intent launchIntent = AppHelper.getLaunchIntentForPackage(appInfo.packageName, userId);
            if (launchIntent == null) {
                menu.removeItem(R.id.menu_launch);
            }
            if (system) {
                menu.findItem(R.id.menu_force_stop).setTitle(R.string.reboot);
                menu.removeItem(R.id.menu_compile_speed);
                menu.removeItem(R.id.menu_other_app);
                menu.removeItem(R.id.menu_app_info);
            }
        });

        holder.checkbox.setChecked(checkedList.contains(appInfo.application));

        holder.checkbox.setOnCheckedChangeListener((v, isChecked) -> onCheckedChange(v, isChecked, appInfo));

        holder.itemView.setOnClickListener(v -> {
            if (enabled) holder.checkbox.toggle();
        });
        holder.itemView.setOnLongClickListener(v -> {
            fragment.searchView.clearFocus();
            selectedApplicationInfo = appInfo.applicationInfo;
            return false;
        });
    }

    @Override
    public long getItemId(int position) {
        PackageInfo info = showList.get(position).packageInfo;
        return (info.packageName + "!" + info.applicationInfo.uid / App.PER_USER_RANGE).hashCode();
    }

    @Override
    public Filter getFilter() {
        return new ApplicationFilter();
    }

    @Override
    public int getItemCount() {
        return showList.size();
    }

    public void refresh() {
        refresh(false);
    }

    public void refresh(boolean force) {
        setLoaded(null, false);
        enabled = moduleUtil.isModuleEnabled(module.packageName);
        fragment.runAsync(() -> {
            List<PackageInfo> appList = AppHelper.getAppList(force);
            denyList = AppHelper.getDenyList(force);
            var tmpRecList = new HashSet<ApplicationWithEquals>();
            var tmpChkList = new HashSet<>(ConfigManager.getModuleScope(module.packageName));
            final var tmpList = new ArrayList<AppInfo>();
            final HashSet<ApplicationWithEquals> installedList = new HashSet<>();
            List<String> scopeList = module.getScopeList();
            boolean emptyCheckedList = tmpChkList.isEmpty();
            appList.parallelStream().forEach(info -> {
                int userId = info.applicationInfo.uid / App.PER_USER_RANGE;
                String packageName = info.packageName;
                if (packageName.equals("system") && userId != 0 ||
                        packageName.equals(module.packageName) ||
                        packageName.equals(BuildConfig.APPLICATION_ID)) {
                    return;
                }

                ApplicationWithEquals application = new ApplicationWithEquals(packageName, userId);

                synchronized (installedList) {
                    installedList.add(application);
                }

                if (userId != module.userId) {
                    return;
                }

                if (scopeList != null && scopeList.contains(packageName)) {
                    synchronized (tmpRecList) {
                        tmpRecList.add(application);
                    }
                    if (emptyCheckedList) {
                        synchronized (tmpChkList) {
                            tmpChkList.add(application);
                        }
                    }
                } else if (shouldHideApp(info, application, tmpChkList)) {
                    return;
                }

                AppInfo appInfo = new AppInfo();
                appInfo.packageInfo = info;
                appInfo.label = AppHelper.getAppLabel(info, pm);
                appInfo.application = application;
                appInfo.packageName = info.packageName;
                appInfo.applicationInfo = info.applicationInfo;
                synchronized (tmpList) {
                    tmpList.add(appInfo);
                }
            });
            tmpChkList.retainAll(installedList);
            checkedList = tmpChkList;
            recommendedList = tmpRecList;
            searchList = tmpList.parallelStream().sorted(this::sortApps).collect(Collectors.toList());

            String queryStr = fragment.searchView != null ? fragment.searchView.getQuery().toString() : "";

            fragment.runOnUiThread(() -> getFilter().filter(queryStr));
        });
    }

    protected void onCheckedChange(CompoundButton buttonView, boolean isChecked, AppInfo appInfo) {
        var tmpChkList = new HashSet<>(checkedList);
        if (isChecked) {
            tmpChkList.add(appInfo.application);
        } else {
            tmpChkList.remove(appInfo.application);
        }
        if (!ConfigManager.setModuleScope(module.packageName, module.legacy, tmpChkList)) {
            fragment.showHint(R.string.failed_to_save_scope_list, true);
            if (!isChecked) {
                tmpChkList.add(appInfo.application);
            } else {
                tmpChkList.remove(appInfo.application);
            }
            buttonView.setChecked(!isChecked);
        } else if (appInfo.packageName.equals("system")) {
            fragment.showHint(R.string.reboot_required, true, R.string.reboot, v -> ConfigManager.reboot());
        } else if (denyList.contains(appInfo.packageName)) {
            fragment.showHint(activity.getString(R.string.deny_list, appInfo.label), true);
        }
        checkedList = tmpChkList;
    }

    @Override
    public boolean isLoaded() {
        return isLoaded;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ConstraintLayout root;
        ImageView appIcon;
        TextView appName;
        TextView appPackageName;
        TextView appVersionName;
        TextView hint;
        MaterialCheckBox checkbox;

        ViewHolder(ItemModuleBinding binding) {
            super(binding.getRoot());
            root = binding.itemRoot;
            appIcon = binding.appIcon;
            appName = binding.appName;
            appPackageName = binding.appPackageName;
            appVersionName = binding.appVersionName;
            checkbox = binding.checkbox;
            hint = binding.hint;
            checkbox.setVisibility(View.VISIBLE);
        }
    }

    private class ApplicationFilter extends Filter {

        private boolean lowercaseContains(String s, String filter) {
            return !TextUtils.isEmpty(s) && s.toLowerCase().contains(filter);
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults filterResults = new FilterResults();
            List<AppInfo> filtered = new ArrayList<>();
            String filter = constraint.toString().toLowerCase();
            for (AppInfo info : searchList) {
                if (lowercaseContains(info.label.toString(), filter)
                        || lowercaseContains(info.packageName, filter)) {
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
            setLoaded((List<AppInfo>) results.values, true);
        }
    }

    public SearchView.OnQueryTextListener getSearchListener() {
        return new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                getFilter().filter(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                getFilter().filter(query);
                return true;
            }
        };
    }

    public void onBackPressed() {
        fragment.searchView.clearFocus();
        if (isLoaded && enabled && checkedList.isEmpty()) {
            var builder = new BlurBehindDialogBuilder(activity, R.style.ThemeOverlay_MaterialAlertDialog_Centered_FullWidthButtons);
            builder.setMessage(!recommendedList.isEmpty() ? R.string.no_scope_selected_has_recommended : R.string.no_scope_selected);
            if (!recommendedList.isEmpty()) {
                builder.setPositiveButton(android.R.string.ok, (dialog, which) -> checkRecommended());
            } else {
                builder.setPositiveButton(android.R.string.cancel, null);
            }
            builder.setNegativeButton(!recommendedList.isEmpty() ? android.R.string.cancel : android.R.string.ok, (dialog, which) -> {
                moduleUtil.setModuleEnabled(module.packageName, false);
                Toast.makeText(activity, activity.getString(R.string.module_disabled_no_selection, module.getAppName()), Toast.LENGTH_LONG).show();
                fragment.navigateUp();
            });
            builder.show();
        } else {
            fragment.navigateUp();
        }
    }

    public static class AppInfo {
        public PackageInfo packageInfo;
        public ApplicationWithEquals application;
        public ApplicationInfo applicationInfo;
        public String packageName;
        public CharSequence label = null;
    }

    public static class ApplicationWithEquals extends Application {
        public ApplicationWithEquals(String packageName, int userId) {
            this.packageName = packageName;
            this.userId = userId;
        }

        public ApplicationWithEquals(Application application) {
            packageName = application.packageName;
            userId = application.userId;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (!(obj instanceof Application)) {
                return false;
            }
            return packageName.equals(((Application) obj).packageName) && userId == ((Application) obj).userId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(packageName, userId);
        }
    }
}
