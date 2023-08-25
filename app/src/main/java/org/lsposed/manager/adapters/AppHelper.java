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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Parcel;
import android.view.MenuItem;

import org.lsposed.manager.ConfigManager;
import org.lsposed.manager.R;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class AppHelper {

    public static final String SETTINGS_CATEGORY = "de.robv.android.xposed.category.MODULE_SETTINGS";
    public static final int FLAG_SHOW_FOR_ALL_USERS = 0x0400;
    private static List<String> denyList;
    private static List<PackageInfo> appList;
    private static final ConcurrentHashMap<PackageInfo, CharSequence> appLabel = new ConcurrentHashMap<>();

    @SuppressLint("WrongConstant")
    public static Intent getSettingsIntent(String packageName, int userId) {
        Intent intentToResolve = new Intent(Intent.ACTION_MAIN);
        intentToResolve.addCategory(SETTINGS_CATEGORY);
        intentToResolve.setPackage(packageName);

        List<ResolveInfo> ris = ConfigManager.queryIntentActivitiesAsUser(intentToResolve, 0, userId);

        if (ris.size() == 0) {
            return getLaunchIntentForPackage(packageName, userId);
        }

        Intent intent = new Intent(intentToResolve);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClassName(ris.get(0).activityInfo.packageName,
                ris.get(0).activityInfo.name);
        intent.putExtra("lsp_no_switch_to_user", (ris.get(0).activityInfo.flags & FLAG_SHOW_FOR_ALL_USERS) != 0);
        return intent;
    }

    @SuppressLint("WrongConstant")
    public static Intent getLaunchIntentForPackage(String packageName, int userId) {
        Intent intentToResolve = new Intent(Intent.ACTION_MAIN);
        intentToResolve.addCategory(Intent.CATEGORY_INFO);
        intentToResolve.setPackage(packageName);
        List<ResolveInfo> ris = ConfigManager.queryIntentActivitiesAsUser(intentToResolve, 0, userId);

        if (ris.size() == 0) {
            intentToResolve.removeCategory(Intent.CATEGORY_INFO);
            intentToResolve.addCategory(Intent.CATEGORY_LAUNCHER);
            intentToResolve.setPackage(packageName);
            ris = ConfigManager.queryIntentActivitiesAsUser(intentToResolve, 0, userId);
        }

        if (ris.size() == 0) {
            return null;
        }

        Intent intent = new Intent(intentToResolve);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClassName(ris.get(0).activityInfo.packageName,
                ris.get(0).activityInfo.name);
        intent.putExtra("lsp_no_switch_to_user", (ris.get(0).activityInfo.flags & FLAG_SHOW_FOR_ALL_USERS) != 0);
        return intent;
    }

    public static boolean onOptionsItemSelected(MenuItem item, SharedPreferences preferences) {
        int itemId = item.getItemId();
        int i = preferences.getInt("list_sort", 0);
        if (itemId == R.id.item_sort_by_name) {
            i = (i % 2 == 0) ? 0 : 1;
        } else if (itemId == R.id.item_sort_by_package_name) {
            i = (i % 2 == 0) ? 2 : 3;
        } else if (itemId == R.id.item_sort_by_install_time) {
            i = (i % 2 == 0) ? 4 : 5;
        } else if (itemId == R.id.item_sort_by_update_time) {
            i = (i % 2 == 0) ? 6 : 7;
        } else if (itemId == R.id.reverse) {
            if (i % 2 == 0) i++;
            else i--;
        } else {
            return false;
        }
        preferences.edit().putInt("list_sort", i).apply();
        if (item.isCheckable())
            item.setChecked(!item.isChecked());
        return true;
    }

    public static Comparator<PackageInfo> getAppListComparator(int sort, PackageManager pm) {
        ApplicationInfo.DisplayNameComparator displayNameComparator = new ApplicationInfo.DisplayNameComparator(pm);
        return switch (sort) {
            case 7 ->
                    Collections.reverseOrder(Comparator.comparingLong((PackageInfo a) -> a.lastUpdateTime));
            case 6 -> Comparator.comparingLong((PackageInfo a) -> a.lastUpdateTime);
            case 5 ->
                    Collections.reverseOrder(Comparator.comparingLong((PackageInfo a) -> a.firstInstallTime));
            case 4 -> Comparator.comparingLong((PackageInfo a) -> a.firstInstallTime);
            case 3 -> Collections.reverseOrder(Comparator.comparing(a -> a.packageName));
            case 2 -> Comparator.comparing(a -> a.packageName);
            case 1 ->
                    Collections.reverseOrder((PackageInfo a, PackageInfo b) -> displayNameComparator.compare(a.applicationInfo, b.applicationInfo));
            default ->
                    (PackageInfo a, PackageInfo b) -> displayNameComparator.compare(a.applicationInfo, b.applicationInfo);
        };
    }

    synchronized public static List<PackageInfo> getAppList(boolean force) {
        if (appList == null || force) {
            appList = ConfigManager.getInstalledPackagesFromAllUsers(PackageManager.GET_META_DATA | PackageManager.MATCH_UNINSTALLED_PACKAGES, true);
            PackageInfo system = null;
            for (var app : appList) {
                if ("android".equals(app.packageName)) {
                    var p = Parcel.obtain();
                    app.writeToParcel(p, 0);
                    p.setDataPosition(0);
                    system = PackageInfo.CREATOR.createFromParcel(p);
                    system.packageName = "system";
                    system.applicationInfo.packageName = system.packageName;
                    break;
                }
            }
            if (system != null) {
                appList.add(system);
            }
        }
        return appList;
    }

    synchronized public static List<String> getDenyList(boolean force) {
        if (denyList == null || force) {
            denyList = ConfigManager.getDenyListPackages();
        }
        return denyList;
    }

    public static CharSequence getAppLabel(PackageInfo info, PackageManager pm) {
        if (info == null || info.applicationInfo == null) return null;
        return appLabel.computeIfAbsent(info, i -> i.applicationInfo.loadLabel(pm));
    }
}
