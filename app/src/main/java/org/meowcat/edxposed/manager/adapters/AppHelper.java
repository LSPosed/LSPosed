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
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.FragmentManager;

import org.meowcat.edxposed.manager.App;
import org.meowcat.edxposed.manager.BuildConfig;
import org.meowcat.edxposed.manager.Constants;
import org.meowcat.edxposed.manager.R;
import org.meowcat.edxposed.manager.util.CompileUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
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

    private static final List<String> FORCE_WHITE_LIST = new ArrayList<>(Collections.singletonList(BuildConfig.APPLICATION_ID));
    public static List<String> FORCE_WHITE_LIST_MODULE = new ArrayList<>(FORCE_WHITE_LIST);

    private static final HashMap<String, List<String>> scopeList = new HashMap<>();

    static void makeSurePath() {
        App.mkdir(WHITE_LIST_PATH);
        App.mkdir(BLACK_LIST_PATH);
    }

    public static boolean isWhiteListMode() {
        return new File(BASE_PATH + WHITE_LIST_MODE).exists();
    }

    private static boolean addWhiteList(String packageName) {
        return whiteListFileName(packageName, true);
    }

    private static boolean addBlackList(String packageName) {
        if (FORCE_WHITE_LIST_MODULE.contains(packageName)) {
            removeBlackList(packageName);
            return false;
        }
        return blackListFileName(packageName, true);
    }

    private static boolean removeWhiteList(String packageName) {
        if (FORCE_WHITE_LIST_MODULE.contains(packageName)) {
            return false;
        }
        return whiteListFileName(packageName, false);
    }

    private static boolean removeBlackList(String packageName) {
        return blackListFileName(packageName, false);
    }

    static List<String> getBlackList() {
        File file = new File(BASE_PATH + BLACK_LIST_PATH);
        File[] files = file.listFiles();
        if (files == null) {
            return new ArrayList<>();
        }
        List<String> s = new ArrayList<>();
        for (File file1 : files) {
            if (!file1.isDirectory()) {
                s.add(file1.getName());
            }
        }
        for (String pn : FORCE_WHITE_LIST_MODULE) {
            if (s.contains(pn)) {
                s.remove(pn);
                removeBlackList(pn);
            }
        }
        return s;
    }

    static List<String> getWhiteList() {
        File file = new File(BASE_PATH + WHITE_LIST_PATH);
        File[] files = file.listFiles();
        if (files == null) {
            return FORCE_WHITE_LIST_MODULE;
        }
        List<String> result = new ArrayList<>();
        for (File file1 : files) {
            result.add(file1.getName());
        }
        for (String pn : FORCE_WHITE_LIST_MODULE) {
            if (!result.contains(pn)) {
                result.add(pn);
                addWhiteList(pn);
            }
        }
        return result;
    }

    @SuppressLint("WorldReadableFiles")
    private static Boolean whiteListFileName(String packageName, boolean isAdd) {
        boolean returns = true;
        File file = new File(BASE_PATH + WHITE_LIST_PATH + packageName);
        if (isAdd) {
            if (!file.exists()) {
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(file.getPath());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                            try {
                                returns = file.createNewFile();
                            } catch (IOException e1) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        } else {
            if (file.exists()) {
                returns = file.delete();
            }
        }
        return returns;
    }

    @SuppressLint("WorldReadableFiles")
    private static Boolean blackListFileName(String packageName, boolean isAdd) {
        boolean returns = true;
        File file = new File(BASE_PATH + BLACK_LIST_PATH + packageName);
        if (isAdd) {
            if (!file.exists()) {
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(file.getPath());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                            try {
                                returns = file.createNewFile();
                            } catch (IOException e1) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        } else {
            if (file.exists()) {
                returns = file.delete();
            }
        }
        return returns;
    }

    static boolean addPackageName(boolean isWhiteListMode, String packageName) {
        return isWhiteListMode ? addWhiteList(packageName) : addBlackList(packageName);
    }

    static boolean removePackageName(boolean isWhiteListMode, String packageName) {
        return isWhiteListMode ? removeWhiteList(packageName) : removeBlackList(packageName);
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
        MenuPopupHelper menuHelper = new MenuPopupHelper(context, (MenuBuilder) appMenu.getMenu(), anchor);
        menuHelper.setForceShowIcon(true);
        menuHelper.show();
    }

    static List<String> getScopeList(String modulePackageName) {
        if (scopeList.containsKey(modulePackageName)) {
            return scopeList.get(modulePackageName);
        }
        File file = new File(BASE_PATH + String.format(SCOPE_LIST_PATH, modulePackageName));
        List<String> s = new ArrayList<>();
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            for (String line; (line = bufferedReader.readLine()) != null; ) {
                s.add(line);
            }
            scopeList.put(modulePackageName, s);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return s;
    }

    @SuppressLint("WorldReadableFiles")
    static boolean saveScopeList(String modulePackageName, List<String> list) {
        File file = new File(BASE_PATH + String.format(SCOPE_LIST_PATH, modulePackageName));
        if (list.size() == 0) {
            scopeList.put(modulePackageName, list);
            return file.delete();
        }
        try {
            PrintWriter pr = new PrintWriter(new FileWriter(file));
            for (String line : list) {
                pr.println(line);
            }
            pr.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        scopeList.put(modulePackageName, list);
        return true;
    }
}
