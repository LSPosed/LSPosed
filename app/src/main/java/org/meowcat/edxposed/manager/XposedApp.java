package org.meowcat.edxposed.manager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.meowcat.edxposed.manager.receivers.PackageChangeReceiver;
import org.meowcat.edxposed.manager.util.ModuleUtil;
import org.meowcat.edxposed.manager.util.NotificationUtil;
import org.meowcat.edxposed.manager.util.RepoLoader;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

import de.robv.android.xposed.installer.util.InstallZipUtil;

public class XposedApp extends de.robv.android.xposed.installer.XposedApp implements Application.ActivityLifecycleCallbacks {
    public static final String TAG = "EdXposedManager";
    public static String BASE_DIR = null;
    public static String ENABLED_MODULES_LIST_FILE = null;
    private static String BASE_DIR_LEGACY = null;
    @SuppressLint("StaticFieldLeak")
    private static XposedApp instance = null;
    private static Thread uiThread;
    private static Handler mainHandler;
    private SharedPreferences pref;
    private AppCompatActivity currentActivity = null;
    private boolean isUiLoaded = false;

    public static XposedApp getInstance() {
        return instance;
    }

    public static InstallZipUtil.XposedProp getXposedProp() {
        return de.robv.android.xposed.installer.XposedApp.getInstance().xposedProp;
    }

    public static void runOnUiThread(Runnable action) {
        if (Thread.currentThread() != uiThread) {
            mainHandler.post(action);
        } else {
            action.run();
        }
    }

    public static Integer getXposedVersion() {
        return getActiveXposedVersion();
    }

    public static SharedPreferences getPreferences() {
        return instance.pref;
    }

    public static void mkdirAndChmod(String dir, int permissions) {
        dir = BASE_DIR + dir;
        //noinspection ResultOfMethodCallIgnored
        new File(dir).mkdir();
        FileUtils.setPermissions(dir, permissions, -1, -1);
    }

    public void onCreate() {
        super.onCreate();
        if (!BuildConfig.DEBUG) {
            try {
                Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {

                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    throwable.printStackTrace(pw);
                    String stackTraceString = sw.toString();

                    //Reduce data to 128KB so we don't get a TransactionTooLargeException when sending the intent.
                    //The limit is 1MB on Android but some devices seem to have it lower.
                    //See: http://developer.android.com/reference/android/os/TransactionTooLargeException.html
                    //And: http://stackoverflow.com/questions/11451393/what-to-do-on-transactiontoolargeexception#comment46697371_12809171
                    if (stackTraceString.length() > 131071) {
                        String disclaimer = " [stack trace too large]";
                        stackTraceString = stackTraceString.substring(0, 131071 - disclaimer.length()) + disclaimer;
                    }
                    Intent intent = new Intent(XposedApp.this, CrashReportActivity.class);
                    intent.putExtra(BuildConfig.APPLICATION_ID + ".EXTRA_STACK_TRACE", stackTraceString);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    XposedApp.this.startActivity(intent);
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(10);
                });
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        final ApplicationInfo appInfo = getApplicationInfo();
        BASE_DIR_LEGACY = appInfo.dataDir;
        BASE_DIR = appInfo.deviceProtectedDataDir + "/";
        ENABLED_MODULES_LIST_FILE = BASE_DIR + "conf/enabled_modules.list";

        instance = this;
        uiThread = Thread.currentThread();
        mainHandler = new Handler();

        pref = PreferenceManager.getDefaultSharedPreferences(this);

        de.robv.android.xposed.installer.XposedApp.getInstance().reloadXposedProp();
        createDirectories();
        NotificationUtil.init();
        registerReceivers();

        registerActivityLifecycleCallbacks(this);

        @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        Date date = new Date();

        if (!Objects.requireNonNull(pref.getString("date", "")).equals(dateFormat.format(date))) {
            pref.edit().putString("date", dateFormat.format(date)).apply();

            try {
                Log.i(TAG, String.format("EdXposedManager - %s - %s", BuildConfig.VERSION_CODE, getPackageManager().getPackageInfo(getPackageName(), 0).versionName));
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }

        RepoLoader.getInstance().triggerFirstLoadIfNecessary();
    }

    private void registerReceivers() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme("package");
        registerReceiver(new PackageChangeReceiver(), filter);

        PendingIntent.getBroadcast(this, 0,
                new Intent(this, PackageChangeReceiver.class), 0);
    }

    @SuppressWarnings({"JavaReflectionMemberAccess", "OctalInteger"})
    @SuppressLint({"PrivateApi", "NewApi"})
    private void createDirectories() {
        FileUtils.setPermissions(BASE_DIR, 00777, -1, -1);
        mkdirAndChmod("conf", 00777);
        mkdirAndChmod("log", 00777);

        try {
            @SuppressLint("SoonBlockedPrivateApi") Method deleteDir = FileUtils.class.getDeclaredMethod("deleteContentsAndDir", File.class);
            deleteDir.invoke(null, new File(BASE_DIR_LEGACY, "bin"));
            deleteDir.invoke(null, new File(BASE_DIR_LEGACY, "conf"));
            deleteDir.invoke(null, new File(BASE_DIR_LEGACY, "log"));
        } catch (ReflectiveOperationException e) {
            Log.w(de.robv.android.xposed.installer.XposedApp.TAG, "Failed to delete obsolete directories", e);
        }
    }

    public void updateProgressIndicator(final SwipeRefreshLayout refreshLayout) {
        final boolean isLoading = RepoLoader.getInstance().isLoading() || ModuleUtil.getInstance().isLoading();
        runOnUiThread(() -> {
            synchronized (XposedApp.this) {
                if (currentActivity != null) {
                    if (refreshLayout != null)
                        refreshLayout.setRefreshing(isLoading);
                }
            }
        });
    }

    @Override
    public synchronized void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
        if (isUiLoaded) {
            return;
        }

        RepoLoader.getInstance().triggerFirstLoadIfNecessary();
        isUiLoaded = true;
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {

    }

    @Override
    public synchronized void onActivityResumed(@NonNull Activity activity) {
        currentActivity = (AppCompatActivity) activity;
        updateProgressIndicator(null);
    }

    @Override
    public synchronized void onActivityPaused(Activity activity) {
        currentActivity = null;
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {

    }
}
