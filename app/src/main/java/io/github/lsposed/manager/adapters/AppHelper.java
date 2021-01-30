package io.github.lsposed.manager.adapters;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.FragmentManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import io.github.lsposed.manager.App;
import io.github.lsposed.manager.Constants;
import io.github.lsposed.manager.R;
import io.github.lsposed.manager.util.CompileUtil;

import static android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS;

public class AppHelper {

    private static final String BASE_PATH = Constants.getBaseDir();
    private static final String SCOPE_LIST_PATH = "conf/%s.conf";

    public static List<String> forceWhiteList = new ArrayList<>();

    private static final HashMap<String, List<String>> scopeList = new HashMap<>();

    public static void showMenu(@NonNull Context context,
                                @NonNull FragmentManager fragmentManager,
                                @NonNull View anchor,
                                @NonNull ApplicationInfo info) {
        PopupMenu appMenu = new PopupMenu(context, anchor);
        appMenu.inflate(R.menu.menu_app_item);
        appMenu.setOnMenuItemClickListener(menuItem -> {
            int itemId = menuItem.getItemId();
            if (itemId == R.id.app_menu_launch) {
                Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(info.packageName);
                if (launchIntent != null) {
                    context.startActivity(launchIntent);
                } else {
                    Toast.makeText(context, context.getString(R.string.module_no_ui), Toast.LENGTH_LONG).show();
                }
            } else if (itemId == R.id.app_menu_stop) {
                try {
                    ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                    manager.killBackgroundProcesses(info.packageName);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (itemId == R.id.app_menu_compile_speed) {
                CompileUtil.compileSpeed(context, fragmentManager, info);
            } else if (itemId == R.id.app_menu_compile_dexopt) {
                CompileUtil.compileDexopt(context, fragmentManager, info);
            } else if (itemId == R.id.app_menu_compile_reset) {
                CompileUtil.reset(context, fragmentManager, info);
            } else if (itemId == R.id.app_menu_store) {
                Uri uri = Uri.parse("market://details?id=" + info.packageName);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    context.startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (itemId == R.id.app_menu_info) {
                context.startActivity(new Intent(ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", info.packageName, null)));
            } else if (itemId == R.id.app_menu_uninstall) {
                context.startActivity(new Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.fromParts("package", info.packageName, null)));
            }
            return true;
        });
        appMenu.show();
    }

    public static boolean onOptionsItemSelected(MenuItem item, SharedPreferences preferences) {
        int itemId = item.getItemId();
        if (itemId == R.id.item_sort_by_name) {
            item.setChecked(true);
            preferences.edit().putInt("list_sort", 0).apply();
        } else if (itemId == R.id.item_sort_by_name_reverse) {
            item.setChecked(true);
            preferences.edit().putInt("list_sort", 1).apply();
        } else if (itemId == R.id.item_sort_by_package_name) {
            item.setChecked(true);
            preferences.edit().putInt("list_sort", 2).apply();
        } else if (itemId == R.id.item_sort_by_package_name_reverse) {
            item.setChecked(true);
            preferences.edit().putInt("list_sort", 3).apply();
        } else if (itemId == R.id.item_sort_by_install_time) {
            item.setChecked(true);
            preferences.edit().putInt("list_sort", 4).apply();
        } else if (itemId == R.id.item_sort_by_install_time_reverse) {
            item.setChecked(true);
            preferences.edit().putInt("list_sort", 5).apply();
        } else if (itemId == R.id.item_sort_by_update_time) {
            item.setChecked(true);
            preferences.edit().putInt("list_sort", 6).apply();
        } else if (itemId == R.id.item_sort_by_update_time_reverse) {
            item.setChecked(true);
            preferences.edit().putInt("list_sort", 7).apply();
        } else {
            return false;
        }
        return true;
    }

    public static Comparator<PackageInfo> getAppListComparator(int sort, PackageManager pm) {
        ApplicationInfo.DisplayNameComparator displayNameComparator = new ApplicationInfo.DisplayNameComparator(pm);
        switch (sort) {
            case 7:
                return Collections.reverseOrder((PackageInfo a, PackageInfo b) -> Long.compare(a.lastUpdateTime, b.lastUpdateTime));
            case 6:
                return (PackageInfo a, PackageInfo b) -> Long.compare(a.lastUpdateTime, b.lastUpdateTime);
            case 5:
                return Collections.reverseOrder((PackageInfo a, PackageInfo b) -> Long.compare(a.firstInstallTime, b.firstInstallTime));
            case 4:
                return (PackageInfo a, PackageInfo b) -> Long.compare(a.firstInstallTime, b.firstInstallTime);
            case 3:
                return Collections.reverseOrder((a, b) -> a.packageName.compareTo(b.packageName));
            case 2:
                return (a, b) -> a.packageName.compareTo(b.packageName);
            case 1:
                return Collections.reverseOrder((PackageInfo a, PackageInfo b) -> displayNameComparator.compare(a.applicationInfo, b.applicationInfo));
            case 0:
            default:
                return (PackageInfo a, PackageInfo b) -> displayNameComparator.compare(a.applicationInfo, b.applicationInfo);
        }
    }

    public static List<String> getEnabledModuleList() {
        Path path = Paths.get(Constants.getEnabledModulesListFile());
        List<String> s = new ArrayList<>();
        try {
            s = Files.readAllLines(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return s;
    }

    public static List<String> getScopeList(String modulePackageName) {
        if (scopeList.containsKey(modulePackageName)) {
            return scopeList.get(modulePackageName);
        }
        Path path = Paths.get(BASE_PATH + String.format(SCOPE_LIST_PATH, modulePackageName));
        List<String> s = new ArrayList<>();
        try {
            s = Files.readAllLines(path);
            scopeList.put(modulePackageName, s);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return s;
    }

    static boolean saveScopeList(String modulePackageName, List<String> list) {
        Path path = Paths.get(BASE_PATH + String.format(SCOPE_LIST_PATH, modulePackageName));
        if (list.size() == 0) {
            scopeList.put(modulePackageName, list);
            try {
                Files.delete(path);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        try {
            Files.write(path, list);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        scopeList.put(modulePackageName, list);
        return true;
    }
}
