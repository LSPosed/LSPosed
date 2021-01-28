package org.meowcat.edxposed.manager.adapters;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.FragmentManager;

import org.meowcat.edxposed.manager.App;
import org.meowcat.edxposed.manager.Constants;
import org.meowcat.edxposed.manager.R;
import org.meowcat.edxposed.manager.util.CompileUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS;

public class AppHelper {

    private static final String BASE_PATH = Constants.getBaseDir();
    private static final String WHITE_LIST_PATH = "conf/whitelist/";
    private static final String BLACK_LIST_PATH = "conf/blacklist/";
    private static final String SCOPE_LIST_PATH = "conf/%s.conf";
    private static final String WHITE_LIST_MODE = "conf/usewhitelist";

    public static List<String> forceWhiteList = new ArrayList<>();

    private static final HashMap<String, List<String>> scopeList = new HashMap<>();

    public static void makeSurePath() {
        App.mkdir(WHITE_LIST_PATH);
        App.mkdir(BLACK_LIST_PATH);
    }

    public static boolean isWhiteListMode() {
        return new File(BASE_PATH + WHITE_LIST_MODE).exists();
    }


    public static List<String> getAppList() {
        return getAppList(isWhiteListMode());
    }

    public static List<String> getAppList(boolean white) {
        Path dir = Paths.get(BASE_PATH + (white ? WHITE_LIST_PATH : BLACK_LIST_PATH));
        List<String> s = new ArrayList<>();
        try {
            Files.list(dir).forEach(path -> {
                if (!Files.isDirectory(path)) {
                    String packageName = path.getFileName().toString();
                    if (forceWhiteList.contains(packageName)) {
                        createAppListFile(packageName, white, white);
                    } else {
                        s.add(packageName);
                    }
                }
            });
            return s;
        } catch (IOException e) {
            return s;
        }
    }

    private static boolean createAppListFile(String packageName, boolean white, boolean add) {
        Path path = Paths.get(BASE_PATH + (white ? WHITE_LIST_PATH : BLACK_LIST_PATH) + packageName);
        try {
            if (Files.exists(path)) {
                if (!add) {
                    Files.delete(path);
                }
            } else if (add) {
                Files.createFile(path);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    static boolean setPackageAppList(String packageName, boolean add) {
        return setPackageAppList(packageName, isWhiteListMode(), add);
    }

    static boolean setPackageAppList(String packageName, boolean white, boolean add) {
        if (add && !white && forceWhiteList.contains(packageName)) {
            createAppListFile(packageName, false, false);
            return false;
        }
        if (!add && white && forceWhiteList.contains(packageName)) {
            return false;
        }
        return createAppListFile(packageName, white, add);
    }

    @SuppressLint("RestrictedApi")
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
                    Objects.requireNonNull(manager).killBackgroundProcesses(info.packageName);
                } catch (Exception ex) {
                    ex.printStackTrace();
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
                } catch (Exception ex) {
                    ex.printStackTrace();
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

    static List<String> getScopeList(String modulePackageName) {
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

    @SuppressLint("WorldReadableFiles")
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
