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

import static android.content.Context.BIND_AUTO_CREATE;
import static org.lsposed.lspd.service.ServiceManager.TAG;

import android.app.IServiceConnection;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.VersionedPackage;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Log;

import org.lsposed.lspd.BuildConfig;
import org.lsposed.lspd.ILSPManagerService;
import org.lsposed.lspd.models.Application;
import org.lsposed.lspd.models.UserInfo;
import org.lsposed.lspd.utils.ParceledListSlice;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;

import de.robv.android.xposed.XposedBridge;

public class LSPManagerService extends ILSPManagerService.Stub {

    public Object guard = null;

    private static final String PROP_NAME = "dalvik.vm.dex2oat-flags";
    private static final String PROP_VALUE = "--inline-max-code-units=0";

    public class ManagerGuard implements IBinder.DeathRecipient {
        private final IBinder binder;
        private final IServiceConnection connection = new IServiceConnection.Stub() {
            @Override
            public void connected(ComponentName name, IBinder service, boolean dead) {
            }
        };

        public ManagerGuard(IBinder binder) {
            guard = this;
            this.binder = binder;
            try {
                this.binder.linkToDeath(this, 0);
                var intent = new Intent();
                intent.setComponent(ComponentName.unflattenFromString("com.miui.securitycore/com.miui.xspace.service.XSpaceService"));
                ActivityManagerService.bindService(intent, intent.getType(), connection, BIND_AUTO_CREATE, "android", 0);
            } catch (Throwable e) {
                Log.e(TAG, "manager guard", e);
            }
        }

        @Override
        public void binderDied() {
            try {
                if (binder != null) binder.unlinkToDeath(this, 0);
                ActivityManagerService.unbindService(connection);
            } catch (Throwable e) {
                Log.e(TAG, "manager guard", e);
            }
            guard = null;
        }
    }

    LSPManagerService() {
    }

    @Override
    public IBinder asBinder() {
        return this;
    }

    @Override
    public int getXposedApiVersion() {
        return XposedBridge.getXposedVersion();
    }

    @Override
    public int getXposedVersionCode() {
        return BuildConfig.VERSION_CODE;
    }

    @Override
    public String getXposedVersionName() {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public ParceledListSlice<PackageInfo> getInstalledPackagesFromAllUsers(int flags, boolean filterNoProcess) throws RemoteException {
        return PackageService.getInstalledPackagesFromAllUsers(flags, filterNoProcess);
    }

    @Override
    public String[] enabledModules() {
        return ConfigManager.getInstance().enabledModules();
    }

    @Override
    public boolean enableModule(String packageName) throws RemoteException {
        PackageInfo pkgInfo = PackageService.getPackageInfo(packageName, PackageService.MATCH_ALL_FLAGS, 0);
        if (pkgInfo == null) return false;
        return ConfigManager.getInstance().enableModule(packageName, pkgInfo.applicationInfo.sourceDir);
    }

    @Override
    public boolean setModuleScope(String packageName, ParceledListSlice<Application> scope) {
        return ConfigManager.getInstance().setModuleScope(packageName, scope.getList());
    }

    @Override
    public ParceledListSlice<Application> getModuleScope(String packageName) {
        List<Application> list = ConfigManager.getInstance().getModuleScope(packageName);
        if (list == null) return null;
        else return new ParceledListSlice<>(list);
    }

    @Override
    public boolean disableModule(String packageName) {
        return ConfigManager.getInstance().disableModule(packageName);
    }

    @Override
    public boolean isResourceHook() {
        return ConfigManager.getInstance().resourceHook();
    }

    @Override
    public void setResourceHook(boolean enabled) {
        ConfigManager.getInstance().setResourceHook(enabled);
    }

    @Override
    public boolean isVerboseLog() {
        return ConfigManager.getInstance().verboseLog();
    }

    @Override
    public void setVerboseLog(boolean enabled) {
        ConfigManager.getInstance().setVerboseLog(enabled);
    }

    @Override
    public ParcelFileDescriptor getVerboseLog() {
        return ConfigManager.getInstance().getVerboseLog();
    }

    @Override
    public ParcelFileDescriptor getModulesLog() {
        return ConfigManager.getInstance().getModulesLog(ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public boolean clearLogs(boolean verbose) {
        return ConfigManager.getInstance().clearLogs(verbose);
    }

    @Override
    public PackageInfo getPackageInfo(String packageName, int flags, int uid) throws RemoteException {
        return PackageService.getPackageInfo(packageName, flags, uid);
    }

    @Override
    public void forceStopPackage(String packageName, int userId) throws RemoteException {
        ActivityManagerService.forceStopPackage(packageName, userId);
    }

    @Override
    public void reboot(boolean confirm, String reason, boolean wait) throws RemoteException {
        PowerService.reboot(confirm, reason, wait);
    }

    @Override
    public boolean uninstallPackage(String packageName, int userId) throws RemoteException {
        try {
            if (ActivityManagerService.startUserInBackground(userId))
                return PackageService.uninstallPackage(new VersionedPackage(packageName, PackageManager.VERSION_CODE_HIGHEST), userId);
            else return false;
        } catch (InterruptedException | InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            Log.e(TAG, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean isSepolicyLoaded() {
        return ConfigManager.getInstance().isSepolicyLoaded();
    }

    @Override
    public List<UserInfo> getUsers() throws RemoteException {
        var users = new LinkedList<UserInfo>();
        for (var user : UserService.getUsers()) {
            var info = new UserInfo();
            info.id = user.id;
            info.name = user.name;
            users.add(info);
        }
        return users;
    }

    @Override
    public int installExistingPackageAsUser(String packageName, int userId) {
        try {
            if (ActivityManagerService.startUserInBackground(userId))
                return PackageService.installExistingPackageAsUser(packageName, userId);
            else return PackageService.INSTALL_FAILED_INTERNAL_ERROR;
        } catch (Throwable e) {
            Log.w(TAG, "install existing package as user: ", e);
            return PackageService.INSTALL_FAILED_INTERNAL_ERROR;
        }
    }

    @Override
    public boolean systemServerRequested() throws RemoteException {
        return ServiceManager.systemServerRequested();
    }

    @Override
    public int startActivityAsUserWithFeature(Intent intent, int userId) throws RemoteException {
        if (!intent.getBooleanExtra("lsp_no_switch_to_user", false)) {
            intent.removeExtra("lsp_no_switch_to_user");
            var currentUser = ActivityManagerService.getCurrentUser();
            if (currentUser == null) return -1;
            var parent = UserService.getProfileParent(userId);
            if (parent < 0) return -1;
            if (currentUser.id != parent && !ActivityManagerService.switchUser(parent)) return -1;
        }
        return ActivityManagerService.startActivityAsUserWithFeature("android", null, intent, intent.getType(), null, null, 0, 0, null, null, userId);
    }

    @Override
    public ParceledListSlice<ResolveInfo> queryIntentActivitiesAsUser(Intent intent, int flags, int userId) throws RemoteException {
        return PackageService.queryIntentActivities(intent, intent.getType(), flags, userId);
    }

    @Override
    public boolean dex2oatFlagsLoaded() {
//        var splitFlags = new ArrayList<>(Arrays.asList(flags.split(" ")));
//        splitFlags.add(PROP_VALUE);
//        SystemProperties.set(PROP_NAME, String.join(" ", splitFlags));
        return SystemProperties.get(PROP_NAME).contains(PROP_VALUE);
    }
}
