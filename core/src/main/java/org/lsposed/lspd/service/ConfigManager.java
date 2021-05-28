/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2021 LSPosed Contributors
 */

package org.lsposed.lspd.service;

import static org.lsposed.lspd.service.ServiceManager.TAG;

import android.content.ContentValues;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SELinux;
import android.os.SharedMemory;
import android.os.SystemClock;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.lsposed.lspd.BuildConfig;
import org.lsposed.lspd.models.Application;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// This config manager assume uid won't change when our service is off.
// Otherwise, user should maintain it manually.
public class ConfigManager {
    public static final int PER_USER_RANGE = 100000;

    private static final String[] MANAGER_PERMISSIONS_TO_GRANT = new String[]{
            "android.permission.INTERACT_ACROSS_USERS",
            "android.permission.WRITE_SECURE_SETTINGS"
    };

    static ConfigManager instance = null;

    private static final File basePath = new File("/data/adb/lspd");
    private static final File configPath = new File(basePath, "config");
    private static final File lockPath = new File(basePath, "lock");
    private static final SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(new File(configPath, "modules_config.db"), null);

    boolean packageStarted = false;

    private static final File resourceHookSwitch = new File(configPath, "enable_resources");
    private boolean resourceHook = false;

    private static final File verboseLogSwitch = new File(configPath, "verbose_log");
    private boolean verboseLog = false;

    private static final File managerPath = new File(configPath, "manager");
    private String manager = null;
    private int managerUid = -1;

    private static final File miscFile = new File(basePath, "misc_path");
    private String miscPath = null;

    private static final File logPath = new File(basePath, "log");
    private static final File modulesLog = new File(logPath, "modules.log");
    private static final File oldModulesLog = new File(logPath, "modules.old.log");
    private static final File verboseLogPath = new File(logPath, "all.log");
    private static FileLock locker = null;

    static {
        try {
            Files.createDirectories(basePath.toPath());
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    private final Handler cacheHandler;

    private final Map<String, SharedMemory> moduleDexes = new ConcurrentHashMap<>();

    private long lastModuleCacheTime = 0;
    private long requestModuleCacheTime = 0;

    private long lastScopeCacheTime = 0;
    private long requestScopeCacheTime = 0;

    private boolean sepolicyLoaded = true;

    static class ProcessScope {
        final String processName;
        final int uid;

        ProcessScope(@NonNull String processName, int uid) {
            this.processName = processName;
            this.uid = uid;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (o instanceof ProcessScope) {
                ProcessScope p = (ProcessScope) o;
                return p.processName.equals(processName) && p.uid == uid;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return processName.hashCode() ^ uid;
        }
    }

    private static final SQLiteStatement createModulesTable = db.compileStatement("CREATE TABLE IF NOT EXISTS modules (" +
            "mid integer PRIMARY KEY AUTOINCREMENT," +
            "module_pkg_name text NOT NULL UNIQUE," +
            "apk_path text NOT NULL, " +
            "enabled BOOLEAN DEFAULT 0 " +
            "CHECK (enabled IN (0, 1))" +
            ");");
    private static final SQLiteStatement createScopeTable = db.compileStatement("CREATE TABLE IF NOT EXISTS scope (" +
            "mid integer," +
            "app_pkg_name text NOT NULL," +
            "user_id integer NOT NULL," +
            "PRIMARY KEY (mid, app_pkg_name, user_id)" +
            ");");

    private final Map<ProcessScope, Map<String, String>> cachedScope = new ConcurrentHashMap<>();

    private final Map<Integer, String> cachedModule = new ConcurrentHashMap<>();

    private void updateCaches(boolean sync) {
        synchronized (this) {
            requestScopeCacheTime = requestModuleCacheTime = SystemClock.elapsedRealtime();
        }
        if (sync) {
            cacheModules();
            cacheScopes();
        } else {
            cacheHandler.post(this::cacheModules);
            cacheHandler.post(this::cacheScopes);
        }
    }

    public boolean tryLock() {
        var openOptions = new HashSet<OpenOption>();
        openOptions.add(StandardOpenOption.CREATE);
        openOptions.add(StandardOpenOption.WRITE);
        var p = PosixFilePermissions.fromString("rw-------");
        var permissions = PosixFilePermissions.asFileAttribute(p);

        try {
            var lockChannel = FileChannel.open(lockPath.toPath(), openOptions, permissions);
            locker = lockChannel.tryLock();
            return locker != null && locker.isValid();
        } catch (Throwable e) {
            return false;
        }
    }

    // for system server, cache is not yet ready, we need to query database for it
    public boolean shouldSkipSystemServer() {
        if (!SELinux.checkSELinuxAccess("u:r:system_server:s0", "u:r:system_server:s0", "process", "execmem")) {
            sepolicyLoaded = false;
            Log.e(TAG, "skip injecting into android because sepolicy was not loaded properly");
            return true; // skip
        }
        try (Cursor cursor = db.query("scope INNER JOIN modules ON scope.mid = modules.mid", new String[]{"modules.mid"}, "app_pkg_name=? AND enabled=1", new String[]{"android"}, null, null, null)) {
            return cursor == null || !cursor.moveToNext();
        }
    }

    public Map<String, String> getModulesForSystemServer() {
        HashMap<String, String> modules = new HashMap<>();
        try (Cursor cursor = db.query("scope INNER JOIN modules ON scope.mid = modules.mid", new String[]{"module_pkg_name", "apk_path"}, "app_pkg_name=? AND enabled=1", new String[]{"android"}, null, null, null)) {
            int apkPathIdx = cursor.getColumnIndex("apk_path");
            int pkgNameIdx = cursor.getColumnIndex("module_pkg_name");
            while (cursor.moveToNext()) {
                modules.put(cursor.getString(pkgNameIdx), cursor.getString(apkPathIdx));
            }
        }
        return modules;
    }

    private static String readText(@NonNull File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath())).trim();
    }

    private static String readText(@NonNull File file, String defaultValue) {
        try {
            if (!file.exists()) return defaultValue;
            return readText(file);
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        return defaultValue;
    }

    private static void writeText(@NonNull File file, String value) {
        try {
            Files.write(file.toPath(), value.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    private static int readInt(@NonNull File file, int defaultValue) {
        try {
            if (!file.exists()) return defaultValue;
            return Integer.parseInt(readText(file));
        } catch (IOException | NumberFormatException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        return defaultValue;
    }

    private static void writeInt(@NonNull File file, int value) {
        writeText(file, String.valueOf(value));
    }

    private synchronized void updateConfig() {
        resourceHook = readInt(resourceHookSwitch, 0) == 1;
        verboseLog = readInt(verboseLogSwitch, 0) == 1;
        miscPath = "/data/misc/" + readText(miscFile, "lspd");
        updateManager();
    }

    public synchronized void updateManager() {
        if (!packageStarted) return;
        try {
            PackageInfo info = PackageService.getPackageInfo(readText(managerPath, BuildConfig.DEFAULT_MANAGER_PACKAGE_NAME), 0, 0);
            if (info != null) {
                managerUid = info.applicationInfo.uid;
                manager = info.packageName;
            } else {
                Log.w(TAG, "manager is not installed");
            }
        } catch (RemoteException ignored) {
        }
    }

    public void ensureManager() {
        if (!packageStarted) return;
        new Thread(() -> {
            if (PackageService.installManagerIfAbsent(manager, new File(basePath, "manager.apk"))) {
                updateManager(BuildConfig.DEFAULT_MANAGER_PACKAGE_NAME);
            }
        }).start();
    }

    public synchronized void updateManager(@NonNull String packageName) {
        writeText(managerPath, packageName);
        manager = packageName;
        updateManager();
    }

    static ConfigManager getInstance() {
        if (instance == null)
            instance = new ConfigManager();
        if (!instance.packageStarted) {
            if (PackageService.getPackageManager() != null) {
                Log.d(TAG, "pm is ready, updating cache");
                instance.packageStarted = true;
                // must ensure cache is valid for later usage
                instance.updateCaches(true);
                instance.updateManager();
            }
        }
        return instance;
    }

    private ConfigManager() {
        HandlerThread cacheThread = new HandlerThread("cache");
        cacheThread.start();
        cacheHandler = new Handler(cacheThread.getLooper());

        createTables();
        updateConfig();
        // must ensure cache is valid for later usage
        updateCaches(true);
    }

    private void createTables() {
        createModulesTable.execute();
        createScopeTable.execute();
    }

    private List<ProcessScope> getAssociatedProcesses(Application app) throws RemoteException {
        Pair<Set<String>, Integer> result = PackageService.fetchProcessesWithUid(app);
        List<ProcessScope> processes = new ArrayList<>();
        for (String processName : result.first) {
            processes.add(new ProcessScope(processName, result.second));
        }
        return processes;
    }

    private synchronized void cacheModules() {
        // skip caching when pm is not yet available
        if (!packageStarted) return;
        if (lastModuleCacheTime >= requestModuleCacheTime) return;
        else lastModuleCacheTime = SystemClock.elapsedRealtime();
        cachedModule.clear();
        try (Cursor cursor = db.query(true, "modules INNER JOIN scope ON scope.mid = modules.mid", new String[]{"module_pkg_name", "user_id"},
                "enabled = 1", null, null, null, null, null)) {
            if (cursor == null) {
                Log.e(TAG, "db cache failed");
                return;
            }
            int pkgNameIdx = cursor.getColumnIndex("module_pkg_name");
            int userIdIdx = cursor.getColumnIndex("user_id");
            Map<String, Map<Integer, PackageInfo>> modules = new HashMap<>();
            Set<String> obsoleteModules = new HashSet<>();
            Set<Application> obsoleteScopes = new HashSet<>();
            while (cursor.moveToNext()) {
                String packageName = cursor.getString(pkgNameIdx);
                int userId = cursor.getInt(userIdIdx);
                var pkgInfo = modules.computeIfAbsent(packageName, m -> {
                    try {
                        return PackageService.getPackageInfoFromAllUsers(m, 0);
                    } catch (Throwable e) {
                        Log.e(TAG, Log.getStackTraceString(e));
                        return Collections.emptyMap();
                    }
                });
                if (pkgInfo.isEmpty()) {
                    obsoleteModules.add(packageName);
                } else if (!pkgInfo.containsKey(userId)) {
                    var module = new Application();
                    module.packageName = packageName;
                    module.userId = userId;
                    obsoleteScopes.add(module);
                } else {
                    var info = pkgInfo.get(userId);
                    assert info != null;
                    cachedModule.computeIfAbsent(info.applicationInfo.uid % PER_USER_RANGE, k -> info.packageName);
                }
            }
            for (var obsoleteModule : obsoleteModules) {
                removeModuleWithoutCache(obsoleteModule);
            }
            for (var obsoleteScope : obsoleteScopes) {
                removeModuleScopeWithoutCache(obsoleteScope);
            }
        }
        Log.d(TAG, "cached modules");
        for (int uid : cachedModule.keySet()) {
            Log.d(TAG, cachedModule.get(uid) + "/" + uid);
        }
    }

    private synchronized void cacheScopes() {
        // skip caching when pm is not yet available
        if (!packageStarted) return;
        if (lastScopeCacheTime >= requestScopeCacheTime) return;
        else lastScopeCacheTime = SystemClock.elapsedRealtime();
        cachedScope.clear();
        try (Cursor cursor = db.query("scope INNER JOIN modules ON scope.mid = modules.mid", new String[]{"app_pkg_name", "module_pkg_name", "user_id", "apk_path"},
                "enabled = 1", null, null, null, null)) {
            if (cursor == null) {
                Log.e(TAG, "db cache failed");
                return;
            }
            int appPkgNameIdx = cursor.getColumnIndex("app_pkg_name");
            int modulePkgNameIdx = cursor.getColumnIndex("module_pkg_name");
            int userIdIdx = cursor.getColumnIndex("user_id");
            int apkPathIdx = cursor.getColumnIndex("apk_path");
            HashSet<Application> obsoletePackages = new HashSet<>();
            while (cursor.moveToNext()) {
                Application app = new Application();
                app.packageName = cursor.getString(appPkgNameIdx);
                app.userId = cursor.getInt(userIdIdx);
                // system server always loads database
                if (app.packageName.equals("android")) continue;
                String apk_path = cursor.getString(apkPathIdx);
                String module_pkg = cursor.getString(modulePkgNameIdx);
                try {
                    List<ProcessScope> processesScope = getAssociatedProcesses(app);
                    if (processesScope.isEmpty()) {
                        obsoletePackages.add(app);
                        continue;
                    }
                    for (ProcessScope processScope : processesScope) {
                        cachedScope.computeIfAbsent(processScope, ignored -> new HashMap<>()).put(module_pkg, apk_path);
                        if (module_pkg.equals(app.packageName)) {
                            var appId = processScope.uid % PER_USER_RANGE;
                            for (var user : UserService.getUsers()) {
                                cachedScope.computeIfAbsent(new ProcessScope(processScope.processName, user.id * PER_USER_RANGE + appId),
                                        ignored -> new HashMap<>()).put(module_pkg, apk_path);
                            }
                        }
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                }
            }
            for (Application obsoletePackage : obsoletePackages) {
                Log.d(TAG, "removing obsolete package: " + obsoletePackage.packageName + "/" + obsoletePackage.userId);
                removeAppWithoutCache(obsoletePackage);
            }
        }
        Log.d(TAG, "cached Scope");
        cachedScope.forEach((ps, module) -> {
            Log.d(TAG, ps.processName + "/" + ps.uid);
            module.forEach((pkg_name, apk_path) -> Log.d(TAG, "\t" + pkg_name));
        });
    }

    // This is called when a new process created, use the cached result
    public Map<String, String> getModulesForProcess(String processName, int uid) {
        return isManager(uid) ? Collections.emptyMap() : cachedScope.getOrDefault(new ProcessScope(processName, uid), Collections.emptyMap());
    }

    // This is called when a new process created, use the cached result
    public boolean shouldSkipProcess(ProcessScope scope) {
        return !cachedScope.containsKey(scope) && !isManager(scope.uid);
    }

    public boolean isUidHooked(int uid) {
        return cachedScope.keySet().stream().reduce(false, (p, scope) -> p || scope.uid == uid, Boolean::logicalOr);
    }

    // This should only be called by manager, so we don't need to cache it
    public List<Application> getModuleScope(String packageName) {
        int mid = getModuleId(packageName);
        if (mid == -1) return null;
        try (Cursor cursor = db.query("scope INNER JOIN modules ON scope.mid = modules.mid", new String[]{"app_pkg_name", "user_id"},
                "scope.mid = ?", new String[]{String.valueOf(mid)}, null, null, null)) {
            if (cursor == null) {
                return null;
            }
            int userIdIdx = cursor.getColumnIndex("user_id");
            int appPkgNameIdx = cursor.getColumnIndex("app_pkg_name");
            ArrayList<Application> result = new ArrayList<>();
            while (cursor.moveToNext()) {
                Application scope = new Application();
                scope.packageName = cursor.getString(appPkgNameIdx);
                scope.userId = cursor.getInt(userIdIdx);
                result.add(scope);
            }
            return result;
        }
    }

    public boolean updateModuleApkPath(String packageName, String apkPath) {
        if (db.inTransaction()) {
            Log.w(TAG, "update module apk path should not be called inside transaction");
            return false;
        }
        ContentValues values = new ContentValues();
        values.put("module_pkg_name", packageName);
        values.put("apk_path", apkPath);
        int count = (int) db.insertWithOnConflict("modules", null, values, SQLiteDatabase.CONFLICT_IGNORE);
        if (count < 0) {
            count = db.updateWithOnConflict("modules", values, "module_pkg_name=?", new String[]{packageName}, SQLiteDatabase.CONFLICT_IGNORE);
        }
        if (count > 0) {
            // Called by oneway binder
            updateCaches(true);
            return true;
        }
        return count >= 0;
    }

    // Only be called before updating modules. No need to cache.
    private int getModuleId(String packageName) {
        if (db.inTransaction()) {
            Log.w(TAG, "get module id should not be called inside transaction");
            return -1;
        }
        try (Cursor cursor = db.query("modules", new String[]{"mid"}, "module_pkg_name=?", new String[]{packageName}, null, null, null)) {
            if (cursor == null) return -1;
            if (cursor.getCount() != 1) return -1;
            cursor.moveToFirst();
            return cursor.getInt(cursor.getColumnIndexOrThrow("mid"));
        }
    }

    public boolean setModuleScope(String packageName, List<Application> scopes) {
        if (scopes == null) return false;
        int mid = getModuleId(packageName);
        if (mid == -1) return false;
        Application self = new Application();
        self.packageName = packageName;
        self.userId = 0;
        scopes.add(self);
        try {
            db.beginTransaction();
            db.delete("scope", "mid = ?", new String[]{String.valueOf(mid)});
            for (Application app : scopes) {
                if (app.packageName.equals("android") && app.userId != 0) continue;
                ContentValues values = new ContentValues();
                values.put("mid", mid);
                values.put("app_pkg_name", app.packageName);
                values.put("user_id", app.userId);
                db.insertWithOnConflict("scope", null, values, SQLiteDatabase.CONFLICT_IGNORE);
            }
        } finally {
            db.setTransactionSuccessful();
            db.endTransaction();
        }
        // Called by manager, should be async
        updateCaches(false);
        return true;
    }

    public String[] enabledModules() {
        try (Cursor cursor = db.query("modules", new String[]{"module_pkg_name"}, "enabled = 1", null, null, null, null)) {
            if (cursor == null) {
                Log.e(TAG, "query enabled modules failed");
                return null;
            }
            int modulePkgNameIdx = cursor.getColumnIndex("module_pkg_name");
            HashSet<String> result = new HashSet<>();
            while (cursor.moveToNext()) {
                result.add(cursor.getString(modulePkgNameIdx));
            }
            return result.toArray(new String[0]);
        }
    }

    public void removeModule(String packageName) {
        if (removeModuleWithoutCache(packageName)) {
            // called by oneway binder
            updateCaches(true);
        }
    }

    private boolean removeModuleWithoutCache(String packageName) {
        int mid = getModuleId(packageName);
        if (mid == -1) return false;
        try {
            db.beginTransaction();
            db.delete("modules", "mid = ?", new String[]{String.valueOf(mid)});
            db.delete("scope", "mid = ?", new String[]{String.valueOf(mid)});
        } finally {
            db.setTransactionSuccessful();
            db.endTransaction();
        }
        return true;
    }

    private boolean removeModuleScopeWithoutCache(Application module) {
        int mid = getModuleId(module.packageName);
        if (mid == -1) return false;
        try {
            db.beginTransaction();
            db.delete("scope", "mid = ? and user_id = ?", new String[]{String.valueOf(mid), String.valueOf(module.userId)});
        } finally {
            db.setTransactionSuccessful();
            db.endTransaction();
        }
        return true;
    }

    public boolean disableModule(String packageName) {
        int mid = getModuleId(packageName);
        if (mid == -1) return false;
        try {
            db.beginTransaction();
            ContentValues values = new ContentValues();
            values.put("enabled", 0);
            db.update("modules", values, "mid = ?", new String[]{String.valueOf(mid)});
        } finally {
            db.setTransactionSuccessful();
            db.endTransaction();
        }
        // called by manager, should be async
        updateCaches(false);
        return true;
    }

    public boolean enableModule(String packageName, String apkPath) {
        if (!updateModuleApkPath(packageName, apkPath)) return false;
        int mid = getModuleId(packageName);
        if (mid == -1) return false;
        try {
            db.beginTransaction();
            ContentValues values = new ContentValues();
            values.put("enabled", 1);
            db.update("modules", values, "mid = ?", new String[]{String.valueOf(mid)});
        } finally {
            db.setTransactionSuccessful();
            db.endTransaction();
        }
        // Called by manager, should be async
        updateCaches(false);
        return true;
    }

    public void updateCache() {
        // Called by oneway binder
        updateCaches(true);
    }

    public void updateAppCache() {
        // Called by oneway binder
        cacheScopes();
    }

    private boolean removeAppWithoutCache(Application app) {
        int count = db.delete("scope", "app_pkg_name = ? AND user_id=?", new String[]{app.packageName, String.valueOf(app.userId)});
        return count >= 1;
    }

    public void setResourceHook(boolean resourceHook) {
        writeInt(resourceHookSwitch, resourceHook ? 1 : 0);
        this.resourceHook = resourceHook;
    }

    public void setVerboseLog(boolean verboseLog) {
        writeInt(verboseLogSwitch, verboseLog ? 1 : 0);
        this.verboseLog = verboseLog;
    }

    public boolean resourceHook() {
        return resourceHook;
    }

    public boolean verboseLog() {
        return verboseLog;
    }

    public ParcelFileDescriptor getModulesLog(int mode) {
        try {
            if (modulesLog.length() > 16 * 1024 * 1024) {
                //noinspection ResultOfMethodCallIgnored
                modulesLog.renameTo(oldModulesLog);
            }
            return ParcelFileDescriptor.open(modulesLog, mode | ParcelFileDescriptor.MODE_CREATE);
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return null;
        }
    }

    public ParcelFileDescriptor getVerboseLog() {
        try {
            return ParcelFileDescriptor.open(verboseLogPath, ParcelFileDescriptor.MODE_READ_ONLY);
        } catch (FileNotFoundException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return null;
        }
    }

    public boolean clearLogs(boolean verbose) {
        try {
            OutputStream os = new FileOutputStream(verbose ? verboseLogPath : modulesLog);
            os.close();
            return true;
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return false;
        }
    }

    public boolean isManager(String packageName) {
        return packageName.equals(manager) || packageName.equals(BuildConfig.DEFAULT_MANAGER_PACKAGE_NAME);
    }

    public boolean isManager(int uid) {
        return uid == managerUid;
    }

    public String getPrefsPath(String fileName, int uid) {
        int userId = uid / PER_USER_RANGE;
        return miscPath + File.separator + "prefs" + (userId == 0 ? "" : String.valueOf(userId)) + File.separator + fileName;
    }

    public static void grantManagerPermission() {
        String managerPackageName = readText(managerPath, BuildConfig.DEFAULT_MANAGER_PACKAGE_NAME);
        Arrays.stream(MANAGER_PERMISSIONS_TO_GRANT).forEach(permission -> {
            try {
                PackageService.grantRuntimePermission(managerPackageName, permission, 0);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        });
    }

    public boolean isModule(int uid) {
        return cachedModule.containsKey(uid % PER_USER_RANGE);
    }

    public boolean isModule(String packageName) {
        return cachedModule.containsValue(packageName);
    }

    private void recursivelyChown(File file, int uid, int gid) throws ErrnoException {
        Os.chown(file.toString(), uid, gid);
        if (file.isDirectory()) {
            for (File subFile : file.listFiles()) {
                recursivelyChown(subFile, uid, gid);
            }
        }
    }

    public boolean ensureModulePrefsPermission(int uid) {
        String packageName = cachedModule.get(uid);
        if (packageName == null) return false;
        File path = new File(getPrefsPath(packageName, uid));
        try {
            if (path.exists() && !path.isDirectory()) path.delete();
            if (!path.exists()) Files.createDirectories(path.toPath());
            recursivelyChown(path, uid, 1000);
            return true;
        } catch (IOException | ErrnoException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return false;
        }
    }

    public String getManagerPackageName() {
        return manager;
    }

    public boolean isSepolicyLoaded() {
        return sepolicyLoaded;
    }
}
