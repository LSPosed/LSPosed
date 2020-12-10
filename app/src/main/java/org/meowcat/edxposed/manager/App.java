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
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.meowcat.edxposed.manager.adapters.AppHelper;
import org.meowcat.edxposed.manager.receivers.PackageChangeReceiver;
import org.meowcat.edxposed.manager.ui.activity.CrashReportActivity;
import org.meowcat.edxposed.manager.util.FileUtils;
import org.meowcat.edxposed.manager.util.ModuleUtil;
import org.meowcat.edxposed.manager.util.NotificationUtil;
import org.meowcat.edxposed.manager.util.RepoLoader;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Objects;

public class App extends Application implements Application.ActivityLifecycleCallbacks {
    public static final String TAG = "EdXposedManager";
    @SuppressLint("StaticFieldLeak")
    private static App instance = null;
    private static Thread uiThread;
    private static Handler mainHandler;
    private SharedPreferences pref;
    private AppCompatActivity currentActivity = null;
    private boolean isUiLoaded = false;

    public static App getInstance() {
        return instance;
    }

    public static void runOnUiThread(Runnable action) {
        if (Thread.currentThread() != uiThread) {
            mainHandler.post(action);
        } else {
            action.run();
        }
    }

    public static SharedPreferences getPreferences() {
        return instance.pref;
    }

    public static void mkdirAndChmod(String dir, int permissions) {
        dir = Constants.getBaseDir() + dir;
        //noinspection ResultOfMethodCallIgnored
        new File(dir).mkdir();
        FileUtils.setPermissions(dir, permissions);
    }

    public static boolean supportScope() {
        return Constants.getXposedApiVersion() >= 92;
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
                    Intent intent = new Intent(App.this, CrashReportActivity.class);
                    intent.putExtra(BuildConfig.APPLICATION_ID + ".EXTRA_STACK_TRACE", stackTraceString);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    App.this.startActivity(intent);
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(10);
                });
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        instance = this;
        uiThread = Thread.currentThread();
        mainHandler = new Handler();

        pref = PreferenceManager.getDefaultSharedPreferences(this);

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

    @SuppressWarnings({"OctalInteger"})
    @SuppressLint({"PrivateApi", "NewApi"})
    private void createDirectories() {
        FileUtils.setPermissions(Constants.getBaseDir(), 00777);
        mkdirAndChmod("conf", 00777);
        mkdirAndChmod("log", 00777);
    }

    public void updateProgressIndicator(final SwipeRefreshLayout refreshLayout) {
        final boolean isLoading = RepoLoader.getInstance().isLoading() || ModuleUtil.getInstance().isLoading();
        runOnUiThread(() -> {
            synchronized (App.this) {
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

        if (pref.getBoolean("hook_modules", true)) {
            Collection<ModuleUtil.InstalledModule> installedModules = ModuleUtil.getInstance().getModules().values();
            for (ModuleUtil.InstalledModule info : installedModules) {
                if (!AppHelper.FORCE_WHITE_LIST_MODULE.contains(info.packageName)) {
                    AppHelper.FORCE_WHITE_LIST_MODULE.add(info.packageName);
                }
            }
            Log.d(TAG, "ApplicationList: Force add modules to list");
        }
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
    public synchronized void onActivityPaused(@NonNull Activity activity) {
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
