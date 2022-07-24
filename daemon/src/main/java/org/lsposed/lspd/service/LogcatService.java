package org.lsposed.lspd.service;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.SELinux;
import android.os.SystemProperties;
import android.system.Os;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;

public class LogcatService implements Runnable {
    private static final String TAG = "LSPosedLogcat";
    private static final int mode = ParcelFileDescriptor.MODE_WRITE_ONLY |
            ParcelFileDescriptor.MODE_CREATE |
            ParcelFileDescriptor.MODE_TRUNCATE |
            ParcelFileDescriptor.MODE_APPEND;
    private int modulesFd = -1;
    private int verboseFd = -1;
    private Thread thread = null;

    static class LogLRU extends LinkedHashMap<File, Object> {
        private static final int MAX_ENTRIES = 10;

        public LogLRU() {
            super(MAX_ENTRIES, 1f, false);
        }

        @Override
        synchronized protected boolean removeEldestEntry(Entry<File, Object> eldest) {
            if (size() > MAX_ENTRIES && eldest.getKey().delete()) {
                Log.d(TAG, "Deleted old log " + eldest.getKey().getAbsolutePath());
                return true;
            }
            return false;
        }
    }

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final LinkedHashMap<File, Object> moduleLogs = new LogLRU();
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final LinkedHashMap<File, Object> verboseLogs = new LogLRU();

    @SuppressLint("UnsafeDynamicallyLoadedCode")
    public LogcatService() {
        String classPath = System.getProperty("java.class.path");
        var abi = Process.is64Bit() ? Build.SUPPORTED_64_BIT_ABIS[0] : Build.SUPPORTED_32_BIT_ABIS[0];
        System.load(classPath + "!/lib/" + abi + "/" + System.mapLibraryName("daemon"));
        ConfigFileManager.moveLogDir();

        // Meizu devices set this prop and prevent debug logs from being recorded
        if (SystemProperties.getInt("persist.sys.log_reject_level", 0) > 0) {
            SystemProperties.set("persist.sys.log_reject_level", "0");
        }

        getprop();
        dmesg();
    }

    private static void getprop() {
        // multithreaded process can not change their context type,
        // start a new process to set restricted context to filter privacy props
        var cmd = "echo -n u:r:untrusted_app:s0 > /proc/thread-self/attr/current; getprop";
        try {
            SELinux.setFSCreateContext("u:object_r:app_data_file:s0");
            new ProcessBuilder("sh", "-c", cmd)
                    .redirectOutput(ConfigFileManager.getPropsPath())
                    .start();
        } catch (IOException e) {
            Log.e(TAG, "getprop: ", e);
        } finally {
            SELinux.setFSCreateContext(null);
        }
    }

    private static void dmesg() {
        try {
            new ProcessBuilder("dmesg")
                    .redirectOutput(ConfigFileManager.getKmsgPath())
                    .start();
        } catch (IOException e) {
            Log.e(TAG, "dmesg: ", e);
        }
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
            File log;
            if (isVerboseLog) {
                checkFd(verboseFd);
                log = ConfigFileManager.getNewVerboseLogPath();
            } else {
                checkFd(modulesFd);
                log = ConfigFileManager.getNewModulesLogPath();
            }
            Log.i(TAG, "New log file: " + log);
            ConfigFileManager.chattr0(log.toPath().getParent());
            int fd = ParcelFileDescriptor.open(log, mode).detachFd();
            if (isVerboseLog) {
                synchronized (verboseLogs) {
                    verboseLogs.put(log, new Object());
                }
                verboseFd = fd;
            } else {
                synchronized (moduleLogs) {
                    moduleLogs.put(log, new Object());
                }
                modulesFd = fd;
            }
            return fd;
        } catch (IOException e) {
            if (isVerboseLog) verboseFd = -1;
            else modulesFd = -1;
            Log.w(TAG, "refreshFd", e);
            return -1;
        }
    }

    private static void checkFd(int fd) {
        if (fd == -1) return;
        try {
            var jfd = new FileDescriptor();
            //noinspection JavaReflectionMemberAccess DiscouragedPrivateApi
            jfd.getClass().getDeclaredMethod("setInt$", int.class).invoke(jfd, fd);
            var stat = Os.fstat(jfd);
            if (stat.st_nlink == 0) {
                var file = Files.readSymbolicLink(fdToPath(fd));
                var parent = file.getParent();
                if (!Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS)) {
                    if (ConfigFileManager.chattr0(parent))
                        Files.deleteIfExists(parent);
                }
                var name = file.getFileName().toString();
                var originName = name.substring(0, name.lastIndexOf(' '));
                Files.copy(file, parent.resolve(originName));
            }
        } catch (Throwable e) {
            Log.w(TAG, "checkFd " + fd, e);
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

    private static Path fdToPath(int fd) {
        if (fd == -1) return null;
        else return Paths.get("/proc/self/fd", String.valueOf(fd));
    }

    public File getVerboseLog() {
        var path = fdToPath(verboseFd);
        return path == null ? null : path.toFile();
    }

    public File getModulesLog() {
        var path = fdToPath(modulesFd);
        return path == null ? null : path.toFile();
    }

    public void checkLogFile() {
        if (modulesFd == -1)
            refresh(false);
        if (verboseFd == -1)
            refresh(true);
    }
}
