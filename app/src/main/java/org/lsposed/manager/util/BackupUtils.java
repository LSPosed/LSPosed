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

package org.lsposed.manager.util;

import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lsposed.manager.App;
import org.lsposed.manager.ConfigManager;
import org.lsposed.manager.adapters.ScopeAdapter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import rikka.core.os.FileUtils;

public class BackupUtils {
    private static final int VERSION = 2;

    public static void backup(Uri uri) throws JSONException, IOException {
        backup(uri, null);
    }

    public static void backup(Uri uri, String packageName) throws IOException, JSONException {
        JSONObject rootObject = new JSONObject();
        rootObject.put("version", VERSION);
        JSONArray modulesArray = new JSONArray();
        var modules = ModuleUtil.getInstance().getModules();
        if (modules == null) return;
        for (ModuleUtil.InstalledModule module : modules.values()) {
            if (packageName != null && !module.packageName.equals(packageName)) {
                continue;
            }
            JSONObject moduleObject = new JSONObject();
            moduleObject.put("enable", ModuleUtil.getInstance().isModuleEnabled(module.packageName));
            moduleObject.put("package", module.packageName);
            List<ScopeAdapter.ApplicationWithEquals> scope = ConfigManager.getModuleScope(module.packageName);
            JSONArray scopeArray = new JSONArray();
            for (ScopeAdapter.ApplicationWithEquals s : scope) {
                JSONObject app = new JSONObject();
                app.put("package", s.packageName);
                app.put("userId", s.userId);
                scopeArray.put(app);
            }
            moduleObject.put("scope", scopeArray);
            modulesArray.put(moduleObject);
        }
        rootObject.put("modules", modulesArray);
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(App.getInstance().getContentResolver().openOutputStream(uri))) {
            gzipOutputStream.write(rootObject.toString().getBytes());
        }
    }

    public static void restore(Uri uri) throws JSONException, IOException {
        restore(uri, null);
    }

    public static void restore(Uri uri, String packageName) throws IOException, JSONException {
        try (GZIPInputStream gzipInputStream = new GZIPInputStream(App.getInstance().getContentResolver().openInputStream(uri), 32)) {
            StringBuilder string = new StringBuilder();
            try (var os = new ByteArrayOutputStream()) {
                FileUtils.copy(gzipInputStream, os);
                string.append(os);
            }
            gzipInputStream.close();
            JSONObject rootObject = new JSONObject(string.toString());
            int version = rootObject.getInt("version");
            if (version == VERSION || version == 1) {
                JSONArray modules = rootObject.getJSONArray("modules");
                for (int i = 0; i < modules.length(); i++) {
                    JSONObject moduleObject = modules.getJSONObject(i);
                    String name = moduleObject.getString("package");
                    if (packageName != null && !name.equals(packageName)) {
                        continue;
                    }
                    ModuleUtil.InstalledModule module = ModuleUtil.getInstance().getModule(name);
                    if (module != null) {
                        var enabled = moduleObject.getBoolean("enable");
                        ModuleUtil.getInstance().setModuleEnabled(name, enabled);
                        if (!enabled) continue;
                        JSONArray scopeArray = moduleObject.getJSONArray("scope");
                        HashSet<ScopeAdapter.ApplicationWithEquals> scope = new HashSet<>();
                        for (int j = 0; j < scopeArray.length(); j++) {
                            if (version == VERSION) {
                                JSONObject app = scopeArray.getJSONObject(j);
                                scope.add(new ScopeAdapter.ApplicationWithEquals(app.getString("package"), app.getInt("userId")));
                            } else {
                                scope.add(new ScopeAdapter.ApplicationWithEquals(scopeArray.getString(j), 0));
                            }
                        }
                        ConfigManager.setModuleScope(name, module.legacy, scope);
                    }
                }
            } else {
                throw new IllegalArgumentException("Unknown backup file version");
            }
        }
    }
}
