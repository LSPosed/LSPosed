package io.github.lsposed.lspd.service;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.database.sqlite.SQLiteDatabase;
import android.os.FileObserver;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.lsposed.lspd.service.Service.TAG;

public class ConfigManager {
    static ConfigManager instance = null;

    final private File configPath = new File("/data/adb/lspd/config");
    final private SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(new File(configPath, "modules_config.db"), null);

    final private File resourceHookSwitch = new File(configPath, "enable_resources");
    private boolean resourceHook = false;

    final private File variantSwitch = new File(configPath, "variant");
    private int variant = -1;

    final private File verboseLogSwitch = new File(configPath, "verbose_log");
    private boolean verboseLog = false;

    final private File selinuxPath = new File("/sys/fs/selinux/enforce");
    // only check on boot
    final private boolean isPermissive;

    final FileObserver configObserver = new FileObserver(configPath) {
        @Override
        public void onEvent(int event, @Nullable String path) {
            updateConfig();
            cacheScopes();
        }
    };

    int readInt(File file, int defaultValue) {
        try {
            return Integer.parseInt(new String(Files.readAllBytes(file.toPath())));
        } catch (IOException | NumberFormatException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        return defaultValue;
    }

    void writeInt(File file, int value) {
        try {
            Files.write(file.toPath(), String.valueOf(value).getBytes(), StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    void updateConfig() {
        resourceHook = resourceHookSwitch.exists();
        variant = readInt(variantSwitch, -1);
        verboseLog = readInt(verboseLogSwitch, 0) == 1;
    }

    SQLiteStatement createEnabledModulesTable = db.compileStatement("CREATE TABLE IF NOT EXISTS enabled_modules (" +
            "mid int PRIMARY KEY AUTOINCREMENT," +
            "package_name text NOT NULL UNIQUE," +
            "apk_path text NOT NULL" +
            ");");
    SQLiteStatement createScopeTable = db.compileStatement("CREATE TABLE IF NOT EXISTS scope (" +
            "mid int," +
            "uid int," +
            "PRIMARY KEY (mid, uid)" +
            ");");

    ConcurrentHashMap<Integer, ArrayList<String>> modulesForUid;

    static ConfigManager getInstance() {
        if (instance == null)
            instance = new ConfigManager();
        return instance;
    }

    private ConfigManager() {
        createTables();
        updateConfig();
        isPermissive = readInt(selinuxPath, 1) == 0;
    }

    private void createTables() {
        createEnabledModulesTable.execute();
        createScopeTable.execute();
    }

    private synchronized void cacheScopes() {
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables("scope INNER JOIN enabled_modules ON scope.mid = enabled_modules.mid");
        Cursor cursor = builder.query(db, new String[]{"scope.uid", "enabled_modules.apk_path"},
                null, null, null, null, null);
        if (cursor == null) {
            Log.e(TAG, "db cache failed");
            return;
        }
        int uid_idx = cursor.getColumnIndex("scope.uid");
        int apk_path_idx = cursor.getColumnIndex("enabled_modules.apk_path");
        while (cursor.moveToNext()) {
            int uid = cursor.getInt(uid_idx);
            String apk_path = cursor.getString(apk_path_idx);
            modulesForUid.computeIfAbsent(uid, ignored -> new ArrayList<>()).add(apk_path);
        }
    }

    // This is called when a new process created, use the cached result
    public List<String> getModulesPathForUid(int uid) {
        return modulesForUid.getOrDefault(uid, null);
    }

    // This is called when a new process created, use the cached result
    // The signature matches Riru's
    public boolean shouldSkipUid(int uid) {
        return !modulesForUid.containsKey(uid);
    }

    // This should only be called by manager, so we don't need to cache it
    public Set<Integer> getModuleScope(String packageName) {
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables("scope INNER JOIN enabled_modules ON scope.mid = enabled_modules.mid");
        Cursor cursor = builder.query(db, new String[]{"scope.uid"},
                null, null, null, null, null);
        if (cursor == null) {
            Log.e(TAG, "db cache failed");
            return null;
        }
        int uid_idx = cursor.getColumnIndex("scope.uid");
        HashSet<Integer> result = new HashSet<>();
        while (cursor.moveToNext()) {
            int uid = cursor.getInt(uid_idx);
            result.add(uid);
        }
        return result;
    }

    public boolean updateModuleApkPath(String packageName, String apkPath) {
        ContentValues values = new ContentValues();
        values.put("apk_path", apkPath);
        int count = db.updateWithOnConflict("enabled_modules", values, "package_name = ?", new String[]{packageName}, SQLiteDatabase.CONFLICT_REPLACE);
        if (count > 1) {
            cacheScopes();
            return true;
        }
        return false;
    }

    // Only be called before updating modules. No need to cache.
    private int getModuleId(String packageName) {
        try {
            db.beginTransaction();
            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            Cursor cursor = builder.query(db, new String[]{"mid"}, "package_name = ?", new String[]{packageName}, null, null, null);
            if (cursor == null) return -1;
            if (cursor.getCount() != 1) return -1;
            cursor.moveToFirst();
            return cursor.getInt(cursor.getColumnIndex("mid"));
        } finally {
            db.setTransactionSuccessful();
            db.endTransaction();
        }
    }

    public boolean setModuleScope(String packageName, String apkPath, List<Integer> uid) {
        if (uid == null || uid.isEmpty()) return false;
        updateModuleApkPath(packageName, apkPath);
        try {
            db.beginTransaction();
            int mid = getModuleId(packageName);
            if (mid == -1) return false;
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

    public boolean removeModule(String packageName) {
        try {
            db.beginTransaction();
            int mid = getModuleId(packageName);
            if (mid == -1) return false;
            db.delete("enabled_modules", "mid = ?", new String[]{String.valueOf(mid)});
            db.delete("scope", "mid = ?", new String[]{String.valueOf(mid)});
        } finally {
            db.setTransactionSuccessful();
            db.endTransaction();
        }
        cacheScopes();
        return true;
    }

    public boolean removeApps(int uid) {
        int count = db.delete("scope", "uid = ?", new String[]{String.valueOf(uid)});
        if (count > 1) {
            cacheScopes();
            return true;
        }
        return false;
    }

    public void setResourceHook(boolean resourceHook) {
        writeInt(resourceHookSwitch, resourceHook ? 1 : 0);
    }

    public void setVerboseLog(boolean verboseLog) {
        writeInt(verboseLogSwitch, verboseLog ? 1 : 0);
    }

    public void setVariant(int variant) {
        writeInt(variantSwitch, variant);
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
}
