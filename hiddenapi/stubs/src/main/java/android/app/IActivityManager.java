/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2021 LSPosed Contributors
 */

package android.app;

import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

import androidx.annotation.RequiresApi;

public interface IActivityManager extends IInterface {
    @RequiresApi(31)
    int broadcastIntentWithFeature(IApplicationThread caller, String callingFeatureId,
                                   Intent intent, String resolvedType, IIntentReceiver resultTo,
                                   int resultCode, String resultData, Bundle resultExtras,
                                   String[] requiredPermissions, String[] excludedPermissions,
                                   String[] excludePackages, int appOp, Bundle bOptions,
                                   boolean serialized, boolean sticky, int userId) throws RemoteException;

    @RequiresApi(31)
    int broadcastIntentWithFeature(IApplicationThread caller, String callingFeatureId,
                                   Intent intent, String resolvedType, IIntentReceiver resultTo,
                                   int resultCode, String resultData, Bundle resultExtras,
                                   String[] requiredPermissions, String[] excludedPermissions,
                                   int appOp, Bundle bOptions,
                                   boolean serialized, boolean sticky, int userId) throws RemoteException;

    @RequiresApi(30)
    int broadcastIntentWithFeature(IApplicationThread caller, String callingFeatureId,
                                   Intent intent, String resolvedType, IIntentReceiver resultTo, int resultCode,
                                   String resultData, Bundle map, String[] requiredPermissions,
                                   int appOp, Bundle options, boolean serialized, boolean sticky, int userId) throws RemoteException;

    int broadcastIntent(IApplicationThread caller, Intent intent,
                        String resolvedType, IIntentReceiver resultTo, int resultCode,
                        String resultData, Bundle map, String[] requiredPermissions,
                        int appOp, Bundle options, boolean serialized, boolean sticky, int userId) throws RemoteException;

    int startActivity(IApplicationThread caller, String callingPackage, Intent intent,
                      String resolvedType, IBinder resultTo, String resultWho, int requestCode,
                      int flags, ProfilerInfo profilerInfo, Bundle options) throws RemoteException;

    @RequiresApi(30)
    int startActivityWithFeature(IApplicationThread caller, String callingPackage,
                                 String callingFeatureId, Intent intent, String resolvedType,
                                 IBinder resultTo, String resultWho, int requestCode, int flags,
                                 ProfilerInfo profilerInfo, Bundle options) throws RemoteException;

    int startActivityAsUser(IApplicationThread caller, String callingPackage,
                            Intent intent, String resolvedType, IBinder resultTo, String resultWho,
                            int requestCode, int flags, ProfilerInfo profilerInfo,
                            Bundle options, int userId) throws RemoteException;

    @RequiresApi(30)
    int startActivityAsUserWithFeature(IApplicationThread caller, String callingPackage,
                                       String callingFeatureId, Intent intent, String resolvedType,
                                       IBinder resultTo, String resultWho, int requestCode, int flags,
                                       ProfilerInfo profilerInfo, Bundle options, int userId) throws RemoteException;

    void forceStopPackage(String packageName, int userId) throws RemoteException;

    boolean startUserInBackground(int userid) throws RemoteException;

    Intent registerReceiver(IApplicationThread caller, String callerPackage,
                            IIntentReceiver receiver, IntentFilter filter,
                            String requiredPermission, int userId, int flags) throws RemoteException;

    void finishReceiver(IBinder caller, int resultCode, String resultData,
                        Bundle resultExtras, boolean resultAbort, int flags) throws RemoteException;

    @RequiresApi(30)
    Intent registerReceiverWithFeature(IApplicationThread caller, String callerPackage,
                                       String callingFeatureId, IIntentReceiver receiver, IntentFilter filter,
                                       String requiredPermission, int userId, int flags) throws RemoteException;

    @RequiresApi(31)
    Intent registerReceiverWithFeature(IApplicationThread caller, String callerPackage, String callingFeatureId,
                                       String receiverId, IIntentReceiver receiver, IntentFilter filter,
                                       String requiredPermission, int userId, int flags) throws RemoteException;

    int bindService(IApplicationThread caller, IBinder token, Intent service,
                    String resolvedType, IServiceConnection connection, int flags,
                    String callingPackage, int userId) throws RemoteException;

    @RequiresApi(34)
    int bindService(IApplicationThread caller, IBinder token, Intent service,
                    String resolvedType, IServiceConnection connection, long flags,
                    String callingPackage, int userId) throws RemoteException;

    boolean unbindService(IServiceConnection connection) throws RemoteException;

    boolean switchUser(int userid) throws RemoteException;

    UserInfo getCurrentUser() throws RemoteException;

    void setActivityController(IActivityController watcher, boolean imAMonkey) throws RemoteException;

    @RequiresApi(29)
    ContentProviderHolder getContentProviderExternal(String name, int userId, IBinder token, String tag) throws RemoteException;

    ContentProviderHolder getContentProviderExternal(String name, int userId, IBinder token) throws RemoteException;

    Configuration getConfiguration() throws RemoteException;

    void registerUidObserver(IUidObserver observer, int which, int cutpoint, String callingPackage) throws RemoteException;

    abstract class Stub extends Binder implements IActivityManager {
        public static int TRANSACTION_setActivityController;

        public static IActivityManager asInterface(IBinder obj) {
            throw new UnsupportedOperationException();
        }
    }
}
