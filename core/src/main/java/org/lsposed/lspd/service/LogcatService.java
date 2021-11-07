package org.lsposed.lspd.service;

import android.annotation.SuppressLint;
import android.os.ParcelFileDescriptor;
import android.os.SystemProperties;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;

import org.lsposed.lspd.BuildConfig;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class LogcatService implements Runnable {
    private static final String TAG = "LSPosedLogcat";
    private static final int mode = ParcelFileDescriptor.MODE_WRITE_ONLY |
            ParcelFileDescriptor.MODE_CREATE |
            ParcelFileDescriptor.MODE_TRUNCATE |
            ParcelFileDescriptor.MODE_APPEND;
    private Path modulesLog = null;
    private Path verboseLog = null;
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
        // Meizu devices set this prop and prevent debug logs from being recorded
        if (BuildConfig.DEBUG && SystemProperties.getInt("persist.sys.log_reject_level", 0) > 0) {
            SystemProperties.set("persist.sys.log_reject_level", "0");
        }
        runLogcat();
        Log.i(TAG, "stopped");
    }

    @SuppressWarnings("unused")
    private int refreshFd(boolean isVerboseLog) {
        try {
            File log;
            if (isVerboseLog) {
                checkFdFile(verboseLog);
                log = ConfigFileManager.getNewVerboseLogPath();
            } else {
                checkFdFile(modulesLog);
                log = ConfigFileManager.getNewModulesLogPath();
            }
            Log.i(TAG, "New log file: " + log);
            int fd = ParcelFileDescriptor.open(log, mode).detachFd();
            var fdFile = Paths.get("/proc/self/fd", String.valueOf(fd));
            if (isVerboseLog) verboseLog = fdFile;
            else modulesLog = fdFile;
            return fd;
        } catch (IOException e) {
            if (isVerboseLog) verboseLog = null;
            else modulesLog = null;
            Log.w(TAG, "refreshFd", e);
            return -1;
        }
    }

    private static void checkFdFile(Path fdFile) {
        if (fdFile == null) return;
        try {
            var file = Files.readSymbolicLink(fdFile);
            if (!Files.exists(file)) {
                var parent = file.getParent();
                if (!Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS)) {
                    Files.deleteIfExists(parent);
                }
                Files.createDirectories(parent);
                var name = file.getFileName().toString();
                var originName = name.substring(0, name.lastIndexOf(' '));
                Files.copy(fdFile, parent.resolve(originName));
            }
        } catch (IOException e) {
            Log.w(TAG, "checkFd " + fdFile, e);
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
        getprop();
    }

    private static class GetProp implements Runnable {
        private volatile InputStream is;

        @Override
        public void run() {
            try {
                Os.setuid(9999); // AID_NOBODY
                is = new ProcessBuilder("getprop").start().getInputStream();
            } catch (ErrnoException | IOException e) {
                e.printStackTrace();
            }
        }

        public InputStream getValue() {
            return is;
        }
    }

    private void getprop() {
        try {
            var get = new GetProp();
            var thread = new Thread(get);
            thread.start();
            thread.join();
            var is = get.getValue();
            var propsLogPath = ConfigFileManager.getpropsLogPath();
            Files.copy(is, propsLogPath.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | InterruptedException e) {
            Log.e(TAG, "getprop: " + e);
        }
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
        return verboseLog.toFile();
    }

    public File getModulesLog() {
        return modulesLog.toFile();
    }

    public void checkLogFile() {
        try {
            modulesLog.toRealPath();
        } catch (IOException e) {
            refresh(false);
        }
        try {
            verboseLog.toRealPath();
        } catch (IOException e) {
            refresh(true);
        }
    }
}
