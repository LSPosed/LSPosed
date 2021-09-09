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

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Process;
import android.system.Os;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.google.gson.JsonParser;

import org.lsposed.hiddenapibypass.HiddenApiBypass;
import org.lsposed.manager.repo.RepoLoader;
import org.lsposed.manager.ui.activity.CrashReportActivity;
import org.lsposed.manager.util.DoHDNS;
import org.lsposed.manager.util.theme.ThemeUtil;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import rikka.material.app.DayNightDelegate;
import rikka.material.app.LocaleDelegate;

public class App extends Application {

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // TODO: set specific class name
            HiddenApiBypass.addHiddenApiExemptions("");
        }
    }

    public static final String TAG = "LSPosedManager";
    private static App instance = null;
    private static OkHttpClient okHttpClient;
    private static Cache okHttpCache;
    private SharedPreferences pref;
    private ExecutorService executorService;

    public static App getInstance() {
        return instance;
    }

    public static SharedPreferences getPreferences() {
        return instance.pref;
    }

    public static ExecutorService getExecutorService() {
        return instance.executorService;
    }

    private void setCrashReport() {
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
            System.exit(10);
            Process.killProcess(Os.getpid());
        });
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (!BuildConfig.DEBUG) {
            setCrashReport();
        }

        instance = this;

        executorService = Executors.newCachedThreadPool();

        pref = PreferenceManager.getDefaultSharedPreferences(this);
        if ("CN".equals(Locale.getDefault().getCountry())) {
            if (!pref.contains("doh")) {
                pref.edit().putBoolean("doh", true).apply();
            }
        }
        DayNightDelegate.setApplicationContext(this);
        DayNightDelegate.setDefaultNightMode(ThemeUtil.getDarkTheme());
        LocaleDelegate.setDefaultLocale(getLocale());

        loadRemoteVersion();
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
            HttpLoggingInterceptor log = new HttpLoggingInterceptor();
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

    private void loadRemoteVersion() {
        var request = new Request.Builder()
                .url("https://api.github.com/repos/LSPosed/LSPosed/releases/latest")
                .addHeader("Accept", "application/vnd.github.v3+json")
                .build();
        var callback = new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (!response.isSuccessful()) return;
                var body = response.body();
                if (body == null) return;
                try {
                    var info = JsonParser.parseReader(body.charStream()).getAsJsonObject();
                    var name = info.getAsJsonArray("assets").get(0).getAsJsonObject().get("name").getAsString();
                    var code = Integer.parseInt(name.split("-", 4)[2]);
                    var now = Instant.now().getEpochSecond();
                    pref.edit()
                            .putInt("latest_version", code)
                            .putLong("latest_check", now)
                            .putBoolean("checked", true)
                            .apply();
                } catch (Throwable t) {
                    Log.e(App.TAG, t.getMessage(), t);
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(App.TAG, "loadRemoteVersion: " + e.getMessage());
                if (pref.getBoolean("checked", false)) return;
                pref.edit().putBoolean("checked", true).apply();
            }
        };
        getOkHttpClient().newCall(request).enqueue(callback);
    }

    public static boolean needUpdate() {
        var pref = getPreferences();
        if (!pref.getBoolean("checked", false)) return false;
        var now = Instant.now();
        var buildTime = Instant.ofEpochSecond(BuildConfig.BUILD_TIME);
        var check = pref.getLong("latest_check", 0);
        if (check > 0) {
            var checkTime = Instant.ofEpochSecond(check);
            if (checkTime.atOffset(ZoneOffset.UTC).plusDays(30).toInstant().isBefore(now))
                return true;
            var code = pref.getInt("latest_version", 0);
            return code > BuildConfig.VERSION_CODE;
        }
        return buildTime.atOffset(ZoneOffset.UTC).plusDays(30).toInstant().isBefore(now);
    }

    public static Locale getLocale() {
        String tag = getPreferences().getString("language", null);
        if (TextUtils.isEmpty(tag) || "SYSTEM".equals(tag)) {
            return Locale.getDefault();
        }
        return Locale.forLanguageTag(tag);
    }
}
