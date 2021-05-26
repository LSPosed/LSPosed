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

import android.app.IActivityManager;
import android.app.IApplicationThread;
import android.app.IServiceConnection;
import android.app.ProfilerInfo;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

public class ActivityManagerService {
    private static IActivityManager am = null;
    private static IBinder binder = null;
    private static IApplicationThread thread = null;
    private static IBinder token = null;

    private static final IBinder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            Log.w(TAG, "am is dead");
            binder.unlinkToDeath(this, 0);
            binder = null;
            am = null;
            thread = null;
            token = null;
        }
    };

    public static IActivityManager getActivityManager() {
        if (binder == null && am == null) {
            binder = ServiceManager.getService("activity");
            if (binder == null) return null;
            try {
                binder.linkToDeath(deathRecipient, 0);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
            am = IActivityManager.Stub.asInterface(binder);
        }
        return am;
    }

    public static int broadcastIntentWithFeature(String callingFeatureId,
                                                 Intent intent, String resolvedType, IIntentReceiver resultTo, int resultCode,
                                                 String resultData, Bundle map, String[] requiredPermissions,
                                                 int appOp, Bundle options, boolean serialized, boolean sticky, int userId) throws RemoteException {
        IActivityManager am = getActivityManager();
        if (am == null || thread == null) return -1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return am.broadcastIntentWithFeature(thread, callingFeatureId, intent, resolvedType, resultTo, resultCode, resultData, map, requiredPermissions, appOp, options, serialized, sticky, userId);
        } else {
            return am.broadcastIntent(thread, intent, resolvedType, resultTo, resultCode, resultData, map, requiredPermissions, appOp, options, serialized, sticky, userId);
        }
    }

    public static void forceStopPackage(String packageName, int userId) throws RemoteException {
        IActivityManager am = getActivityManager();
        if (am == null) return;
        am.forceStopPackage(packageName, userId);
    }

    public static boolean startUserInBackground(int userId) throws RemoteException {
        IActivityManager am = getActivityManager();
        if (am == null) return false;
        return am.startUserInBackground(userId);
    }

    public static Intent registerReceiver(String callerPackage,
                                          String callingFeatureId, IIntentReceiver receiver, IntentFilter filter,
                                          String requiredPermission, int userId, int flags) throws RemoteException {
        IActivityManager am = getActivityManager();
        if (am == null || thread == null) return null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S || (Build.VERSION.SDK_INT == Build.VERSION_CODES.R && Build.VERSION.PREVIEW_SDK_INT != 0))
                return am.registerReceiverWithFeature(thread, callerPackage, callingFeatureId, "null", receiver, filter, requiredPermission, userId, flags);
        } catch (Throwable ignored) {
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return am.registerReceiverWithFeature(thread, callerPackage, callingFeatureId, receiver, filter, requiredPermission, userId, flags);
        } else {
            return am.registerReceiver(thread, callerPackage, receiver, filter, requiredPermission, userId, flags);
        }
    }

    public static void finishReceiver(IBinder who, int resultCode, String resultData, Bundle map,
                                      boolean abortBroadcast, int flags) throws RemoteException {
        IActivityManager am = getActivityManager();
        if (am == null || thread == null) return;
        am.finishReceiver(who, resultCode, resultData, map, abortBroadcast, flags);
    }

    public static int bindService(Intent service,
                                  String resolvedType, IServiceConnection connection, int flags,
                                  String callingPackage, int userId) throws RemoteException {

        IActivityManager am = getActivityManager();
        if (am == null || thread == null) return -1;
        return am.bindService(thread, token, service, resolvedType, connection, flags, callingPackage, userId);
    }

    public static boolean unbindService(IServiceConnection connection) throws RemoteException {
        IActivityManager am = getActivityManager();
        if (am == null) return false;
        return am.unbindService(connection);
    }

    public static int startActivityAsUserWithFeature(String callingPackage,
                                                     String callingFeatureId, Intent intent, String resolvedType,
                                                     IBinder resultTo, String resultWho, int requestCode, int flags,
                                                     ProfilerInfo profilerInfo, Bundle options, int userId) throws RemoteException {
        IActivityManager am = getActivityManager();
        if (am == null || thread == null) return -1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return am.startActivityAsUserWithFeature(thread, callingPackage, callingFeatureId, intent, resolvedType, resultTo, resultWho, requestCode, flags, profilerInfo, options, userId);
        } else {
            return am.startActivityAsUser(thread, callingPackage, intent, resolvedType, resultTo, resultWho, requestCode, flags, profilerInfo, options, userId);
        }
    }

    public static void onSystemServerContext(IApplicationThread thread, IBinder token) {
        ActivityManagerService.thread = thread;
        ActivityManagerService.token = token;
    }

    public static boolean switchUser(int userid) throws RemoteException {
        IActivityManager am = getActivityManager();
        if (am == null) return false;
        return am.switchUser(userid);
    }

    public static UserInfo getCurrentUser() throws RemoteException {
        IActivityManager am = getActivityManager();
        if (am == null) return null;
        return am.getCurrentUser();
    }
}
