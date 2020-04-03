package org.meowcat.edxposed.manager.adapters;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.FileUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.FragmentManager;

import org.meowcat.edxposed.manager.BuildConfig;
import org.meowcat.edxposed.manager.R;
import org.meowcat.edxposed.manager.XposedApp;
import org.meowcat.edxposed.manager.util.CompileUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS;

@SuppressWarnings("deprecation")
public class AppHelper {

    public static final String TAG = XposedApp.TAG;

    private static final String BASE_PATH = XposedApp.BASE_DIR;
    private static final String WHITE_LIST_PATH = "conf/whitelist/";
    private static final String BLACK_LIST_PATH = "conf/blacklist/";
    private static final String COMPAT_LIST_PATH = "conf/compatlist/";
    private static final String WHITE_LIST_MODE = "conf/usewhitelist";
    private static final String BLACK_LIST_MODE = "conf/blackwhitelist";

    private static final List<String> FORCE_WHITE_LIST = new ArrayList<>(Collections.singletonList(BuildConfig.APPLICATION_ID));
    public static List<String> FORCE_WHITE_LIST_MODULE = new ArrayList<>(FORCE_WHITE_LIST);

    @SuppressWarnings("OctalInteger")
    static void makeSurePath() {
        XposedApp.mkdirAndChmod(WHITE_LIST_PATH, 00777);
        XposedApp.mkdirAndChmod(BLACK_LIST_PATH, 00777);
        XposedApp.mkdirAndChmod(COMPAT_LIST_PATH, 00777);
    }

    public static boolean isWhiteListMode() {
        return new File(BASE_PATH + WHITE_LIST_MODE).exists();
    }

    public static boolean isBlackListMode() {
        return new File(BASE_PATH + BLACK_LIST_MODE).exists();
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
                    setFilePermissionsFromMode(file.getPath(), Context.MODE_WORLD_READABLE);
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

    @SuppressWarnings("SameParameterValue")
    @SuppressLint({"WorldReadableFiles", "WorldWriteableFiles"})
    private static void setFilePermissionsFromMode(String name, int mode) {
        int perms = FileUtils.S_IRUSR | FileUtils.S_IWUSR
                | FileUtils.S_IRGRP | FileUtils.S_IWGRP;
        if ((mode & Context.MODE_WORLD_READABLE) != 0) {
            perms |= FileUtils.S_IROTH;
        }
        if ((mode & Context.MODE_WORLD_WRITEABLE) != 0) {
            perms |= FileUtils.S_IWOTH;
        }
        FileUtils.setPermissions(name, perms, -1, -1);
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
                    setFilePermissionsFromMode(file.getPath(), Context.MODE_WORLD_READABLE);
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
    private static Boolean compatListFileName(String packageName, boolean isAdd) {
        boolean returns = true;
        File file = new File(BASE_PATH + COMPAT_LIST_PATH + packageName);
        if (isAdd) {
            if (!file.exists()) {
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(file.getPath());
                    setFilePermissionsFromMode(file.getPath(), Context.MODE_WORLD_READABLE);
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
            switch (menuItem.getItemId()) {
                case R.id.app_menu_launch:
                    Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(info.packageName);
                    if (launchIntent != null) {
                        context.startActivity(launchIntent);
                    } else {
                        Toast.makeText(context, context.getString(R.string.module_no_ui), Toast.LENGTH_LONG).show();
                    }
                    break;
                case R.id.app_menu_stop:
                    try {
                        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                        Objects.requireNonNull(manager).killBackgroundProcesses(info.packageName);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    break;
                case R.id.app_menu_compile_speed:
                    CompileUtil.compileSpeed(context, fragmentManager, info);
                    break;
                case R.id.app_menu_compile_dexopt:
                    CompileUtil.compileDexopt(context, fragmentManager, info);
                    break;
                case R.id.app_menu_compile_reset:
                    CompileUtil.reset(context, fragmentManager, info);
                    break;
                case R.id.app_menu_store:
                    Uri uri = Uri.parse("market://details?id=" + info.packageName);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        context.startActivity(intent);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    break;
                case R.id.app_menu_info:
                    context.startActivity(new Intent(ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", info.packageName, null)));
                    break;
                case R.id.app_menu_uninstall:
                    context.startActivity(new Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.fromParts("package", info.packageName, null)));
                    break;
            }
            return true;
        });
        MenuPopupHelper menuHelper = new MenuPopupHelper(context, (MenuBuilder) appMenu.getMenu(), anchor);
        menuHelper.setForceShowIcon(true);
        menuHelper.show();
    }

    static List<String> getCompatList() {
        File file = new File(BASE_PATH + COMPAT_LIST_PATH);
        File[] files = file.listFiles();
        if (files == null) {
            return new ArrayList<>();
        }
        List<String> s = new ArrayList<>();
        for (File file1 : files) {
            s.add(file1.getName());
        }
        return s;
    }

    static boolean addCompatList(String packageName) {
        return compatListFileName(packageName, true);
    }

    static boolean removeCompatList(String packageName) {
        return compatListFileName(packageName, false);
    }
}
