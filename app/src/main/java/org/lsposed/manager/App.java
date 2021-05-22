/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2020 EdXposed Contributors
 * Copyright (C) 2021 LSPosed Contributors
 */

package org.lsposed.manager;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import org.lsposed.hiddenapibypass.HiddenApiBypass;
import org.lsposed.manager.repo.RepoLoader;
import org.lsposed.manager.ui.activity.CrashReportActivity;
import org.lsposed.manager.util.DoHDNS;
import org.lsposed.manager.util.theme.ThemeUtil;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import rikka.material.app.DayNightDelegate;

public class App extends Application {

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // TODO: set specific class name instead of `L`
            HiddenApiBypass.addHiddenApiExemptions("L");
        }
    }

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
        if ("CN".equals(Locale.getDefault().getCountry())) {
            if (!pref.contains("doh")) {
                pref.edit().putBoolean("doh", true).apply();
            }
        }
        DayNightDelegate.setApplicationContext(this);
        DayNightDelegate.setDefaultNightMode(ThemeUtil.getDarkTheme());
        RepoLoader.getInstance().loadRemoteData();
    }

    @NonNull
    public static OkHttpClient getOkHttpClient() {
        if (okHttpClient == null) {
            OkHttpClient.Builder builder = new OkHttpClient.Builder().cache(getOkHttpCache());
            builder.addInterceptor(chain -> {
                var request = chain.request().newBuilder();
                request.header("User-Agent", TAG);
                return chain.proceed(request.build());
            });
            HttpLoggingInterceptor.Logger logger = s -> Log.v(TAG, s);
            HttpLoggingInterceptor log = new HttpLoggingInterceptor(logger);
            log.setLevel(HttpLoggingInterceptor.Level.HEADERS);
            if (BuildConfig.DEBUG) builder.addInterceptor(log);
            okHttpClient = builder.dns(new DoHDNS(builder.build())).build();
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
