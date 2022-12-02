package org.lsposed.lspd.service;

import static org.lsposed.lspd.service.ServiceManager.TAG;

import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.ServiceManager;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.Map;

public class BridgeService {

    static final int TRANSACTION_CODE = ('_' << 24) | ('L' << 16) | ('S' << 8) | 'P'; // 1598837584
    private static final String DESCRIPTOR = "LSPosed";
    private static final String SERVICE_NAME = "activity";

    enum ACTION {
        ACTION_UNKNOWN,
        ACTION_SEND_BINDER,
        ACTION_GET_BINDER,
    }

    public interface Listener {
        void onSystemServerRestarted();

        void onResponseFromBridgeService(boolean response);

        void onSystemServerDied();
    }

    private static IBinder serviceBinder = null;

    private static Listener listener;
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

    public static void send(LSPosedService service, Listener listener) {
        BridgeService.listener = listener;
        BridgeService.serviceBinder = service.asBinder();
        sendToBridge(serviceBinder, false);
    }
}
