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

package org.lsposed.manager.receivers;

import android.content.pm.PackageInfo;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import org.lsposed.lspd.Application;
import org.lsposed.lspd.ILSPManagerService;
import org.lsposed.lspd.utils.ParceledListSlice;

import java.util.List;

public class LSPosedManagerServiceClient {

    @SuppressWarnings("FieldMayBeFinal")
    private static IBinder binder = null;
    private static ILSPManagerService service = null;

    private static void ensureService() throws NullPointerException {
        if (service == null) {
            if (binder != null) {
                service = ILSPManagerService.Stub.asInterface(binder);
            } else {
                throw new NullPointerException("binder is null");
            }
        }
    }

    public static int getXposedApiVersion() throws RemoteException, NullPointerException {
        ensureService();
        return service.getXposedApiVersion();
    }

    public static String getXposedVersionName() throws RemoteException, NullPointerException {
        ensureService();
        return service.getXposedVersionName();
    }

    public static int getXposedVersionCode() throws RemoteException, NullPointerException {
        ensureService();
        return service.getXposedVersionCode();
    }


    public static List<PackageInfo> getInstalledPackagesFromAllUsers(int flags, boolean filterNoProcess) throws RemoteException, NullPointerException {
        ensureService();
        ParceledListSlice<PackageInfo> parceledListSlice = service.getInstalledPackagesFromAllUsers(flags, filterNoProcess);
        //
        return parceledListSlice.getList();
    }

    public static String[] enabledModules() throws RemoteException, NullPointerException {
        ensureService();
        return service.enabledModules();
    }

    public static boolean enableModule(String packageName) throws RemoteException, NullPointerException {
        ensureService();
        return service.enableModule(packageName);
    }

    public static boolean disableModule(String packageName) throws RemoteException, NullPointerException {
        ensureService();
        return service.disableModule(packageName);
    }

    public static boolean setModuleScope(String packageName, ParceledListSlice<Application> list) throws RemoteException, NullPointerException {
        ensureService();
        return service.setModuleScope(packageName, list);
    }

    public static ParceledListSlice<Application> getModuleScope(String packageName) throws RemoteException, NullPointerException {
        ensureService();
        return service.getModuleScope(packageName);
    }

    public static boolean isResourceHook() throws RemoteException, NullPointerException {
        ensureService();
        return service.isResourceHook();
    }

    public static void setResourceHook(boolean enabled) throws RemoteException, NullPointerException {
        ensureService();
        service.setResourceHook(enabled);
    }

    public static boolean isVerboseLog() throws RemoteException, NullPointerException {
        ensureService();
        return service.isVerboseLog();
    }

    public static void setVerboseLog(boolean enabled) throws RemoteException, NullPointerException {
        ensureService();
        service.setVerboseLog(enabled);
    }

    public static ParcelFileDescriptor getVerboseLog() throws RemoteException, NullPointerException {
        ensureService();
        return service.getVerboseLog();
    }

    public static ParcelFileDescriptor getModulesLog() throws RemoteException, NullPointerException {
        ensureService();
        return service.getModulesLog();
    }

    public static boolean clearLogs(boolean verbose) throws RemoteException, NullPointerException {
        ensureService();
        return service.clearLogs(verbose);
    }

    public static PackageInfo getPackageInfo(String packageName, int flags, int uid) throws RemoteException, NullPointerException {
        ensureService();
        return service.getPackageInfo(packageName, flags, uid);
    }

    public static void forceStopPackage(String packageName, int userId) throws RemoteException, NullPointerException {
        ensureService();
        service.forceStopPackage(packageName, userId);
    }

    public static void reboot(boolean confirm, String reason, boolean wait) throws RemoteException, NullPointerException {
        ensureService();
        service.reboot(confirm, reason, wait);
    }

    public static boolean uninstallPackage(String packageName) throws RemoteException, NullPointerException {
        ensureService();
        return service.uninstallPackage(packageName);
    }

    public static boolean isSepolicyLoaded() throws RemoteException, NullPointerException {
        ensureService();
        return service.isSepolicyLoaded();
    }
}
