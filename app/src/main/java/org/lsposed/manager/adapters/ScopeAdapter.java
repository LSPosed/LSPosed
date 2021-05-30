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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
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
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.snackbar.Snackbar;

import org.lsposed.lspd.models.Application;
import org.lsposed.manager.App;
import org.lsposed.manager.BuildConfig;
import org.lsposed.manager.ConfigManager;
import org.lsposed.manager.R;
import org.lsposed.manager.databinding.ItemModuleBinding;
import org.lsposed.manager.ui.fragment.AppListFragment;
import org.lsposed.manager.ui.fragment.CompileDialogFragment;
import org.lsposed.manager.util.GlideApp;
import org.lsposed.manager.util.ModuleUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

import rikka.core.res.ResourcesKt;
import rikka.widget.switchbar.SwitchBar;

@SuppressLint("NotifyDataSetChanged")
public class ScopeAdapter extends RecyclerView.Adapter<ScopeAdapter.ViewHolder> implements Filterable, Handler.Callback {

    private final Activity activity;
    private final AppListFragment fragment;
    private final PackageManager pm;
    private final SharedPreferences preferences;
    private final Handler loadAppListHandler;
    private final ModuleUtil moduleUtil;

    private final ModuleUtil.InstalledModule module;

    private final HashSet<ApplicationWithEquals> recommendedList = new HashSet<>();
    private final HashSet<ApplicationWithEquals> checkedList = new HashSet<>();
    private final ConcurrentLinkedQueue<AppInfo> searchList = new ConcurrentLinkedQueue<>();
    private final List<AppInfo> showList = new ArrayList<>();

    private final SwitchBar.OnCheckedChangeListener switchBarOnCheckedChangeListener = new SwitchBar.OnCheckedChangeListener() {
        @Override
        public boolean onCheckedChanged(SwitchBar view, boolean isChecked) {
            if (!moduleUtil.setModuleEnabled(module.packageName, isChecked)) {
                return false;
            }
            enabled = isChecked;
            notifyDataSetChanged();
            return true;
        }
    };
    private final Runnable dataReadyRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (this) {
                fragment.binding.progress.setIndeterminate(false);
                fragment.binding.swipeRefreshLayout.setRefreshing(false);
                String queryStr = fragment.searchView != null ? fragment.searchView.getQuery().toString() : "";
                getFilter().filter(queryStr);
                this.notify();
            }
        }
    };

    private ApplicationInfo selectedInfo;
    private boolean refreshing = false;
    private boolean enabled = true;

    public ScopeAdapter(AppListFragment fragment, ModuleUtil.InstalledModule module) {
        this.fragment = fragment;
        this.activity = fragment.requireActivity();
        this.module = module;
        moduleUtil = ModuleUtil.getInstance();
        HandlerThread handlerThread = new HandlerThread("appList");
        handlerThread.start();
        loadAppListHandler = new Handler(handlerThread.getLooper(), this);
        preferences = App.getPreferences();
        pm = activity.getPackageManager();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemModuleBinding.inflate(activity.getLayoutInflater(), parent, false));
    }

    private boolean shouldHideApp(PackageInfo info, ApplicationWithEquals app) {
        if (info.packageName.equals("android")) {
            return false;
        }
        if (checkedList.contains(app)) {
            return false;
        }
        if (preferences.getBoolean("filter_modules", true)) {
            if (info.applicationInfo.metaData != null && info.applicationInfo.metaData.containsKey("xposedminversion")) {
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
        if ((info.applicationInfo.flags & ApplicationInfo.FLAG_HAS_CODE) == 0) {
            return true;
        }
        return preferences.getBoolean("filter_system_apps", true) && (info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }

    private void sortApps(List<AppInfo> list) {
        Comparator<PackageInfo> comparator = AppHelper.getAppListComparator(preferences.getInt("list_sort", 0), pm);
        Comparator<AppInfo> frameworkComparator = (a, b) -> {
            if (a.packageName.equals("android") == b.packageName.equals("android")) {
                return comparator.compare(a.packageInfo, b.packageInfo);
            } else if (a.packageName.equals("android")) {
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
        list.sort((a, b) -> {
            boolean aChecked = checkedList.contains(a.application);
            boolean bChecked = checkedList.contains(b.application);
            if (aChecked == bChecked) {
                return recommendedComparator.compare(a, b);
            } else if (aChecked) {
                return -1;
            } else {
                return 1;
            }
        });
    }

    private void checkRecommended() {
        checkedList.clear();
        checkedList.addAll(recommendedList);
        ConfigManager.setModuleScope(module.packageName, checkedList);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.use_recommended) {
            if (!checkedList.isEmpty()) {
                new AlertDialog.Builder(activity)
                        .setMessage(R.string.use_recommended_message)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            checkRecommended();
                            notifyDataSetChanged();
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            } else {
                checkRecommended();
                notifyDataSetChanged();
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
        } else if (itemId == R.id.menu_launch) {
            Intent launchIntent = AppHelper.getSettingsIntent(module.packageName, module.userId);
            if (launchIntent != null) {
                ConfigManager.startActivityAsUserWithFeature(launchIntent, module.userId);
            } else {
                fragment.makeSnackBar(R.string.module_no_ui, Snackbar.LENGTH_LONG);
            }
            return true;
        } else if (itemId == R.id.backup) {
            Calendar now = Calendar.getInstance();
            fragment.backupLauncher.launch(String.format(Locale.US,
                    "%s_%04d%02d%02d_%02d%02d%02d.lsp",
                    module.getAppName(),
                    now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1,
                    now.get(Calendar.DAY_OF_MONTH), now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE), now.get(Calendar.SECOND)));
            return true;
        } else if (itemId == R.id.restore) {
            fragment.restoreLauncher.launch(new String[]{"*/*"});
            return true;
        } else if (!AppHelper.onOptionsItemSelected(item, preferences)) {
            return false;
        }
        refresh(false);
        return true;
    }

    public boolean onContextItemSelected(@NonNull MenuItem item) {
        ApplicationInfo info = selectedInfo;
        if (info == null) {
            return false;
        }
        int itemId = item.getItemId();
        if (itemId == R.id.menu_launch) {
            Intent launchIntent = AppHelper.getLaunchIntentForPackage(info.packageName, info.uid / 100000);
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
            if (info.packageName.equals("android")) {
                ConfigManager.reboot(false, null, false);
            } else {
                new AlertDialog.Builder(activity)
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
        Intent intent = AppHelper.getSettingsIntent(module.packageName, module.userId);
        if (intent == null) {
            menu.removeItem(R.id.menu_launch);
        }
        List<String> scopeList = module.getScopeList();
        if (scopeList == null || scopeList.isEmpty()) {
            menu.removeItem(R.id.use_recommended);
        }
        menu.findItem(R.id.item_filter_system).setChecked(preferences.getBoolean("filter_system_apps", true));
        menu.findItem(R.id.item_filter_games).setChecked(preferences.getBoolean("filter_games", true));
        menu.findItem(R.id.item_filter_modules).setChecked(preferences.getBoolean("filter_modules", true));
        switch (preferences.getInt("list_sort", 0)) {
            case 7:
                menu.findItem(R.id.item_sort_by_update_time_reverse).setChecked(true);
                break;
            case 6:
                menu.findItem(R.id.item_sort_by_update_time).setChecked(true);
                break;
            case 5:
                menu.findItem(R.id.item_sort_by_install_time_reverse).setChecked(true);
                break;
            case 4:
                menu.findItem(R.id.item_sort_by_install_time).setChecked(true);
                break;
            case 3:
                menu.findItem(R.id.item_sort_by_package_name_reverse).setChecked(true);
                break;
            case 2:
                menu.findItem(R.id.item_sort_by_package_name).setChecked(true);
                break;
            case 1:
                menu.findItem(R.id.item_sort_by_name_reverse).setChecked(true);
                break;
            case 0:
                menu.findItem(R.id.item_sort_by_name).setChecked(true);
                break;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.root.setAlpha(enabled ? 1.0f : .5f);
        AppInfo appInfo = showList.get(position);
        boolean android = appInfo.packageName.equals("android");
        CharSequence appName;
        int userId = appInfo.applicationInfo.uid / 100000;
        appName = android ? activity.getString(R.string.android_framework) : appInfo.label;
        holder.appName.setText(appName);
        GlideApp.with(holder.appIcon)
                .load(appInfo.packageInfo)
                .into(new CustomTarget<Drawable>() {
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
        SpannableStringBuilder sb = new SpannableStringBuilder(android ? "" : activity.getString(R.string.app_description, appInfo.packageName, appInfo.packageInfo.versionName));
        holder.appDescription.setVisibility(View.VISIBLE);
        if (!recommendedList.isEmpty() && recommendedList.contains(appInfo.application)) {
            if (!android) sb.append("\n");
            String recommended = activity.getString(R.string.requested_by_module);
            sb.append(recommended);
            final ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(ResourcesKt.resolveColor(activity.getTheme(), R.attr.colorAccent));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                final TypefaceSpan typefaceSpan = new TypefaceSpan(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                sb.setSpan(typefaceSpan, sb.length() - recommended.length(), sb.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            } else {
                final StyleSpan styleSpan = new StyleSpan(Typeface.BOLD);
                sb.setSpan(styleSpan, sb.length() - recommended.length(), sb.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            }
            sb.setSpan(foregroundColorSpan, sb.length() - recommended.length(), sb.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        } else if (android) {
            holder.appDescription.setVisibility(View.GONE);
        }
        holder.appDescription.setText(sb);

        holder.itemView.setOnCreateContextMenuListener((menu, v, menuInfo) -> {
            activity.getMenuInflater().inflate(R.menu.menu_app_item, menu);
            menu.setHeaderTitle(appName);
            Intent launchIntent = AppHelper.getLaunchIntentForPackage(appInfo.packageName, userId);
            if (launchIntent == null) {
                menu.removeItem(R.id.menu_launch);
            }
            if (android) {
                menu.findItem(R.id.menu_force_stop).setTitle(R.string.reboot);
                menu.removeItem(R.id.menu_compile_speed);
            }
        });

        holder.checkbox.setOnCheckedChangeListener(null);
        holder.checkbox.setChecked(checkedList.contains(appInfo.application));

        holder.checkbox.setOnCheckedChangeListener((v, isChecked) -> onCheckedChange(v, isChecked, appInfo));
        holder.itemView.setOnClickListener(v -> {
            if (enabled) holder.checkbox.toggle();
        });
        holder.itemView.setOnLongClickListener(v -> {
            selectedInfo = appInfo.applicationInfo;
            return false;
        });
    }

    @Override
    public long getItemId(int position) {
        PackageInfo info = showList.get(position).packageInfo;
        return (info.packageName + "!" + info.applicationInfo.uid / 100000).hashCode();
    }

    @Override
    public Filter getFilter() {
        return new ApplicationFilter();
    }

    @Override
    public int getItemCount() {
        return showList.size();
    }

    public void refresh(boolean force) {
        synchronized (this) {
            if (refreshing) {
                return;
            }
            refreshing = true;
        }
        loadAppListHandler.removeMessages(0);
        if (!force) {
            fragment.binding.progress.setVisibility(View.INVISIBLE);
            fragment.binding.progress.setIndeterminate(true);
            fragment.binding.progress.setVisibility(View.VISIBLE);
        }
        enabled = moduleUtil.isModuleEnabled(module.packageName);
        fragment.binding.masterSwitch.setOnCheckedChangeListener(null);
        fragment.binding.masterSwitch.setChecked(enabled);
        fragment.binding.masterSwitch.setOnCheckedChangeListener(switchBarOnCheckedChangeListener);
        loadAppListHandler.sendMessage(Message.obtain(loadAppListHandler, 0, force));
    }

    protected void onCheckedChange(CompoundButton buttonView, boolean isChecked, AppInfo appInfo) {
        if (isChecked) {
            checkedList.add(appInfo.application);
        } else {
            checkedList.remove(appInfo.application);
        }
        if (!ConfigManager.setModuleScope(module.packageName, checkedList)) {
            fragment.makeSnackBar(R.string.failed_to_save_scope_list, Snackbar.LENGTH_SHORT);
            if (!isChecked) {
                checkedList.add(appInfo.application);
            } else {
                checkedList.remove(appInfo.application);
            }
            buttonView.setChecked(!isChecked);
        } else if (appInfo.packageName.equals("android")) {
            Snackbar.make(fragment.binding.snackbar, R.string.reboot_required, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.reboot, v -> ConfigManager.reboot(false, null, false))
                    .show();
        }
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        if (msg.what != 0) {
            return false;
        }
        try {
            List<PackageInfo> appList = AppHelper.getAppList((Boolean) msg.obj);
            checkedList.clear();
            recommendedList.clear();
            var tmpList = new ArrayList<AppInfo>();
            checkedList.addAll(ConfigManager.getModuleScope(module.packageName));
            HashSet<ApplicationWithEquals> installedList = new HashSet<>();
            List<String> scopeList = module.getScopeList();
            boolean emptyCheckedList = checkedList.isEmpty();
            for (PackageInfo info : appList) {
                int userId = info.applicationInfo.uid / 100000;
                String packageName = info.packageName;
                if (packageName.equals("android") && userId != 0 ||
                        packageName.equals(module.packageName) ||
                        packageName.equals(BuildConfig.APPLICATION_ID)) {
                    continue;
                }

                ApplicationWithEquals application = new ApplicationWithEquals(packageName, userId);

                installedList.add(application);

                if (userId != module.userId) {
                    continue;
                }

                if (scopeList != null && scopeList.contains(packageName)) {
                    recommendedList.add(application);
                    if (emptyCheckedList) {
                        checkedList.add(application);
                    }
                } else if (shouldHideApp(info, application)) {
                    continue;
                }

                AppInfo appInfo = new AppInfo();
                appInfo.packageInfo = info;
                appInfo.label = info.applicationInfo.loadLabel(pm);
                appInfo.application = application;
                appInfo.packageName = info.packageName;
                appInfo.applicationInfo = info.applicationInfo;
                tmpList.add(appInfo);
            }
            checkedList.retainAll(installedList);
            if (emptyCheckedList) {
                ConfigManager.setModuleScope(module.packageName, checkedList);
            }
            sortApps(tmpList);
            searchList.clear();
            searchList.addAll(tmpList);
            synchronized (dataReadyRunnable) {
                synchronized (this) {
                    refreshing = false;
                }
                activity.runOnUiThread(dataReadyRunnable);
                dataReadyRunnable.wait();
            }
            return true;
        } catch (Exception e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return false;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ConstraintLayout root;
        ImageView appIcon;
        TextView appName;
        TextView appDescription;
        MaterialCheckBox checkbox;

        ViewHolder(ItemModuleBinding binding) {
            super(binding.getRoot());
            root = binding.itemRoot;
            appIcon = binding.appIcon;
            appName = binding.appName;
            appDescription = binding.description;
            checkbox = binding.checkbox;
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
            if (constraint.toString().isEmpty()) {
                filtered.addAll(searchList);
            } else {
                String filter = constraint.toString().toLowerCase();
                for (AppInfo info : searchList) {
                    if (lowercaseContains(info.label.toString(), filter)
                            || lowercaseContains(info.packageName, filter)) {
                        filtered.add(info);
                    }
                }
            }
            filterResults.values = filtered;
            filterResults.count = filtered.size();
            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            showList.clear();
            //noinspection unchecked
            showList.addAll((Collection<AppInfo>) results.values);
            notifyDataSetChanged();
        }
    }

    public SearchView.OnQueryTextListener getSearchListener() {
        return new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                refresh(false);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                refresh(false);
                return true;
            }
        };
    }

    public boolean onBackPressed() {
        if (!refreshing && fragment.binding.masterSwitch.isChecked() && checkedList.isEmpty()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setMessage(!recommendedList.isEmpty() ? R.string.no_scope_selected_has_recommended : R.string.no_scope_selected);
            if (!recommendedList.isEmpty()) {
                builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    checkRecommended();
                    notifyDataSetChanged();
                });
            } else {
                builder.setPositiveButton(android.R.string.cancel, null);
            }
            builder.setNegativeButton(!recommendedList.isEmpty() ? android.R.string.cancel : android.R.string.ok, (dialog, which) -> {
                moduleUtil.setModuleEnabled(module.packageName, false);
                Toast.makeText(activity, activity.getString(R.string.module_disabled_no_selection, module.getAppName()), Toast.LENGTH_LONG).show();
                activity.finish();
            });
            builder.show();
            return false;
        } else {
            return true;
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
