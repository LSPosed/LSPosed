package org.lsposed.lspd.service;

import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class LogcatService implements Runnable {
    private static final String TAG = "LSPosedLogcat";
    private Thread thread;
    private final File log;
    private final File oldLod;
    private ParcelFileDescriptor fd;

    public LogcatService(File logPath) {
        System.loadLibrary("daemon");
        log = new File(logPath, "logcat.txt");
        oldLod = new File(logPath, "logcat.old.txt");
    }

    @Override
    public void run() {
        Log.i(TAG, "start running");
        runLogcat(thread.getId());
        Log.i(TAG, "stoped");
    }

    private native void runLogcat(long tid);

    private int refreshFd() {
        if (log.length() > 32 * 1024 * 1024) {
            //noinspection ResultOfMethodCallIgnored
            log.renameTo(oldLod);

            if (fd != null) {
                try {
                    fd.close();
                } catch (IOException ignored) {
                }
                fd = null;
            }
        }

        if (fd == null) {
            var mode = ParcelFileDescriptor.MODE_WRITE_ONLY | ParcelFileDescriptor.MODE_APPEND |
                    ParcelFileDescriptor.MODE_CREATE;
            try {
                fd = ParcelFileDescriptor.open(log, mode);
            } catch (FileNotFoundException e) {
                Log.w(TAG, "someone chattr +i ?", e); //TODO: Random file name
                return 1; // FileDescriptor.out
            }
        }

        return fd.getFd();
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
        Log.i(TAG, "!!stop!!" + thread.getId());
    }

    public boolean isRunning() {
        return thread != null && thread.isAlive();
    }

    public File getLog() {
        return log;
    }
}
