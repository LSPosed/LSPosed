package io.github.lsposed.manager.adapters;

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
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.github.lsposed.manager.App;
import io.github.lsposed.manager.R;
import io.github.lsposed.manager.ui.activity.AppListActivity;
import io.github.lsposed.manager.ui.widget.MasterSwitch;
import io.github.lsposed.manager.util.CompileUtil;
import io.github.lsposed.manager.util.GlideApp;
import io.github.lsposed.manager.util.ModuleUtil;

import static android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS;

public class ScopeAdapter extends RecyclerView.Adapter<ScopeAdapter.ViewHolder> implements Filterable {

    private final AppListActivity activity;
    private final PackageManager pm;
    private final ApplicationFilter filter;
    private final SharedPreferences preferences;
    private final String modulePackageName;
    private final String moduleName;
    private final MasterSwitch masterSwitch;
    private List<PackageInfo> fullList, showList;
    private List<String> checkedList;
    private final List<String> recommendedList;
    private boolean enabled = true;
    private ApplicationInfo selectedInfo;

    public ScopeAdapter(AppListActivity activity, String moduleName, String modulePackageName, MasterSwitch masterSwitch) {
        this.activity = activity;
        this.moduleName = moduleName;
        this.modulePackageName = modulePackageName;
        this.masterSwitch = masterSwitch;
        preferences = App.getPreferences();
        fullList = showList = Collections.emptyList();
        checkedList = Collections.emptyList();
        filter = new ApplicationFilter();
        pm = activity.getPackageManager();
        masterSwitch.setOnCheckedChangedListener(new MasterSwitch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(boolean checked) {
                enabled = checked;
                ModuleUtil.getInstance().setModuleEnabled(modulePackageName, enabled);
                notifyDataSetChanged();
            }
        });
        ModuleUtil.InstalledModule module = ModuleUtil.getInstance().getModule(modulePackageName);
        recommendedList = module.getScopeList();
        enabled = ModuleUtil.getInstance().isModuleEnabled(modulePackageName);
        refresh();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(activity).inflate(R.layout.item_module, parent, false);
        return new ViewHolder(v);
    }

    private void loadApps() {
        activity.runOnUiThread(() -> masterSwitch.setChecked(enabled));
        checkedList = AppHelper.getScopeList(modulePackageName);
        if (checkedList.isEmpty() && hasRecommended()) {
            checkRecommended();
        }
        fullList = pm.getInstalledPackages(PackageManager.GET_META_DATA);
        List<String> installedList = new ArrayList<>();
        List<PackageInfo> rmList = new ArrayList<>();
        for (PackageInfo info : fullList) {
            installedList.add(info.packageName);
            if (info.packageName.equals(this.modulePackageName)) {
                rmList.add(info);
                continue;
            }
            if (checkedList.contains(info.packageName) || info.packageName.equals("android")) {
                continue;
            }
            if (!preferences.getBoolean("show_modules", false)) {
                if (info.applicationInfo.metaData != null && info.applicationInfo.metaData.containsKey("xposedmodule")) {
                    rmList.add(info);
                    continue;
                }
            }
            if (!preferences.getBoolean("show_games", false)) {
                if (info.applicationInfo.category == ApplicationInfo.CATEGORY_GAME) {
                    rmList.add(info);
                    continue;
                }
                //noinspection deprecation
                if ((info.applicationInfo.flags & ApplicationInfo.FLAG_IS_GAME) != 0) {
                    rmList.add(info);
                    continue;
                }
            }
            if ((info.applicationInfo.flags & ApplicationInfo.FLAG_HAS_CODE) == 0) {
                rmList.add(info);
                continue;
            }
            if (!preferences.getBoolean("show_system_apps", false) && (info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                rmList.add(info);
            }
        }
        checkedList.retainAll(installedList);
        if (rmList.size() > 0) {
            fullList.removeAll(rmList);
        }
        showList = sortApps(fullList);
        activity.onDataReady();
    }

    private List<PackageInfo> sortApps(List<PackageInfo> list) {
        Comparator<PackageInfo> comparator = AppHelper.getAppListComparator(preferences.getInt("list_sort", 0), pm);
        Comparator<PackageInfo> frameworkComparator = (a, b) -> {
            if (a.packageName.equals("android") == b.packageName.equals("android")) {
                return comparator.compare(a, b);
            } else if (a.packageName.equals("android")) {
                return -1;
            } else {
                return 1;
            }
        };
        Comparator<PackageInfo> recommendedComparator = (a, b) -> {
            boolean aRecommended = hasRecommended() && recommendedList.contains(a.packageName);
            boolean bRecommended = hasRecommended() && recommendedList.contains(b.packageName);
            if (aRecommended == bRecommended) {
                return frameworkComparator.compare(a, b);
            } else if (aRecommended) {
                return -1;
            } else {
                return 1;
            }
        };
        list.sort((a, b) -> {
            boolean aChecked = checkedList.contains(a.packageName);
            boolean bChecked = checkedList.contains(b.packageName);
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
        AppHelper.saveScopeList(modulePackageName, checkedList);
        notifyDataSetChanged();
    }

    private boolean hasRecommended() {
        return recommendedList != null && !recommendedList.isEmpty();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.use_recommended) {
            if (!checkedList.isEmpty()) {
                new MaterialAlertDialogBuilder(activity)
                        .setTitle(R.string.use_recommended)
                        .setMessage(R.string.use_recommended_message)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> checkRecommended())
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            } else {
                checkRecommended();
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
        } else if (!AppHelper.onOptionsItemSelected(item, preferences)) {
            return false;
        }
        refresh();
        return true;
    }

    public boolean onContextItemSelected(@NonNull MenuItem item) {
        ApplicationInfo info = selectedInfo;
        if (info == null) {
            return false;
        }
        int itemId = item.getItemId();
        if (itemId == R.id.app_menu_launch) {
            Intent launchIntent = pm.getLaunchIntentForPackage(info.packageName);
            if (launchIntent != null) {
                activity.startActivity(launchIntent);
            }
        } else if (itemId == R.id.app_menu_compile_reset) {
            CompileUtil.reset(activity, activity.getSupportFragmentManager(), info);
        } else if (itemId == R.id.app_menu_store) {
            Uri uri = Uri.parse("market://details?id=" + info.packageName);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                activity.startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (itemId == R.id.app_menu_info) {
            activity.startActivity(new Intent(ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", info.packageName, null)));
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
        if (!hasRecommended()) {
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
        PackageInfo info = showList.get(position);
        boolean android = info.packageName.equals("android");
        holder.appName.setText(android ? activity.getString(R.string.android_framework) : getAppLabel(info.applicationInfo, pm));
        GlideApp.with(holder.appIcon)
                .load(info)
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        holder.appIcon.setImageDrawable(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });
        SpannableStringBuilder sb = new SpannableStringBuilder(android ? "" : activity.getString(R.string.app_description, info.packageName, info.versionName));
        holder.appDescription.setVisibility(View.VISIBLE);
        if (hasRecommended() && recommendedList.contains(info.packageName)) {
            if (!android) sb.append("\n");
            String recommended = activity.getString(R.string.requested_by_module);
            sb.append(recommended);
            final ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(activity.getThemedColor(R.attr.colorAccent));
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
            Intent launchIntent = pm.getLaunchIntentForPackage(info.packageName);
            if (launchIntent == null) {
                menu.removeItem(R.id.app_menu_launch);
            }
            if (android) {
                menu.removeItem(R.id.app_menu_compile_reset);
                menu.removeItem(R.id.app_menu_store);
            }
        });

        holder.checkbox.setOnCheckedChangeListener(null);
        holder.checkbox.setChecked(checkedList.contains(info.packageName));

        holder.checkbox.setOnCheckedChangeListener((v, isChecked) -> onCheckedChange(v, isChecked, info.packageName));
        holder.itemView.setOnClickListener(v -> {
            if (enabled) holder.checkbox.toggle();
        });
        holder.itemView.setOnLongClickListener(v -> {
            selectedInfo = info.applicationInfo;
            return false;
        });
    }

    @Override
    public long getItemId(int position) {
        return showList.get(position).packageName.hashCode();
    }

    @Override
    public Filter getFilter() {
        return new ApplicationFilter();
    }

    @Override
    public int getItemCount() {
        return showList.size();
    }

    public void filter(String constraint) {
        filter.filter(constraint);
    }

    public void refresh() {
        AsyncTask.THREAD_POOL_EXECUTOR.execute(this::loadApps);
    }

    protected void onCheckedChange(CompoundButton buttonView, boolean isChecked, String packageName) {
        if (isChecked) {
            checkedList.add(packageName);
        } else {
            checkedList.remove(packageName);
        }
        if (!AppHelper.saveScopeList(modulePackageName, checkedList)) {
            activity.makeSnackBar(R.string.failed_to_save_scope_list, Snackbar.LENGTH_SHORT);
            if (!isChecked) {
                checkedList.add(packageName);
            } else {
                checkedList.remove(packageName);
            }
            buttonView.setChecked(!isChecked);
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

        private boolean lowercaseContains(String s, CharSequence filter) {
            return !TextUtils.isEmpty(s) && s.toLowerCase().contains(filter);
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            if (constraint.toString().isEmpty()) {
                showList = fullList;
            } else {
                ArrayList<PackageInfo> filtered = new ArrayList<>();
                String filter = constraint.toString().toLowerCase();
                for (PackageInfo info : fullList) {
                    if (lowercaseContains(getAppLabel(info.applicationInfo, pm), filter)
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
        if (masterSwitch.isChecked() && checkedList.isEmpty()) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
            builder.setTitle(R.string.use_recommended);
            builder.setMessage(hasRecommended() ? R.string.no_scope_selected_has_recommended : R.string.no_scope_selected);
            if (hasRecommended()) {
                builder.setPositiveButton(android.R.string.ok, (dialog, which) -> checkRecommended());
            } else {
                builder.setPositiveButton(android.R.string.cancel, null);
            }
            builder.setNegativeButton(hasRecommended() ? android.R.string.cancel : android.R.string.ok, (dialog, which) -> {
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
}
