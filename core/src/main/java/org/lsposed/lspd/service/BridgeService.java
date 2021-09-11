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
import static hidden.HiddenApiBridge.Binder_allowBlocking;
import static hidden.HiddenApiBridge.Context_getActivityToken;

import android.app.ActivityThread;
import android.app.IActivityController;
import android.app.IActivityManager;
import android.app.IApplicationThread;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Map;

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
    private static IBinder bridgeService;
    private static final IBinder.DeathRecipient bridgeRecipient = new IBinder.DeathRecipient() {

        @Override
        public void binderDied() {
            Log.i(TAG, "service " + SERVICE_NAME + " is dead. ");

            try {
                //noinspection JavaReflectionMemberAccess DiscouragedPrivateApi
                Field field = ServiceManager.class.getDeclaredField("sServiceManager");
                field.setAccessible(true);
                field.set(null, null);

                //noinspection JavaReflectionMemberAccess DiscouragedPrivateApi
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
            bridgeService = null;
            listener.onSystemServerDied();
            new Handler(Looper.getMainLooper()).post(() -> sendToBridge(serviceBinder, true));
        }
    };

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

    public interface Listener {

        void onSystemServerRestarted();

        void onResponseFromBridgeService(boolean response);

        void onSystemServerDied();
    }

    private static Listener listener;

    // For service
    private static synchronized void sendToBridge(IBinder binder, boolean isRestart) {
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
            bridgeService.linkToDeath(bridgeRecipient, 0);
        } catch (Throwable e) {
            Log.w(TAG, "linkToDeath " + Log.getStackTraceString(e));
            var snapshot = bridgeService;
            sendToBridge(binder, snapshot == null || !snapshot.isBinderAlive());
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
                if (bridgeService == null) break;
                res = bridgeService.transact(TRANSACTION_CODE, data, reply, 0);
                reply.readException();
            } catch (Throwable e) {
                Log.e(TAG, "send binder " + Log.getStackTraceString(e));
                var snapshot = bridgeService;
                sendToBridge(binder, snapshot == null || !snapshot.isBinderAlive());
                return;
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
            Context ct = ActivityThread.currentActivityThread().getSystemContext();
            service.dispatchSystemServerContext(at.asBinder(), Context_getActivityToken(ct));
        } catch (Throwable e) {
            Log.e(TAG, "dispatch context: ", e);
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

    private static IActivityController replaceActivityController(IActivityController controller) {
        Log.e(TAG, "android.app.IActivityManager.setActivityController is called");
        return new IActivityController.Stub() {
            @Override
            public boolean activityStarting(Intent intent, String pkg) {
                Log.d(TAG, "activity from " + pkg + " with " + intent + " is starting");
                return controller == null || controller.activityStarting(intent, pkg);
            }

            @Override
            public boolean activityResuming(String pkg) {
                return controller == null || controller.activityResuming(pkg);
            }

            @Override
            public boolean appCrashed(String processName, int pid, String shortMsg, String longMsg, long timeMillis, String stackTrace) {
                return controller == null || controller.appCrashed(processName, pid, shortMsg, longMsg, timeMillis, stackTrace);
            }

            @Override
            public int appEarlyNotResponding(String processName, int pid, String annotation) {
                return controller == null ? 0 : controller.appNotResponding(processName, pid, annotation);
            }

            @Override
            public int appNotResponding(String processName, int pid, String processStats) {
                return controller == null ? 0 : controller.appNotResponding(processName, pid, processStats);
            }

            @Override
            public int systemNotResponding(String msg) {
                return controller == null ? 0 : controller.systemNotResponding(msg);
            }

            @Override
            public IBinder asBinder() {
                return this;
            }
        };
    }

    public static boolean replaceShellCommand(IBinder obj, int code, long dataObj, long replyObj, int flags) {
        try {
            String descriptor = obj.getInterfaceDescriptor();
            if (descriptor == null || (!descriptor.equals("android.app.IActivityManager") &&
                    !descriptor.equals("com.sonymobile.hookservice.HookActivityService"))) {
                return false;
            }
        } catch (Throwable e) {
            Log.e(TAG, "replace shell command", e);
            return false;
        }
        Parcel data = ParcelUtils.fromNativePointer(dataObj);
        Parcel reply = ParcelUtils.fromNativePointer(replyObj);

        if (data == null || reply == null) {
            Log.w(TAG, "Got transaction with null data or reply");
            return false;
        }

        try {
            var in = data.readFileDescriptor();
            var out = data.readFileDescriptor();
            var err = data.readFileDescriptor();
            String[] args = data.createStringArray();
            var originController = Class.forName("com.android.server.am.ActivityManagerShellCommand$MyActivityController", false, obj.getClass().getClassLoader());
            var ctor = originController.getDeclaredConstructor(IActivityManager.class, PrintWriter.class, InputStream.class,
                    String.class, boolean.class);
            ctor.setAccessible(true);
            var runner = originController.getDeclaredMethod("run");
            runner.setAccessible(true);
            ShellCallback shellCallback = ShellCallback.CREATOR.createFromParcel(data);
            ResultReceiver resultReceiver = ResultReceiver.CREATOR.createFromParcel(data);

            if (args.length > 0 && serviceBinder != null && serviceBinder.isBinderAlive() && "monitor".equals(args[0])) {
                new ShellCommand() {
                    @Override
                    public int onCommand(String cmd) {
                        final PrintWriter pw = getOutPrintWriter();
                        String opt;
                        String gdbPort = null;
                        boolean monkey = false;
                        while ((opt = getNextOption()) != null) {
                            if (opt.equals("--gdb")) {
                                gdbPort = getNextArgRequired();
                            } else if (opt.equals("-m")) {
                                monkey = true;
                            } else {
                                getErrPrintWriter().println("Error: Unknown option: " + opt);
                                return -1;
                            }
                        }

                        try {
                            var ctrl = ctor.newInstance(
                                    Proxy.newProxyInstance(BridgeService.class.getClassLoader(),
                                            obj.getClass().getSuperclass().getInterfaces(),
                                            (proxy, method, args1) -> {
                                                if (method.getName().equals("setActivityController")) {
                                                    try {
                                                        args1[0] = replaceActivityController((IActivityController) args1[0]);
                                                    } catch (Throwable e) {
                                                        Log.e(TAG, "replace activity controller", e);
                                                    }
                                                }
                                                return method.invoke(obj, args1);
                                            }), pw,
                                    getRawInputStream(), gdbPort, monkey);
                            runner.invoke(ctrl);
                        } catch (Throwable e) {
                            Log.e(TAG, "run monitor", e);
                        }
                        return 0;
                    }

                    @Override
                    public void onHelp() {

                    }
                }.exec((Binder) obj, in.getFileDescriptor(), out.getFileDescriptor(), err.getFileDescriptor(), args, shellCallback, resultReceiver);
                return true;
            }
        } catch (Throwable e) {
            Log.e(TAG, "replace shell command", e);
        } finally {
            data.setDataPosition(0);
        }

        return false;
    }

    public static boolean replaceActivityController(IBinder obj, int code, long dataObj, long replyObj, int flags) {
        Parcel data = ParcelUtils.fromNativePointer(dataObj);
        Parcel reply = ParcelUtils.fromNativePointer(replyObj);

        if (data == null || reply == null) {
            Log.w(TAG, "Got transaction with null data or reply");
            return false;
        }
        try {
            String descriptor = ParcelUtils.readInterfaceDescriptor(data);
            if (!descriptor.equals("android.app.IActivityManager") &&
                    !descriptor.equals("com.sonymobile.hookservice.HookActivityService")) {
                return false;
            }
            var position = data.dataPosition();
            var controller = replaceActivityController(IActivityController.Stub.asInterface(data.readStrongBinder()));
            var b = data.readInt();
            data.setDataSize(position);
            data.setDataPosition(position);
            data.writeStrongInterface(controller);
            data.writeInt(b);
        } catch (Throwable e) {
            Log.e(TAG, "replace activity controller", e);
        } finally {
            data.setDataPosition(0);
        }
        return false;
    }

    public static boolean execTransact(IBinder obj, int code, long dataObj, long replyObj, int flags) {
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
            var service = ILSPSystemServerService.Stub.asInterface(binder);
            var applicationService = service.requestApplicationService(Process.myUid(), Process.myPid(), "android", heartBeat);
            if (applicationService != null) return applicationService.asBinder();
        } catch (Throwable e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        return null;
    }

}
