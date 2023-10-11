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

import static org.lsposed.lspd.service.PackageService.MATCH_ALL_FLAGS;
import static org.lsposed.lspd.service.PackageService.PER_USER_RANGE;
import static org.lsposed.lspd.service.ServiceManager.TAG;
import static org.lsposed.lspd.service.ServiceManager.existsInGlobalNamespace;
import static org.lsposed.lspd.service.ServiceManager.toGlobalNamespace;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageParser;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelFileDescriptor;
import android.os.Process;
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

import org.apache.commons.lang3.SerializationUtils;
import org.lsposed.daemon.BuildConfig;
import org.lsposed.lspd.models.Application;
import org.lsposed.lspd.models.Module;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import hidden.HiddenApiBridge;

public class ConfigManager {
    private static ConfigManager instance = null;

    private final SQLiteDatabase db = openDb();

    private boolean verboseLog = true;
    private boolean dexObfuscate = true;
    private boolean enableStatusNotification = true;
    private Path miscPath = null;

    private int managerUid = -1;

    private final Handler cacheHandler;

    private long lastModuleCacheTime = 0;
    private long requestModuleCacheTime = 0;

    private long lastScopeCacheTime = 0;
    private long requestScopeCacheTime = 0;

    private String api = "(???)";

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
            return Objects.hashCode(processName) ^ uid;
        }
    }

    private final SQLiteStatement createModulesTable = db.compileStatement("CREATE TABLE IF NOT EXISTS modules (" +
            "mid integer PRIMARY KEY AUTOINCREMENT," +
            "module_pkg_name text NOT NULL UNIQUE," +
            "apk_path text NOT NULL, " +
            "enabled BOOLEAN DEFAULT 0 " +
            "CHECK (enabled IN (0, 1))" +
            ");");
    private final SQLiteStatement createScopeTable = db.compileStatement("CREATE TABLE IF NOT EXISTS scope (" +
            "mid integer," +
            "app_pkg_name text NOT NULL," +
            "user_id integer NOT NULL," +
            "PRIMARY KEY (mid, app_pkg_name, user_id)," +
            "CONSTRAINT scope_module_constraint" +
            "  FOREIGN KEY (mid)" +
            "  REFERENCES modules (mid)" +
            "  ON DELETE CASCADE" +
            ");");
    private final SQLiteStatement createConfigTable = db.compileStatement("CREATE TABLE IF NOT EXISTS configs (" +
            "module_pkg_name text NOT NULL," +
            "user_id integer NOT NULL," +
            "`group` text NOT NULL," +
            "`key` text NOT NULL," +
            "data blob NOT NULL," +
            "PRIMARY KEY (module_pkg_name, user_id, `group`, `key`)," +
            "CONSTRAINT config_module_constraint" +
            "  FOREIGN KEY (module_pkg_name)" +
            "  REFERENCES modules (module_pkg_name)" +
            "  ON DELETE CASCADE" +
            ");");

    private final Map<ProcessScope, List<Module>> cachedScope = new ConcurrentHashMap<>();

    // packageName, Module
    private final Map<String, Module> cachedModule = new ConcurrentHashMap<>();

    // packageName, userId, group, key, value
    private final Map<Pair<String, Integer>, Map<String, HashMap<String, Object>>> cachedConfig = new ConcurrentHashMap<>();

    private Set<String> scopeRequestBlocked = new HashSet<>();

    private static SQLiteDatabase openDb() {
        var params = new SQLiteDatabase.OpenParams.Builder()
                .addOpenFlags(SQLiteDatabase.CREATE_IF_NECESSARY | SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING)
                .setErrorHandler(sqLiteDatabase -> Log.w(TAG, "database corrupted"));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.setSynchronousMode("NORMAL");
        }
        return SQLiteDatabase.openDatabase(ConfigFileManager.dbPath.getAbsoluteFile(), params.build());
    }

    private void updateCaches(boolean sync) {
        synchronized (cacheHandler) {
            requestScopeCacheTime = requestModuleCacheTime = SystemClock.elapsedRealtime();
        }
        if (sync) {
            cacheModules();
        } else {
            cacheHandler.post(this::cacheModules);
        }
    }

    // for system server, cache is not yet ready, we need to query database for it
    public boolean shouldSkipSystemServer() {
        if (!SELinux.checkSELinuxAccess("u:r:system_server:s0", "u:r:system_server:s0", "process", "execmem")) {
            Log.e(TAG, "skip injecting into android because sepolicy was not loaded properly");
            return true; // skip
        }
        try (Cursor cursor = db.query("scope INNER JOIN modules ON scope.mid = modules.mid", new String[]{"modules.mid"}, "app_pkg_name=? AND enabled=1", new String[]{"system"}, null, null, null)) {
            return cursor == null || !cursor.moveToNext();
        }
    }

    @SuppressLint("BlockedPrivateApi")
    public List<Module> getModulesForSystemServer() {
        List<Module> modules = new LinkedList<>();
        try (Cursor cursor = db.query("scope INNER JOIN modules ON scope.mid = modules.mid", new String[]{"module_pkg_name", "apk_path"}, "app_pkg_name=? AND enabled=1", new String[]{"system"}, null, null, null)) {
            int apkPathIdx = cursor.getColumnIndex("apk_path");
            int pkgNameIdx = cursor.getColumnIndex("module_pkg_name");
            while (cursor.moveToNext()) {
                var module = new Module();
                module.apkPath = cursor.getString(apkPathIdx);
                module.packageName = cursor.getString(pkgNameIdx);
                var cached = cachedModule.get(module.packageName);
                if (cached != null) {
                    modules.add(cached);
                    continue;
                }
                var statPath = toGlobalNamespace("/data/user_de/0/" + module.packageName).getAbsolutePath();
                try {
                    module.appId = Os.stat(statPath).st_uid;
                } catch (ErrnoException e) {
                    Log.w(TAG, "cannot stat " + statPath, e);
                    module.appId = -1;
                }
                try {
                    var apkFile = new File(module.apkPath);
                    var pkg = new PackageParser().parsePackage(apkFile, 0, false);
                    module.applicationInfo = pkg.applicationInfo;
                    module.applicationInfo.sourceDir = module.apkPath;
                    module.applicationInfo.dataDir = statPath;
                    module.applicationInfo.deviceProtectedDataDir = statPath;
                    HiddenApiBridge.ApplicationInfo_credentialProtectedDataDir(module.applicationInfo, statPath);
                    module.applicationInfo.processName = module.packageName;
                } catch (PackageParser.PackageParserException e) {
                    Log.w(TAG, "failed to parse " + module.apkPath, e);
                }
                module.service = new LSPInjectedModuleService(module.packageName);
                modules.add(module);
            }
        }

        return modules.parallelStream().filter(m -> {
            var file = ConfigFileManager.loadModule(m.apkPath, dexObfuscate);
            if (file == null) {
                Log.w(TAG, "Can not load " + m.apkPath + ", skip!");
                return false;
            }
            m.file = file;
            cachedModule.putIfAbsent(m.packageName, m);
            return true;
        }).collect(Collectors.toList());
    }

    private synchronized void updateConfig() {
        Map<String, Object> config = getModulePrefs("lspd", 0, "config");

        Object bool = config.get("enable_verbose_log");
        verboseLog = bool == null || (boolean) bool;

        bool = config.get("enable_dex_obfuscate");
        dexObfuscate = bool == null || (boolean) bool;

        bool = config.get("enable_auto_add_shortcut");
        if (bool != null) {
            // TODO: remove
            updateModulePrefs("lspd", 0, "config", "enable_auto_add_shortcut", null);
        }

        bool = config.get("enable_status_notification");
        enableStatusNotification = bool == null || (boolean) bool;

        var set = (Set<String>) config.get("scope_request_blocked");
        scopeRequestBlocked = set == null ? new HashSet<>() : set;

        // Don't migrate to ConfigFileManager, as XSharedPreferences will be restored soon
        String string = (String) config.get("misc_path");
        if (string == null) {
            miscPath = Paths.get("/data", "misc", UUID.randomUUID().toString());
            updateModulePrefs("lspd", 0, "config", "misc_path", miscPath.toString());
        } else {
            miscPath = Paths.get(string);
        }
        try {
            var perms = PosixFilePermissions.fromString("rwx--x--x");
            Files.createDirectories(miscPath, PosixFilePermissions.asFileAttribute(perms));
            walkFileTree(miscPath, f -> SELinux.setFileContext(f.toString(), "u:object_r:magisk_file:s0"));
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }

        updateManager(false);

        cacheHandler.post(this::getPreloadDex);
    }

    public synchronized void updateManager(boolean uninstalled) {
        if (uninstalled) {
            managerUid = -1;
            return;
        }
        if (!PackageService.isAlive()) return;
        try {
            PackageInfo info = PackageService.getPackageInfo(BuildConfig.DEFAULT_MANAGER_PACKAGE_NAME, 0, 0);
            if (info != null) {
                managerUid = info.applicationInfo.uid;
            } else {
                managerUid = -1;
                Log.i(TAG, "manager is not installed");
            }
        } catch (RemoteException ignored) {
        }
    }

    static ConfigManager getInstance() {
        if (instance == null)
            instance = new ConfigManager();
        boolean needCached;
        synchronized (instance.cacheHandler) {
            needCached = instance.lastModuleCacheTime == 0 || instance.lastScopeCacheTime == 0;
        }
        if (needCached) {
            if (PackageService.isAlive() && UserService.isAlive()) {
                Log.d(TAG, "pm & um are ready, updating cache");
                // must ensure cache is valid for later usage
                instance.updateCaches(true);
                instance.updateManager(false);
            }
        }
        return instance;
    }

    private ConfigManager() {
        HandlerThread cacheThread = new HandlerThread("cache");
        cacheThread.start();
        cacheHandler = new Handler(cacheThread.getLooper());

        initDB();
        updateConfig();
        // must ensure cache is valid for later usage
        updateCaches(true);
    }


    private <T> T executeInTransaction(Supplier<T> execution) {
        try {
            db.beginTransaction();
            var res = execution.get();
            db.setTransactionSuccessful();
            return res;
        } finally {
            db.endTransaction();
        }
    }

    private void executeInTransaction(Runnable execution) {
        executeInTransaction((Supplier<Void>) () -> {
            execution.run();
            return null;
        });
    }

    private void initDB() {
        try {
            db.setForeignKeyConstraintsEnabled(true);
            switch (db.getVersion()) {
                case 0:
                    executeInTransaction(() -> {
                        createModulesTable.execute();
                        createScopeTable.execute();
                        createConfigTable.execute();
                        var values = new ContentValues();
                        values.put("module_pkg_name", "lspd");
                        values.put("apk_path", ConfigFileManager.managerApkPath.toString());
                        // dummy module for config
                        db.insertWithOnConflict("modules", null, values, SQLiteDatabase.CONFLICT_IGNORE);
                        db.setVersion(1);
                    });
                case 1:
                    executeInTransaction(() -> {
                        db.compileStatement("DROP INDEX IF EXISTS configs_idx;").execute();
                        db.compileStatement("DROP TABLE IF EXISTS config;").execute();
                        db.compileStatement("ALTER TABLE scope RENAME TO old_scope;").execute();
                        db.compileStatement("ALTER TABLE configs RENAME TO old_configs;").execute();
                        createConfigTable.execute();
                        createScopeTable.execute();
                        db.compileStatement("CREATE INDEX IF NOT EXISTS configs_idx ON configs (module_pkg_name, user_id);").execute();
                        executeInTransaction(() -> {
                            try {
                                db.compileStatement("INSERT INTO scope SELECT * FROM old_scope;").execute();
                            } catch (Throwable e) {
                                Log.w(TAG, "migrate scope", e);
                            }
                        });
                        executeInTransaction(() -> {
                            try {
                                executeInTransaction(() -> db.compileStatement("INSERT INTO configs SELECT * FROM old_configs;").execute());
                            } catch (Throwable e) {
                                Log.w(TAG, "migrate config", e);
                            }
                        });
                        db.compileStatement("DROP TABLE old_scope;").execute();
                        db.compileStatement("DROP TABLE old_configs;").execute();
                        db.setVersion(2);
                    });
                case 2:
                    executeInTransaction(() -> {
                        db.compileStatement("UPDATE scope SET app_pkg_name = 'system' WHERE app_pkg_name = 'android';").execute();
                        db.setVersion(3);
                    });
                default:
                    break;
            }
        } catch (Throwable e) {
            Log.e(TAG, "init db", e);
        }

    }

    private List<ProcessScope> getAssociatedProcesses(Application app) throws RemoteException {
        Pair<Set<String>, Integer> result = PackageService.fetchProcessesWithUid(app);
        List<ProcessScope> processes = new ArrayList<>();
        if (app.packageName.equals("android")) {
            // this is hardcoded for ResolverActivity
            processes.add(new ProcessScope("system:ui", Process.SYSTEM_UID));
        }
        for (String processName : result.first) {
            var uid = result.second;
            if (uid == Process.SYSTEM_UID && processName.equals("system")) {
                // code run in system_server
                continue;
            }
            processes.add(new ProcessScope(processName, uid));
        }
        return processes;
    }

    private @NonNull
    Map<String, HashMap<String, Object>> fetchModuleConfig(String name, int user_id) {
        var config = new ConcurrentHashMap<String, HashMap<String, Object>>();

        try (Cursor cursor = db.query("configs", new String[]{"`group`", "`key`", "data"},
                "module_pkg_name = ? and user_id = ?", new String[]{name, String.valueOf(user_id)}, null, null, null)) {
            if (cursor == null) {
                Log.e(TAG, "db cache failed");
                return config;
            }
            int groupIdx = cursor.getColumnIndex("group");
            int keyIdx = cursor.getColumnIndex("key");
            int dataIdx = cursor.getColumnIndex("data");
            while (cursor.moveToNext()) {
                var group = cursor.getString(groupIdx);
                var key = cursor.getString(keyIdx);
                var data = cursor.getBlob(dataIdx);
                var object = SerializationUtils.deserialize(data);
                if (object == null) continue;
                config.computeIfAbsent(group, g -> new HashMap<>()).put(key, object);
            }
        }
        return config;
    }

    public void updateModulePrefs(String moduleName, int userId, String group, String key, Object value) {
        Map<String, Object> values = new HashMap<>();
        values.put(key, value);
        updateModulePrefs(moduleName, userId, group, values);
    }

    public void updateModulePrefs(String moduleName, int userId, String group, Map<String, Object> values) {
        var config = cachedConfig.computeIfAbsent(new Pair<>(moduleName, userId), module -> fetchModuleConfig(module.first, module.second));
        config.compute(group, (g, prefs) -> {
            HashMap<String, Object> newPrefs = prefs == null ? new HashMap<>() : new HashMap<>(prefs);
            executeInTransaction(() -> {
                for (var entry : values.entrySet()) {
                    var key = entry.getKey();
                    var value = entry.getValue();
                    if (value instanceof Serializable) {
                        newPrefs.put(key, value);
                        var contents = new ContentValues();
                        contents.put("`group`", group);
                        contents.put("`key`", key);
                        contents.put("data", SerializationUtils.serialize((Serializable) value));
                        contents.put("module_pkg_name", moduleName);
                        contents.put("user_id", String.valueOf(userId));
                        db.insertWithOnConflict("configs", null, contents, SQLiteDatabase.CONFLICT_REPLACE);
                    } else {
                        newPrefs.remove(key);
                        db.delete("configs", "module_pkg_name=? and user_id=? and `group`=? and `key`=?", new String[]{moduleName, String.valueOf(userId), group, key});
                    }
                }
                var bundle = new Bundle();
                bundle.putSerializable("config", (Serializable) config);
                if (bundle.size() > 1024 * 1024) {
                    throw new IllegalArgumentException("Preference too large");
                }
            });
            return newPrefs;
        });
    }

    public void deleteModulePrefs(String moduleName, int userId, String group) {
        db.delete("configs", "module_pkg_name=? and user_id=? and `group`=?", new String[]{moduleName, String.valueOf(userId), group});
        var config = cachedConfig.getOrDefault(new Pair<>(moduleName, userId), null);
        if (config != null) {
            config.remove(group);
        }
    }

    public HashMap<String, Object> getModulePrefs(String moduleName, int userId, String group) {
        var config = cachedConfig.computeIfAbsent(new Pair<>(moduleName, userId), module -> fetchModuleConfig(module.first, module.second));
        return config.getOrDefault(group, new HashMap<>());
    }

    private synchronized void clearCache() {
        synchronized (cacheHandler) {
            lastScopeCacheTime = 0;
            lastModuleCacheTime = 0;
        }
        cachedModule.clear();
        cachedScope.clear();
    }

    private synchronized void cacheModules() {
        // skip caching when pm is not yet available
        if (!PackageService.isAlive() || !UserService.isAlive()) return;
        synchronized (cacheHandler) {
            if (lastModuleCacheTime >= requestModuleCacheTime) return;
            else lastModuleCacheTime = SystemClock.elapsedRealtime();
        }
        Set<SharedMemory> toClose = ConcurrentHashMap.newKeySet();
        try (Cursor cursor = db.query(true, "modules", new String[]{"module_pkg_name", "apk_path"},
                "enabled = 1", null, null, null, null, null)) {
            if (cursor == null) {
                Log.e(TAG, "db cache failed");
                return;
            }
            int pkgNameIdx = cursor.getColumnIndex("module_pkg_name");
            int apkPathIdx = cursor.getColumnIndex("apk_path");
            Set<String> obsoleteModules = ConcurrentHashMap.newKeySet();
            // packageName, apkPath
            Map<String, String> obsoletePaths = new ConcurrentHashMap<>();
            cachedModule.values().removeIf(m -> {
                if (m.apkPath == null || !existsInGlobalNamespace(m.apkPath)) {
                    toClose.addAll(m.file.preLoadedDexes);
                    return true;
                }
                return false;
            });
            List<Module> modules = new ArrayList<>();
            while (cursor.moveToNext()) {
                String packageName = cursor.getString(pkgNameIdx);
                String apkPath = cursor.getString(apkPathIdx);
                if (packageName.equals("lspd")) continue;
                var module = new Module();
                module.packageName = packageName;
                module.apkPath = apkPath;
                modules.add(module);
            }

            modules.stream().parallel().filter(m -> {
                var oldModule = cachedModule.get(m.packageName);
                PackageInfo pkgInfo = null;
                try {
                    pkgInfo = PackageService.getPackageInfoFromAllUsers(m.packageName, MATCH_ALL_FLAGS).values().stream().findFirst().orElse(null);
                } catch (Throwable e) {
                    Log.w(TAG, "Get package info of " + m.packageName, e);
                }
                if (pkgInfo == null || pkgInfo.applicationInfo == null) {
                    Log.w(TAG, "Failed to find package info of " + m.packageName);
                    obsoleteModules.add(m.packageName);
                    return false;
                }

                if (oldModule != null &&
                        pkgInfo.applicationInfo.sourceDir != null &&
                        m.apkPath != null && oldModule.apkPath != null &&
                        existsInGlobalNamespace(m.apkPath) &&
                        Objects.equals(m.apkPath, oldModule.apkPath) &&
                        Objects.equals(new File(pkgInfo.applicationInfo.sourceDir).getParent(), new File(m.apkPath).getParent())) {
                    if (oldModule.appId != -1) {
                        Log.d(TAG, m.packageName + " did not change, skip caching it");
                    } else {
                        // cache from system server, update application info
                        oldModule.applicationInfo = pkgInfo.applicationInfo;
                    }
                    return false;
                }
                m.apkPath = getModuleApkPath(pkgInfo.applicationInfo);
                if (m.apkPath == null) {
                    Log.w(TAG, "Failed to find path of " + m.packageName);
                    obsoleteModules.add(m.packageName);
                    return false;
                } else {
                    obsoletePaths.put(m.packageName, m.apkPath);
                }
                m.appId = pkgInfo.applicationInfo.uid;
                m.applicationInfo = pkgInfo.applicationInfo;
                m.service = oldModule != null ? oldModule.service : new LSPInjectedModuleService(m.packageName);
                return true;
            }).forEach(m -> {
                var file = ConfigFileManager.loadModule(m.apkPath, dexObfuscate);
                if (file == null) {
                    Log.w(TAG, "failed to load module " + m.packageName);
                    obsoleteModules.add(m.packageName);
                    return;
                }
                m.file = file;
                cachedModule.put(m.packageName, m);
            });

            if (PackageService.isAlive()) {
                obsoleteModules.forEach(this::removeModuleWithoutCache);
                obsoletePaths.forEach((packageName, path) -> updateModuleApkPath(packageName, path, true));
            } else {
                Log.w(TAG, "pm is dead while caching. invalidating...");
                clearCache();
                return;
            }
        }
        Log.d(TAG, "cached modules");
        for (var module : cachedModule.entrySet()) {
            Log.d(TAG, module.getKey() + " " + module.getValue().apkPath);
        }
        cacheScopes();
        toClose.forEach(SharedMemory::close);
    }

    private synchronized void cacheScopes() {
        // skip caching when pm is not yet available
        if (!PackageService.isAlive()) return;
        synchronized (cacheHandler) {
            if (lastScopeCacheTime >= requestScopeCacheTime) return;
            else lastScopeCacheTime = SystemClock.elapsedRealtime();
        }
        cachedScope.clear();
        try (Cursor cursor = db.query("scope INNER JOIN modules ON scope.mid = modules.mid", new String[]{"app_pkg_name", "module_pkg_name", "user_id"},
                "enabled = 1", null, null, null, null)) {
            int appPkgNameIdx = cursor.getColumnIndex("app_pkg_name");
            int modulePkgNameIdx = cursor.getColumnIndex("module_pkg_name");
            int userIdIdx = cursor.getColumnIndex("user_id");

            final var obsoletePackages = new HashSet<Application>();
            final var obsoleteModules = new HashSet<Application>();
            final var moduleAvailability = new HashMap<Pair<String, Integer>, Boolean>();
            final var cachedProcessScope = new HashMap<Pair<String, Integer>, List<ProcessScope>>();

            final var denylist = new HashSet<>(getDenyListPackages());
            while (cursor.moveToNext()) {
                Application app = new Application();
                app.packageName = cursor.getString(appPkgNameIdx);
                app.userId = cursor.getInt(userIdIdx);
                var modulePackageName = cursor.getString(modulePkgNameIdx);

                // check if module is present in this user
                if (!moduleAvailability.computeIfAbsent(new Pair<>(modulePackageName, app.userId), n -> {
                    var available = false;
                    try {
                        available = PackageService.isPackageAvailable(n.first, n.second, true) && cachedModule.containsKey(modulePackageName);
                    } catch (Throwable e) {
                        Log.w(TAG, "check package availability ", e);
                    }
                    if (!available) {
                        var obsoleteModule = new Application();
                        obsoleteModule.packageName = modulePackageName;
                        obsoleteModule.userId = app.userId;
                        obsoleteModules.add(obsoleteModule);
                    }
                    return available;
                })) continue;

                // system server always loads database
                if (app.packageName.equals("system")) continue;

                try {
                    List<ProcessScope> processesScope = cachedProcessScope.computeIfAbsent(new Pair<>(app.packageName, app.userId), (k) -> {
                        try {
                            if (denylist.contains(app.packageName))
                                Log.w(TAG, app.packageName + " is on denylist. It may not take effect.");
                            return getAssociatedProcesses(app);
                        } catch (RemoteException e) {
                            return Collections.emptyList();
                        }
                    });
                    if (processesScope.isEmpty()) {
                        obsoletePackages.add(app);
                        continue;
                    }
                    var module = cachedModule.get(modulePackageName);
                    assert module != null;
                    for (ProcessScope processScope : processesScope) {
                        cachedScope.computeIfAbsent(processScope,
                                ignored -> new LinkedList<>()).add(module);
                        // Always allow the module to inject itself
                        if (modulePackageName.equals(app.packageName)) {
                            var appId = processScope.uid % PER_USER_RANGE;
                            for (var user : UserService.getUsers()) {
                                var moduleUid = user.id * PER_USER_RANGE + appId;
                                if (moduleUid == processScope.uid) continue; // skip duplicate
                                var moduleSelf = new ProcessScope(processScope.processName, moduleUid);
                                cachedScope.computeIfAbsent(moduleSelf,
                                        ignored -> new LinkedList<>()).add(module);
                            }
                        }
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                }
            }
            if (PackageService.isAlive()) {
                for (Application obsoletePackage : obsoletePackages) {
                    Log.d(TAG, "removing obsolete package: " + obsoletePackage.packageName + "/" + obsoletePackage.userId);
                    removeAppWithoutCache(obsoletePackage);
                }
                for (Application obsoleteModule : obsoleteModules) {
                    Log.d(TAG, "removing obsolete module: " + obsoleteModule.packageName + "/" + obsoleteModule.userId);
                    removeModuleScopeWithoutCache(obsoleteModule);
                    removeBlockedScopeRequest(obsoleteModule.packageName);
                }
            } else {
                Log.w(TAG, "pm is dead while caching. invalidating...");
                clearCache();
                return;
            }
        }
        Log.d(TAG, "cached scope");
        cachedScope.forEach((ps, modules) -> {
            Log.d(TAG, ps.processName + "/" + ps.uid);
            modules.forEach(module -> Log.d(TAG, "\t" + module.packageName));
        });
    }

    // This is called when a new process created, use the cached result
    public List<Module> getModulesForProcess(String processName, int uid) {
        return isManager(uid) ? Collections.emptyList() : cachedScope.getOrDefault(new ProcessScope(processName, uid), Collections.emptyList());
    }

    // This is called when a new process created, use the cached result
    public boolean shouldSkipProcess(ProcessScope scope) {
        return !cachedScope.containsKey(scope) && !isManager(scope.uid);
    }

    public boolean isUidHooked(int uid) {
        return cachedScope.keySet().stream().reduce(false, (p, scope) -> p || scope.uid == uid, Boolean::logicalOr);
    }

    @Nullable
    public List<Application> getModuleScope(String packageName) {
        if (packageName.equals("lspd")) return null;
        try (Cursor cursor = db.query("scope INNER JOIN modules ON scope.mid = modules.mid", new String[]{"app_pkg_name", "user_id"},
                "modules.module_pkg_name = ?", new String[]{packageName}, null, null, null)) {
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

    @Nullable
    public String getModuleApkPath(ApplicationInfo info) {
        String[] apks;
        if (info.splitSourceDirs != null) {
            apks = Arrays.copyOf(info.splitSourceDirs, info.splitSourceDirs.length + 1);
            apks[info.splitSourceDirs.length] = info.sourceDir;
        } else apks = new String[]{info.sourceDir};
        var apkPath = Arrays.stream(apks).parallel().filter(apk -> {
            if (apk == null) {
                Log.w(TAG, info.packageName + " has null apk path???");
                return false;
            }
            try (var zip = new ZipFile(toGlobalNamespace(apk))) {
                return zip.getEntry("META-INF/xposed/java_init.list") != null || zip.getEntry("assets/xposed_init") != null;
            } catch (IOException e) {
                return false;
            }
        }).findFirst();
        return apkPath.orElse(null);
    }

    public boolean updateModuleApkPath(String packageName, String apkPath, boolean force) {
        if (apkPath == null || packageName.equals("lspd")) return false;
        if (db.inTransaction()) {
            Log.w(TAG, "update module apk path should not be called inside transaction");
            return false;
        }

        ContentValues values = new ContentValues();
        values.put("module_pkg_name", packageName);
        values.put("apk_path", apkPath);
        // insert or update in two step since insert or replace will change the autoincrement mid
        int count = (int) db.insertWithOnConflict("modules", null, values, SQLiteDatabase.CONFLICT_IGNORE);
        if (count < 0) {
            var cached = cachedModule.getOrDefault(packageName, null);
            if (force || cached == null || cached.apkPath == null || !cached.apkPath.equals(apkPath))
                count = db.updateWithOnConflict("modules", values, "module_pkg_name=?", new String[]{packageName}, SQLiteDatabase.CONFLICT_IGNORE);
            else
                count = 0;
        }
        // force update is because cache is already update to date
        // skip caching again
        if (!force && count > 0) {
            // Called by oneway binder
            updateCaches(true);
            return true;
        }
        return count > 0;
    }

    // Only be called before updating modules. No need to cache.
    private int getModuleId(String packageName) {
        if (packageName.equals("lspd")) return -1;
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

    public boolean setModuleScope(String packageName, List<Application> scopes) throws RemoteException {
        if (scopes == null) return false;
        enableModule(packageName);
        int mid = getModuleId(packageName);
        if (mid == -1) return false;
        executeInTransaction(() -> {
            db.delete("scope", "mid = ?", new String[]{String.valueOf(mid)});
            for (Application app : scopes) {
                if (app.packageName.equals("system") && app.userId != 0) continue;
                ContentValues values = new ContentValues();
                values.put("mid", mid);
                values.put("app_pkg_name", app.packageName);
                values.put("user_id", app.userId);
                db.insertWithOnConflict("scope", null, values, SQLiteDatabase.CONFLICT_IGNORE);
            }
        });
        // Called by manager, should be async
        updateCaches(false);
        return true;
    }

    public boolean setModuleScope(String packageName, String scopePackageName, int userId) {
        if (scopePackageName == null) return false;
        int mid = getModuleId(packageName);
        if (mid == -1) return false;
        if (scopePackageName.equals("system") && userId != 0) return false;
        executeInTransaction(() -> {
            ContentValues values = new ContentValues();
            values.put("mid", mid);
            values.put("app_pkg_name", scopePackageName);
            values.put("user_id", userId);
            db.insertWithOnConflict("scope", null, values, SQLiteDatabase.CONFLICT_IGNORE);
        });
        // Called by xposed service, should be async
        updateCaches(false);
        return true;
    }

    public boolean removeModuleScope(String packageName, String scopePackageName, int userId) {
        if (scopePackageName == null) return false;
        int mid = getModuleId(packageName);
        if (mid == -1) return false;
        if (scopePackageName.equals("system") && userId != 0) return false;
        executeInTransaction(() -> {
            db.delete("scope", "mid = ? AND app_pkg_name = ? AND user_id = ?", new String[]{String.valueOf(mid), scopePackageName, String.valueOf(userId)});
        });
        // Called by xposed service, should be async
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
                var pkgName = cursor.getString(modulePkgNameIdx);
                if (pkgName.equals("lspd")) continue;
                result.add(pkgName);
            }
            return result.toArray(new String[0]);
        }
    }

    public boolean removeModule(String packageName) {
        if (removeModuleWithoutCache(packageName)) {
            // called by oneway binder
            // Called only when the application is completely uninstalled
            // If it's a module we need to return as soon as possible to broadcast to the manager
            // for updating the module status
            updateCaches(false);
            return true;
        }
        return false;
    }

    private boolean removeModuleWithoutCache(String packageName) {
        if (packageName.equals("lspd")) return false;
        boolean res = executeInTransaction(() -> db.delete("modules", "module_pkg_name = ?", new String[]{packageName}) > 0);
        try {
            for (var user : UserService.getUsers()) {
                removeModulePrefs(user.id, packageName);
            }
        } catch (Throwable e) {
            Log.w(TAG, "remove module prefs for " + packageName);
        }
        return res;
    }

    private boolean removeModuleScopeWithoutCache(Application module) {
        if (module.packageName.equals("lspd")) return false;
        int mid = getModuleId(module.packageName);
        if (mid == -1) return false;
        boolean res = executeInTransaction(() -> db.delete("scope", "mid = ? and user_id = ?", new String[]{String.valueOf(mid), String.valueOf(module.userId)}) > 0);
        try {
            removeModulePrefs(module.userId, module.packageName);
        } catch (IOException e) {
            Log.w(TAG, "removeModulePrefs", e);
        }
        return res;
    }

    private boolean removeAppWithoutCache(Application app) {
        return executeInTransaction(() -> db.delete("scope", "app_pkg_name = ? AND user_id=?",
                new String[]{app.packageName, String.valueOf(app.userId)}) > 0);
    }

    public boolean disableModule(String packageName) {
        if (packageName.equals("lspd")) return false;
        boolean changed = executeInTransaction(() -> {
            ContentValues values = new ContentValues();
            values.put("enabled", 0);
            return db.update("modules", values, "module_pkg_name = ?", new String[]{packageName}) > 0;
        });
        if (changed) {
            // called by manager, should be async
            updateCaches(false);
            return true;
        } else {
            return false;
        }
    }

    public boolean enableModule(String packageName) throws RemoteException {
        if (packageName.equals("lspd")) return false;
        PackageInfo pkgInfo = PackageService.getPackageInfoFromAllUsers(packageName, PackageService.MATCH_ALL_FLAGS).values().stream().findFirst().orElse(null);
        if (pkgInfo == null || pkgInfo.applicationInfo == null) return false;
        var modulePath = getModuleApkPath(pkgInfo.applicationInfo);
        if (modulePath == null) return false;
        boolean changed = updateModuleApkPath(packageName, modulePath, false);
        changed = executeInTransaction(() -> {
            ContentValues values = new ContentValues();
            values.put("enabled", 1);
            return db.update("modules", values, "module_pkg_name = ?", new String[]{packageName}) > 0;
        }) || changed;
        if (changed) {
            // Called by manager, should be async
            updateCaches(false);
            return true;
        } else {
            return false;
        }
    }

    public void updateCache() {
        // Called by oneway binder
        updateCaches(true);
    }

    public void updateAppCache() {
        // Called by oneway binder
        cacheScopes();
    }

    public void setVerboseLog(boolean on) {
        if (BuildConfig.DEBUG) return;
        var logcatService = ServiceManager.getLogcatService();
        if (on) {
            logcatService.startVerbose();
        } else {
            logcatService.stopVerbose();
        }
        updateModulePrefs("lspd", 0, "config", "enable_verbose_log", on);
        verboseLog = on;
    }

    public boolean verboseLog() {
        return BuildConfig.DEBUG || verboseLog;
    }

    public void setDexObfuscate(boolean on) {
        updateModulePrefs("lspd", 0, "config", "enable_dex_obfuscate", on);
    }

    public boolean scopeRequestBlocked(String packageName) {
        return scopeRequestBlocked.contains(packageName);
    }

    public void blockScopeRequest(String packageName) {
        var set = new HashSet<>(scopeRequestBlocked);
        set.add(packageName);
        updateModulePrefs("lspd", 0, "config", "scope_request_blocked", set);
        scopeRequestBlocked = set;
    }

    public void removeBlockedScopeRequest(String packageName) {
        var set = new HashSet<>(scopeRequestBlocked);
        set.remove(packageName);
        updateModulePrefs("lspd", 0, "config", "scope_request_blocked", set);
        scopeRequestBlocked = set;
    }

    // this is for manager and should not use the cache result
    boolean dexObfuscate() {
        var bool = getModulePrefs("lspd", 0, "config").get("enable_dex_obfuscate");
        return bool == null || (boolean) bool;
    }

    public boolean enableStatusNotification() {
        Log.d(TAG, "show status notification = " + enableStatusNotification);
        return enableStatusNotification;
    }

    public void setEnableStatusNotification(boolean enable) {
        updateModulePrefs("lspd", 0, "config", "enable_status_notification", enable);
        enableStatusNotification = enable;
    }

    public ParcelFileDescriptor getManagerApk() {
        try {
            return ConfigFileManager.getManagerApk();
        } catch (Throwable e) {
            Log.e(TAG, "failed to open manager apk", e);
            return null;
        }
    }

    public ParcelFileDescriptor getModulesLog() {
        try {
            var modulesLog = ServiceManager.getLogcatService().getModulesLog();
            if (modulesLog == null) return null;
            return ParcelFileDescriptor.open(modulesLog, ParcelFileDescriptor.MODE_READ_ONLY);
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return null;
        }
    }

    public ParcelFileDescriptor getVerboseLog() {
        try {
            var verboseLog = ServiceManager.getLogcatService().getVerboseLog();
            if (verboseLog == null) return null;
            return ParcelFileDescriptor.open(verboseLog, ParcelFileDescriptor.MODE_READ_ONLY);
        } catch (FileNotFoundException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return null;
        }
    }

    public boolean clearLogs(boolean verbose) {
        ServiceManager.getLogcatService().refresh(verbose);
        return true;
    }

    public boolean isManager(int uid) {
        return uid == managerUid;
    }

    public boolean isManagerInstalled() {
        return managerUid != -1;
    }

    public String getPrefsPath(String packageName, int uid) {
        int userId = uid / PER_USER_RANGE;
        var path = miscPath.resolve("prefs" + (userId == 0 ? "" : String.valueOf(userId))).resolve(packageName);
        var module = cachedModule.getOrDefault(packageName, null);
        if (module != null && module.appId == uid % PER_USER_RANGE) {
            try {
                var perms = PosixFilePermissions.fromString("rwx--x--x");
                Files.createDirectories(path, PosixFilePermissions.asFileAttribute(perms));
                walkFileTree(path, p -> {
                    try {
                        Os.chown(p.toString(), uid, uid);
                    } catch (ErrnoException e) {
                        Log.e(TAG, Log.getStackTraceString(e));
                    }
                });
            } catch (IOException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }
        return path.toString();
    }

    // this is slow, avoid using it
    public Module getModule(int uid) {
        for (var module : cachedModule.values()) {
            if (module.appId == uid % PER_USER_RANGE) return module;
        }
        return null;
    }

    private void walkFileTree(Path rootDir, Consumer<Path> action) throws IOException {
        if (Files.notExists(rootDir)) return;
        Files.walkFileTree(rootDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                action.accept(dir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                action.accept(file);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void removeModulePrefs(int uid, String packageName) throws IOException {
        if (packageName == null) return;
        var path = Paths.get(getPrefsPath(packageName, uid));
        ConfigFileManager.deleteFolderIfExists(path);
    }

    public List<String> getDenyListPackages() {
        List<String> result = new ArrayList<>();
        if (!getApi().equals("Zygisk")) return result;
        if (!ConfigFileManager.magiskDbPath.exists()) return result;
        try (final SQLiteDatabase magiskDb =
                     SQLiteDatabase.openDatabase(ConfigFileManager.magiskDbPath, new SQLiteDatabase.OpenParams.Builder().addOpenFlags(SQLiteDatabase.OPEN_READONLY).build())) {
            try (Cursor cursor = magiskDb.query("settings", new String[]{"value"}, "`key`=?", new String[]{"denylist"}, null, null, null)) {
                if (!cursor.moveToNext()) return result;
                int valueIndex = cursor.getColumnIndex("value");
                if (valueIndex >= 0 && cursor.getInt(valueIndex) == 0) return result;
            }
            try (Cursor cursor = magiskDb.query(true, "denylist", new String[]{"package_name"}, null, null, null, null, null, null, null)) {
                if (cursor == null) return result;
                int packageNameIdx = cursor.getColumnIndex("package_name");
                while (cursor.moveToNext()) {
                    result.add(cursor.getString(packageNameIdx));
                }
                return result;
            }
        } catch (Throwable e) {
            Log.e(TAG, "get denylist", e);
        }
        return result;
    }

    public void setApi(String api) {
        this.api = api;
    }

    public String getApi() {
        return api;
    }

    public void exportScopes(ZipOutputStream os) throws IOException {
        os.putNextEntry(new ZipEntry("scopes.txt"));
        cachedScope.forEach((scope, modules) -> {
            try {
                os.write((scope.processName + "/" + scope.uid + "\n").getBytes(StandardCharsets.UTF_8));
                for (var module : modules) {
                    os.write(("\t" + module.packageName + "\n").getBytes(StandardCharsets.UTF_8));
                    for (var cn : module.file.moduleClassNames) {
                        os.write(("\t\t" + cn + "\n").getBytes(StandardCharsets.UTF_8));
                    }
                    for (var ln : module.file.moduleLibraryNames) {
                        os.write(("\t\t" + ln + "\n").getBytes(StandardCharsets.UTF_8));
                    }
                }
            } catch (IOException e) {
                Log.w(TAG, scope.processName, e);
            }
        });
        os.closeEntry();
    }

    synchronized SharedMemory getPreloadDex() {
        return ConfigFileManager.getPreloadDex(dexObfuscate);
    }
}
