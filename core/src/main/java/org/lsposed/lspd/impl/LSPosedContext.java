package org.lsposed.lspd.impl;

import static de.robv.android.xposed.callbacks.XCallback.PRIORITY_DEFAULT;

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
import android.content.res.XModuleResources;
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
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import android.view.Display;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.lsposed.lspd.core.BuildConfig;
import org.lsposed.lspd.models.Module;
import org.lsposed.lspd.nativebridge.HookBridge;
import org.lsposed.lspd.service.ILSPInjectedModuleService;
import org.lsposed.lspd.util.LspModuleClassLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedInit;
import io.github.libxposed.XposedContext;
import io.github.libxposed.XposedModule;
import io.github.libxposed.XposedModuleInterface;
import io.github.libxposed.util.DexParser;

public class LSPosedContext extends XposedContext {

    private static final String TAG = "LSPosedContext";
    public static boolean isSystemServer;
    public static String appDir;
    public static String processName;

    static final Set<XposedModule> modules = ConcurrentHashMap.newKeySet();

    private final Object mSync = new Object();

    private final Context mBase;
    private final String mPackageName;
    private final String mApkPath;
    private final ILSPInjectedModuleService service;
    private final Map<String, SharedPreferences> mRemotePrefs = new ConcurrentHashMap<>();

    LSPosedContext(Context base, String packageName, String apkPath, ILSPInjectedModuleService service) {
        this.mBase = base;
        this.mPackageName = packageName;
        this.mApkPath = apkPath;
        this.service = service;
    }

    public static void callOnPackageLoaded(XposedModuleInterface.PackageLoadedParam param) {
        for (XposedModule module : modules) {
            try {
                module.onPackageLoaded(param);
            } catch (Throwable t) {
                Log.e(TAG, "Error when calling onPackageLoaded of " + ((LSPosedContext) module.getBaseContext()).mPackageName, t);
            }
        }
    }

    public static void callOnResourceLoaded(XposedModuleInterface.ResourcesLoadedParam param) {
        for (XposedModule module : modules) {
            try {
                module.onResourceLoaded(param);
            } catch (Throwable t) {
                Log.e(TAG, "Error when calling onResourceLoaded of " + ((LSPosedContext) module.getBaseContext()).mPackageName, t);
            }
        }
    }

    public static boolean loadModule(ActivityThread at, Module module) {
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
            XposedHelpers.setObjectField(loadedApk, "mDataDir", appDir);
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
            var ctx = new LSPosedContext((Context) ctor.newInstance(args), module.packageName, module.apkPath, module.service);
            for (var entry : module.file.moduleClassNames) {
                var moduleClass = ctx.getClassLoader().loadClass(entry);
                Log.d(TAG, "  Loading class " + moduleClass);
                if (!XposedModule.class.isAssignableFrom(moduleClass)) {
                    Log.e(TAG, "    This class doesn't implement any sub-interface of XposedModule, skipping it");
                    continue;
                }
                try {
                    if (moduleClass.getMethod("onResourceLoaded", XposedModuleInterface.ResourcesLoadedParam.class).getDeclaringClass() != XposedModule.class) {
                        XposedInit.hookResources();
                    }
                    var moduleEntry = moduleClass.getConstructor(XposedContext.class, XposedModuleInterface.ModuleLoadedParam.class);
                    var moduleContext = (XposedModule) moduleEntry.newInstance(ctx, new XposedModuleInterface.ModuleLoadedParam() {
                        @Override
                        public boolean isSystemServer() {
                            return isSystemServer;
                        }

                        @NonNull
                        @Override
                        public String getProcessName() {
                            return processName;
                        }

                        @NonNull
                        @Override
                        public String getAppDataDir() {
                            return appDir;
                        }

                        @Nullable
                        @Override
                        public Bundle getExtras() {
                            return null;
                        }
                    });
                    modules.add(moduleContext);
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
        return getResources().getAssets();
    }

    @Override
    public Resources getResources() {
        synchronized (mSync) {
            var res = mBase.getResources();
            if (res == null) {
                res = XModuleResources.createInstance(mBase.getPackageCodePath(), null);
                XposedHelpers.setObjectField(mBase, "mResources", res);
            }
            return res;
        }
    }

    @Override
    public PackageManager getPackageManager() {
        return mBase.getPackageManager();
    }

    @Override
    public ContentResolver getContentResolver() {
        throw new AbstractMethodError();
    }

    @Override
    public Looper getMainLooper() {
        return mBase.getMainLooper();
    }

    @Override
    public Context getApplicationContext() {
        throw new AbstractMethodError();
    }

    @Override
    public void setTheme(int resid) {
        mBase.setTheme(resid);
    }

    @Override
    public Resources.Theme getTheme() {
        return mBase.getTheme();
    }

    @Override
    public ClassLoader getClassLoader() {
        return mBase.getClassLoader();
    }

    @Override
    public String getPackageName() {
        return mBase.getPackageName();
    }

    @Override
    public ApplicationInfo getApplicationInfo() {
        return mBase.getApplicationInfo();
    }

    @Override
    public String getPackageResourcePath() {
        return mApkPath;
    }

    @Override
    public String getPackageCodePath() {
        return mApkPath;
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        if (name == null) throw new IllegalArgumentException("name must not be null");
        return mRemotePrefs.computeIfAbsent(name, __ -> new LSPosedRemotePreferences(service, name));
    }

    @Override
    public boolean moveSharedPreferencesFrom(Context sourceContext, String name) {
        throw new AbstractMethodError();
    }

    @Override
    public boolean deleteSharedPreferences(String name) {
        throw new AbstractMethodError();
    }

    @Override
    public FileInputStream openFileInput(String name) throws FileNotFoundException {
        try {
            return new FileInputStream(service.openRemoteFile(name).getFileDescriptor());
        } catch (RemoteException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    @Override
    public FileOutputStream openFileOutput(String name, int mode) {
        throw new AbstractMethodError();
    }

    @Override
    public boolean deleteFile(String name) {
        throw new AbstractMethodError();
    }

    @Override
    public File getFileStreamPath(String name) {
        throw new AbstractMethodError();
    }

    @Override
    public File getDataDir() {
        return mBase.getDataDir();
    }

    @Override
    public File getFilesDir() {
        throw new AbstractMethodError();
    }

    @Override
    public File getNoBackupFilesDir() {
        throw new AbstractMethodError();
    }

    @Nullable
    @Override
    public File getExternalFilesDir(@Nullable String type) {
        throw new AbstractMethodError();
    }

    @Override
    public File[] getExternalFilesDirs(String type) {
        throw new AbstractMethodError();
    }

    @Override
    public File getObbDir() {
        throw new AbstractMethodError();
    }

    @Override
    public File[] getObbDirs() {
        throw new AbstractMethodError();
    }

    @Override
    public File getCacheDir() {
        return mBase.getCacheDir();
    }

    @Override
    public File getCodeCacheDir() {
        throw new AbstractMethodError();
    }

    @Nullable
    @Override
    public File getExternalCacheDir() {
        throw new AbstractMethodError();
    }

    @Override
    public File[] getExternalCacheDirs() {
        throw new AbstractMethodError();
    }

    @Override
    public File[] getExternalMediaDirs() {
        throw new AbstractMethodError();
    }

    @Override
    public String[] fileList() {
        throw new AbstractMethodError();
    }

    @Override
    public File getDir(String name, int mode) {
        throw new AbstractMethodError();
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory) {
        throw new AbstractMethodError();
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory, @Nullable DatabaseErrorHandler errorHandler) {
        throw new AbstractMethodError();
    }

    @Override
    public boolean moveDatabaseFrom(Context sourceContext, String name) {
        throw new AbstractMethodError();
    }

    @Override
    public boolean deleteDatabase(String name) {
        throw new AbstractMethodError();
    }

    @Override
    public File getDatabasePath(String name) {
        throw new AbstractMethodError();
    }

    @Override
    public String[] databaseList() {
        throw new AbstractMethodError();
    }

    @Override
    public Drawable getWallpaper() {
        throw new AbstractMethodError();
    }

    @Override
    public Drawable peekWallpaper() {
        throw new AbstractMethodError();
    }

    @Override
    public int getWallpaperDesiredMinimumWidth() {
        throw new AbstractMethodError();
    }

    @Override
    public int getWallpaperDesiredMinimumHeight() {
        throw new AbstractMethodError();
    }

    @Override
    public void setWallpaper(Bitmap bitmap) {
        throw new AbstractMethodError();
    }

    @Override
    public void setWallpaper(InputStream data) {
        throw new AbstractMethodError();
    }

    @Override
    public void clearWallpaper() {
        throw new AbstractMethodError();
    }

    @Override
    public void startActivity(Intent intent) {
        throw new AbstractMethodError();
    }

    @Override
    public void startActivity(Intent intent, @Nullable Bundle options) {
        throw new AbstractMethodError();
    }

    @Override
    public void startActivities(Intent[] intents) {
        throw new AbstractMethodError();
    }

    @Override
    public void startActivities(Intent[] intents, Bundle options) {
        throw new AbstractMethodError();
    }

    @Override
    public void startIntentSender(IntentSender intent, @Nullable Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags) {
        throw new AbstractMethodError();
    }

    @Override
    public void startIntentSender(IntentSender intent, @Nullable Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags, @Nullable Bundle options) {
        throw new AbstractMethodError();
    }

    @Override
    public void sendBroadcast(Intent intent) {
        throw new AbstractMethodError();
    }

    @Override
    public void sendBroadcast(Intent intent, @Nullable String receiverPermission) {
        throw new AbstractMethodError();
    }

    @Override
    public void sendOrderedBroadcast(Intent intent, @Nullable String receiverPermission) {
        throw new AbstractMethodError();
    }

    @Override
    public void sendOrderedBroadcast(@NonNull Intent intent, @Nullable String receiverPermission, @Nullable BroadcastReceiver resultReceiver, @Nullable Handler scheduler, int initialCode, @Nullable String initialData, @Nullable Bundle initialExtras) {
        throw new AbstractMethodError();
    }

    @Override
    public void sendBroadcastAsUser(Intent intent, UserHandle user) {
        throw new AbstractMethodError();
    }

    @Override
    public void sendBroadcastAsUser(Intent intent, UserHandle user, @Nullable String receiverPermission) {
        throw new AbstractMethodError();
    }

    @Override
    public void sendOrderedBroadcastAsUser(Intent intent, UserHandle user, @Nullable String receiverPermission, BroadcastReceiver resultReceiver, @Nullable Handler scheduler, int initialCode, @Nullable String initialData, @Nullable Bundle initialExtras) {
        throw new AbstractMethodError();
    }

    @Override
    public void sendStickyBroadcast(Intent intent) {
        throw new AbstractMethodError();
    }

    @Override
    public void sendStickyOrderedBroadcast(Intent intent, BroadcastReceiver resultReceiver, @Nullable Handler scheduler, int initialCode, @Nullable String initialData, @Nullable Bundle initialExtras) {
        throw new AbstractMethodError();
    }

    @Override
    public void removeStickyBroadcast(Intent intent) {
        throw new AbstractMethodError();
    }

    @Override
    public void sendStickyBroadcastAsUser(Intent intent, UserHandle user) {
        throw new AbstractMethodError();
    }

    @Override
    public void sendStickyOrderedBroadcastAsUser(Intent intent, UserHandle user, BroadcastReceiver resultReceiver, @Nullable Handler scheduler, int initialCode, @Nullable String initialData, @Nullable Bundle initialExtras) {
        throw new AbstractMethodError();
    }

    @Override
    public void removeStickyBroadcastAsUser(Intent intent, UserHandle user) {
        throw new AbstractMethodError();
    }

    @Nullable
    @Override
    public Intent registerReceiver(@Nullable BroadcastReceiver receiver, IntentFilter filter) {
        throw new AbstractMethodError();
    }

    @Nullable
    @Override
    public Intent registerReceiver(@Nullable BroadcastReceiver receiver, IntentFilter filter, int flags) {
        throw new AbstractMethodError();
    }

    @Nullable
    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, @Nullable String broadcastPermission, @Nullable Handler scheduler) {
        throw new AbstractMethodError();
    }

    @Nullable
    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, @Nullable String broadcastPermission, @Nullable Handler scheduler, int flags) {
        throw new AbstractMethodError();
    }

    @Override
    public void unregisterReceiver(BroadcastReceiver receiver) {
        throw new AbstractMethodError();
    }

    @Nullable
    @Override
    public ComponentName startService(Intent service) {
        throw new AbstractMethodError();
    }

    @Nullable
    @Override
    public ComponentName startForegroundService(Intent service) {
        throw new AbstractMethodError();
    }

    @Override
    public boolean stopService(Intent service) {
        throw new AbstractMethodError();
    }

    @Override
    public boolean bindService(Intent service, @NonNull ServiceConnection conn, int flags) {
        throw new AbstractMethodError();
    }

    @Override
    public void unbindService(@NonNull ServiceConnection conn) {
        throw new AbstractMethodError();
    }

    @Override
    public boolean startInstrumentation(@NonNull ComponentName className, @Nullable String profileFile, @Nullable Bundle arguments) {
        throw new AbstractMethodError();
    }

    @Override
    public Object getSystemService(@NonNull String name) {
        return mBase.getSystemService(name);
    }

    @Nullable
    @Override
    public String getSystemServiceName(@NonNull Class<?> serviceClass) {
        throw new AbstractMethodError();
    }

    @Override
    public int checkPermission(@NonNull String permission, int pid, int uid) {
        throw new AbstractMethodError();
    }

    @Override
    public int checkCallingPermission(@NonNull String permission) {
        throw new AbstractMethodError();
    }

    @Override
    public int checkCallingOrSelfPermission(@NonNull String permission) {
        throw new AbstractMethodError();
    }

    @Override
    public int checkSelfPermission(@NonNull String permission) {
        throw new AbstractMethodError();
    }

    @Override
    public void enforcePermission(@NonNull String permission, int pid, int uid, @Nullable String message) {
        throw new AbstractMethodError();
    }

    @Override
    public void enforceCallingPermission(@NonNull String permission, @Nullable String message) {
        throw new AbstractMethodError();
    }

    @Override
    public void enforceCallingOrSelfPermission(@NonNull String permission, @Nullable String message) {
        throw new AbstractMethodError();
    }

    @Override
    public void grantUriPermission(String toPackage, Uri uri, int modeFlags) {
        throw new AbstractMethodError();
    }

    @Override
    public void revokeUriPermission(Uri uri, int modeFlags) {
        throw new AbstractMethodError();
    }

    @Override
    public void revokeUriPermission(String toPackage, Uri uri, int modeFlags) {
        throw new AbstractMethodError();
    }

    @Override
    public int checkUriPermission(Uri uri, int pid, int uid, int modeFlags) {
        throw new AbstractMethodError();
    }

    @Override
    public int checkCallingUriPermission(Uri uri, int modeFlags) {
        throw new AbstractMethodError();
    }

    @Override
    public int checkCallingOrSelfUriPermission(Uri uri, int modeFlags) {
        throw new AbstractMethodError();
    }

    @Override
    public int checkUriPermission(@Nullable Uri uri, @Nullable String readPermission, @Nullable String writePermission, int pid, int uid, int modeFlags) {
        throw new AbstractMethodError();
    }

    @Override
    public void enforceUriPermission(Uri uri, int pid, int uid, int modeFlags, String message) {
        throw new AbstractMethodError();
    }

    @Override
    public void enforceCallingUriPermission(Uri uri, int modeFlags, String message) {
        throw new AbstractMethodError();
    }

    @Override
    public void enforceCallingOrSelfUriPermission(Uri uri, int modeFlags, String message) {
        throw new AbstractMethodError();
    }

    @Override
    public void enforceUriPermission(@Nullable Uri uri, @Nullable String readPermission, @Nullable String writePermission, int pid, int uid, int modeFlags, @Nullable String message) {
        throw new AbstractMethodError();
    }

    @Override
    public Context createPackageContext(String packageName, int flags) {
        throw new AbstractMethodError();
    }

    @Override
    public Context createContextForSplit(String splitName) {
        throw new AbstractMethodError();
    }

    @Override
    public Context createConfigurationContext(@NonNull Configuration overrideConfiguration) {
        throw new AbstractMethodError();
    }

    @Override
    public Context createDisplayContext(@NonNull Display display) {
        throw new AbstractMethodError();
    }

    @Override
    public Context createDeviceProtectedStorageContext() {
        throw new AbstractMethodError();
    }

    @Override
    public boolean isDeviceProtectedStorage() {
        throw new AbstractMethodError();
    }

    @NonNull
    @Override
    public String getFrameworkName() {
        return "LSPosed";
    }

    @NonNull
    @Override
    public String getFrameworkVersion() {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public long getFrameworkVersionCode() {
        return BuildConfig.VERSION_CODE;
    }

    private <T, U extends Executable> MethodUnhooker<T, U> doHook(U hookMethod, int priority, T callback) {
        if (Modifier.isAbstract(hookMethod.getModifiers())) {
            throw new IllegalArgumentException("Cannot hook abstract methods: " + hookMethod);
        } else if (hookMethod.getDeclaringClass().getClassLoader() == LSPosedContext.class.getClassLoader()) {
            throw new IllegalArgumentException("Do not allow hooking inner methods");
        } else if (hookMethod.getDeclaringClass() == Method.class && hookMethod.getName().equals("invoke")) {
            throw new IllegalArgumentException("Cannot hook Method.invoke");
        }

        if (callback == null) {
            throw new IllegalArgumentException("hooker should not be null!");
        }

        if (HookBridge.hookMethod(hookMethod, XposedBridge.AdditionalHookInfo.class, priority, callback)) {
            return new MethodUnhooker<>() {
                @NonNull
                @Override
                public U getOrigin() {
                    return hookMethod;
                }

                @NonNull
                @Override
                public T getHooker() {
                    return callback;
                }

                @Override
                public void unhook() {
                    HookBridge.unhookMethod(hookMethod, callback);
                }
            };
        }
        log("Cannot hook " + hookMethod);
        return null;
    }

    @Override
    @Nullable
    public MethodUnhooker<BeforeHooker<Method>, Method> hookBefore(@NonNull Method origin, @NonNull BeforeHooker<Method> hooker) {
        return doHook(origin, PRIORITY_DEFAULT, hooker);
    }

    @Override
    @Nullable
    public MethodUnhooker<AfterHooker<Method>, Method> hookAfter(@NonNull Method origin, @NonNull AfterHooker<Method> hooker) {
        return doHook(origin, PRIORITY_DEFAULT, hooker);
    }

    @Override
    @Nullable
    public MethodUnhooker<Hooker<Method>, Method> hook(@NonNull Method origin, @NonNull Hooker<Method> hooker) {
        return doHook(origin, PRIORITY_DEFAULT, hooker);
    }

    @Override
    @Nullable
    public MethodUnhooker<BeforeHooker<Method>, Method> hookBefore(@NonNull Method origin, int priority, @NonNull BeforeHooker<Method> hooker) {
        return doHook(origin, priority, hooker);
    }

    @Override
    @Nullable
    public MethodUnhooker<AfterHooker<Method>, Method> hookAfter(@NonNull Method origin, int priority, @NonNull AfterHooker<Method> hooker) {
        return doHook(origin, priority, hooker);
    }

    @Override
    @Nullable
    public MethodUnhooker<Hooker<Method>, Method> hook(@NonNull Method origin, int priority, @NonNull Hooker<Method> hooker) {
        return doHook(origin, priority, hooker);
    }

    @Override
    @Nullable
    public <T> MethodUnhooker<BeforeHooker<Constructor<T>>, Constructor<T>> hookBefore(@NonNull Constructor<T> origin, @NonNull BeforeHooker<Constructor<T>> hooker) {
        return doHook(origin, PRIORITY_DEFAULT, hooker);
    }

    @Override
    @Nullable
    public <T> MethodUnhooker<AfterHooker<Constructor<T>>, Constructor<T>> hookAfter(@NonNull Constructor<T> origin, @NonNull AfterHooker<Constructor<T>> hooker) {
        return doHook(origin, PRIORITY_DEFAULT, hooker);
    }

    @Override
    @Nullable
    public <T> MethodUnhooker<Hooker<Constructor<T>>, Constructor<T>> hook(@NonNull Constructor<T> origin, @NonNull Hooker<Constructor<T>> hooker) {
        return doHook(origin, PRIORITY_DEFAULT, hooker);
    }

    @Override
    @Nullable
    public <T> MethodUnhooker<BeforeHooker<Constructor<T>>, Constructor<T>> hookBefore(@NonNull Constructor<T> origin, int priority, @NonNull BeforeHooker<Constructor<T>> hooker) {
        return doHook(origin, priority, hooker);
    }

    @Override
    @Nullable
    public <T> MethodUnhooker<AfterHooker<Constructor<T>>, Constructor<T>> hookAfter(@NonNull Constructor<T> origin, int priority, @NonNull AfterHooker<Constructor<T>> hooker) {
        return doHook(origin, priority, hooker);
    }

    @Override
    @Nullable
    public <T> MethodUnhooker<Hooker<Constructor<T>>, Constructor<T>> hook(@NonNull Constructor<T> origin, int priority, @NonNull Hooker<Constructor<T>> hooker) {
        return doHook(origin, priority, hooker);
    }

    private static boolean doDeoptimize(@NonNull Executable method) {
        if (Modifier.isAbstract(method.getModifiers())) {
            throw new IllegalArgumentException("Cannot deoptimize abstract methods: " + method);
        } else if (Proxy.isProxyClass(method.getDeclaringClass())) {
            throw new IllegalArgumentException("Cannot deoptimize methods from proxy class: " + method);
        }
        return HookBridge.deoptimizeMethod(method);
    }

    @Override
    public boolean deoptimize(@NonNull Method method) {
        return doDeoptimize(method);
    }

    @Override
    public <T> boolean deoptimize(@NonNull Constructor<T> constructor) {
        return doDeoptimize(constructor);
    }

    @Override
    public synchronized void log(@NonNull String message) {
        Log.i(TAG, mPackageName + ": " + message);
    }

    @Override
    public synchronized void log(@NonNull String message, @NonNull Throwable throwable) {
        Log.e(TAG, mPackageName + ": " + message, throwable);
    }

    @Nullable
    @Override
    public DexParser parseDex(ByteBuffer dexData) throws IOException {
        return null;
    }
}
