package org.lsposed.manager.util;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.lsposed.manager.App;
import org.lsposed.manager.BuildConfig;
import org.lsposed.manager.ConfigManager;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import okio.Okio;
import rikka.material.app.LocaleDelegate;

public class UpdateUtil {
    public static void loadRemoteVersion() {
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
                String api = ConfigManager.isBinderAlive() ? ConfigManager.getApi() : "riru";
                try {
                    var info = JsonParser.parseReader(body.charStream()).getAsJsonObject();
                    var notes = info.get("body").getAsString();
                    var assetsArray = info.getAsJsonArray("assets");
                    for (var assets : assetsArray) {
                        checkAssets(assets.getAsJsonObject(), notes, api.toLowerCase(LocaleDelegate.getDefaultLocale()));
                    }
                } catch (Throwable t) {
                    Log.e(App.TAG, t.getMessage(), t);
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(App.TAG, "loadRemoteVersion: " + e.getMessage());
                var pref = App.getPreferences();
                if (pref.getBoolean("checked", false)) return;
                pref.edit().putBoolean("checked", true).apply();
            }
        };
        App.getOkHttpClient().newCall(request).enqueue(callback);
    }

    private static void checkAssets(JsonObject assets, String releaseNotes, String api) {
        var pref = App.getPreferences();
        var name = assets.get("name").getAsString();
        var splitName = name.split("-");
        if (!splitName[3].equals(api)) return;
        pref.edit()
                .putInt("latest_version", Integer.parseInt(splitName[2]))
                .putLong("latest_check", Instant.now().getEpochSecond())
                .putString("release_notes", releaseNotes)
                .putBoolean("checked", true)
                .apply();
        var updatedAt = Instant.parse(assets.get("updated_at").getAsString());
        var downloadUrl = assets.get("browser_download_url").getAsString();
        var nowZipTime = pref.getLong("zip_time", 0);
        if (updatedAt.isAfter(Instant.ofEpochSecond(nowZipTime))) {
            var zip = downloadNewZipSync(downloadUrl, name);
            var size = assets.get("size").getAsLong();
            if (zip != null && zip.length() == size) {
                pref.edit()
                        .putLong("zip_time", updatedAt.getEpochSecond())
                        .putString("zip_file", zip.getAbsolutePath())
                        .apply();
            }
        }
    }

    public static boolean needUpdate() {
        var pref = App.getPreferences();
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

    @Nullable
    private static File downloadNewZipSync(String url, String name) {
        var request = new Request.Builder().url(url).build();
        var zip = new File(App.getInstance().getCacheDir(), name);
        try (Response response = App.getOkHttpClient().newCall(request).execute()) {
            var body = response.body();
            if (!response.isSuccessful() || body == null) return null;
            try (var source = body.source();
                 var sink = Okio.buffer(Okio.sink(zip))) {
                sink.writeAll(source);
            }
        } catch (IOException e) {
            Log.e(App.TAG, "downloadNewZipSync: " + e.getMessage());
            return null;
        }
        return zip;
    }

    public static boolean canInstall() {
        if (!ConfigManager.isBinderAlive()) return false;
        var pref = App.getPreferences();
        var zip = pref.getString("zip_file", null);
        return zip != null && new File(zip).isFile();
    }
}
