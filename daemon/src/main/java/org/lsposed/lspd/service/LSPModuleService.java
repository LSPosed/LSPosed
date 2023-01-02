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
 * Copyright (C) 2021 LSPosed Contributors
 */

package org.lsposed.lspd.service;

import android.app.IUidObserver;
import android.content.AttributionSource;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.github.libxposed.service.IXposedService;

public class LSPModuleService extends IXposedService.Stub {

    private final static String TAG = "LSPosedModuleService";


    private final static Set<Integer> uidSet = ConcurrentHashMap.newKeySet();
    private final static Map<Integer, LSPModuleService> serviceMap = new ConcurrentHashMap<>();

    private final int uid;
    private final String packageName;

    static void uidStarts(int uid) {
        if (!uidSet.contains(uid)) {
            uidSet.add(uid);
            sendBinder(getService(uid));
        }
    }

    static void uidGone(int uid) {
        uidSet.remove(uid);
    }

    private static void sendBinder(LSPModuleService service) {
        if (service == null) return;
        var uid = service.uid;
        var name = service.packageName;
        try {
            int userId = uid / PackageService.PER_USER_RANGE;
            var authority = name + AUTHORITY_SUFFIX;
            var provider = ActivityManagerService.getContentProvider(authority, userId);
            if (provider == null) {
                Log.d(TAG, "no service provider for " + name);
                return;
            }
            var extra = new Bundle();
            extra.putBinder("binder", service.asBinder());
            Bundle reply = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                reply = provider.call(new AttributionSource.Builder(1000).setPackageName("android").build(),
                        authority, SEND_BINDER, null, extra);
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
                reply = provider.call("android", null, authority, SEND_BINDER, null, extra);
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                reply = provider.call("android", authority, SEND_BINDER, null, extra);
            }
            if (reply != null) {
                Log.d(TAG, "sent module binder to " + name);
            } else {
                Log.w(TAG, "failed to send module binder to " + name);
            }
        } catch (Throwable e) {
            Log.w(TAG, "failed to send module binder for uid " + uid, e);
        }
    }

    public static LSPModuleService getService(int uid) {
        var module = ConfigManager.getInstance().getModule(uid);
        if (module == null) return null;
        return serviceMap.computeIfAbsent(uid, __ -> new LSPModuleService(module, uid));
    }

    public static void removeService(int uid) {
        serviceMap.remove(uid);
    }

    private LSPModuleService(String name, int uid) {
        this.uid = uid;
        this.packageName = name;
    }

    @Override
    public long getAPIVersion() {
        return API;
    }
}
