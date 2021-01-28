package io.github.lsposed.manager;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import androidx.preference.PreferenceManager;

import io.github.lsposed.manager.ui.activity.CrashReportActivity;
import io.github.lsposed.manager.util.CompileUtil;
import io.github.lsposed.manager.util.NotificationUtil;
import io.github.lsposed.manager.util.RebootUtil;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

import rikka.shizuku.Shizuku;
import rikka.sui.Sui;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class App extends Application {
    public static final String TAG = "LSPosedManager";
    @SuppressLint("StaticFieldLeak")
    private static App instance = null;
    private static Thread uiThread;
    private static Handler mainHandler;
    private SharedPreferences pref;

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
        //noinspection ResultOfMethodCallIgnored
        new File(Constants.getBaseDir() + dir).mkdir();
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

        master();
        NotificationUtil.init();

        Shizuku.addRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER);
    }

    private void master() {
        // This will affect the fengshui of the whole app, don't remove this
        Constants.getXposedVersionCode();
        Constants.getXposedVersion();
        Constants.getXposedApiVersion();
        Constants.getXposedVariant();
        Constants.getBaseDir();
        Constants.getModulesListFile();
        Constants.getEnabledModulesListFile();
    }

}
