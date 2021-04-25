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

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.VersionedPackage;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import de.robv.android.xposed.XposedBridge;
import org.lsposed.lspd.Application;
import org.lsposed.lspd.BuildConfig;
import org.lsposed.lspd.ILSPManagerService;
import org.lsposed.lspd.utils.ParceledListSlice;

import static org.lsposed.lspd.service.ServiceManager.TAG;

public class LSPManagerService extends ILSPManagerService.Stub {

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
        PackageInfo pkgInfo = PackageService.getPackageInfo(packageName, 0, 0);
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
    public boolean uninstallPackage(String packageName) throws RemoteException {
        try {
            return PackageService.uninstallPackage(new VersionedPackage(packageName, PackageManager.VERSION_CODE_HIGHEST));
        } catch (InterruptedException | InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            Log.e(TAG, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean isSepolicyLoaded() throws RemoteException {
        return ConfigManager.getInstance().isSepolicyLoaded();
    }
}
