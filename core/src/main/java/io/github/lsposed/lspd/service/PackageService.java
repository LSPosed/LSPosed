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

package io.github.lsposed.lspd.service;

import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.util.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.lsposed.lspd.Application;
import io.github.lsposed.lspd.utils.ParceledListSlice;

import static android.content.pm.ServiceInfo.FLAG_ISOLATED_PROCESS;
import static io.github.lsposed.lspd.service.ServiceManager.TAG;

public class PackageService {
    private static IPackageManager pm = null;
    private static IBinder binder = null;

    public static IPackageManager getPackageManager() {
        if (binder == null && pm == null) {
            binder = ServiceManager.getService("package");
            if (binder == null) return null;
            try {
                binder.linkToDeath(new IBinder.DeathRecipient() {
                    @Override
                    public void binderDied() {
                        Log.w(TAG, "pm is dead");
                        binder.unlinkToDeath(this, 0);
                        binder = null;
                        pm = null;
                    }
                }, 0);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
            pm = IPackageManager.Stub.asInterface(binder);
        }
        return pm;
    }

    public static PackageInfo getPackageInfo(String packageName, int flags, int userId) throws RemoteException {
        IPackageManager pm = getPackageManager();
        if (pm == null) return null;
        return pm.getPackageInfo(packageName, flags, userId);
    }

    public static ApplicationInfo getApplicationInfo(String packageName, int flags, int userId) throws RemoteException {
        IPackageManager pm = getPackageManager();
        if (pm == null) return null;
        return pm.getApplicationInfo(packageName, flags, userId);
    }

    public static String[] getPackagesForUid(int uid) throws RemoteException {
        IPackageManager pm = getPackageManager();
        if (pm == null) return new String[0];
        return pm.getPackagesForUid(uid);
    }

    public static ParceledListSlice<PackageInfo> getInstalledPackagesFromAllUsers(int flags, boolean filterNoProcess) throws RemoteException {
        List<PackageInfo> res = new ArrayList<>();
        IPackageManager pm = getPackageManager();
        if (pm == null) return ParceledListSlice.emptyList();
        for (int userId : UserService.getUsers()) {
            res.addAll(pm.getInstalledPackages(flags, userId).getList());
        }
        if (filterNoProcess) {
            res = res.stream().filter(packageInfo -> {
                int baseFlag = PackageManager.MATCH_DISABLED_COMPONENTS | PackageManager.MATCH_DIRECT_BOOT_AWARE | PackageManager.MATCH_DIRECT_BOOT_UNAWARE | PackageManager.MATCH_UNINSTALLED_PACKAGES;
                try {
                    PackageInfo pkgInfo = getPackageInfoWithComponents(packageInfo.packageName, baseFlag, packageInfo.applicationInfo.uid / 100000);
                    return !fetchProcesses(pkgInfo).isEmpty();
                } catch (RemoteException e) {
                    return true;
                }
            }).collect(Collectors.toList());
        }
        return new ParceledListSlice<>(res);
    }

    public static void grantRuntimePermission(String packageName, String permissionName, int userId) throws RemoteException {
        IPackageManager pm = getPackageManager();
        if (pm == null) return;
        pm.grantRuntimePermission(packageName, permissionName, userId);
    }

    private static Set<String> fetchProcesses(PackageInfo pkgInfo) {
        HashSet<String> processNames = new HashSet<>();
        if (pkgInfo == null) return processNames;
        for (ComponentInfo[] componentInfos : new ComponentInfo[][]{pkgInfo.activities, pkgInfo.receivers, pkgInfo.providers}) {
            if (componentInfos == null) continue;
            for (ComponentInfo componentInfo : componentInfos) {
                processNames.add(componentInfo.processName);
            }
        }
        if (pkgInfo.services == null) return processNames;
        for (ServiceInfo service : pkgInfo.services) {
            if ((service.flags & FLAG_ISOLATED_PROCESS) == 0) {
                processNames.add(service.processName);
            }
        }
        return processNames;
    }

    public static Pair<Set<String>, Integer> fetchProcessesWithUid(Application app) throws RemoteException {
        IPackageManager pm = getPackageManager();
        if (pm == null) return new Pair<>(Collections.emptySet(), -1);
        int baseFlag = PackageManager.MATCH_DISABLED_COMPONENTS | PackageManager.MATCH_UNINSTALLED_PACKAGES | PackageManager.MATCH_DIRECT_BOOT_AWARE | PackageManager.MATCH_DIRECT_BOOT_UNAWARE;
        PackageInfo pkgInfo = getPackageInfoWithComponents(app.packageName, baseFlag, app.userId);
        if (pkgInfo == null || pkgInfo.applicationInfo == null)
            return new Pair<>(Collections.emptySet(), -1);
        return new Pair<>(fetchProcesses(pkgInfo), pkgInfo.applicationInfo.uid);
    }

    private static PackageInfo getPackageInfoWithComponents(String packageName, int flags, int userId) throws RemoteException {
        PackageInfo pkgInfo;
        try {
            pkgInfo = pm.getPackageInfo(packageName, flags | PackageManager.GET_ACTIVITIES | PackageManager.GET_SERVICES | PackageManager.GET_RECEIVERS | PackageManager.GET_PROVIDERS, userId);
        } catch (Exception e) {
            pkgInfo = pm.getPackageInfo(packageName, flags, userId);
            if (pkgInfo == null) return null;
            try {
                pkgInfo.activities = pm.getPackageInfo(packageName, flags | PackageManager.GET_ACTIVITIES, userId).activities;
            } catch (Exception ignored) {

            }
            try {
                pkgInfo.services = pm.getPackageInfo(packageName, flags | PackageManager.GET_SERVICES, userId).services;
            } catch (Exception ignored) {

            }
            try {
                pkgInfo.receivers = pm.getPackageInfo(packageName, flags | PackageManager.GET_RECEIVERS, userId).receivers;
            } catch (Exception ignored) {

            }
            try {
                pkgInfo.providers = pm.getPackageInfo(packageName, flags | PackageManager.GET_PROVIDERS, userId).providers;
            } catch (Exception ignored) {

            }
        }
        if (pkgInfo == null || pkgInfo.applicationInfo == null || (!pkgInfo.packageName.equals("android") && (pkgInfo.applicationInfo.sourceDir == null || pkgInfo.applicationInfo.deviceProtectedDataDir == null || !new File(pkgInfo.applicationInfo.sourceDir).exists() || !new File(pkgInfo.applicationInfo.deviceProtectedDataDir).exists())))
            return null;
        return pkgInfo;
    }
}
