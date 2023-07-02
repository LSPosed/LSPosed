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

package org.lsposed.lspd.service;

import static android.content.pm.ServiceInfo.FLAG_ISOLATED_PROCESS;
import static org.lsposed.lspd.service.ServiceManager.TAG;
import static org.lsposed.lspd.service.ServiceManager.existsInGlobalNamespace;

import android.content.IIntentReceiver;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.VersionedPackage;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.lsposed.lspd.models.Application;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import rikka.parcelablelist.ParcelableListSlice;

public class PackageService {

    static final int INSTALL_FAILED_INTERNAL_ERROR = -110;
    static final int INSTALL_REASON_UNKNOWN = 0;
    static final int MATCH_ANY_USER = 0x00400000; // PackageManager.MATCH_ANY_USER

    static final int MATCH_ALL_FLAGS = PackageManager.MATCH_DISABLED_COMPONENTS | PackageManager.MATCH_DIRECT_BOOT_AWARE | PackageManager.MATCH_DIRECT_BOOT_UNAWARE | PackageManager.MATCH_UNINSTALLED_PACKAGES | MATCH_ANY_USER;
    public static final int PER_USER_RANGE = 100000;

    private static IPackageManager pm = null;
    private static IBinder binder = null;

    static boolean isAlive() {
        var pm = getPackageManager();
        return pm != null && pm.asBinder().isBinderAlive();
    }

    private static final IBinder.DeathRecipient recipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            Log.w(TAG, "pm is dead");
            binder.unlinkToDeath(this, 0);
            binder = null;
            pm = null;
        }
    };

    private static IPackageManager getPackageManager() {
        if (binder == null || pm == null) {
            binder = ServiceManager.getService("package");
            if (binder == null) return null;
            try {
                binder.linkToDeath(recipient, 0);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
            pm = IPackageManager.Stub.asInterface(binder);
        }
        return pm;
    }

    @Nullable
    public static PackageInfo getPackageInfo(String packageName, int flags, int userId) throws RemoteException {
        IPackageManager pm = getPackageManager();
        if (pm == null) return null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return pm.getPackageInfo(packageName, (long) flags, userId);
        }
        return pm.getPackageInfo(packageName, flags, userId);
    }

    public static @NonNull
    Map<Integer, PackageInfo> getPackageInfoFromAllUsers(String packageName, int flags) throws RemoteException {
        IPackageManager pm = getPackageManager();
        Map<Integer, PackageInfo> res = new HashMap<>();
        if (pm == null) return res;
        for (var user : UserService.getUsers()) {
            var info = getPackageInfo(packageName, flags, user.id);
            if (info != null && info.applicationInfo != null) res.put(user.id, info);
        }
        return res;
    }

    @Nullable
    public static ApplicationInfo getApplicationInfo(String packageName, int flags, int userId) throws RemoteException {
        IPackageManager pm = getPackageManager();
        if (pm == null) return null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return pm.getApplicationInfo(packageName, (long) flags, userId);
        }
        return pm.getApplicationInfo(packageName, flags, userId);
    }

    // Only for manager
    public static ParcelableListSlice<PackageInfo> getInstalledPackagesFromAllUsers(int flags, boolean filterNoProcess) throws RemoteException {
        List<PackageInfo> res = new ArrayList<>();
        IPackageManager pm = getPackageManager();
        if (pm == null) return ParcelableListSlice.emptyList();
        for (var user : UserService.getUsers()) {
            // in case pkginfo of other users in primary user
            ParceledListSlice<PackageInfo> infos;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                infos = pm.getInstalledPackages((long) flags, user.id);
            } else {
                infos = pm.getInstalledPackages(flags, user.id);
            }
            res.addAll(infos
                    .getList().parallelStream()
                    .filter(info -> info.applicationInfo != null && info.applicationInfo.uid / PER_USER_RANGE == user.id)
                    .filter(info -> {
                        try {
                            return isPackageAvailable(info.packageName, user.id, true);
                        } catch (RemoteException e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList()));
        }
        if (filterNoProcess) {
            return new ParcelableListSlice<>(res.parallelStream().filter(packageInfo -> {
                try {
                    PackageInfo pkgInfo = getPackageInfoWithComponents(packageInfo.packageName, MATCH_ALL_FLAGS, packageInfo.applicationInfo.uid / PER_USER_RANGE);
                    return !fetchProcesses(pkgInfo).isEmpty();
                } catch (RemoteException e) {
                    Log.w(TAG, "filter failed", e);
                    return true;
                }
            }).collect(Collectors.toList()));
        }
        return new ParcelableListSlice<>(res);
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
        PackageInfo pkgInfo = getPackageInfoWithComponents(app.packageName, MATCH_ALL_FLAGS, app.userId);
        if (pkgInfo == null || pkgInfo.applicationInfo == null)
            return new Pair<>(Collections.emptySet(), -1);
        return new Pair<>(fetchProcesses(pkgInfo), pkgInfo.applicationInfo.uid);
    }

    public static boolean isPackageAvailable(String packageName, int userId, boolean ignoreHidden) throws RemoteException {
        return pm.isPackageAvailable(packageName, userId) || (ignoreHidden && pm.getApplicationHiddenSettingAsUser(packageName, userId));
    }

    @SuppressWarnings({"ConstantConditions", "SameParameterValue"})
    @Nullable
    private static PackageInfo getPackageInfoWithComponents(String packageName, int flags, int userId) throws RemoteException {
        IPackageManager pm = getPackageManager();
        if (pm == null) return null;
        PackageInfo pkgInfo;
        try {
            pkgInfo = getPackageInfo(packageName, flags | PackageManager.GET_ACTIVITIES | PackageManager.GET_SERVICES | PackageManager.GET_RECEIVERS | PackageManager.GET_PROVIDERS, userId);
        } catch (Exception e) {
            pkgInfo = getPackageInfo(packageName, flags, userId);
            if (pkgInfo == null) return null;
            try {
                pkgInfo.activities = getPackageInfo(packageName, flags | PackageManager.GET_ACTIVITIES, userId).activities;
            } catch (Exception ignored) {

            }
            try {
                pkgInfo.services = getPackageInfo(packageName, flags | PackageManager.GET_SERVICES, userId).services;
            } catch (Exception ignored) {

            }
            try {
                pkgInfo.receivers = getPackageInfo(packageName, flags | PackageManager.GET_RECEIVERS, userId).receivers;
            } catch (Exception ignored) {

            }
            try {
                pkgInfo.providers = getPackageInfo(packageName, flags | PackageManager.GET_PROVIDERS, userId).providers;
            } catch (Exception ignored) {

            }
        }
        if (pkgInfo == null || pkgInfo.applicationInfo == null || (!pkgInfo.packageName.equals("android") && (pkgInfo.applicationInfo.sourceDir == null || !existsInGlobalNamespace(pkgInfo.applicationInfo.sourceDir) || !isPackageAvailable(packageName, userId, true))))
            return null;
        return pkgInfo;
    }

    static abstract class IntentSenderAdaptor extends IIntentSender.Stub {
        public abstract void send(Intent intent);

        @Override
        public int send(int code, Intent intent, String resolvedType, IIntentReceiver finishedReceiver, String requiredPermission, Bundle options) {
            send(intent);
            return 0;
        }

        @Override
        public void send(int code, Intent intent, String resolvedType, IBinder whitelistToken, IIntentReceiver finishedReceiver, String requiredPermission, Bundle options) {
            send(intent);
        }

        public IntentSender getIntentSender() throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException {
            @SuppressWarnings("JavaReflectionMemberAccess")
            Constructor<IntentSender> intentSenderConstructor = IntentSender.class.getConstructor(IIntentSender.class);
            intentSenderConstructor.setAccessible(true);
            return intentSenderConstructor.newInstance(this);
        }
    }

    public static boolean uninstallPackage(VersionedPackage versionedPackage, int userId) throws RemoteException, InterruptedException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = {false};
        var flag = userId == -1 ? 0x00000002 : 0; //PackageManager.DELETE_ALL_USERS = 0x00000002; UserHandle ALL = new UserHandle(-1);
        pm.getPackageInstaller().uninstall(versionedPackage, "android", flag, new IntentSenderAdaptor() {
            @Override
            public void send(Intent intent) {
                int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
                result[0] = status == PackageInstaller.STATUS_SUCCESS;
                Log.d(TAG, intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE));
                latch.countDown();
            }
        }.getIntentSender(), userId == -1 ? 0 : userId);
        latch.await();
        return result[0];
    }

    public static int installExistingPackageAsUser(String packageName, int userId) throws RemoteException {
        IPackageManager pm = getPackageManager();
        Log.d(TAG, "about to install existing package " + packageName + "/" + userId);
        if (pm == null) return INSTALL_FAILED_INTERNAL_ERROR;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return pm.installExistingPackageAsUser(packageName, userId, 0, INSTALL_REASON_UNKNOWN, null);
        } else {
            return pm.installExistingPackageAsUser(packageName, userId, 0, INSTALL_REASON_UNKNOWN);
        }
    }

    @Nullable
    public static ParcelableListSlice<ResolveInfo> queryIntentActivities(Intent intent, String resolvedType, int flags, int userId) {
        try {
            IPackageManager pm = getPackageManager();
            if (pm == null) return null;
            ParceledListSlice<ResolveInfo> infos;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                infos = pm.queryIntentActivities(intent, resolvedType, (long) flags, userId);
            } else {
                infos = pm.queryIntentActivities(intent, resolvedType, flags, userId);
            }
            return new ParcelableListSlice<>(infos.getList());
        } catch (Exception e) {
            Log.e(TAG, "queryIntentActivities", e);
            return new ParcelableListSlice<>(new ArrayList<>());
        }
    }

    @Nullable
    public static Intent getLaunchIntentForPackage(String packageName) throws RemoteException {
        Intent intentToResolve = new Intent(Intent.ACTION_MAIN);
        intentToResolve.addCategory(Intent.CATEGORY_INFO);
        intentToResolve.setPackage(packageName);
        var ris = queryIntentActivities(intentToResolve, intentToResolve.getType(), 0, 0);

        // Otherwise, try to find a main launcher activity.
        if (ris == null || ris.getList().size() == 0) {
            // reuse the intent instance
            intentToResolve.removeCategory(Intent.CATEGORY_INFO);
            intentToResolve.addCategory(Intent.CATEGORY_LAUNCHER);
            intentToResolve.setPackage(packageName);
            ris = queryIntentActivities(intentToResolve, intentToResolve.getType(), 0, 0);
        }
        if (ris == null || ris.getList().size() == 0) {
            return null;
        }
        Intent intent = new Intent(intentToResolve);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClassName(ris.getList().get(0).activityInfo.packageName,
                ris.getList().get(0).activityInfo.name);
        return intent;
    }

    public static void clearApplicationProfileData(String packageName) throws RemoteException {
        IPackageManager pm = getPackageManager();
        if (pm == null) return;
        pm.clearApplicationProfileData(packageName);
    }

    public static boolean performDexOptMode(String packageName) throws RemoteException {
        IPackageManager pm = getPackageManager();
        if (pm == null) return false;
        return pm.performDexOptMode(packageName,
                SystemProperties.getBoolean("dalvik.vm.usejitprofiles", false),
                SystemProperties.get("pm.dexopt.install", "speed-profile"), true, true, null);
    }
}
