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

package io.github.lsposed.manager.repo;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.github.lsposed.manager.App;
import io.github.lsposed.manager.repo.model.OnlineModule;
import io.github.lsposed.manager.util.ModuleUtil;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class RepoLoader {
    private static RepoLoader instance = null;
    private OnlineModule[] onlineModules = new OnlineModule[0];
    private final Path repoFile = Paths.get(App.getInstance().getFilesDir().getAbsolutePath(), "repo.json");
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private boolean isLoading = false;

    public static synchronized RepoLoader getInstance() {
        if (instance == null) {
            instance = new RepoLoader();
            instance.loadRemoteData();
        }
        return instance;
    }

    public void loadRemoteData() {
        synchronized (this) {
            if (isLoading) {
                return;
            }
            isLoading = true;
        }
        App.getOkHttpClient().newCall(new Request.Builder()
                .url("https://modules.lsposed.org/modules.json")
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                synchronized (this) {
                    isLoading = false;
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                ResponseBody body = response.body();
                if (body != null) {
                    String bodyString = body.string();
                    Gson gson = new Gson();
                    onlineModules = gson.fromJson(bodyString, OnlineModule[].class);
                    Files.write(repoFile, bodyString.getBytes(StandardCharsets.UTF_8));
                }
                for (Listener listener : listeners) {
                    listener.repoLoaded();
                }
                synchronized (this) {
                    isLoading = false;
                }
            }
        });
    }

    public void addListener(Listener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public OnlineModule[] getOnlineModules() {
        return onlineModules;
    }

    public interface Listener {
        void repoLoaded();
    }
}
