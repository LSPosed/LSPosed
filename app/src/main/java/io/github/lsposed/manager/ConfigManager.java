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

package io.github.lsposed.manager;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import io.github.lsposed.lspd.Application;
import io.github.lsposed.lspd.utils.ParceledListSlice;
import io.github.lsposed.manager.adapters.ScopeAdapter;
import io.github.lsposed.manager.receivers.LSPosedManagerServiceClient;

public class ConfigManager {

    public static int getXposedApiVersion() {
        try {
            return LSPosedManagerServiceClient.getXposedApiVersion();
        } catch (RemoteException | NullPointerException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return -1;
        }
    }

    public static String getXposedVersionName() {
        try {
            return LSPosedManagerServiceClient.getXposedVersionName();
        } catch (RemoteException | NullPointerException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return null;
        }
    }

    public static int getXposedVersionCode() {
        try {
            return LSPosedManagerServiceClient.getXposedVersionCode();
        } catch (RemoteException | NullPointerException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return -1;
        }
    }

    public static List<PackageInfo> getInstalledPackagesFromAllUsers(int flags, boolean filterNoProcess) {
        List<PackageInfo> list = new ArrayList<>();
        try {
            list.addAll(LSPosedManagerServiceClient.getInstalledPackagesFromAllUsers(flags, filterNoProcess));
        } catch (RemoteException | NullPointerException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
        }
        return list;
    }

    public static String[] getEnabledModules() {
        try {
            return LSPosedManagerServiceClient.enabledModules();
        } catch (RemoteException | NullPointerException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return new String[0];
        }
    }

    public static boolean setModuleEnabled(String packageName, boolean enable) {
        try {
            return enable ? LSPosedManagerServiceClient.enableModule(packageName) : LSPosedManagerServiceClient.disableModule(packageName);
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
            return LSPosedManagerServiceClient.setModuleScope(packageName, new ParceledListSlice<>(list));
        } catch (RemoteException | NullPointerException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return false;
        }
    }

    public static List<ScopeAdapter.ApplicationWithEquals> getModuleScope(String packageName) {
        List<ScopeAdapter.ApplicationWithEquals> list = new ArrayList<>();
        try {
            List<Application> applications = LSPosedManagerServiceClient.getModuleScope(packageName).getList();
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
            return LSPosedManagerServiceClient.isResourceHook();
        } catch (RemoteException | NullPointerException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return false;
        }
    }

    public static boolean setResourceHookEnabled(boolean enabled) {
        try {
            LSPosedManagerServiceClient.setResourceHook(enabled);
            return true;
        } catch (RemoteException | NullPointerException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return false;
        }
    }

    public static boolean isVerboseLogEnabled() {
        try {
            return LSPosedManagerServiceClient.isVerboseLog();
        } catch (RemoteException | NullPointerException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return false;
        }
    }

    public static boolean setVerboseLogEnabled(boolean enabled) {
        try {
            LSPosedManagerServiceClient.setVerboseLog(enabled);
            return true;
        } catch (RemoteException | NullPointerException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return false;
        }
    }

    public static int getVariant() {
        try {
            return LSPosedManagerServiceClient.getVariant();
        } catch (RemoteException | NullPointerException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return 1;
        }
    }

    public static String getVariantString() {
        int variant = getVariant();
        switch (variant) {
            case 1:
                return "YAHFA";
            case 2:
                return "SandHook";
            default:
                return "Unknown";
        }
    }

    public static boolean setVariant(int variant) {
        try {
            LSPosedManagerServiceClient.setVariant(variant);
            return true;
        } catch (RemoteException | NullPointerException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return false;
        }
    }

    public static boolean isPermissive() {
        try {
            return LSPosedManagerServiceClient.isPermissive();
        } catch (RemoteException | NullPointerException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return true;
        }
    }

    public static ParcelFileDescriptor getLogs(boolean verbose) {
        try {
            return verbose ? LSPosedManagerServiceClient.getVerboseLog() : LSPosedManagerServiceClient.getModulesLog();
        } catch (RemoteException | NullPointerException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return null;
        }
    }

    public static boolean clearLogs(boolean verbose) {
        try {
            return LSPosedManagerServiceClient.clearLogs(verbose);
        } catch (RemoteException | NullPointerException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            return false;
        }
    }

    public static PackageInfo getPackageInfo(String packageName, int flags) throws PackageManager.NameNotFoundException {
        try {
            return LSPosedManagerServiceClient.getPackageInfo(packageName, flags, 0);
        } catch (RemoteException | NullPointerException e) {
            Log.e(App.TAG, Log.getStackTraceString(e));
            throw new PackageManager.NameNotFoundException();
        }
    }
}
