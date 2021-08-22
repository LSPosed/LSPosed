package org.lsposed.lspd.service;

import android.annotation.SuppressLint;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.lsposed.lspd.util.Utils;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class LogcatService implements Runnable {
    private static final String TAG = "LSPosedLogcat";
    private static final int mode = ParcelFileDescriptor.MODE_WRITE_ONLY | ParcelFileDescriptor.MODE_CREATE |
            ParcelFileDescriptor.MODE_TRUNCATE | ParcelFileDescriptor.MODE_APPEND;
    public final File modulesLog;
    private final File logPath;
    private final DateTimeFormatter logTimeFormat;
    private File log = null;
    private Thread thread = null;
    boolean verboseLog = true;

    @SuppressLint("UnsafeDynamicallyLoadedCode")
    public LogcatService(File logPath) {
        String libraryPath = System.getProperty("lsp.library.path");
        System.load(libraryPath + "/" + System.mapLibraryName("daemon"));
        this.logPath = logPath;
        modulesLog = new File(logPath, "module.log");
        logTimeFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(Utils.getZoneId());
    }

    @Override
    public void run() {
        Log.i(TAG, "start running");
        int moduleFd = -1;
        try (var fd = ParcelFileDescriptor.open(modulesLog, mode)) {
            moduleFd = fd.detachFd();
        } catch (IOException e) {
            Log.w(TAG, "someone chattr +i ?", e);
        }
        runLogcat(thread.getId(), moduleFd, verboseLog);
        Log.i(TAG, "stoped");
    }

    private native void runLogcat(long tid, int fd, boolean verboseLog);

    @SuppressWarnings("unused")
    private int refreshFd() {
        log = new File(logPath, logTimeFormat.format(Instant.now()) + ".log");

        try (var fd = ParcelFileDescriptor.open(log, mode)) {
            return fd.detachFd();
        } catch (IOException e) {
            Log.w(TAG, "someone chattr +i ?", e);
            return -1;
        }
    }

    public void start() {
        if (thread != null) Log.i(TAG, "!!start_verbose!!" + thread.getId());
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

    public void stop() {
        // logcat thread is listening for this keyword
        Log.i(TAG, "!!stop_verbose!!" + thread.getId());
    }

    public boolean isRunning() {
        return thread != null && thread.isAlive();
    }

    @Nullable
    public File getLog() {
        return log;
    }

    @NonNull
    public File getModulesLog() {
        return modulesLog;
    }
}
