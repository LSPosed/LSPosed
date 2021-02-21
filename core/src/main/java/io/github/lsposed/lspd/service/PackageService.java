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

import android.content.pm.ComponentInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import io.github.lsposed.lspd.Application;
import io.github.lsposed.lspd.utils.ParceledListSlice;

import static android.content.pm.ServiceInfo.FLAG_ISOLATED_PROCESS;

public class PackageService {
    private static IPackageManager pm = null;
    private static IBinder binder = null;

    public static IPackageManager getPackageManager() {
        if (binder == null && pm == null) {
            binder = ServiceManager.getService("package");
            pm = IPackageManager.Stub.asInterface(binder);
        }
        return pm;
    }

    public static PackageInfo getPackageInfo(String packageName, int flags, int uid) throws RemoteException {
        IPackageManager pm = getPackageManager();
        if (pm == null) return null;
        return pm.getPackageInfo(packageName, flags, uid);
    }

    public static String[] getPackagesForUid(int uid) throws RemoteException {
        IPackageManager pm = getPackageManager();
        if (pm == null) return new String[0];
        return pm.getPackagesForUid(uid);
    }

    public static ParceledListSlice<PackageInfo> getInstalledPackagesFromAllUsers(int flags) throws RemoteException {
        ArrayList<PackageInfo> res = new ArrayList<>();
        IPackageManager pm = getPackageManager();
        if (pm == null) return ParceledListSlice.emptyList();
        for (int userId : UserService.getUsers()) {
            res.addAll(pm.getInstalledPackages(flags, userId).getList());
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
        PackageInfo pkgInfo = pm.getPackageInfo(app.packageName, PackageManager.MATCH_DISABLED_COMPONENTS |
                PackageManager.MATCH_UNINSTALLED_PACKAGES | PackageManager.GET_ACTIVITIES | PackageManager.MATCH_DIRECT_BOOT_AWARE | PackageManager.MATCH_DIRECT_BOOT_UNAWARE |
                PackageManager.GET_SERVICES | PackageManager.GET_RECEIVERS | PackageManager.GET_PROVIDERS, app.userId);
        if (pkgInfo == null || pkgInfo.applicationInfo == null)
            return new Pair<>(Collections.emptySet(), -1);
        return new Pair<>(fetchProcesses(pkgInfo), pkgInfo.applicationInfo.uid);
    }
}
