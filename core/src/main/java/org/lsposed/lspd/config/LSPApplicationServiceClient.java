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

package org.lsposed.lspd.config;

import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import java.io.File;

import org.lsposed.lspd.service.ILSPApplicationService;
import org.lsposed.lspd.util.Utils;

public class LSPApplicationServiceClient implements ILSPApplicationService {
    static ILSPApplicationService service = null;
    static IBinder serviceBinder = null;

    static String baseCachePath = null;
    static String processName = null;

    public static LSPApplicationServiceClient serviceClient = null;

    public static void Init(IBinder binder, String niceName) {
        if (serviceClient == null && binder != null && serviceBinder == null && service == null) {
            serviceBinder = binder;
            processName = niceName;
            try {
                serviceBinder.linkToDeath(
                        new IBinder.DeathRecipient() {
                            @Override
                            public void binderDied() {
                                serviceBinder.unlinkToDeath(this, 0);
                                serviceBinder = null;
                                service = null;
                            }
                        }, 0);
            } catch (RemoteException e) {
                Utils.logE("link to death error: ", e);
            }
            service = ILSPApplicationService.Stub.asInterface(binder);
            serviceClient = new LSPApplicationServiceClient();
        }
    }

    @Override
    public IBinder requestModuleBinder() {
        try {
            return service.requestModuleBinder();
        } catch (RemoteException | NullPointerException ignored) {
        }
        return null;
    }

    @Override
    public IBinder requestManagerBinder() {
        try {
            return service.requestManagerBinder();
        } catch (RemoteException | NullPointerException ignored) {
        }
        return null;
    }

    @Override
    public boolean isResourcesHookEnabled() {
        try {
            return service.isResourcesHookEnabled();
        } catch (RemoteException | NullPointerException ignored) {
        }
        return false;
    }

    @Override
    public String[] getModulesList(String processName) {
        try {
            return service.getModulesList(processName);
        } catch (RemoteException | NullPointerException ignored) {
        }
        return new String[0];
    }

    public String[] getModulesList() {
        return getModulesList(processName);
    }

    @Override
    public String getPrefsPath(String packageName) {
        try {
            return service.getPrefsPath(packageName);
        } catch (RemoteException | NullPointerException ignored) {
        }
        return null;
    }

    @Override
    public String getCachePath(String fileName) {
        try {
            if (baseCachePath == null)
                baseCachePath = service.getCachePath("");
            return baseCachePath + File.separator + fileName;
        } catch (RemoteException | NullPointerException ignored) {
        }
        return null;
    }

    @Override
    public ParcelFileDescriptor getModuleLogger() {
        try {
            return service.getModuleLogger();
        } catch (RemoteException | NullPointerException ignored) {
        }
        return null;
    }

    @Override
    public IBinder asBinder() {
        return serviceBinder;
    }
}
