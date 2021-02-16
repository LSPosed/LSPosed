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

package io.github.lsposed.lspd.service;

import android.annotation.SuppressLint;
import android.app.ActivityThread;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.RemoteException;
import android.os.UserHandle;
import android.widget.Toast;

import io.github.lsposed.lspd.util.Utils;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

import de.robv.android.xposed.XposedHelpers;

public class PackageReceiver {
    private static final BroadcastReceiver RECEIVER = new BroadcastReceiver() {

        private final String MODULES_LIST_FILENAME = "conf/modules.list";
        private final String ENABLED_MODULES_LIST_FILENAME = "conf/enabled_modules.list";
        private final String MODULE_UPDATED = "io.github.lsposed.action.MODULE_UPDATED";
        private final String MODULE_NOT_ACTIVATAED = "io.github.lsposed.action.MODULE_NOT_ACTIVATAED";

        private String getPackageName(Intent intent) {
            Uri uri = intent.getData();
            return (uri != null) ? uri.getSchemeSpecificPart() : null;
        }


        private boolean isXposedModule(ApplicationInfo app) {
            return app != null && app.enabled && app.metaData != null && app.metaData.containsKey("xposedmodule");
        }

        private PackageInfo getPackageInfo(String packageName, int uid) {
            try {
                return PackageService.getPackageInfo(packageName, PackageManager.GET_META_DATA, uid);
            } catch (RemoteException e) {
                return null;
            }
        }

        private Map<String, String> loadEnabledModules(int uid) {
            HashMap<String, String> result = new HashMap<>();
            // TODO: FIXME
//            try {
//                File enabledModules = new File(ConfigManager.getMiscPath(), uid + "/" + ENABLED_MODULES_LIST_FILENAME);
//                if (!enabledModules.exists()) return result;
//                Scanner scanner = new Scanner(enabledModules);
//                while (scanner.hasNextLine()) {
//                    String packageName = scanner.nextLine();
//                    PackageInfo info = getPackageInfo(packageName, 0);
//                    if (info != null && isXposedModule(info.applicationInfo))
//                        result.put(packageName, info.applicationInfo.sourceDir);
//                    else if (info == null)
//                        result.put(packageName, null);
//                }
//            } catch (Throwable e) {
//                Utils.logE("Unable to read enabled modules", e);
//            }
            return result;
        }

        private boolean updateModuleList(int uid, String packageName) {
            Map<String, String> enabledModules = loadEnabledModules(uid);
            // TODO: FIXME
//
//            if (!enabledModules.containsKey(packageName)) return false;
//
//            try {
//                File moduleListFile = new File(ConfigManager.getMiscPath(), uid + "/" + MODULES_LIST_FILENAME);
//                File enabledModuleListFile = new File(ConfigManager.getMiscPath(), uid + "/" + ENABLED_MODULES_LIST_FILENAME);
//                if (moduleListFile.exists() && !moduleListFile.canWrite()) {
//                    moduleListFile.delete();
//                    moduleListFile.createNewFile();
//                }
//                if (enabledModuleListFile.exists() && !enabledModuleListFile.canWrite()) {
//                    enabledModuleListFile.delete();
//                    enabledModuleListFile.createNewFile();
//                }
//                PrintWriter modulesList = new PrintWriter(moduleListFile);
//                PrintWriter enabledModulesList = new PrintWriter(enabledModuleListFile);
//                for (Map.Entry<String, String> module : enabledModules.entrySet()) {
//                    String apkPath = module.getValue();
//                    if (apkPath != null) {
//                        modulesList.println(module.getValue());
//                        enabledModulesList.println(module.getKey());
//                    } else {
//                        Utils.logI(String.format("remove obsolete package %s", packageName));
//                        File prefsDir = new File(ConfigManager.getMiscPath(), uid + "/prefs/" + packageName);
//                        File[] fileList = prefsDir.listFiles();
//                        if (fileList != null) {
//                            for (File childFile : fileList) {
//                                childFile.delete();
//                            }
//                        }
//                    }
//                }
//                modulesList.close();
//                enabledModulesList.close();
//            } catch (Throwable e) {
//                Utils.logE("Fail to update module list", e);
//            }
            return true;
        }

        @SuppressLint("WrongConstant")
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.requireNonNull(intent.getAction()).equals(Intent.ACTION_PACKAGE_REMOVED) && intent.getBooleanExtra(Intent.EXTRA_REPLACING, false))
                // Ignore existing packages being removed in order to be updated
                return;
            String packageName = getPackageName(intent);
            if (packageName == null)
                return;

            if (intent.getAction().equals(Intent.ACTION_PACKAGE_CHANGED)) {
                // make sure that the change is for the complete package, not only a
                // component
                String[] components = intent.getStringArrayExtra(Intent.EXTRA_CHANGED_COMPONENT_NAME_LIST);
                if (components != null) {
                    boolean isForPackage = false;
                    for (String component : components) {
                        if (packageName.equals(component)) {
                            isForPackage = true;
                            break;
                        }
                    }
                    if (!isForPackage)
                        return;
                }
            }

            PackageInfo pkgInfo = getPackageInfo(packageName, intent.getIntExtra(Intent.EXTRA_USER, 0));

            if (pkgInfo != null && !isXposedModule(pkgInfo.applicationInfo)) return;
            // TODO: FIXME
//
//            try {
//                for (int uid : UserService.getUsers()) {
//                    Utils.logI("updating uid: " + uid);
//                    boolean activated = updateModuleList(uid, packageName);
//                    UserHandle userHandle = null;
//                    try {
//                        userHandle = (UserHandle) XposedHelpers.callStaticMethod(UserHandle.class, "of", uid);
//                    } catch (Throwable t) {
//                        Utils.logW("get user handle failed", t);
//                    }
//                    if (userHandle != null) {
//                        try {
//                            Intent broadCast = new Intent(activated ? MODULE_UPDATED : MODULE_NOT_ACTIVATAED);
//                            broadCast.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES | 0x01000000);
//                            broadCast.setData(intent.getData());
//                            broadCast.setPackage(ConfigManager.getInstallerPackageName());
//                            XposedHelpers.callMethod(context, "sendBroadcastAsUser", broadCast, userHandle);
//                            Utils.logI("broadcast to " + ConfigManager.getInstallerPackageName());
//                        } catch (Throwable t) {
//                            Utils.logW("send broadcast failed", t);
//                            Toast.makeText(context, "LSPosed: Updated " + packageName, Toast.LENGTH_SHORT).show();
//                        }
//                    } else if (activated) {
//                        Toast.makeText(context, "LSPosed: Updated " + packageName, Toast.LENGTH_SHORT).show();
//                    }
//                }
//            } catch (Throwable e) {
//                Utils.logW("update failed", e);
//            }
        }
    };

    public static void register() {
        ActivityThread activityThread = ActivityThread.currentActivityThread();
        if (activityThread == null) {
            Utils.logW("ActivityThread is null");
            return;
        }
        Context context = activityThread.getSystemContext();
        if (context == null) {
            Utils.logW("context is null");
            return;
        }

        UserHandle userHandleAll;
        try {
            //noinspection JavaReflectionMemberAccess
            Field field = UserHandle.class.getDeclaredField("ALL");
            userHandleAll = (UserHandle) field.get(null);
        } catch (Throwable e) {
            Utils.logW("UserHandle.ALL", e);
            return;
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addDataScheme("package");

        HandlerThread thread = new HandlerThread("lspd-PackageReceiver");
        thread.start();
        Handler handler = new Handler(thread.getLooper());

        try {
            @SuppressLint("DiscouragedPrivateApi")
            Method method = Context.class.getDeclaredMethod("registerReceiverAsUser", BroadcastReceiver.class, UserHandle.class, IntentFilter.class, String.class, Handler.class);
            method.invoke(context, RECEIVER, userHandleAll, intentFilter, null, handler);
            Utils.logI("registered package receiver");
        } catch (Throwable e) {
            Utils.logW("registerReceiver failed", e);
        }

    }
}
