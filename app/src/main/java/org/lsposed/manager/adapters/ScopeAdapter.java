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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
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
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.snackbar.Snackbar;

import org.lsposed.lspd.Application;
import org.lsposed.manager.App;
import org.lsposed.manager.BuildConfig;
import org.lsposed.manager.ConfigManager;
import org.lsposed.manager.R;
import org.lsposed.manager.ui.activity.AppListActivity;
import org.lsposed.manager.ui.fragment.CompileDialogFragment;
import org.lsposed.manager.util.GlideApp;
import org.lsposed.manager.util.ModuleUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import rikka.core.res.ResourcesKt;
import rikka.widget.switchbar.SwitchBar;

@SuppressLint("NotifyDataSetChanged")
public class ScopeAdapter extends RecyclerView.Adapter<ScopeAdapter.ViewHolder> implements Filterable {

    private final AppListActivity activity;
    private final PackageManager pm;
    private final SharedPreferences preferences;
    private final String modulePackageName;
    private final String moduleName;
    private final HashSet<ApplicationWithEquals> recommendedList = new HashSet<>();
    private final HashSet<ApplicationWithEquals> checkedList = new HashSet<>();
    private final List<AppInfo> searchList = new ArrayList<>();
    private final SwitchBar.OnCheckedChangeListener switchBarOnCheckedChangeListener = new SwitchBar.OnCheckedChangeListener() {
        @Override
        public boolean onCheckedChanged(SwitchBar view, boolean isChecked) {
            if (!ModuleUtil.getInstance().setModuleEnabled(modulePackageName, isChecked)) {
                return false;
            }
            enabled = isChecked;
            notifyDataSetChanged();
            return true;
        }
    };
    private List<AppInfo> showList = new ArrayList<>();
    private ApplicationInfo selectedInfo;
    private boolean refreshing = false;
    private boolean enabled = true;

    public ScopeAdapter(AppListActivity activity, String moduleName, String modulePackageName) {
        this.activity = activity;
        this.moduleName = moduleName;
        this.modulePackageName = modulePackageName;
        preferences = App.getPreferences();
        pm = activity.getPackageManager();
        refresh(false);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(activity).inflate(R.layout.item_module, parent, false);
        return new ViewHolder(v);
    }

    private void loadApps(boolean force) {
        List<PackageInfo> appList = AppHelper.getAppList(force);
        checkedList.clear();
        recommendedList.clear();
        searchList.clear();
        showList.clear();

        checkedList.addAll(ConfigManager.getModuleScope(modulePackageName));
        HashSet<ApplicationWithEquals> installedList = new HashSet<>();
        List<String> scopeList = ModuleUtil.getInstance().getModule(modulePackageName).getScopeList();
        boolean emptyCheckedList = checkedList.isEmpty();
        for (PackageInfo info : appList) {
            int uid = info.applicationInfo.uid;
            if (info.packageName.equals("android") && uid / 100000 != 0) {
                continue;
            }

            ApplicationWithEquals application = new ApplicationWithEquals(info.packageName, uid / 100000);

            installedList.add(application);

            if (scopeList != null && scopeList.contains(info.packageName)) {
                recommendedList.add(application);
                if (emptyCheckedList) {
                    checkedList.add(application);
                }
            }

            if (shouldHideApp(info, application)) {
                continue;
            }

            AppInfo appInfo = new AppInfo();
            appInfo.packageInfo = info;
            appInfo.label = getAppLabel(info.applicationInfo, pm);
            appInfo.application = application;
            appInfo.packageName = info.packageName;
            appInfo.applicationInfo = info.applicationInfo;
            searchList.add(appInfo);
        }
        checkedList.retainAll(installedList);
        if (emptyCheckedList) {
            ConfigManager.setModuleScope(modulePackageName, checkedList);
        }
        showList = sortApps(searchList);
        synchronized (this) {
            refreshing = false;
        }
        activity.onDataReady();
    }

    private boolean shouldHideApp(PackageInfo info, ApplicationWithEquals app) {
        if (info.packageName.equals(this.modulePackageName)) {
            return true;
        }
        if (info.packageName.equals(BuildConfig.APPLICATION_ID)) {
            return true;
        }
        if (info.packageName.equals("android")) {
            return false;
        }
        if (checkedList.contains(app)) {
            return false;
        }
        if (!preferences.getBoolean("show_modules", false)) {
            if (info.applicationInfo.metaData != null && info.applicationInfo.metaData.containsKey("xposedmodule")) {
                return true;
            }
        }
        if (!preferences.getBoolean("show_games", false)) {
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
        return !preferences.getBoolean("show_system_apps", false) && (info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }

    private List<AppInfo> sortApps(List<AppInfo> list) {
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
        return list;
    }

    private void checkRecommended() {
        checkedList.clear();
        checkedList.addAll(recommendedList);
        ConfigManager.setModuleScope(modulePackageName, checkedList);
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
        } else if (itemId == R.id.item_show_system) {
            item.setChecked(!item.isChecked());
            preferences.edit().putBoolean("show_system_apps", item.isChecked()).apply();
        } else if (itemId == R.id.item_show_games) {
            item.setChecked(!item.isChecked());
            preferences.edit().putBoolean("show_games", item.isChecked()).apply();
        } else if (itemId == R.id.item_show_modules) {
            item.setChecked(!item.isChecked());
            preferences.edit().putBoolean("show_modules", item.isChecked()).apply();
        } else if (itemId == R.id.menu_launch) {
            Intent launchIntent = AppHelper.getSettingsIntent(modulePackageName, pm);
            if (launchIntent != null) {
                activity.startActivity(launchIntent);
            } else {
                activity.makeSnackBar(R.string.module_no_ui, Snackbar.LENGTH_LONG);
            }
            return true;
        } else if (itemId == R.id.backup) {
            Calendar now = Calendar.getInstance();
            activity.backupLauncher.launch(String.format(Locale.US,
                    "%s_%04d%02d%02d_%02d%02d%02d.lsp",
                    moduleName,
                    now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1,
                    now.get(Calendar.DAY_OF_MONTH), now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE), now.get(Calendar.SECOND)));
            return true;
        } else if (itemId == R.id.restore) {
            activity.restoreLauncher.launch(new String[]{"*/*"});
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
            Intent launchIntent = pm.getLaunchIntentForPackage(info.packageName);
            if (launchIntent != null) {
                activity.startActivity(launchIntent);
            }
        } else if (itemId == R.id.menu_compile_speed) {
            CompileDialogFragment.speed(activity.getSupportFragmentManager(), info);
        } else if (itemId == R.id.menu_app_store) {
            Uri uri = Uri.parse("market://details?id=" + info.packageName);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                activity.startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (itemId == R.id.menu_app_info) {
            activity.startActivity(new Intent(ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", info.packageName, null)));
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

    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_app_list, menu);
        Intent intent = AppHelper.getSettingsIntent(modulePackageName, pm);
        if (intent == null) {
            menu.removeItem(R.id.menu_launch);
        }
        List<String> scopeList = ModuleUtil.getInstance().getModule(modulePackageName).getScopeList();
        if (scopeList == null || scopeList.isEmpty()) {
            menu.removeItem(R.id.use_recommended);
        }
        menu.findItem(R.id.item_show_system).setChecked(preferences.getBoolean("show_system_apps", false));
        menu.findItem(R.id.item_show_games).setChecked(preferences.getBoolean("show_games", false));
        menu.findItem(R.id.item_show_modules).setChecked(preferences.getBoolean("show_modules", false));
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
        if (userId != 0) {
            appName = String.format("%s (%s)", appInfo.label, userId);
        } else {
            appName = android ? activity.getString(R.string.android_framework) : appInfo.label;
        }
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
            Intent launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName);
            if (launchIntent == null) {
                menu.removeItem(R.id.menu_launch);
            }
            if (userId != 0) {
                menu.removeItem(R.id.menu_launch);
                menu.removeItem(R.id.menu_app_info);
            }
            if (android) {
                menu.findItem(R.id.menu_force_stop).setTitle(R.string.reboot);
                menu.removeItem(R.id.menu_compile_speed);
                menu.removeItem(R.id.menu_app_store);
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
        if (!force) {
            activity.binding.progress.setVisibility(View.INVISIBLE);
            activity.binding.progress.setIndeterminate(true);
            activity.binding.progress.setVisibility(View.VISIBLE);
        }
        enabled = ModuleUtil.getInstance().isModuleEnabled(modulePackageName);
        activity.binding.masterSwitch.setOnCheckedChangeListener(null);
        activity.binding.masterSwitch.setChecked(enabled);
        activity.binding.masterSwitch.setOnCheckedChangeListener(switchBarOnCheckedChangeListener);
        AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> loadApps(force));
    }

    protected void onCheckedChange(CompoundButton buttonView, boolean isChecked, AppInfo appInfo) {
        if (isChecked) {
            checkedList.add(appInfo.application);
        } else {
            checkedList.remove(appInfo.application);
        }
        if (!ConfigManager.setModuleScope(modulePackageName, checkedList)) {
            activity.makeSnackBar(R.string.failed_to_save_scope_list, Snackbar.LENGTH_SHORT);
            if (!isChecked) {
                checkedList.add(appInfo.application);
            } else {
                checkedList.remove(appInfo.application);
            }
            buttonView.setChecked(!isChecked);
        } else if (appInfo.packageName.equals("android")) {
            Snackbar.make(activity.binding.snackbar, R.string.reboot_required, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.reboot, v -> ConfigManager.reboot(false, null, false))
                    .show();
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        View root;
        ImageView appIcon;
        TextView appName;
        TextView appDescription;
        MaterialCheckBox checkbox;

        ViewHolder(View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.item_root);
            appIcon = itemView.findViewById(R.id.app_icon);
            appName = itemView.findViewById(R.id.app_name);
            appDescription = itemView.findViewById(R.id.description);
            checkbox = itemView.findViewById(R.id.checkbox);
            checkbox.setVisibility(View.VISIBLE);
        }
    }

    private class ApplicationFilter extends Filter {

        private boolean lowercaseContains(String s, String filter) {
            return !TextUtils.isEmpty(s) && s.toLowerCase().contains(filter);
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            if (constraint.toString().isEmpty()) {
                showList = searchList;
            } else {
                ArrayList<AppInfo> filtered = new ArrayList<>();
                String filter = constraint.toString().toLowerCase();
                for (AppInfo info : searchList) {
                    if (lowercaseContains(info.label.toString(), filter)
                            || lowercaseContains(info.packageName, filter)) {
                        filtered.add(info);
                    }
                }
                showList = filtered;
            }
            return null;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            notifyDataSetChanged();
        }
    }

    public boolean onBackPressed() {
        if (!refreshing && activity.binding.masterSwitch.isChecked() && checkedList.isEmpty()) {
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
                ModuleUtil.getInstance().setModuleEnabled(modulePackageName, false);
                Toast.makeText(activity, activity.getString(R.string.module_disabled_no_selection, moduleName), Toast.LENGTH_LONG).show();
                activity.finish();
            });
            builder.show();
            return false;
        } else {
            return true;
        }
    }

    public static String getAppLabel(ApplicationInfo info, PackageManager pm) {
        return info.loadLabel(pm).toString();
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
