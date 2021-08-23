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

import android.content.ContentValues;
import android.content.pm.ApplicationInfo;
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
import android.system.OsConstants;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.SerializationUtils;
import org.lsposed.lspd.BuildConfig;
import org.lsposed.lspd.models.Application;
import org.lsposed.lspd.models.Module;
import org.lsposed.lspd.models.PreLoadedApk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Paths;
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
import java.util.zip.ZipFile;

public class ConfigManager {
    private static ConfigManager instance = null;

    private final SQLiteDatabase db =
            SQLiteDatabase.openOrCreateDatabase(ConfigFileManager.dbPath, null);

    private boolean packageStarted = false;

    private boolean resourceHook = false;
    private boolean verboseLog = true;
    private String miscPath = null;

    private final String manager = BuildConfig.DEFAULT_MANAGER_PACKAGE_NAME;
    private int managerUid = -1;

    private final Handler cacheHandler;

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
            "PRIMARY KEY (mid, app_pkg_name, user_id)" +
            ");");
    private final SQLiteStatement createConfigTable = db.compileStatement("CREATE TABLE IF NOT EXISTS configs (" +
            "module_pkg_name text NOT NULL," +
            "user_id integer NOT NULL," +
            "`group` text NOT NULL," +
            "`key` text NOT NULL," +
            "data blob NOT NULL," +
            "PRIMARY KEY (module_pkg_name, user_id, `group`, `key`)" +
            ");");
    private final SQLiteStatement createConfigIndex = db.compileStatement("CREATE INDEX IF NOT EXISTS configs_idx ON configs (module_pkg_name, user_id);")

    private final Map<ProcessScope, List<Module>> cachedScope = new ConcurrentHashMap<>();

    // packageName, Module
    private final Map<String, Module> cachedModule = new ConcurrentHashMap<>();

    // packageName, userId, group, key, value
    private final Map<Pair<String, Integer>, Map<String, ConcurrentHashMap<String, Object>>> cachedConfig = new ConcurrentHashMap<>();

    private void updateCaches(boolean sync) {
        synchronized (this) {
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
            sepolicyLoaded = false;
            Log.e(TAG, "skip injecting into android because sepolicy was not loaded properly");
            return true; // skip
        }
        try (Cursor cursor = db.query("scope INNER JOIN modules ON scope.mid = modules.mid", new String[]{"modules.mid"}, "app_pkg_name=? AND enabled=1", new String[]{"android"}, null, null, null)) {
            return cursor == null || !cursor.moveToNext();
        }
    }

    public List<Module> getModulesForSystemServer() {
        List<Module> modules = new LinkedList<>();
        try (Cursor cursor = db.query("scope INNER JOIN modules ON scope.mid = modules.mid", new String[]{"module_pkg_name", "apk_path"}, "app_pkg_name=? AND enabled=1", new String[]{"android"}, null, null, null)) {
            int apkPathIdx = cursor.getColumnIndex("apk_path");
            int pkgNameIdx = cursor.getColumnIndex("module_pkg_name");
            while (cursor.moveToNext()) {
                var path = cursor.getString(apkPathIdx);
                var packageName = cursor.getString(pkgNameIdx);
                var m = cachedModule.computeIfAbsent(packageName, p -> {
                    var module = new Module();
                    var file = loadModule(path);
                    if (file == null) {
                        Log.w(TAG, "Can not load " + path + ", skip!");
                        return null;
                    }
                    module.packageName = cursor.getString(pkgNameIdx);
                    module.apkPath = path;
                    module.file = file;
                    module.appId = -1;
                    return module;
                });
                if (m != null) modules.add(m);
            }
        }
        return modules;
    }

    private synchronized void updateConfig() {
        ConfigFileManager.migrateOldConfig(this);
        Map<String, Object> config = getModulePrefs("lspd", 0, "config");

        Object bool = config.get("enable_resources");
        resourceHook = bool != null && (boolean) bool;

        bool = config.get("enable_verbose_log");
        verboseLog = bool == null || (boolean) bool;

        // Don't migrate to ConfigFileManager, as XSharedPreferences will be restored soon
        String string = (String) config.get("misc_path");
        if (string == null) {
            miscPath = "/data/misc/" + UUID.randomUUID().toString();
            updateModulePrefs("lspd", 0, "config", "misc_path", miscPath);
        } else {
            miscPath = string;
        }
        try {
            Files.createDirectories(Paths.get(miscPath));
            recursivelyContext(new File(miscPath), "u:object_r:magisk_file:s0");
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }

        updateManager();
    }

    public synchronized void updateManager() {
        if (!packageStarted) return;
        try {
            PackageInfo info = PackageService.getPackageInfo(manager, 0, 0);
            if (info != null) {
                managerUid = info.applicationInfo.uid;
            } else {
                Log.w(TAG, "manager is not installed");
            }
        } catch (RemoteException ignored) {
        }
    }

    public void ensureManager() {
        if (!packageStarted) return;
        new Thread(() -> {
            if (PackageService.installManagerIfAbsent(manager, ConfigFileManager.managerApkPath)) {
                updateManager();
            }
        }).start();
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
        createConfigTable.execute();
        createConfigIndex.execute();
    }

    private List<ProcessScope> getAssociatedProcesses(Application app) throws RemoteException {
        Pair<Set<String>, Integer> result = PackageService.fetchProcessesWithUid(app);
        List<ProcessScope> processes = new ArrayList<>();
        for (String processName : result.first) {
            processes.add(new ProcessScope(processName, result.second));
        }
        return processes;
    }

    private @NonNull
    Map<String, ConcurrentHashMap<String, Object>> fetchModuleConfig(String name, int user_id) {
        var config = new ConcurrentHashMap<String, ConcurrentHashMap<String, Object>>();

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
                config.computeIfAbsent(group, g -> new ConcurrentHashMap<>()).put(key, object);
            }
        }
        return config;
    }

    public void updateModulePrefs(String moduleName, int userId, String group, String key, Object value) {
        var config = cachedConfig.computeIfAbsent(new Pair<>(moduleName, userId), module -> fetchModuleConfig(module.first, module.second));
        var prefs = config.computeIfAbsent(group, g -> new ConcurrentHashMap<>());
        if (value instanceof Serializable) {
            prefs.put(key, value);
            var values = new ContentValues();
            values.put("`group`", group);
            values.put("`key`", key);
            values.put("data", SerializationUtils.serialize((Serializable) value));
            values.put("module_pkg_name", moduleName);
            values.put("user_id", String.valueOf(userId));
            db.insertWithOnConflict("configs", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } else {
            prefs.remove(key);
            db.delete("configs", "module_pkg_name=? and user_id=? and `group`=? and `key`=?", new String[]{moduleName, String.valueOf(userId), group, key});
        }
    }

    public ConcurrentHashMap<String, Object> getModulePrefs(String moduleName, int userId, String group) {
        var config = cachedConfig.computeIfAbsent(new Pair<>(moduleName, userId), module -> fetchModuleConfig(module.first, module.second));
        return config.getOrDefault(group, new ConcurrentHashMap<>());
    }

    private synchronized void cacheModules() {
        // skip caching when pm is not yet available
        if (!packageStarted) return;
        if (lastModuleCacheTime >= requestModuleCacheTime) return;
        else lastModuleCacheTime = SystemClock.elapsedRealtime();
        try (Cursor cursor = db.query(true, "modules", new String[]{"module_pkg_name", "apk_path"},
                "enabled = 1", null, null, null, null, null)) {
            if (cursor == null) {
                Log.e(TAG, "db cache failed");
                return;
            }
            int pkgNameIdx = cursor.getColumnIndex("module_pkg_name");
            int apkPathIdx = cursor.getColumnIndex("apk_path");
            Set<String> obsoleteModules = new HashSet<>();
            // packageName, apkPath
            Map<String, String> obsoletePaths = new HashMap<>();
            cachedModule.values().removeIf(m -> m.apkPath == null || !existsInGlobalNamespace(m.apkPath));
            while (cursor.moveToNext()) {
                String packageName = cursor.getString(pkgNameIdx);
                String apkPath = cursor.getString(apkPathIdx);
                // if still present after removeIf, this package did not change.
                var oldModule = cachedModule.get(packageName);
                PackageInfo pkgInfo = null;
                try {
                    pkgInfo = PackageService.getPackageInfo(packageName, MATCH_ALL_FLAGS, 0);
                } catch (Throwable e) {
                    Log.w(TAG, "get package info of " + packageName, e);
                }
                if (pkgInfo == null || pkgInfo.applicationInfo == null) {
                    obsoleteModules.add(packageName);
                    continue;
                }
                if (oldModule != null &&
                        pkgInfo.applicationInfo.sourceDir != null &&
                        apkPath != null && oldModule.apkPath != null &&
                        existsInGlobalNamespace(apkPath) &&
                        Objects.equals(apkPath, oldModule.apkPath) &&
                        Objects.equals(new File(pkgInfo.applicationInfo.sourceDir).getParent(), new File(apkPath).getParent())) {
                    if (oldModule.appId != -1) {
                        Log.d(TAG, packageName + " did not change, skip caching it");
                    } else {
                        // cache from system server, keep it and set only the appId
                        oldModule.appId = pkgInfo.applicationInfo.uid;
                    }
                    continue;
                }
                apkPath = getModuleApkPath(pkgInfo.applicationInfo);
                if (apkPath == null) obsoleteModules.add(packageName);
                else obsoletePaths.put(packageName, apkPath);
                var file = loadModule(apkPath);
                if (file == null) {
                    Log.w(TAG, "failed to load module " + packageName);
                    obsoleteModules.add(packageName);
                    continue;
                }
                var module = new Module();
                module.apkPath = apkPath;
                module.packageName = packageName;
                module.file = file;
                module.appId = pkgInfo.applicationInfo.uid;
                cachedModule.put(packageName, module);
            }
            obsoleteModules.forEach(this::removeModuleWithoutCache);
            obsoletePaths.forEach(this::updateModuleApkPath);
        }
        Log.d(TAG, "cached modules");
        for (String module : cachedModule.keySet()) {
            Log.d(TAG, module);
        }
        cacheScopes();
    }

    private synchronized void cacheScopes() {
        // skip caching when pm is not yet available
        if (!packageStarted) return;
        if (lastScopeCacheTime >= requestScopeCacheTime) return;
        else lastScopeCacheTime = SystemClock.elapsedRealtime();
        cachedScope.clear();
        try (Cursor cursor = db.query("scope INNER JOIN modules ON scope.mid = modules.mid", new String[]{"app_pkg_name", "module_pkg_name", "user_id"},
                "enabled = 1", null, null, null, null)) {
            int appPkgNameIdx = cursor.getColumnIndex("app_pkg_name");
            int modulePkgNameIdx = cursor.getColumnIndex("module_pkg_name");
            int userIdIdx = cursor.getColumnIndex("user_id");

            final var obsoletePackages = new HashSet<Application>();
            final var obsoleteModules = new HashSet<Application>();
            final var moduleAvailability = new HashMap<Pair<String, Integer>, Boolean>();
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
                if (app.packageName.equals("android")) continue;

                try {
                    List<ProcessScope> processesScope = getAssociatedProcesses(app);
                    if (processesScope.isEmpty()) {
                        obsoletePackages.add(app);
                        continue;
                    }
                    var module = cachedModule.get(modulePackageName);
                    for (ProcessScope processScope : processesScope) {
                        cachedScope.computeIfAbsent(processScope,
                                ignored -> new LinkedList<>()).add(module);
                        // Always allow the module to inject itself
                        if (modulePackageName.equals(app.packageName)) {
                            var appId = processScope.uid % PER_USER_RANGE;
                            for (var user : UserService.getUsers()) {
                                var moduleUid = user.id * PER_USER_RANGE + appId;
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
            for (Application obsoletePackage : obsoletePackages) {
                Log.d(TAG, "removing obsolete package: " + obsoletePackage.packageName + "/" + obsoletePackage.userId);
                removeAppWithoutCache(obsoletePackage);
            }
            for (Application obsoleteModule : obsoleteModules) {
                Log.d(TAG, "removing obsolete module: " + obsoleteModule.packageName + "/" + obsoleteModule.userId);
                removeModuleScopeWithoutCache(obsoleteModule);
            }
        }
        Log.d(TAG, "cached Scope");
        cachedScope.forEach((ps, modules) -> {
            Log.d(TAG, ps.processName + "/" + ps.uid);
            modules.forEach(module -> Log.d(TAG, "\t" + module.packageName));
        });
    }

    private void readDexes(ZipFile apkFile, List<SharedMemory> preLoadedDexes) {
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

    private void readName(ZipFile apkFile, String initName, List<String> names) {
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
    private PreLoadedApk loadModule(String path) {
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

    // This is called when a new process created, use the cached result
    public List<Module> getModulesForProcess(String processName, int uid) {
        return isManager(uid) ? Collections.emptyList() : cachedScope.getOrDefault(new ProcessScope(processName, uid), Collections.emptyList());
    }

    // This is called when a new process created, use the cached result
    public boolean shouldSkipProcess(ProcessScope scope) {
        return !cachedScope.containsKey(scope) &&
                !isManager(scope.uid) &&
                !shouldBlock(scope.processName);
    }

    public boolean isUidHooked(int uid) {
        return cachedScope.keySet().stream().reduce(false, (p, scope) -> p || scope.uid == uid, Boolean::logicalOr);
    }

    @Nullable
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
                return zip.getEntry("assets/xposed_init") != null;
            } catch (IOException e) {
                return false;
            }
        }).findFirst();
        return apkPath.orElse(null);
    }

    public boolean updateModuleApkPath(String packageName, String apkPath) {
        if (apkPath == null) return false;
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

    public boolean removeModule(String packageName) {
        if (removeModuleWithoutCache(packageName)) {
            // called by oneway binder
            updateCaches(true);
            return true;
        }
        return false;
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

    public boolean enableModule(String packageName, ApplicationInfo info) {
        if (!updateModuleApkPath(packageName, getModuleApkPath(info))) return false;
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
        updateModulePrefs("lspd", 0, "config", "enable_resources", resourceHook);
        this.resourceHook = resourceHook;
    }

    public void setVerboseLog(boolean on) {
        var logcatService = ServiceManager.getLogcatService();
        if (on) {
            logcatService.startVerbose();
        } else {
            logcatService.stopVerbose();
        }
        updateModulePrefs("lspd", 0, "config", "enable_verbose_log", on);
        verboseLog = on;
    }

    public boolean resourceHook() {
        return resourceHook;
    }

    public boolean verboseLog() {
        return verboseLog;
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
        var logcatService = ServiceManager.getLogcatService();
        File logFile = verbose ? logcatService.getVerboseLog() : logcatService.getModulesLog();
        if (logFile == null) return true;
        try {
            OutputStream os = new FileOutputStream(logFile);
            os.close();
            return true;
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return false;
        }
    }

    public boolean isManager(String packageName) {
        return packageName.equals(manager);
    }

    public boolean isManager(int uid) {
        return uid == managerUid;
    }

    public boolean shouldBlock(String packageName) {
        return packageName.equals("io.github.lsposed.manager") || isManager(packageName);
    }

    public String getPrefsPath(String fileName, int uid) {
        int userId = uid / PER_USER_RANGE;
        return miscPath + "/prefs" + (userId == 0 ? "" : String.valueOf(userId)) + "/" + fileName;
    }

    // this is slow, avoid using it
    public String getModule(int uid) {
        for (var module : cachedModule.values()) {
            if (module.appId == uid % PER_USER_RANGE) return module.packageName;
        }
        return null;
    }

    public boolean isModule(int uid, String name) {
        var module = cachedModule.getOrDefault(name, null);
        return module != null && module.appId == uid % PER_USER_RANGE;
    }

    private void recursivelyChown(File file, int uid, int gid) throws ErrnoException {
        Os.chown(file.toString(), uid, gid);
        if (file.isDirectory()) {
            for (File subFile : file.listFiles()) {
                recursivelyChown(subFile, uid, gid);
            }
        }
    }

    private void recursivelyContext(File file, String context) {
        SELinux.setFileContext(file.getPath(), context);
        if (file.isDirectory()) {
            for (File subFile : file.listFiles()) {
                recursivelyContext(subFile, context);
            }
        }
    }

    public boolean ensureModulePrefsPermission(int uid, String packageName) {
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
