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

import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import org.lsposed.lspd.models.Module;
import org.lsposed.lspd.service.ILSPApplicationService;
import org.lsposed.lspd.util.Utils;

import java.util.Collections;
import java.util.List;

public class LSPApplicationServiceClient extends ApplicationServiceClient {
    static ILSPApplicationService service = null;
    static IBinder serviceBinder = null;

    static String processName = null;

    private static final IBinder.DeathRecipient recipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            serviceBinder.unlinkToDeath(this, 0);
            serviceBinder = null;
            service = null;
        }
    };

    public static void Init(IBinder binder, String niceName) {
        if (serviceClient == null && binder != null && serviceBinder == null && service == null) {
            serviceBinder = binder;
            processName = niceName;
            try {
                serviceBinder.linkToDeath(recipient, 0);
            } catch (RemoteException e) {
                Utils.logE("link to death error: ", e);
            }
            service = ILSPApplicationService.Stub.asInterface(binder);
            serviceClient = new LSPApplicationServiceClient();
        }
    }

    @Override
    public IBinder requestModuleBinder(String name) {
        try {
            return service.requestModuleBinder(name);
        } catch (RemoteException | NullPointerException ignored) {
        }
        return null;
    }

    @Override
    public List<Module> getModulesList(String processName) {
        try {
            return service.getModulesList(processName);
        } catch (RemoteException | NullPointerException ignored) {
        }
        return Collections.emptyList();
    }

    public List<Module> getModulesList() {
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
    public Bundle requestRemotePreference(String packageName, int userId, IBinder callback) {
        return null;
    }

    @Override
    public ParcelFileDescriptor requestInjectedManagerBinder(List<IBinder> binder) {
        try {
            return service.requestInjectedManagerBinder(binder);
        } catch (RemoteException | NullPointerException ignored) {
        }
        return null;
    }

    @Override
    public IBinder asBinder() {
        return serviceBinder;
    }
}
