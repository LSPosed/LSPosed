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
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.lsposed.lspd.service.ServiceManager.TAG;

// This config manager assume uid won't change when our service is off.
// Otherwise, user should maintain it manually.
public class ConfigManager {
    static ConfigManager instance = null;

    final private File basePath = new File("/data/adb/lspd");
    final private File configPath = new File(basePath, "config");
    final private SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(new File(configPath, "modules_config.db"), null);

    final private File resourceHookSwitch = new File(configPath, "enable_resources");
    private boolean resourceHook = false;

    final private File variantSwitch = new File(configPath, "variant");
    private int variant = -1;

    final private File verboseLogSwitch = new File(configPath, "verbose_log");
    private boolean verboseLog = false;

    final private String DEFAULT_MANAGER_PACKAGE_NAME = "io.github.lsposed.manager";

    final private File managerPath = new File(configPath, "manager");
    private String manager = null;
    private int managerUid = -1;

    final private File miscFile = new File(basePath, "misc_path");
    private String miscPath = null;

    final private File selinuxPath = new File("/sys/fs/selinux/enforce");
    // only check on boot
    final private boolean isPermissive;

    final private File logPath = new File(basePath, "log");
    final private File modulesLogPath = new File(logPath, "modules.log");
    final private File verboseLogPath = new File(logPath, "all.log");

    final FileObserver configObserver = new FileObserver(configPath) {
        @Override
        public void onEvent(int event, @Nullable String path) {
            updateConfig();
            cacheScopes();
        }
    };

    private String readText(@NonNull File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath())).trim();
    }

    private String readText(@NonNull File file, String defaultValue) {
        try {
            if (!file.exists()) return defaultValue;
            return readText(file);
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        return defaultValue;
    }

    private void writeText(@NonNull File file, String value) {
        try {
            Files.write(file.toPath(), value.getBytes(), StandardOpenOption.CREATE);
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    private int readInt(@NonNull File file, int defaultValue) {
        try {
            if (!file.exists()) return defaultValue;
            return Integer.parseInt(readText(file));
        } catch (IOException | NumberFormatException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        return defaultValue;
    }

    private void writeInt(@NonNull File file, int value) {
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
        updateManager();
    }

    private final SQLiteStatement createModulesTable = db.compileStatement("CREATE TABLE IF NOT EXISTS modules (" +
            "mid integer PRIMARY KEY AUTOINCREMENT," +
            "package_name text NOT NULL UNIQUE," +
            "apk_path text NOT NULL, " +
            "enabled BOOLEAN DEFAULT 0 " +
            "CHECK (enabled IN (0, 1))" +
            ");");
    private final SQLiteStatement createScopeTable = db.compileStatement("CREATE TABLE IF NOT EXISTS scope (" +
            "mid integer," +
            "uid integer," +
            "PRIMARY KEY (mid, uid)" +
            ");");

    private final ConcurrentHashMap<Integer, ArrayList<String>> modulesForUid = new ConcurrentHashMap<>();

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

    private synchronized void cacheScopes() {
        modulesForUid.clear();
        try (Cursor cursor = db.query("scope INNER JOIN modules ON scope.mid = modules.mid", new String[]{"uid", "apk_path"},
                "enabled = ?", new String[]{"1"}, null, null, null)) {
            if (cursor == null) {
                Log.e(TAG, "db cache failed");
                return;
            }
            int uid_idx = cursor.getColumnIndex("uid");
            int apk_path_idx = cursor.getColumnIndex("apk_path");
            while (cursor.moveToNext()) {
                int uid = cursor.getInt(uid_idx);
                String apk_path = cursor.getString(apk_path_idx);
                modulesForUid.computeIfAbsent(uid, ignored -> new ArrayList<>()).add(apk_path);
            }
        }
    }

    // This is called when a new process created, use the cached result
    public String[] getModulesPathForUid(int uid) {
        return isManager(uid) ? new String[0] : modulesForUid.getOrDefault(uid, new ArrayList<>()).toArray(new String[0]);
    }

    // This is called when a new process created, use the cached result
    // The signature matches Riru's
    public boolean shouldSkipUid(int uid) {
        return !modulesForUid.containsKey(uid) && !isManager(uid);
    }

    // This should only be called by manager, so we don't need to cache it
    public int[] getModuleScope(String packageName) {
        int mid = getModuleId(packageName);
        if (mid == -1) return null;
        try (Cursor cursor = db.query("scope INNER JOIN modules ON scope.mid = modules.mid", new String[]{"uid"},
                "scope.mid = ?", new String[]{String.valueOf(mid)}, null, null, null)) {
            if (cursor == null) {
                return null;
            }
            int uid_idx = cursor.getColumnIndex("uid");
            HashSet<Integer> result = new HashSet<>();
            while (cursor.moveToNext()) {
                int uid = cursor.getInt(uid_idx);
                result.add(uid);
            }
            return result.stream().mapToInt(i -> i).toArray();
        }
    }

    public boolean updateModuleApkPath(String packageName, String apkPath) {
        if (db.inTransaction()) {
            Log.w(TAG, "update module apk path should not be called inside transaction");
            return false;
        }
        ContentValues values = new ContentValues();
        values.put("package_name", packageName);
        values.put("apk_path", apkPath);
        int count = (int) db.insertWithOnConflict("modules", null, values, SQLiteDatabase.CONFLICT_IGNORE);
        if (count < 0) {
            count = db.updateWithOnConflict("modules", values, "package_name=?", new String[]{packageName}, SQLiteDatabase.CONFLICT_IGNORE);
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
        try (Cursor cursor = db.query("modules", new String[]{"mid"}, "package_name=?", new String[]{packageName}, null, null, null)) {
            if (cursor == null) return -1;
            if (cursor.getCount() != 1) return -1;
            cursor.moveToFirst();
            return cursor.getInt(cursor.getColumnIndex("mid"));
        }
    }

    public boolean setModuleScope(String packageName, int[] uid) {
        if (uid == null || uid.length == 0) return false;
        int mid = getModuleId(packageName);
        if (mid == -1) return false;
        try {
            db.beginTransaction();
            db.delete("scope", "mid = ?", new String[]{String.valueOf(mid)});
            for (int id : uid) {
                ContentValues values = new ContentValues();
                values.put("mid", mid);
                values.put("uid", id);
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
        try (Cursor cursor = db.query("modules", new String[]{"package_name"}, "enabled = ?", new String[]{"1"}, null, null, null)) {
            if (cursor == null) {
                Log.e(TAG, "query enabled modules failed");
                return null;
            }
            int pkg_idx = cursor.getColumnIndex("package_name");
            HashSet<String> result = new HashSet<>();
            while (cursor.moveToNext()) {
                result.add(cursor.getString(pkg_idx));
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

    public boolean removeApp(int uid) {
        int count = db.delete("scope", "uid = ?", new String[]{String.valueOf(uid)});
        if (count >= 1) {
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

    public boolean isManager(String package_name) {
        return package_name.equals(manager);
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
}
