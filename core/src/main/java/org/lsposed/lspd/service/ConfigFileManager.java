package org.lsposed.lspd.service;

import static org.lsposed.lspd.service.ServiceManager.TAG;

import android.os.ParcelFileDescriptor;
import android.os.SELinux;
import android.util.Log;

import org.lsposed.lspd.util.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

class ConfigFileManager {
    static final File basePath = new File("/data/adb/lspd");
    static final File managerApkPath = new File(basePath, "manager.apk");
    private static final File lockPath = new File(basePath, "lock");
    private static final File configDirPath = new File(basePath, "config");
    static final File dbPath = new File(configDirPath, "modules_config.db");
    private static final File logDirPath = new File(basePath, "log");
    private static final File oldLogDirPath = new File(basePath, "log.old");
    private static final DateTimeFormatter formatter =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(Utils.getZoneId());
    @SuppressWarnings("FieldCanBeLocal")
    private static FileLocker locker = null;

    static {
        try {
            Files.createDirectories(basePath.toPath());
            SELinux.setFileContext(basePath.getPath(), "u:object_r:system_file:s0");
            Files.createDirectories(configDirPath.toPath());
            Files.createDirectories(logDirPath.toPath());
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    static void deleteFolderIfExists(Path target) throws IOException {
        if (!Files.exists(target)) return;
        Files.walkFileTree(target, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException e)
                    throws IOException {
                if (e == null) {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                } else {
                    throw e;
                }
            }
        });
    }

    private static void moveFolderIfExists(Path source, Path target) throws IOException {
        if (!Files.exists(source)) return;
        deleteFolderIfExists(target);
        Files.move(source, target);
    }

    static void moveLogDir() {
        try {
            moveFolderIfExists(logDirPath.toPath(), oldLogDirPath.toPath());
            Files.createDirectories(logDirPath.toPath());
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    private static String getNewLogFileName(String prefix) {
        return prefix + "_" + formatter.format(Instant.now()) + ".txt";
    }

    static File getNewVerboseLogPath() {
        return new File(logDirPath, getNewLogFileName("verbose"));
    }

    static File getNewModulesLogPath() {
        return new File(logDirPath, getNewLogFileName("modules"));
    }

    static Map<String, ParcelFileDescriptor> getLogs() {
        var map = new LinkedHashMap<String, ParcelFileDescriptor>();
        try {
            putFds(map, logDirPath.toPath());
            putFds(map, oldLogDirPath.toPath());
        } catch (IOException e) {
            Log.e(TAG, "getLogs", e);
        }
        return map;
    }

    private static void putFds(Map<String, ParcelFileDescriptor> map, Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                var name = path.relativize(file).toString();
                var fd = ParcelFileDescriptor.open(file.toFile(), ParcelFileDescriptor.MODE_READ_ONLY);
                map.put(name, fd);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static String readText(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath())).trim();
    }

    // TODO: Remove after next release
    @SuppressWarnings("ResultOfMethodCallIgnored")
    static void migrateOldConfig(ConfigManager configManager) {
        var miscPath = new File(basePath, "misc_path");
        var enableResources = new File(configDirPath, "enable_resources");
        var manager = new File(configDirPath, "manager");
        var verboseLog = new File(configDirPath, "verbose_log");

        if (miscPath.exists()) {
            try {
                var s = "/data/misc/" + readText(miscPath);
                configManager.updateModulePrefs("lspd", 0, "config", "misc_path", s);
                miscPath.delete();
            } catch (IOException ignored) {
            }
        }
        if (enableResources.exists()) {
            try {
                var s = readText(enableResources);
                var i = Integer.parseInt(s);
                configManager.updateModulePrefs("lspd", 0, "config", "enable_resources", i == 1);
                enableResources.delete();
            } catch (IOException ignored) {
            }
        }
        manager.delete();
        verboseLog.delete();
    }

    static boolean tryLock() {
        var openOptions = new HashSet<OpenOption>();
        openOptions.add(StandardOpenOption.CREATE);
        openOptions.add(StandardOpenOption.WRITE);
        var p = PosixFilePermissions.fromString("rw-------");
        var permissions = PosixFilePermissions.asFileAttribute(p);

        try {
            var lockChannel = FileChannel.open(lockPath.toPath(), openOptions, permissions);
            locker = new FileLocker(lockChannel);
            return locker.isValid();
        } catch (Throwable e) {
            return false;
        }
    }

    private static class FileLocker {
        private final FileChannel lockChannel;
        private final FileLock locker;

        FileLocker(FileChannel lockChannel) throws IOException {
            this.lockChannel = lockChannel;
            this.locker = lockChannel.tryLock();
        }

        boolean isValid() {
            return this.locker != null && this.locker.isValid();
        }

        @Override
        protected void finalize() throws Throwable {
            this.locker.release();
            this.lockChannel.close();
        }
    }
}
