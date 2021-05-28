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

import static org.lsposed.lspd.service.ServiceManager.TAG;

import android.content.pm.UserInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.IUserManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class UserService {
    private static IUserManager um = null;
    private static IBinder binder = null;
    private static IBinder.DeathRecipient recipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            Log.w(TAG, "um is dead");
            binder.unlinkToDeath(this, 0);
            binder = null;
            um = null;
        }
    };

    public static IUserManager getUserManager() {
        if (binder == null && um == null) {
            binder = ServiceManager.getService("user");
            if (binder == null) return null;
            try {
                binder.linkToDeath(recipient, 0);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
            um = IUserManager.Stub.asInterface(binder);
        }
        return um;
    }

    public static List<UserInfo> getUsers() throws RemoteException {
        IUserManager um = getUserManager();
        List<UserInfo> users = new LinkedList<>();
        if (um == null) return users;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            users = um.getUsers(true, true, true);
        } else {
            try {
                users = um.getUsers(true);
            } catch (NoSuchMethodError e) {
                users = um.getUsers(true, true, true);
            }
        }
        return users;
    }

    public static int getProfileParent(int userId) throws RemoteException {
        IUserManager um = getUserManager();
        if (um == null) return -1;
        var userInfo = um.getProfileParent(userId);
        if (userInfo == null) return userId;
        else return userInfo.id;
    }
}
