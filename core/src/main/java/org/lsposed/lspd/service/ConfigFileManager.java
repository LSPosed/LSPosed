package org.lsposed.lspd.service;

import static org.lsposed.lspd.service.ServiceManager.TAG;
import static org.lsposed.lspd.service.ServiceManager.toGlobalNamespace;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.ParcelFileDescriptor;
import android.os.SELinux;
import android.os.SharedMemory;
import android.os.SystemProperties;
import android.system.ErrnoException;
import android.system.OsConstants;
import android.util.Log;

import androidx.annotation.Nullable;

import org.lsposed.lspd.models.PreLoadedApk;
import org.lsposed.lspd.util.InstallerVerifier;
import org.lsposed.lspd.util.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.channels.Channels;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipFile;

public class ConfigFileManager {
    static final Path basePath = Paths.get("/data/adb/lspd");
    private static final Path managerApkPath = basePath.resolve("manager.apk");
    private static final Path lockPath = basePath.resolve("lock");
    private static final Path configDirPath = basePath.resolve("config");
    static final File dbPath = configDirPath.resolve("modules_config.db").toFile();
    private static final Path logDirPath = basePath.resolve("log");
    private static final Path oldLogDirPath = basePath.resolve("log.old");
    private static final DateTimeFormatter formatter =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(Utils.getZoneId());
    @SuppressWarnings("FieldCanBeLocal")
    private static FileLocker locker = null;
    private static Resources res = null;
    private static ParcelFileDescriptor fd = null;

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

    public static Resources getResources() {
        loadRes();
        return res;
    }

    // from AndroidRuntime.cpp
    private static String readLocale() {
        String locale = SystemProperties.get("persist.sys.locale", "");
        if (!locale.isEmpty()) {
            return locale;
        }

        String language = SystemProperties.get("persist.sys.language", "");
        if (!language.isEmpty()) {
            String country = SystemProperties.get("persist.sys.country", "");
            String variant = SystemProperties.get("persist.sys.localevar", "");

            String out = language;
            if (!country.isEmpty()) {
                out = out + "-" + country;
            }

            if (!variant.isEmpty()) {
                out = out + "-" + variant;
            }

            return out;
        }

        String productLocale = SystemProperties.get("ro.product.locale", "");
        if (!productLocale.isEmpty()) {
            return productLocale;
        }

        // If persist.sys.locale and ro.product.locale are missing,
        // construct a locale value from the individual locale components.
        String productLanguage = SystemProperties.get("ro.product.locale.language", "en");
        String productRegion = SystemProperties.get("ro.product.locale.region", "US");

        return productLanguage + "-" + productRegion;
    }

    private static void loadRes() {
        if (res != null) return;
        try {
            var am = AssetManager.class.newInstance();
            //noinspection JavaReflectionMemberAccess DiscouragedPrivateApi
            Method addAssetPath = AssetManager.class.getDeclaredMethod("addAssetPath", String.class);
            addAssetPath.setAccessible(true);
            //noinspection ConstantConditions
            if ((int) addAssetPath.invoke(am, managerApkPath.toString()) > 0)
                //noinspection deprecation
                res = new Resources(am, null, null);
        } catch (Throwable e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    static void reloadConfiguration() {
        loadRes();
        try {
            var conf = ActivityManagerService.getConfiguration();
            if (conf != null)
                //noinspection deprecation
                res.updateConfiguration(conf, res.getDisplayMetrics());
        } catch (Throwable e) {
            Log.e(TAG, "reload configuration", e);
        }
    }

    static ParcelFileDescriptor getManagerApk() throws IOException {
        if (fd != null) return fd.dup();
        if (!InstallerVerifier.verifyInstallerSignature(managerApkPath.toString())) return null;

        SELinux.setFileContext(managerApkPath.toString(), "u:object_r:system_file:s0");
        fd = ParcelFileDescriptor.open(managerApkPath.toFile(), ParcelFileDescriptor.MODE_READ_ONLY);
        return fd.dup();
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

    static File getNewVerboseLogPath() throws IOException {
        Files.createDirectories(logDirPath);
        return logDirPath.resolve(getNewLogFileName("verbose")).toFile();
    }

    static File getNewModulesLogPath() throws IOException {
        Files.createDirectories(logDirPath);
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

    private static void readDexes(ZipFile apkFile, List<SharedMemory> preLoadedDexes) {
        int secondary = 2;
        for (var dexFile = apkFile.getEntry("classes.dex"); dexFile != null;
             dexFile = apkFile.getEntry("classes" + secondary + ".dex"), secondary++) {
            try (var in = apkFile.getInputStream(dexFile)) {
                var memory = SharedMemory.create(null, in.available());
                var byteBuffer = memory.mapReadWrite();
                Channels.newChannel(in).read(byteBuffer);
                SharedMemory.unmap(byteBuffer);
                memory.setProtect(OsConstants.PROT_READ);
                preLoadedDexes.add(memory);
            } catch (IOException | ErrnoException e) {
                Log.w(TAG, "Can not load " + dexFile + " in " + apkFile, e);
            }
        }
    }

    private static void readName(ZipFile apkFile, String initName, List<String> names) {
        var initEntry = apkFile.getEntry(initName);
        if (initEntry == null) return;
        try (var in = apkFile.getInputStream(initEntry)) {
            var reader = new BufferedReader(new InputStreamReader(in));
            String name;
            while ((name = reader.readLine()) != null) {
                name = name.trim();
                if (name.isEmpty() || name.startsWith("#")) continue;
                names.add(name);
            }
        } catch (IOException e) {
            Log.e(TAG, "Can not open " + initEntry, e);
        }
    }

    @Nullable
    static PreLoadedApk loadModule(String path) {
        if (path == null) return null;
        var file = new PreLoadedApk();
        var preLoadedDexes = new ArrayList<SharedMemory>();
        var moduleClassNames = new ArrayList<String>(1);
        var moduleLibraryNames = new ArrayList<String>(1);
        try (var apkFile = new ZipFile(toGlobalNamespace(path))) {
            readDexes(apkFile, preLoadedDexes);
            readName(apkFile, "assets/xposed_init", moduleClassNames);
            readName(apkFile, "assets/native_init", moduleLibraryNames);
        } catch (IOException e) {
            Log.e(TAG, "Can not open " + path, e);
            return null;
        }
        if (preLoadedDexes.isEmpty()) return null;
        if (moduleClassNames.isEmpty()) return null;
        file.preLoadedDexes = preLoadedDexes;
        file.moduleClassNames = moduleClassNames;
        file.moduleLibraryNames = moduleLibraryNames;
        return file;
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
