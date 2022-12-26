package org.lsposed.lspd.impl;

import android.app.ActivityThread;
import android.app.LoadedApk;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextParams;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.UserHandle;
import android.util.Log;
import android.view.Display;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.lsposed.lspd.models.Module;
import org.lsposed.lspd.util.LspModuleClassLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedInit;
import io.github.libxposed.XposedContext;
import io.github.libxposed.XposedModule;
import io.github.libxposed.XposedModuleInterface;

public class LSPosedContext extends XposedContext {

    public static final String TAG = "LSPosedContext";

    private final Context base;

    LSPosedContext(Context base) {
        this.base = base;
    }

    public static boolean loadModules(ActivityThread at, Module module) {
        try {
            Log.d(TAG, "Loading module " + module.packageName);
            var sb = new StringBuilder();
            var abis = Process.is64Bit() ? Build.SUPPORTED_64_BIT_ABIS : Build.SUPPORTED_32_BIT_ABIS;
            for (String abi : abis) {
                sb.append(module.apkPath).append("!/lib/").append(abi).append(File.pathSeparator);
            }
            var librarySearchPath = sb.toString();
            var initLoader = XposedModule.class.getClassLoader();
            var mcl = LspModuleClassLoader.loadApk(module.apkPath, module.file.preLoadedDexes, librarySearchPath, initLoader);
            if (mcl.loadClass(XposedModule.class.getName()).getClassLoader() != initLoader) {
                Log.e(TAG, "  Cannot load module: " + module.packageName);
                Log.e(TAG, "  The Xposed API classes are compiled into the module's APK.");
                Log.e(TAG, "  This may cause strange issues and must be fixed by the module developer.");
                return false;
            }
            var loadedApk = at.getPackageInfoNoCheck(module.applicationInfo, null);
            XposedHelpers.setObjectField(loadedApk, "mClassLoader", mcl);
            var c = Class.forName("android.app.ContextImpl");
            var ctor = c.getDeclaredConstructors()[0];
            ctor.setAccessible(true);
            var args = new Object[ctor.getParameterTypes().length];
            for (int i = 0; i < ctor.getParameterTypes().length; ++i) {
                if (ctor.getParameterTypes()[i] == LoadedApk.class) {
                    args[i] = loadedApk;
                    continue;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ctor.getParameterTypes()[i] == ContextParams.class) {
                        args[i] = new ContextParams.Builder().build();
                        continue;
                    }
                }
                if (ctor.getParameterTypes()[i] == ActivityThread.class) {
                    args[i] = at;
                    continue;
                }
                if (ctor.getParameterTypes()[i] == int.class) {
                    args[i] = 0;
                    continue;
                }
                args[i] = null;
            }
            var ctx = new LSPosedContext((Context) ctor.newInstance(args));
            for (var entry : module.file.moduleClassNames) {
                var moduleClass = ctx.getClassLoader().loadClass(entry);
                Log.d(TAG, "  Loading class " + moduleClass);
                if (!XposedModule.class.isAssignableFrom(moduleClass)) {
                    Log.e(TAG, "    This class doesn't implement any sub-interface of XposedModule, skipping it");
                }
                try {
                    if (moduleClass.getMethod("onResourceLoaded").getDeclaringClass() != XposedModuleInterface.class) {
                        XposedInit.hookResources();
                    }
                    var moduleEntry = moduleClass.getConstructor(XposedContext.class);
                    moduleEntry.newInstance(ctx);
                } catch (Throwable e) {
                    Log.e(TAG, "    Failed to load class " + moduleClass, e);
                }
            }
            Log.d(TAG, "Loaded module " + module.packageName + ": " + ctx);
            return true;
        } catch (Throwable e) {
            Log.d(TAG, "Loading module " + module.packageName, e);
        }
        return false;
    }

    @Override
    public AssetManager getAssets() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Resources getResources() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public PackageManager getPackageManager() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public ContentResolver getContentResolver() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Looper getMainLooper() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Context getApplicationContext() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void setTheme(int resid) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Resources.Theme getTheme() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public ClassLoader getClassLoader() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public String getPackageName() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public ApplicationInfo getApplicationInfo() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public String getPackageResourcePath() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public String getPackageCodePath() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean moveSharedPreferencesFrom(Context sourceContext, String name) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean deleteSharedPreferences(String name) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public FileInputStream openFileInput(String name) throws FileNotFoundException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean deleteFile(String name) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public File getFileStreamPath(String name) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public File getDataDir() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public File getFilesDir() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public File getNoBackupFilesDir() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Nullable
    @Override
    public File getExternalFilesDir(@Nullable String type) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public File[] getExternalFilesDirs(String type) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public File getObbDir() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public File[] getObbDirs() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public File getCacheDir() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public File getCodeCacheDir() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Nullable
    @Override
    public File getExternalCacheDir() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public File[] getExternalCacheDirs() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public File[] getExternalMediaDirs() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public String[] fileList() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public File getDir(String name, int mode) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory, @Nullable DatabaseErrorHandler errorHandler) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean moveDatabaseFrom(Context sourceContext, String name) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean deleteDatabase(String name) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public File getDatabasePath(String name) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public String[] databaseList() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Drawable getWallpaper() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Drawable peekWallpaper() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public int getWallpaperDesiredMinimumWidth() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public int getWallpaperDesiredMinimumHeight() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void setWallpaper(Bitmap bitmap) throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void setWallpaper(InputStream data) throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void clearWallpaper() throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void startActivity(Intent intent) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void startActivity(Intent intent, @Nullable Bundle options) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void startActivities(Intent[] intents) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void startActivities(Intent[] intents, Bundle options) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void startIntentSender(IntentSender intent, @Nullable Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags) throws IntentSender.SendIntentException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void startIntentSender(IntentSender intent, @Nullable Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags, @Nullable Bundle options) throws IntentSender.SendIntentException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void sendBroadcast(Intent intent) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void sendBroadcast(Intent intent, @Nullable String receiverPermission) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void sendOrderedBroadcast(Intent intent, @Nullable String receiverPermission) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void sendOrderedBroadcast(@NonNull Intent intent, @Nullable String receiverPermission, @Nullable BroadcastReceiver resultReceiver, @Nullable Handler scheduler, int initialCode, @Nullable String initialData, @Nullable Bundle initialExtras) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void sendBroadcastAsUser(Intent intent, UserHandle user) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void sendBroadcastAsUser(Intent intent, UserHandle user, @Nullable String receiverPermission) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void sendOrderedBroadcastAsUser(Intent intent, UserHandle user, @Nullable String receiverPermission, BroadcastReceiver resultReceiver, @Nullable Handler scheduler, int initialCode, @Nullable String initialData, @Nullable Bundle initialExtras) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void sendStickyBroadcast(Intent intent) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void sendStickyOrderedBroadcast(Intent intent, BroadcastReceiver resultReceiver, @Nullable Handler scheduler, int initialCode, @Nullable String initialData, @Nullable Bundle initialExtras) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void removeStickyBroadcast(Intent intent) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void sendStickyBroadcastAsUser(Intent intent, UserHandle user) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void sendStickyOrderedBroadcastAsUser(Intent intent, UserHandle user, BroadcastReceiver resultReceiver, @Nullable Handler scheduler, int initialCode, @Nullable String initialData, @Nullable Bundle initialExtras) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void removeStickyBroadcastAsUser(Intent intent, UserHandle user) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Nullable
    @Override
    public Intent registerReceiver(@Nullable BroadcastReceiver receiver, IntentFilter filter) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Nullable
    @Override
    public Intent registerReceiver(@Nullable BroadcastReceiver receiver, IntentFilter filter, int flags) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Nullable
    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, @Nullable String broadcastPermission, @Nullable Handler scheduler) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Nullable
    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, @Nullable String broadcastPermission, @Nullable Handler scheduler, int flags) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void unregisterReceiver(BroadcastReceiver receiver) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Nullable
    @Override
    public ComponentName startService(Intent service) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Nullable
    @Override
    public ComponentName startForegroundService(Intent service) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean stopService(Intent service) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean bindService(Intent service, @NonNull ServiceConnection conn, int flags) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void unbindService(@NonNull ServiceConnection conn) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean startInstrumentation(@NonNull ComponentName className, @Nullable String profileFile, @Nullable Bundle arguments) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Object getSystemService(@NonNull String name) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Nullable
    @Override
    public String getSystemServiceName(@NonNull Class<?> serviceClass) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public int checkPermission(@NonNull String permission, int pid, int uid) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public int checkCallingPermission(@NonNull String permission) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public int checkCallingOrSelfPermission(@NonNull String permission) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public int checkSelfPermission(@NonNull String permission) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void enforcePermission(@NonNull String permission, int pid, int uid, @Nullable String message) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void enforceCallingPermission(@NonNull String permission, @Nullable String message) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void enforceCallingOrSelfPermission(@NonNull String permission, @Nullable String message) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void grantUriPermission(String toPackage, Uri uri, int modeFlags) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void revokeUriPermission(Uri uri, int modeFlags) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void revokeUriPermission(String toPackage, Uri uri, int modeFlags) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public int checkUriPermission(Uri uri, int pid, int uid, int modeFlags) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public int checkCallingUriPermission(Uri uri, int modeFlags) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public int checkCallingOrSelfUriPermission(Uri uri, int modeFlags) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public int checkUriPermission(@Nullable Uri uri, @Nullable String readPermission, @Nullable String writePermission, int pid, int uid, int modeFlags) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void enforceUriPermission(Uri uri, int pid, int uid, int modeFlags, String message) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void enforceCallingUriPermission(Uri uri, int modeFlags, String message) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void enforceCallingOrSelfUriPermission(Uri uri, int modeFlags, String message) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void enforceUriPermission(@Nullable Uri uri, @Nullable String readPermission, @Nullable String writePermission, int pid, int uid, int modeFlags, @Nullable String message) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Context createPackageContext(String packageName, int flags) throws PackageManager.NameNotFoundException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Context createContextForSplit(String splitName) throws PackageManager.NameNotFoundException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Context createConfigurationContext(@NonNull Configuration overrideConfiguration) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Context createDisplayContext(@NonNull Display display) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Context createDeviceProtectedStorageContext() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean isDeviceProtectedStorage() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void hook() {
        throw new UnsupportedOperationException("Not implemented");
    }
}
