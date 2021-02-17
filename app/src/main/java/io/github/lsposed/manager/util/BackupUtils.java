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

package io.github.lsposed.manager.util;

import android.content.Context;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class BackupUtils {
    private static final int VERSION = 1;

    public static boolean backup(Context context, Uri uri) {
        return backup(context, uri, null);
    }

    public static boolean backup(Context context, Uri uri, String packageName) {
        try {
            JSONObject rootObject = new JSONObject();
            rootObject.put("version", VERSION);
            JSONArray modulesArray = new JSONArray();
            Map<String, ModuleUtil.InstalledModule> modules = ModuleUtil.getInstance().getModules();
            for (ModuleUtil.InstalledModule module : modules.values()) {
                if (packageName != null && !module.packageName.equals(packageName)) {
                    continue;
                }
                JSONObject moduleObject = new JSONObject();
                moduleObject.put("enable", ModuleUtil.getInstance().isModuleEnabled(module.packageName));
                moduleObject.put("package", module.packageName);
                //List<String> scope = AppHelper.getScopeList(module.packageName);
                JSONArray scopeArray = new JSONArray();
                //for (String s : scope) {
                //    scopeArray.put(s);
                //}
                moduleObject.put("scope", scopeArray);
                modulesArray.put(moduleObject);
            }
            rootObject.put("modules", modulesArray);
            OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream);
            gzipOutputStream.write(rootObject.toString().getBytes());
            gzipOutputStream.close();
            outputStream.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean restore(Context context, Uri uri) {
        return restore(context, uri, null);
    }

    public static boolean restore(Context context, Uri uri, String packageName) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream, 32);
            StringBuilder string = new StringBuilder();
            byte[] data = new byte[32];
            int bytesRead;
            while ((bytesRead = gzipInputStream.read(data)) != -1) {
                string.append(new String(data, 0, bytesRead));
            }
            gzipInputStream.close();
            inputStream.close();
            JSONObject rootObject = new JSONObject(string.toString());
            if (rootObject.getInt("version") == VERSION) {
                JSONArray modules = rootObject.getJSONArray("modules");
                for (int i = 0; i < modules.length(); i++) {
                    JSONObject moduleObject = modules.getJSONObject(i);
                    String name = moduleObject.getString("package");
                    if (packageName != null && !name.equals(packageName)) {
                        continue;
                    }
                    ModuleUtil.InstalledModule module = ModuleUtil.getInstance().getModule(name);
                    if (module != null) {
                        ModuleUtil.getInstance().setModuleEnabled(name, moduleObject.getBoolean("enable"));
                        JSONArray scopeArray = moduleObject.getJSONArray("scope");
                        List<String> scope = new ArrayList<>();
                        for (int j = 0; j < scopeArray.length(); j++) {
                            scope.add(scopeArray.getString(j));
                        }
                        //AppHelper.saveScopeList(name, scope);
                    }
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
