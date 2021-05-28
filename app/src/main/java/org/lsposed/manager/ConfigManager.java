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
 * Copyright (C) 2021 LSPosed Contributors
 */

package org.lsposed.manager;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import org.lsposed.lspd.models.Application;
import org.lsposed.lspd.models.UserInfo;
import org.lsposed.lspd.utils.ParceledListSlice;
import org.lsposed.manager.adapters.ScopeAdapter;
import org.lsposed.manager.receivers.LSPManagerServiceClient;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class ConfigManager {

    public static int getXposedApiVersion() {
        try {
            return LSPManagerServiceClient.getXposedApiVersion();
        } catch (RemoteException | NullPointerException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return -1;
        }
    }

    public static String getXposedVersionName() {
        try {
            return LSPManagerServiceClient.getXposedVersionName();
        } catch (RemoteException | NullPointerException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return null;
        }
    }

    public static int getXposedVersionCode() {
        try {
            return LSPManagerServiceClient.getXposedVersionCode();
        } catch (RemoteException | NullPointerException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return -1;
        }
    }

    public static List<PackageInfo> getInstalledPackagesFromAllUsers(int flags, boolean filterNoProcess) {
        List<PackageInfo> list = new ArrayList<>();
        try {
            list.addAll(LSPManagerServiceClient.getInstalledPackagesFromAllUsers(flags, filterNoProcess));
        } catch (RemoteException | NullPointerException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
        }
        return list;
    }

    public static String[] getEnabledModules() {
        try {
            return LSPManagerServiceClient.enabledModules();
        } catch (RemoteException | NullPointerException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return new String[0];
        }
    }

    public static boolean setModuleEnabled(String packageName, boolean enable) {
        try {
            return enable ? LSPManagerServiceClient.enableModule(packageName) : LSPManagerServiceClient.disableModule(packageName);
        } catch (RemoteException | NullPointerException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return false;
        }
    }

    public static boolean setModuleScope(String packageName, HashSet<ScopeAdapter.ApplicationWithEquals> applications) {
        try {
            List<Application> list = new ArrayList<>();
            applications.forEach(application -> {
                Application app = new Application();
                app.userId = application.userId;
                app.packageName = application.packageName;
                list.add(app);
            });
            return LSPManagerServiceClient.setModuleScope(packageName, new ParceledListSlice<>(list));
        } catch (RemoteException | NullPointerException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return false;
        }
    }

    public static List<ScopeAdapter.ApplicationWithEquals> getModuleScope(String packageName) {
        List<ScopeAdapter.ApplicationWithEquals> list = new ArrayList<>();
        try {
            List<Application> applications = LSPManagerServiceClient.getModuleScope(packageName).getList();
            if (applications == null) {
                return list;
            }
            applications.forEach(application -> {
                if (!application.packageName.equals(packageName)) {
                    list.add(new ScopeAdapter.ApplicationWithEquals(application));
                }
            });
        } catch (RemoteException | NullPointerException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
        }
        return list;
    }

    public static boolean isResourceHookEnabled() {
        try {
            return LSPManagerServiceClient.isResourceHook();
        } catch (RemoteException | NullPointerException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return false;
        }
    }

    public static boolean setResourceHookEnabled(boolean enabled) {
        try {
            LSPManagerServiceClient.setResourceHook(enabled);
            return true;
        } catch (RemoteException | NullPointerException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return false;
        }
    }

    public static boolean isVerboseLogEnabled() {
        try {
            return LSPManagerServiceClient.isVerboseLog();
        } catch (RemoteException | NullPointerException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return false;
        }
    }

    public static boolean setVerboseLogEnabled(boolean enabled) {
        try {
            LSPManagerServiceClient.setVerboseLog(enabled);
            return true;
        } catch (RemoteException | NullPointerException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return false;
        }
    }

    public static ParcelFileDescriptor getLogs(boolean verbose) {
        try {
            return verbose ? LSPManagerServiceClient.getVerboseLog() : LSPManagerServiceClient.getModulesLog();
        } catch (RemoteException | NullPointerException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return null;
        }
    }

    public static boolean clearLogs(boolean verbose) {
        try {
            return LSPManagerServiceClient.clearLogs(verbose);
        } catch (RemoteException | NullPointerException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return false;
        }
    }

    public static PackageInfo getPackageInfo(String packageName, int flags, int userId) throws PackageManager.NameNotFoundException {
        try {
            return LSPManagerServiceClient.getPackageInfo(packageName, flags, userId);
        } catch (RemoteException | NullPointerException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            throw new PackageManager.NameNotFoundException();
        }
    }

    public static boolean forceStopPackage(String packageName, int userId) {
        try {
            LSPManagerServiceClient.forceStopPackage(packageName, userId);
            return true;
        } catch (RemoteException | NullPointerException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return false;
        }
    }

    public static boolean reboot(boolean confirm, String reason, boolean wait) {
        try {
            LSPManagerServiceClient.reboot(confirm, reason, wait);
            return true;
        } catch (RemoteException | NullPointerException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return false;
        }
    }

    public static boolean uninstallPackage(String packageName, int userId) {
        try {
            return LSPManagerServiceClient.uninstallPackage(packageName, userId);
        } catch (RemoteException | NullPointerException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return false;
        }
    }

    public static boolean isSepolicyLoaded() {
        try {
            return LSPManagerServiceClient.isSepolicyLoaded();
        } catch (RemoteException | NullPointerException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return false;
        }
    }

    public static List<UserInfo> getUsers() {
        try {
            return LSPManagerServiceClient.getUsers();
        } catch (RemoteException | NullPointerException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return null;
        }
    }

    public static boolean installExistingPackageAsUser(String packageName, int userId) {
        final int INSTALL_SUCCEEDED = 1;
        try {
            var ret = LSPManagerServiceClient.installExistingPackageAsUser(packageName, userId);
            return ret == INSTALL_SUCCEEDED;
        } catch (RemoteException | NullPointerException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return false;
        }
    }

    public static boolean isMagiskInstalled() {
        return Arrays.stream(System.getenv("PATH").split(File.pathSeparator))
                .anyMatch(str -> new File(str, "magisk").exists());
    }

    public static boolean systemServerRequested() {
        try {
            return LSPManagerServiceClient.systemServerRequested();
        } catch (Throwable e) {
            return false;
        }
    }

    public static int startActivityAsUserWithFeature(Intent intent, int userId) {
        try {
            return LSPManagerServiceClient.startActivityAsUserWithFeature(intent, userId);
        } catch (Throwable e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return -1;
        }
    }

    public static List<ResolveInfo> queryIntentActivitiesAsUser(Intent intent, int flags, int userId) {
        List<ResolveInfo> list = new ArrayList<>();
        try {
            list.addAll(LSPManagerServiceClient.queryIntentActivitiesAsUser(intent, flags, userId).getList());
        } catch (Throwable e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
        }
        return list;
    }
}
