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
import java.nio.file.Paths;
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
    static final Path basePath = Paths.get("/data/adb/lspd");
    static final File managerApkPath = basePath.resolve("manager.apk").toFile();
    private static final Path lockPath = basePath.resolve("lock");
    private static final Path configDirPath = basePath.resolve("config");
    static final File dbPath = configDirPath.resolve("modules_config.db").toFile();
    private static final Path logDirPath = basePath.resolve("log");
    private static final Path oldLogDirPath = basePath.resolve("log.old");
    private static final DateTimeFormatter formatter =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(Utils.getZoneId());
    @SuppressWarnings("FieldCanBeLocal")
    private static FileLocker locker = null;

    static {
        try {
            Files.createDirectories(basePath);
            SELinux.setFileContext(basePath.toString(), "u:object_r:system_file:s0");
            Files.createDirectories(configDirPath);
            Files.createDirectories(logDirPath);
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    static void deleteFolderIfExists(Path target) throws IOException {
        if (Files.notExists(target)) return;
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

    static void moveLogDir() {
        try {
            if (Files.exists(logDirPath)) {
                deleteFolderIfExists(oldLogDirPath);
                Files.move(logDirPath, oldLogDirPath);
            }
            Files.createDirectories(logDirPath);
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    private static String getNewLogFileName(String prefix) {
        return prefix + "_" + formatter.format(Instant.now()) + ".txt";
    }

    static File getNewVerboseLogPath() {
        return logDirPath.resolve(getNewLogFileName("verbose")).toFile();
    }

    static File getNewModulesLogPath() {
        return logDirPath.resolve(getNewLogFileName("modules")).toFile();
    }

    static Map<String, ParcelFileDescriptor> getLogs() {
        var map = new LinkedHashMap<String, ParcelFileDescriptor>();
        try {
            putFds(map, logDirPath);
            putFds(map, oldLogDirPath);
        } catch (IOException e) {
            Log.e(TAG, "getLogs", e);
        }
        return map;
    }

    private static void putFds(Map<String, ParcelFileDescriptor> map, Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                var name = path.getParent().relativize(file).toString();
                var fd = ParcelFileDescriptor.open(file.toFile(), ParcelFileDescriptor.MODE_READ_ONLY);
                map.put(name, fd);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static String readText(Path file) throws IOException {
        return new String(Files.readAllBytes(file)).trim();
    }

    // TODO: Remove after next release
    static void migrateOldConfig(ConfigManager configManager) {
        var miscPath = basePath.resolve("misc_path");
        var enableResources = configDirPath.resolve("enable_resources");
        var manager = configDirPath.resolve("manager");
        var verboseLog = configDirPath.resolve("verbose_log");

        if (Files.exists(miscPath)) {
            try {
                var s = "/data/misc/" + readText(miscPath);
                configManager.updateModulePrefs("lspd", 0, "config", "misc_path", s);
                Files.delete(miscPath);
            } catch (IOException ignored) {
            }
        }
        if (Files.exists(enableResources)) {
            try {
                var s = readText(enableResources);
                var i = Integer.parseInt(s);
                configManager.updateModulePrefs("lspd", 0, "config", "enable_resources", i == 1);
                Files.delete(enableResources);
            } catch (IOException ignored) {
            }
        }
        try {
            Files.deleteIfExists(manager);
            Files.deleteIfExists(verboseLog);
        } catch (IOException ignored) {
        }
    }

    static boolean tryLock() {
        var openOptions = new HashSet<OpenOption>();
        openOptions.add(StandardOpenOption.CREATE);
        openOptions.add(StandardOpenOption.WRITE);
        var p = PosixFilePermissions.fromString("rw-------");
        var permissions = PosixFilePermissions.asFileAttribute(p);

        try {
            var lockChannel = FileChannel.open(lockPath, openOptions, permissions);
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
