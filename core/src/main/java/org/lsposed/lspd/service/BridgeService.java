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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.SystemServiceManager;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.ProcessRecord;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;

import static hidden.HiddenApiBridge.Binder_allowBlocking;
import static org.lsposed.lspd.service.ServiceManager.TAG;

public class BridgeService {
    private static final int TRANSACTION_CODE = ('_' << 24) | ('L' << 16) | ('S' << 8) | 'P';
    private static final String DESCRIPTOR = "LSPosed";
    private static final String SERVICE_NAME = "activity";

    enum ACTION {
        ACTION_UNKNOWN,
        ACTION_SEND_BINDER,
        ACTION_GET_BINDER,
    }

    // for client
    private static IBinder serviceBinder = null;
    private static ILSPosedService service = null;

    // for service
    static class BridgeServiceDeathRecipient implements IBinder.DeathRecipient {
        private final IBinder bridgeService;

        BridgeServiceDeathRecipient(IBinder bridgeService) throws RemoteException {
            this.bridgeService = bridgeService;
            bridgeService.linkToDeath(this, 0);
        }

        @Override
        public void binderDied() {
            Log.i(TAG, "service " + SERVICE_NAME + " is dead. ");

            try {
                @SuppressWarnings("JavaReflectionMemberAccess")
                Field field = ServiceManager.class.getDeclaredField("sServiceManager");
                field.setAccessible(true);
                field.set(null, null);

                //noinspection JavaReflectionMemberAccess
                field = ServiceManager.class.getDeclaredField("sCache");
                field.setAccessible(true);
                Object sCache = field.get(null);
                if (sCache instanceof Map) {
                    //noinspection rawtypes
                    ((Map) sCache).clear();
                }
                Log.i(TAG, "clear ServiceManager");
            } catch (Throwable e) {
                Log.w(TAG, "clear ServiceManager: " + Log.getStackTraceString(e));
            }

            bridgeService.unlinkToDeath(this, 0);
            listener.onSystemServerDied();
            sendToBridge(serviceBinder, true);
        }
    }

    // for client
    private static final IBinder.DeathRecipient LSPSERVICE_DEATH_RECIPIENT = () -> {
        serviceBinder = null;
        service = null;
        Log.e(TAG, "service is dead");
    };

    public interface Listener {

        void onSystemServerRestarted();

        void onResponseFromBridgeService(boolean response);

        void onSystemServerDied();
    }

    private static Listener listener;

    // For service
    private static void sendToBridge(IBinder binder, boolean isRestart) {
        IBinder bridgeService;
        do {
            bridgeService = ServiceManager.getService(SERVICE_NAME);
            if (bridgeService != null && bridgeService.pingBinder()) {
                break;
            }

            Log.i(TAG, "service " + SERVICE_NAME + " is not started, wait 1s.");

            try {
                //noinspection BusyWait
                Thread.sleep(1000);
            } catch (Throwable e) {
                Log.w(TAG, "sleep" + Log.getStackTraceString(e));
            }
        } while (true);

        if (isRestart && listener != null) {
            listener.onSystemServerRestarted();
        }

        try {
            new BridgeServiceDeathRecipient(bridgeService);
        } catch (Throwable e) {
            Log.w(TAG, "linkToDeath " + Log.getStackTraceString(e));
            sendToBridge(binder, false);
            return;
        }

        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        boolean res = false;
        // try at most three times
        for (int i = 0; i < 3; i++) {
            try {
                data.writeInterfaceToken(DESCRIPTOR);
                data.writeInt(ACTION.ACTION_SEND_BINDER.ordinal());
                Log.v(TAG, "binder " + binder.toString());
                data.writeStrongBinder(binder);
                res = bridgeService.transact(TRANSACTION_CODE, data, reply, 0);
                reply.readException();
            } catch (Throwable e) {
                Log.e(TAG, "send binder " + Log.getStackTraceString(e));
            } finally {
                data.recycle();
                reply.recycle();
            }

            if (res) break;

            Log.w(TAG, "no response from bridge, retry in 1s");

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }

        if (listener != null) {
            listener.onResponseFromBridgeService(res);
        }
    }

    // For client
    private static void receiveFromBridge(IBinder binder) {
        if (binder == null) {
            Log.e(TAG, "received empty binder");
            return;
        }

        if (serviceBinder == null) {
            PackageReceiver.register(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (service == null) {
                        Log.e(TAG, "Service is dead, missing package changed: " + intent);
                        return;
                    }
                    try {
                        service.dispatchPackageChanged(intent);
                    } catch (RemoteException e) {
                        Log.e(TAG, Log.getStackTraceString(e));
                    }
                }
            });
            BootReceiver.register(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    try {
                        service.dispatchBootCompleted(intent);
                    } catch (RemoteException e) {
                        Log.e(TAG, Log.getStackTraceString(e));
                    }
                }
            });
        } else {
            serviceBinder.unlinkToDeath(LSPSERVICE_DEATH_RECIPIENT, 0);
        }

        serviceBinder = Binder_allowBlocking(binder);
        service = ILSPosedService.Stub.asInterface(serviceBinder);
        try {
            serviceBinder.linkToDeath(LSPSERVICE_DEATH_RECIPIENT, 0);
        } catch (RemoteException ignored) {
        }

        Log.i(TAG, "binder received");
    }

    public static void send(LSPosedService service, Listener listener) {
        BridgeService.listener = listener;
        BridgeService.service = service;
        BridgeService.serviceBinder = service.asBinder();
        sendToBridge(serviceBinder, false);
    }

    public static ILSPosedService getService() {
        return service;
    }

    @SuppressWarnings({"unused", "RedundantSuppression"})
    public static boolean onTransact(@NonNull Parcel data, @Nullable Parcel reply, int flags) {
        data.enforceInterface(DESCRIPTOR);

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
        return false;
    }

    public static boolean execTransact(int code, long dataObj, long replyObj, int flags) {
        if (code != TRANSACTION_CODE) return false;

        Parcel data = ParcelUtils.fromNativePointer(dataObj);
        Parcel reply = ParcelUtils.fromNativePointer(replyObj);

        if (data == null || reply == null) {
            Log.w(TAG, "Got transaction with null data or reply");
            return false;
        }

        boolean res = false;

        try {
            String descriptor = ParcelUtils.readInterfaceDescriptor(data);
            data.setDataPosition(0);
            if (descriptor.equals(DESCRIPTOR)) {
                res = onTransact(data, reply, flags);
            }
        } catch (Exception e) {
            if ((flags & IBinder.FLAG_ONEWAY) != 0) {
                Log.w(TAG, "Caught a Exception from the binder stub implementation. " + Log.getStackTraceString(e));
            } else {
                reply.setDataPosition(0);
                reply.writeException(e);
            }
            res = true;
        }

        if (res) {
            data.recycle();
            reply.recycle();
        }

        return res;
    }

    public static IBinder getApplicationServiceForSystemServer(IBinder binder, IBinder heartBeat) {
        if (binder == null || heartBeat == null) return null;
        try {
            ILSPosedService service = ILSPosedService.Stub.asInterface(binder);
            ILSPApplicationService applicationService = service.requestApplicationService(Process.myUid(), Process.myPid(), "android", heartBeat);
            if (applicationService != null) return applicationService.asBinder();
        } catch (Throwable e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        return null;
    }

    private static void tryGetActivityManagerServiceInstance() {
        try {
            Log.e(TAG, "Trying to get the ams");
            Field localServiceField = LocalServices.class.getDeclaredField("sLocalServiceObjects");
            localServiceField.setAccessible(true);
            ArrayMap<Class<?>, Object> localServiceMap = (ArrayMap<Class<?>, Object>) localServiceField.get(null);
            Class<?> systemServiceManagerClass = null;
            for (Class<?> clazz : localServiceMap.keySet()) {
                if (clazz.getName().equals("com.android.server.SystemServiceManager")) {
                    systemServiceManagerClass = clazz;
                }

            }
            Field parentField = ClassLoader.class.getDeclaredField("parent");
            parentField.setAccessible(true);
            parentField.set(BridgeService.class.getClassLoader(), systemServiceManagerClass.getClassLoader());
            SystemServiceManager systemServiceManager = LocalServices.getService(SystemServiceManager.class);
            ArrayList<SystemService> services;
            try {
                Field servicesField = systemServiceManagerClass.getDeclaredField("mServices");
                servicesField.setAccessible(true);
                services = (ArrayList<SystemService>) servicesField.get(systemServiceManager);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                Log.e(TAG, Log.getStackTraceString(e));
                return;
            }

            ActivityManagerService.Lifecycle lifecycle = null;

            for (SystemService service : services) {
                if (service instanceof ActivityManagerService.Lifecycle) {
                    lifecycle = (ActivityManagerService.Lifecycle) service;
                }
            }
            if (lifecycle == null) {
                Log.e(TAG, "I cannot get the lifecycle...");
            }
            ActivityManagerService activityManagerService = lifecycle.getService();
            if (activityManagerService != null) {
                Log.e(TAG, "I got the ams!!!: " + activityManagerService);
            } else {
                Log.e(TAG, "I cannot get the ams");
            }
            Method findProcessLockedMethod = ActivityManagerService.class.getDeclaredMethod("findProcessLocked", String.class, int.class, String.class);
            findProcessLockedMethod.setAccessible(true);
            ProcessRecord record = (ProcessRecord) findProcessLockedMethod.invoke(activityManagerService, String.valueOf(Binder.getCallingPid()), 0, "LSPosed");
            Field processNameField = ProcessRecord.class.getDeclaredField("processName");
            processNameField.setAccessible(true);
            if (record != null) {
                Log.e(TAG, "I got the record!!!: " + record);
                Log.e(TAG, "I got the process name: " + processNameField.get(record));
            }
        } catch (Throwable e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }
}
