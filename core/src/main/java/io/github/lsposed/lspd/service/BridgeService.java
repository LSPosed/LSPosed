package io.github.lsposed.lspd.service;

import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Field;
import java.util.Map;

import io.github.lsposed.lspd.nativebridge.ConfigManager;
import io.github.xposed.xposedservice.IXposedService;

import static android.os.Binder.getCallingUid;
import static io.github.lsposed.lspd.service.LSPosedService.TAG;

public class BridgeService {
    private static final int TRANSACTION_CODE = ('_' << 24) | ('L' << 16) | ('S' << 8) | 'P';
    private static final String DESCRIPTOR = "android.app.IActivityManager";
    private static final String SERVICE_NAME = "activity";

    private static final int ACTION_SEND_BINDER = 1;
    private static final int ACTION_GET_BINDER = ACTION_SEND_BINDER + 1;

    private static IBinder serviceBinder = null;
    private static IXposedService service = null;

    private static final IBinder.DeathRecipient BRIDGE_SERVICE_DEATH_RECIPIENT = () -> {
        Log.i(TAG, "service " + SERVICE_NAME + " is dead. ");

        try {
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

        sendToBridge(true);
    };

    private static final IBinder.DeathRecipient LSPSERVICE_DEATH_RECIPIENT = () -> {
        serviceBinder = null;
        service = null;
        Log.e(TAG, "service is dead");
    };

    public interface Listener {

        void onSystemServerRestarted();

        void onResponseFromBridgeService(boolean response);
    }

    private static Listener listener;

    private static PackageManager pm = null;

    private static void sendToBridge(boolean isRestart) {
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
            bridgeService.linkToDeath(BRIDGE_SERVICE_DEATH_RECIPIENT, 0);
        } catch (Throwable e) {
            Log.w(TAG, "linkToDeath " + Log.getStackTraceString(e));
            sendToBridge(false);
            return;
        }

        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        boolean res = false;
        // try at most three times
        for (int i = 0; i < 3; i++) {
            try {
                data.writeInterfaceToken(DESCRIPTOR);
                data.writeInt(ACTION_SEND_BINDER);
                Log.v(TAG, "binder " + serviceBinder.toString());
                data.writeStrongBinder(serviceBinder);
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

    private static void receiveFromBridge(IBinder binder) {
        if (binder == null) {
            Log.e(TAG, "received empty binder");
            return;
        }

        if (serviceBinder == null) {
            PackageReceiver.register();
        } else {
            serviceBinder.unlinkToDeath(LSPSERVICE_DEATH_RECIPIENT, 0);
        }

        serviceBinder = binder;
        service = IXposedService.Stub.asInterface(serviceBinder);
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
        sendToBridge(false);
    }

    public static IXposedService getService() {
        return service;
    }

    public static IBinder requireBinder() {
        IBinder binder = ServiceManager.getService(SERVICE_NAME);
        if (binder == null) return null;

        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(DESCRIPTOR);
            data.writeInt(ACTION_GET_BINDER);
            binder.transact(TRANSACTION_CODE, data, reply, 0);
            reply.readException();
            IBinder received = reply.readStrongBinder();
            if (received != null) {
                return received;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            data.recycle();
            reply.recycle();
        }
        return null;
    }

    @SuppressWarnings({"unused", "RedundantSuppression"})
    public static boolean onTransact(int code, @NonNull Parcel data, @Nullable Parcel reply, int flags) {
        data.enforceInterface(DESCRIPTOR);

        int action = data.readInt();
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
                try {
                    if (!PackageService.isInstaller(getCallingUid())) return false;
                } catch (Throwable ignored) {
                    return false;
                }
                if (reply != null) {
                    reply.writeNoException();
                    Log.d(TAG, "saved binder is " + serviceBinder.toString());
                    reply.writeStrongBinder(serviceBinder);
                }
                return true;
            }
        }
        return false;
    }

    public static boolean execTransact(int code, long dataObj, long replyObj, int flags) {
        Log.d(TAG, String.valueOf(code));
        if (code != TRANSACTION_CODE) return false;

        Parcel data = ParcelUtils.fromNativePointer(dataObj);
        Parcel reply = ParcelUtils.fromNativePointer(replyObj);

        if (data == null) {
            return false;
        }

        boolean res = false;

        try {
            String descriptor = ParcelUtils.readInterfaceDescriptor(data);
            data.setDataPosition(0);

            if (descriptor.equals(DESCRIPTOR)) {
                res = onTransact(code, data, reply, flags);
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
            if (data != null) data.recycle();
            if (reply != null) reply.recycle();
        }

        return res;
    }
}
