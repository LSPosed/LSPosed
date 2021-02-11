package io.github.lsposed.lspd.service;

import android.os.Looper;
import android.util.Log;

public class Service {
    public static final String TAG = "LSPosedService";
    // call by ourselves
    public static void start() {
        Log.i(TAG, "starting server...");

        Looper.prepare();
        new LSPosedService();
        new LSPManagerService();
        Looper.loop();

        Log.i(TAG, "server exited");
        System.exit(0);
    }

}
