package org.lsposed.lspd.service;

import android.annotation.SuppressLint;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class LogcatService implements Runnable {
    private static final String TAG = "LSPosedLogcat";
    private static final int mode = ParcelFileDescriptor.MODE_WRITE_ONLY |
            ParcelFileDescriptor.MODE_CREATE |
            ParcelFileDescriptor.MODE_TRUNCATE |
            ParcelFileDescriptor.MODE_APPEND;
    private File modulesLog = null;
    private File verboseLog = null;
    private Thread thread = null;

    @SuppressLint("UnsafeDynamicallyLoadedCode")
    public LogcatService() {
        String libraryPath = System.getProperty("lsp.library.path");
        System.load(libraryPath + "/" + System.mapLibraryName("daemon"));
        ConfigFileManager.moveLogDir();
    }

    private native void runLogcat();

    @Override
    public void run() {
        Log.i(TAG, "start running");
        runLogcat();
        Log.i(TAG, "stopped");
    }

    @SuppressWarnings("unused")
    private int refreshFd(boolean isVerboseLog) {
        try {
            File log = isVerboseLog ? (verboseLog = ConfigFileManager.getNewVerboseLogPath()) :
                    (modulesLog = ConfigFileManager.getNewModulesLogPath());
            Log.i(TAG, "New " + (isVerboseLog ? "verbose" : "modules") + " log file: " + log);
            return ParcelFileDescriptor.open(log, mode).detachFd();
        } catch (IOException e) {
            Log.w(TAG, "someone chattr +i ?", e);
            return -1;
        }
    }

    public boolean isRunning() {
        return thread != null && thread.isAlive();
    }

    public void start() {
        if (isRunning()) return;
        thread = new Thread(this);
        thread.setName("logcat");
        thread.setUncaughtExceptionHandler((t, e) -> {
            Log.e(TAG, "Crash unexpectedly: ", e);
            thread = null;
            start();
        });
        thread.start();
    }

    public void startVerbose() {
        Log.i(TAG, "!!start_verbose!!");
    }

    public void stopVerbose() {
        Log.i(TAG, "!!stop_verbose!!");
    }

    public void refresh(boolean isVerboseLog) {
        if (isVerboseLog) {
            Log.i(TAG, "!!refresh_verbose!!");
        } else {
            Log.i(TAG, "!!refresh_modules!!");
        }
    }

    public File getVerboseLog() {
        return verboseLog;
    }

    public File getModulesLog() {
        return modulesLog;
    }
}
