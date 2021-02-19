package io.github.lsposed.lspd.service;

import android.content.ContentValues;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.FileObserver;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.github.lsposed.lspd.Application;

import static io.github.lsposed.lspd.service.ServiceManager.TAG;

// This config manager assume uid won't change when our service is off.
// Otherwise, user should maintain it manually.
public class ConfigManager {
    static ConfigManager instance = null;

    static final private File basePath = new File("/data/adb/lspd");
    static final private File configPath = new File(basePath, "config");
    static final private SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(new File(configPath, "modules_config.db"), null);

    static final private File resourceHookSwitch = new File(configPath, "enable_resources");
    private boolean resourceHook = false;

    static final private File variantSwitch = new File(configPath, "variant");
    private int variant = -1;

    static final private File verboseLogSwitch = new File(configPath, "verbose_log");
    private boolean verboseLog = false;

    static final private String DEFAULT_MANAGER_PACKAGE_NAME = "io.github.lsposed.manager";

    static final private File managerPath = new File(configPath, "manager");
    private String manager = null;
    private int managerUid = -1;

    static final private File miscFile = new File(basePath, "misc_path");
    private String miscPath = null;

    static final private File selinuxPath = new File("/sys/fs/selinux/enforce");
    // only check on boot
    final private boolean isPermissive;

    static final private File logPath = new File(basePath, "log");
    static final private File modulesLogPath = new File(logPath, "modules.log");
    static final private File verboseLogPath = new File(logPath, "all.log");

    final FileObserver configObserver = new FileObserver(configPath) {
        @Override
        public void onEvent(int event, @Nullable String path) {
            updateConfig();
            cacheScopes();
        }
    };

    static class ProcessScope {
        String processName;
        int uid;

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
    }

    static private final SQLiteStatement createModulesTable = db.compileStatement("CREATE TABLE IF NOT EXISTS modules (" +
            "mid integer PRIMARY KEY AUTOINCREMENT," +
            "module_pkg_name text NOT NULL UNIQUE," +
            "apk_path text NOT NULL, " +
            "enabled BOOLEAN DEFAULT 0 " +
            "CHECK (enabled IN (0, 1))" +
            ");");
    static private final SQLiteStatement createScopeTable = db.compileStatement("CREATE TABLE IF NOT EXISTS scope (" +
            "mid integer," +
            "app_pkg_name text NOT NULL," +
            "user_id integer NOT NULL," +
            "PRIMARY KEY (mid, app_pkg_name, user_id)" +
            ");");

    private final ConcurrentHashMap<ProcessScope, Set<String>> cachedScope = new ConcurrentHashMap<>();

    public static boolean shouldSkipSystemServer() {
        try (Cursor cursor = db.query("scope", new String[]{"mid"}, "app_pkg_name=?", new String[]{"android"}, null, null, null)) {
            return cursor == null || !cursor.moveToNext();
        }
    }

    public static String[] getModulesPathForSystemServer() {
        HashSet<String> modules = new HashSet<>();
        try (Cursor cursor = db.query("scope INNER JOIN modules ON scope.mid = modules.mid", new String[]{"apk_path"}, "app_pkg_name=?", new String[]{"android"}, null, null, null)) {
            int apkPathIdx = cursor.getColumnIndex("apk_path");
            while (cursor.moveToNext()) {
                modules.add(cursor.getString(apkPathIdx));
            }
        }
        return modules.toArray(new String[0]);
    }

    static private String readText(@NonNull File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath())).trim();
    }

    static private String readText(@NonNull File file, String defaultValue) {
        try {
            if (!file.exists()) return defaultValue;
            return readText(file);
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        return defaultValue;
    }

    static private void writeText(@NonNull File file, String value) {
        try {
            Files.write(file.toPath(), value.getBytes(), StandardOpenOption.CREATE);
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    static private int readInt(@NonNull File file, int defaultValue) {
        try {
            if (!file.exists()) return defaultValue;
            return Integer.parseInt(readText(file));
        } catch (IOException | NumberFormatException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        return defaultValue;
    }

    static private void writeInt(@NonNull File file, int value) {
        writeText(file, String.valueOf(value));
    }

    private synchronized void updateConfig() {
        resourceHook = readInt(resourceHookSwitch, 0) == 1;
        variant = readInt(variantSwitch, -1);
        verboseLog = readInt(verboseLogSwitch, 0) == 1;
        miscPath = "/data/misc/" + readText(miscFile, "lspd");
        updateManager();
    }

    public synchronized void updateManager() {
        try {
            PackageInfo info = PackageService.getPackageInfo(readText(managerPath, DEFAULT_MANAGER_PACKAGE_NAME), 0, 0);
            if (info != null) {
                managerUid = info.applicationInfo.uid;
                manager = info.packageName;
            }
        } catch (RemoteException ignored) {
        }
    }

    public synchronized void updateManager(@NonNull String packageName) {
        writeText(managerPath, packageName);
        manager = packageName;
        updateManager();
    }

    static ConfigManager getInstance() {
        if (instance == null)
            instance = new ConfigManager();
        return instance;
    }

    private ConfigManager() {
        createTables();
        updateConfig();
        isPermissive = readInt(selinuxPath, 1) == 0;
        configObserver.startWatching();
        cacheScopes();
    }

    private void createTables() {
        createModulesTable.execute();
        createScopeTable.execute();
    }

    private List<ProcessScope> getAssociatedProcesses(Application app) throws RemoteException {
        PackageInfo pkgInfo = PackageService.getPackageInfo(app.packageName, 0, app.userId);
        List<ProcessScope> processes = new ArrayList<>();
        if (pkgInfo != null && pkgInfo.applicationInfo != null) {
            for (String process : PackageService.getProcessesForUid(pkgInfo.applicationInfo.uid)) {
                processes.add(new ProcessScope(process, pkgInfo.applicationInfo.uid));
            }
        }
        return processes;
    }

    private synchronized void cacheScopes() {
        cachedScope.clear();
        try (Cursor cursor = db.query("scope INNER JOIN modules ON scope.mid = modules.mid", new String[]{"app_pkg_name", "user_id", "apk_path"},
                "enabled = ?", new String[]{"1"}, null, null, null)) {
            if (cursor == null) {
                Log.e(TAG, "db cache failed");
                return;
            }
            int appPkgNameIdx = cursor.getColumnIndex("app_pkg_name");
            int userIdIdx = cursor.getColumnIndex("user_id");
            int apkPathIdx = cursor.getColumnIndex("apk_path");
            HashSet<Application> obsoletePackages = new HashSet<>();
            while (cursor.moveToNext()) {
                Application app = new Application();
                app.packageName = cursor.getString(appPkgNameIdx);
                app.userId = cursor.getInt(userIdIdx);
                String apk_path = cursor.getString(apkPathIdx);
                try {
                    List<ProcessScope> processesScope = getAssociatedProcesses(app);
                    if (processesScope.isEmpty()) {
                        obsoletePackages.add(app);
                        continue;
                    }
                    for (ProcessScope processScope : processesScope)
                        cachedScope.computeIfAbsent(processScope, ignored -> new HashSet<>()).add(apk_path);
                } catch (RemoteException e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                }
            }
            for (Application obsoletePackage : obsoletePackages) {
                removeAppWithoutCache(obsoletePackage);
            }
        }
    }

    // This is called when a new process created, use the cached result
    public String[] getModulesPathForProcess(String processName, int uid) {
        return isManager(uid) ? new String[0] : cachedScope.getOrDefault(new ProcessScope(processName, uid), Collections.emptySet()).toArray(new String[0]);
    }

    // This is called when a new process created, use the cached result
    public boolean shouldSkipProcess(ProcessScope scope) {
        return !cachedScope.containsKey(scope) && !isManager(scope.uid);
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
        if (count >= 1) {
            cacheScopes();
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
            return cursor.getInt(cursor.getColumnIndex("mid"));
        }
    }

    public boolean setModuleScope(String packageName, List<Application> scopes) {
        if (scopes.isEmpty()) return false;
        int mid = getModuleId(packageName);
        if (mid == -1) return false;
        try {
            db.beginTransaction();
            db.delete("scope", "mid = ?", new String[]{String.valueOf(mid)});
            for (Application app : scopes) {
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
        cacheScopes();
        return true;
    }

    public String[] enabledModules() {
        try (Cursor cursor = db.query("modules", new String[]{"module_pkg_name"}, "enabled = ?", new String[]{"1"}, null, null, null)) {
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
        cacheScopes();
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
        cacheScopes();
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
        cacheScopes();
        return true;
    }

    public boolean removeAppWithoutCache(Application app) {
        int count = db.delete("scope", "app_pkg_name = ? AND user_id=?", new String[]{app.packageName, String.valueOf(app.userId)});
        return count >= 1;
    }

    public boolean removeApp(Application scope) {
        if (removeAppWithoutCache(scope)) {
            cacheScopes();
            return true;
        }
        return false;
    }

    public void setResourceHook(boolean resourceHook) {
        writeInt(resourceHookSwitch, resourceHook ? 1 : 0);
        this.resourceHook = resourceHook;
    }

    public void setVerboseLog(boolean verboseLog) {
        writeInt(verboseLogSwitch, verboseLog ? 1 : 0);
        this.verboseLog = verboseLog;
    }

    public void setVariant(int variant) {
        writeInt(variantSwitch, variant);
        this.variant = variant;
    }

    public boolean isPermissive() {
        return isPermissive;
    }

    public boolean resourceHook() {
        return resourceHook;
    }

    public boolean verboseLog() {
        return verboseLog;
    }

    public int variant() {
        return variant;
    }

    public ParcelFileDescriptor getModulesLog(int mode) {
        try {
            return ParcelFileDescriptor.open(modulesLogPath, mode | ParcelFileDescriptor.MODE_CREATE);
        } catch (FileNotFoundException e) {
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
            OutputStream os = new FileOutputStream(verbose ? verboseLogPath : modulesLogPath);
            os.close();
            return true;
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return false;
        }
    }

    public boolean isManager(String module_pkg_name) {
        return module_pkg_name.equals(manager);
    }

    public boolean isManager(int uid) {
        return uid == managerUid;
    }

    public String getCachePath(String fileName) {
        return miscPath + File.separator + "cache" + File.separator + fileName;
    }

    public String getPrefsPath(String fileName) {
        return miscPath + File.separator + "prefs" + File.separator + fileName;
    }

    public void grantManagerPermission() {
        try {
            PackageService.grantRuntimePermission(manager, "android.permission.INTERACT_ACROSS_USERS", 0);
        } catch (RemoteException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }
}
