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

import static hidden.HiddenApiBridge.Binder_allowBlocking;
import static hidden.HiddenApiBridge.Context_getActivityToken;

import android.app.ActivityThread;
import android.app.IApplicationThread;
import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.lsposed.lspd.BuildConfig;

public class BridgeService {
    private static final int TRANSACTION_CODE = ('_' << 24) | ('L' << 16) | ('S' << 8) | 'P';
    private static final String DESCRIPTOR = "LSPosed";
    protected static final String TAG = "LSPosed Bridge";

    enum ACTION {
        ACTION_UNKNOWN,
        ACTION_SEND_BINDER,
        ACTION_GET_BINDER,
    }

    // for client
    private static IBinder serviceBinder = null;
    private static ILSPosedService service = null;

    // for client
    private static final IBinder.DeathRecipient serviceRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            serviceBinder.unlinkToDeath(this, 0);
            serviceBinder = null;
            service = null;
            Log.e(TAG, "service is dead");
        }
    };

    // For client
    private static void receiveFromBridge(IBinder binder) {
        if (binder == null) {
            Log.e(TAG, "received empty binder");
            return;
        }

        var token = Binder.clearCallingIdentity();
        if (serviceBinder != null) {
            serviceBinder.unlinkToDeath(serviceRecipient, 0);
        }
        Binder.restoreCallingIdentity(token);

        serviceBinder = Binder_allowBlocking(binder);
        service = ILSPosedService.Stub.asInterface(serviceBinder);
        try {
            serviceBinder.linkToDeath(serviceRecipient, 0);
        } catch (Throwable e) {
            Log.e(TAG, "service link to death: ", e);
        }
        try {
            IApplicationThread at = ActivityThread.currentActivityThread().getApplicationThread();
            Context ctx = ActivityThread.currentActivityThread().getSystemContext();
            service.dispatchSystemServerContext(at.asBinder(), Context_getActivityToken(ctx), BuildConfig.FLAVOR);
        } catch (Throwable e) {
            Log.e(TAG, "dispatch context: ", e);
        }
        Log.i(TAG, "binder received");
    }

    public static ILSPosedService getService() {
        return service;
    }

    @SuppressWarnings({"unused", "RedundantSuppression"})
    public static boolean onTransact(@NonNull Parcel data, @Nullable Parcel reply, int flags) {
        if (!ParcelUtils.safeEnforceInterface(data, DESCRIPTOR)) return false;

        try {
            ACTION action = ACTION.values()[data.readInt()];

            Log.d(TAG, "onTransact: action=" + action + ", callingUid=" + Binder.getCallingUid() + ", callingPid=" + Binder.getCallingPid());

            switch (action) {
                case ACTION_SEND_BINDER: {
                    if (Binder.getCallingUid() == 0) {
                        receiveFromBridge(data.readStrongBinder());
                        if (reply != null) {
                            reply.writeNoException();
                        }
                        return true;
                    }
                    break;
                }
                case ACTION_GET_BINDER: {
                    IBinder binder = null;
                    try {
                        String processName = data.readString();
                        IBinder heartBeat = data.readStrongBinder();
                        var applicationService = service == null ? null : service.requestApplicationService(Binder.getCallingUid(), Binder.getCallingPid(), processName, heartBeat);
                        if (applicationService != null) binder = applicationService.asBinder();
                    } catch (RemoteException e) {
                        Log.e(TAG, Log.getStackTraceString(e));
                    }
                    if (binder != null && reply != null) {
                        reply.writeNoException();
                        Log.d(TAG, "got binder is " + binder);
                        reply.writeStrongBinder(binder);
                        return true;
                    }
                    return false;
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, "onTransact", e);
        }
        return false;
    }

    @SuppressWarnings("unused")
    public static boolean replaceShellCommand(IBinder obj, int code, long dataObj, long replyObj, int flags) {
        Parcel data = ParcelUtils.fromNativePointer(dataObj);
        Parcel reply = ParcelUtils.fromNativePointer(replyObj);

        if (data == null || reply == null) {
            Log.w(TAG, "Got transaction with null data or reply");
            return false;
        }

        try {
            String descriptor = obj.getInterfaceDescriptor();
            if (!"android.app.IActivityManager".equals(descriptor) &&
                    !"com.sonymobile.hookservice.HookActivityService".equals(descriptor)) {
                return false;
            }
            return ActivityController.replaceShellCommand(obj, data, reply);
        } catch (Throwable e) {
            Log.e(TAG, "replace shell command", e);
            return false;
        } finally {
            data.setDataPosition(0);
        }
    }

    @SuppressWarnings("unused")
    public static boolean replaceActivityController(IBinder obj, int code, long dataObj, long replyObj, int flags) {
        Parcel data = ParcelUtils.fromNativePointer(dataObj);
        Parcel reply = ParcelUtils.fromNativePointer(replyObj);

        if (data == null || reply == null) {
            Log.w(TAG, "Got transaction with null data or reply");
            return false;
        }

        try {
            if (!ParcelUtils.safeEnforceInterface(data, "android.app.IActivityManager") &&
                    !ParcelUtils.safeEnforceInterface(data, "com.sonymobile.hookservice.HookActivityService")) {
                return false;
            }
            return ActivityController.replaceActivityController(data);
        } finally {
            data.setDataPosition(0);
        }
    }

    @SuppressWarnings("unused")
    public static boolean execTransact(IBinder obj, int code, long dataObj, long replyObj, int flags) {
        if (code != TRANSACTION_CODE) return false;

        Parcel data = ParcelUtils.fromNativePointer(dataObj);
        Parcel reply = ParcelUtils.fromNativePointer(replyObj);

        if (data == null || reply == null) {
            Log.w(TAG, "Got transaction with null data or reply");
            return false;
        }

        try {
            try {
                return onTransact(data, reply, flags);
            } catch (Exception e) {
                if ((flags & IBinder.FLAG_ONEWAY) != 0) {
                    Log.w(TAG, "Caught a Exception from the binder stub implementation. ", e);
                } else {
                    reply.setDataPosition(0);
                    reply.writeException(e);
                }
                Log.w(TAG, "on transact", e);
                return true;
            }
        } finally {
            data.recycle();
            reply.recycle();
        }
    }
}
