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

import android.app.IActivityManager;
import android.app.IApplicationThread;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import static org.lsposed.lspd.service.ServiceManager.TAG;

public class ActivityManagerService {
    private static IActivityManager am = null;
    private static IBinder binder = null;

    public static IActivityManager getActivityManager() {
        if (binder == null && am == null) {
            binder = ServiceManager.getService("activity");
            if (binder == null) return null;
            try {
                binder.linkToDeath(new IBinder.DeathRecipient() {
                    @Override
                    public void binderDied() {
                        Log.w(TAG, "am is dead");
                        binder.unlinkToDeath(this, 0);
                        binder = null;
                        am = null;
                    }
                }, 0);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
            am = IActivityManager.Stub.asInterface(binder);
        }
        return am;
    }

    public static int broadcastIntentWithFeature(IApplicationThread caller, String callingFeatureId,
                                                 Intent intent, String resolvedType, IIntentReceiver resultTo, int resultCode,
                                                 String resultData, Bundle map, String[] requiredPermissions,
                                                 int appOp, Bundle options, boolean serialized, boolean sticky, int userId) throws RemoteException {
        IActivityManager am = getActivityManager();
        if (am == null) return -1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return am.broadcastIntentWithFeature(caller, callingFeatureId, intent, resolvedType, resultTo, resultCode, resultData, map, requiredPermissions, appOp, options, serialized, sticky, userId);
        } else {
            return am.broadcastIntent(caller, intent, resolvedType, resultTo, resultCode, resultData, map, requiredPermissions, appOp, options, serialized, sticky, userId);
        }
    }

    public static void forceStopPackage(String packageName, int userId) throws RemoteException {
        IActivityManager am = getActivityManager();
        if (am == null) return;
        am.forceStopPackage(packageName, userId);
    }
}
