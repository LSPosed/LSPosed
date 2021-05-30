/*
 * <!--This file is part of LSPosed.
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
 * Copyright (C) 2021 LSPosed Contributors-->
 */

package org.lsposed.lspd.service;

import static org.lsposed.lspd.service.ServiceManager.TAG;
import static org.lsposed.lspd.service.ServiceManager.getSystemServiceManager;

import android.os.Build;
import android.os.IBinder;
import android.os.IServiceCallback;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

public class LSPSystemServerService extends ILSPSystemServerService.Stub implements IBinder.DeathRecipient {

    public static final String PROXY_SERVICE_NAME = "serial";

    private IBinder originService = null;
    private boolean requested = false;

    public boolean systemServerRequested() {
        return requested;
    }

    public void putBinderForSystemServer() {
        android.os.ServiceManager.addService(PROXY_SERVICE_NAME, this);
        binderDied();
    }

    public LSPSystemServerService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            var serviceCallback = new IServiceCallback.Stub() {
                @Override
                public void onRegistration(String name, IBinder binder) {
                    if (name.equals(PROXY_SERVICE_NAME) && binder != null && binder != LSPSystemServerService.this) {
                        Log.d(TAG, "Register " + name + " " + binder);
                        originService = binder;
                        LSPSystemServerService.this.linkToDeath();
                    }
                }

                @Override
                public IBinder asBinder() {
                    return this;
                }
            };
            try {
                getSystemServiceManager().registerForNotifications(PROXY_SERVICE_NAME, serviceCallback);
            } catch (Throwable e) {
                Log.e(TAG, "unregister: ", e);
            }
        }
    }

    @Override
    public ILSPApplicationService requestApplicationService(int uid, int pid, String processName, IBinder heartBeat) throws RemoteException {
        requested = true;
        if (ConfigManager.getInstance().shouldSkipSystemServer() || uid != 1000 || heartBeat == null || !"android".equals(processName))
            return null;
        else
            return ServiceManager.requestApplicationService(uid, pid, heartBeat);
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (originService != null) {
            return originService.transact(code, data, reply, flags);
        }

        return super.onTransact(code, data, reply, flags);
    }

    public void linkToDeath() {
        try {
            originService.linkToDeath(this, 0);
        } catch (Throwable e) {
            Log.e(TAG, "system server service: link to death", e);
        }
    }

    @Override
    public void binderDied() {
        if (originService != null) {
            originService.unlinkToDeath(this, 0);
            originService = null;
        }
        requested = false;
    }
}
