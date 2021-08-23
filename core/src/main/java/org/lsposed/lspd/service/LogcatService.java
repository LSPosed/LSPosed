package org.lsposed.lspd.service;

import android.annotation.SuppressLint;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.Nullable;

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
    }

    private native void runLogcat(long loggerId);

    @Override
    public void run() {
        Log.i(TAG, "start running");
        runLogcat(thread.getId());
        Log.i(TAG, "stoped");
    }

    @SuppressWarnings("unused")
    private int refreshFd(boolean isVerboseLog) {
        File log;
        if (isVerboseLog) {
            verboseLog = ConfigFileManager.getNewVerboseLogPath();
            log = verboseLog;
        } else {
            modulesLog = ConfigFileManager.getNewModulesLogPath();
            log = modulesLog;
        }
        try (var fd = ParcelFileDescriptor.open(log, mode)) {
            return fd.detachFd();
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
        Log.i(TAG, "!!start_verbose!!" + thread.getId());
    }

    public void stopVerbose() {
        Log.i(TAG, "!!stop_verbose!!" + thread.getId());
    }

    @Nullable
    public File getVerboseLog() {
        return verboseLog;
    }

    public File getModulesLog() {
        return modulesLog;
    }
}
