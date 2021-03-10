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

import android.content.pm.UserInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.IUserManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import java.util.List;

import static org.lsposed.lspd.service.ServiceManager.TAG;

public class UserService {
    private static IUserManager um = null;
    private static IBinder binder = null;

    public static IUserManager getUserManager() {
        if (binder == null && um == null) {
            binder = ServiceManager.getService("user");
            if (binder == null) return null;
            try {
                binder.linkToDeath(new IBinder.DeathRecipient() {
                    @Override
                    public void binderDied() {
                        Log.w(TAG, "um is dead");
                        binder.unlinkToDeath(this, 0);
                        binder = null;
                        um = null;
                    }
                }, 0);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
            um = IUserManager.Stub.asInterface(binder);
        }
        return um;
    }

    public static int[] getUsers() throws RemoteException {
        IUserManager um = getUserManager();
        if (um == null) return new int[0];
        List<UserInfo> users;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            users = um.getUsers(true, true, true);
        } else {
            try {
                users = um.getUsers(true);
            } catch (NoSuchMethodError e) {
                users = um.getUsers(true, true, true);
            }
        }
        int[] userArray = new int[users.size()];
        for (int i = 0; i < users.size(); i++) {
            UserInfo uh = users.get(i);
            userArray[i] = uh.id;
        }
        return userArray;
    }
}
