package org.meowcat.edxposed.manager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import org.meowcat.edxposed.manager.ui.activity.CrashReportActivity;
import org.meowcat.edxposed.manager.util.CompileUtil;
import org.meowcat.edxposed.manager.util.NotificationUtil;
import org.meowcat.edxposed.manager.util.RebootUtil;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

import rikka.shizuku.Shizuku;
import rikka.sui.Sui;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class App extends Application implements Application.ActivityLifecycleCallbacks {
    public static final String TAG = "EdXposedManager";
    @SuppressLint("StaticFieldLeak")
    private static App instance = null;
    private static Thread uiThread;
    private static Handler mainHandler;
    private SharedPreferences pref;
    //private AppCompatActivity currentActivity = null;
    private final boolean isUiLoaded = false;

    private final Shizuku.OnRequestPermissionResultListener REQUEST_PERMISSION_RESULT_LISTENER = this::onRequestPermissionsResult;

    static {
        Sui.init(BuildConfig.APPLICATION_ID);
    }

    private void onRequestPermissionsResult(int requestCode, int grantResult) {
        if (requestCode < 10) {
            RebootUtil.onRequestPermissionsResult(requestCode, grantResult);
        } else {
            CompileUtil.onRequestPermissionsResult(requestCode, grantResult);
        }
    }

    public static int checkPermission(int code) {
        try {
            if (!Shizuku.isPreV11() && Shizuku.getVersion() >= 11) {
                if (Shizuku.checkSelfPermission() == PERMISSION_GRANTED) {
                    return 0;
                } else if (Shizuku.shouldShowRequestPermissionRationale()) {
                    return -1;
                } else {
                    Shizuku.requestPermission(code);
                    return -1;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return -2;
    }

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

    public static void mkdir(String dir) {
        dir = Constants.getBaseDir() + dir;
        //noinspection ResultOfMethodCallIgnored
        new File(dir).mkdir();
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
        mainHandler = new Handler(Looper.getMainLooper());

        pref = PreferenceManager.getDefaultSharedPreferences(this);

        createDirectories();
        NotificationUtil.init();

        Shizuku.addRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER);
    }

    @SuppressLint({"PrivateApi", "NewApi"})
    private void createDirectories() {
        mkdir("conf");
        mkdir("log");
    }

    @Override
    public synchronized void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
        if (isUiLoaded) {
            return;
        }
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {

    }

    @Override
    public synchronized void onActivityResumed(@NonNull Activity activity) {

    }

    @Override
    public synchronized void onActivityPaused(@NonNull Activity activity) {

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
