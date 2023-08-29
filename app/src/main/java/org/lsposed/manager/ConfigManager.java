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

import org.lsposed.lspd.ILSPManagerService;
import org.lsposed.lspd.models.Application;
import org.lsposed.lspd.models.UserInfo;
import org.lsposed.manager.adapters.ScopeAdapter;
import org.lsposed.manager.receivers.LSPManagerServiceHolder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ConfigManager {

    public static boolean isBinderAlive() {
        return LSPManagerServiceHolder.getService() != null;
    }

    public static int getXposedApiVersion() {
        try {
            return LSPManagerServiceHolder.getService().getXposedApiVersion();
        } catch (RemoteException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return -1;
        }
    }

    public static String getXposedVersionName() {
        try {
            return LSPManagerServiceHolder.getService().getXposedVersionName();
        } catch (RemoteException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return "";
        }
    }

    public static int getXposedVersionCode() {
        try {
            return LSPManagerServiceHolder.getService().getXposedVersionCode();
        } catch (RemoteException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return -1;
        }
    }

    public static List<PackageInfo> getInstalledPackagesFromAllUsers(int flags, boolean filterNoProcess) {
        List<PackageInfo> list = new ArrayList<>();
        try {
            list.addAll(LSPManagerServiceHolder.getService().getInstalledPackagesFromAllUsers(flags, filterNoProcess).getList());
        } catch (RemoteException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
        }
        return list;
    }

    public static String[] getEnabledModules() {
        try {
            return LSPManagerServiceHolder.getService().enabledModules();
        } catch (RemoteException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return new String[0];
        }
    }

    public static boolean setModuleEnabled(String packageName, boolean enable) {
        try {
            return enable ? LSPManagerServiceHolder.getService().enableModule(packageName) : LSPManagerServiceHolder.getService().disableModule(packageName);
        } catch (RemoteException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return false;
        }
    }

    public static boolean setModuleScope(String packageName, boolean legacy, Set<ScopeAdapter.ApplicationWithEquals> applications) {
        try {
            List<Application> list = new ArrayList<>();
            applications.forEach(application -> {
                Application app = new Application();
                app.userId = application.userId;
                app.packageName = application.packageName;
                list.add(app);
            });
            if (legacy) {
                Application app = new Application();
                app.userId = 0;
                app.packageName = packageName;
                list.add(app);
            }
            return LSPManagerServiceHolder.getService().setModuleScope(packageName, list);
        } catch (RemoteException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return false;
        }
    }

    public static List<ScopeAdapter.ApplicationWithEquals> getModuleScope(String packageName) {
        List<ScopeAdapter.ApplicationWithEquals> list = new ArrayList<>();
        try {
            var applications = LSPManagerServiceHolder.getService().getModuleScope(packageName);
            if (applications == null) {
                return list;
            }
            applications.forEach(application -> {
                if (!application.packageName.equals(packageName)) {
                    list.add(new ScopeAdapter.ApplicationWithEquals(application));
                }
            });
        } catch (RemoteException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
        }
        return list;
    }

    public static boolean enableStatusNotification() {
        try {
            return LSPManagerServiceHolder.getService().enableStatusNotification();
        } catch (RemoteException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return false;
        }
    }

    public static boolean setEnableStatusNotification(boolean enabled) {
        try {
            LSPManagerServiceHolder.getService().setEnableStatusNotification(enabled);
            return true;
        } catch (RemoteException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return false;
        }
    }

    public static boolean isVerboseLogEnabled() {
        try {
            return LSPManagerServiceHolder.getService().isVerboseLog();
        } catch (RemoteException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return false;
        }
    }

    public static boolean setVerboseLogEnabled(boolean enabled) {
        try {
            LSPManagerServiceHolder.getService().setVerboseLog(enabled);
            return true;
        } catch (RemoteException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return false;
        }
    }

    public static ParcelFileDescriptor getLog(boolean verbose) {
        try {
            return verbose ? LSPManagerServiceHolder.getService().getVerboseLog() : LSPManagerServiceHolder.getService().getModulesLog();
        } catch (RemoteException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return null;
        }
    }

    public static boolean clearLogs(boolean verbose) {
        try {
            return LSPManagerServiceHolder.getService().clearLogs(verbose);
        } catch (RemoteException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return false;
        }
    }

    public static PackageInfo getPackageInfo(String packageName, int flags, int userId) throws PackageManager.NameNotFoundException {
        try {
            var info = LSPManagerServiceHolder.getService().getPackageInfo(packageName, flags, userId);
            if (info == null) throw new PackageManager.NameNotFoundException();
            return info;
        } catch (RemoteException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            throw new PackageManager.NameNotFoundException();
        }
    }

    public static boolean forceStopPackage(String packageName, int userId) {
        try {
            LSPManagerServiceHolder.getService().forceStopPackage(packageName, userId);
            return true;
        } catch (RemoteException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return false;
        }
    }

    public static boolean reboot() {
        try {
            LSPManagerServiceHolder.getService().reboot();
            return true;
        } catch (RemoteException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return false;
        }
    }

    public static boolean uninstallPackage(String packageName, int userId) {
        try {
            return LSPManagerServiceHolder.getService().uninstallPackage(packageName, userId);
        } catch (RemoteException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return false;
        }
    }

    public static boolean isSepolicyLoaded() {
        try {
            return LSPManagerServiceHolder.getService().isSepolicyLoaded();
        } catch (RemoteException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return false;
        }
    }

    public static List<UserInfo> getUsers() {
        try {
            return LSPManagerServiceHolder.getService().getUsers();
        } catch (RemoteException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return null;
        }
    }

    public static boolean installExistingPackageAsUser(String packageName, int userId) {
        final int INSTALL_SUCCEEDED = 1;
        try {
            var ret = LSPManagerServiceHolder.getService().installExistingPackageAsUser(packageName, userId);
            return ret == INSTALL_SUCCEEDED;
        } catch (RemoteException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return false;
        }
    }

    public static boolean isMagiskInstalled() {
        var path = System.getenv("PATH");
        if (path == null) return false;
        else return Arrays.stream(path.split(File.pathSeparator))
                .anyMatch(str -> new File(str, "magisk").exists());
    }

    public static boolean systemServerRequested() {
        try {
            return LSPManagerServiceHolder.getService().systemServerRequested();
        } catch (RemoteException e) {
            return false;
        }
    }

    public static boolean dex2oatFlagsLoaded() {
        try {
            return LSPManagerServiceHolder.getService().dex2oatFlagsLoaded();
        } catch (RemoteException e) {
            return false;
        }
    }

    public static int startActivityAsUserWithFeature(Intent intent, int userId) {
        try {
            return LSPManagerServiceHolder.getService().startActivityAsUserWithFeature(intent, userId);
        } catch (RemoteException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return -1;
        }
    }

    public static List<ResolveInfo> queryIntentActivitiesAsUser(Intent intent, int flags, int userId) {
        List<ResolveInfo> list = new ArrayList<>();
        try {
            list.addAll(LSPManagerServiceHolder.getService().queryIntentActivitiesAsUser(intent, flags, userId).getList());
        } catch (RemoteException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
        }
        return list;
    }

    public static boolean setHiddenIcon(boolean hide) {
        try {
            LSPManagerServiceHolder.getService().setHiddenIcon(hide);
            return true;
        } catch (RemoteException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return false;
        }
    }

    public static String getApi() {
        try {
            return LSPManagerServiceHolder.getService().getApi();
        } catch (RemoteException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return e.toString();
        }
    }

    public static List<String> getDenyListPackages() {
        List<String> list = new ArrayList<>();
        try {
            list.addAll(LSPManagerServiceHolder.getService().getDenyListPackages());
        } catch (RemoteException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
        }
        return list;
    }

    public static void flashZip(String zipPath, ParcelFileDescriptor outputStream) {
        try {
            LSPManagerServiceHolder.getService().flashZip(zipPath, outputStream);
        } catch (RemoteException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
        }
    }

    public static boolean isDexObfuscateEnabled() {
        try {
            return LSPManagerServiceHolder.getService().getDexObfuscate();
        } catch (RemoteException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return false;
        }
    }

    public static boolean setDexObfuscateEnabled(boolean enabled) {
        try {
            LSPManagerServiceHolder.getService().setDexObfuscate(enabled);
            return true;
        } catch (RemoteException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return false;
        }
    }

    public static int getDex2OatWrapperCompatibility() {
        try {
            return LSPManagerServiceHolder.getService().getDex2OatWrapperCompatibility();
        } catch (RemoteException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return ILSPManagerService.DEX2OAT_CRASHED;
        }
    }
}
