package org.lsposed.lspd.service;

import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.Nullable;

import org.lsposed.lspd.util.Utils;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class LogcatService implements Runnable {
    private static final String TAG = "LSPosedLogcat";
    private final File logPath;
    private final DateTimeFormatter logTimeFormat;
    private File log = null;
    private Thread thread = null;

    public LogcatService(File logPath) {
        System.loadLibrary("daemon");
        this.logPath = logPath;
        logTimeFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(Utils.getZoneId());
    }

    @Override
    public void run() {
        Log.i(TAG, "start running");
        runLogcat(thread.getId());
        Log.i(TAG, "stoped");
    }

    private native void runLogcat(long tid);

    @SuppressWarnings("unused")
    private int refreshFd() {
        log = new File(logPath, logTimeFormat.format(Instant.now()) + ".log");

        var mode = ParcelFileDescriptor.MODE_WRITE_ONLY | ParcelFileDescriptor.MODE_CREATE |
                ParcelFileDescriptor.MODE_TRUNCATE | ParcelFileDescriptor.MODE_APPEND;
        try (var fd = ParcelFileDescriptor.open(log, mode)) {
            return fd.detachFd();
        } catch (IOException e) {
            Log.w(TAG, "someone chattr +i ?", e);
            return -1;
        }
    }

    public void start() {
        if (isRunning()) return;
        thread = new Thread(this);
        thread.setName("logcat");
        thread.setUncaughtExceptionHandler((t, e) -> {
            Log.e(TAG, "Crash unexpectedly: ", e);
            thread = null;
        });
        thread.start();
    }

    public void stop() {
        // logcat thread is listening for this keyword
        Log.i(TAG, "!!stop!!" + thread.getId());
    }

    public boolean isRunning() {
        return thread != null && thread.isAlive();
    }

    @Nullable
    public File getLog() {
        return log;
    }
}
