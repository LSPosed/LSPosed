package io.github.lsposed.manager;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

import io.github.lsposed.manager.repo.RepoLoader;
import io.github.lsposed.manager.ui.activity.CrashReportActivity;
import okhttp3.Cache;
import okhttp3.OkHttpClient;

public class App extends Application {
    public static final String TAG = "LSPosedManager";
    @SuppressLint("StaticFieldLeak")
    private static App instance = null;
    private static OkHttpClient okHttpClient;
    private static Cache okHttpCache;
    private SharedPreferences pref;

    public static App getInstance() {
        return instance;
    }

    public static SharedPreferences getPreferences() {
        return instance.pref;
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

        pref = PreferenceManager.getDefaultSharedPreferences(this);
        RepoLoader.getInstance().loadRemoteData();
    }

    @NonNull
    public static OkHttpClient getOkHttpClient() {
        if (okHttpClient == null) {
            okHttpClient = new OkHttpClient.Builder()
                    .cache(getOkHttpCache())
                    .build();
        }
        return okHttpClient;
    }

    @NonNull
    private static Cache getOkHttpCache() {
        if (okHttpCache == null) {
            okHttpCache = new Cache(new File(App.getInstance().getCacheDir(), "http_cache"), 50L * 1024L * 1024L);
        }
        return okHttpCache;
    }
}
