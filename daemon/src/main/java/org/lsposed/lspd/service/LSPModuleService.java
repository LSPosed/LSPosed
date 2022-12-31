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
import android.os.RemoteException;
import android.util.Log;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import hidden.HiddenApiBridge;
import io.github.libxposed.service.XposedService;

public class LSPModuleService extends XposedService {

    private static final String TAG = "LSPosedModuleService";

    private final Set<Integer> uidSet = ConcurrentHashMap.newKeySet();
    private final IUidObserver uidObserver = new IUidObserver.Stub() {
        @Override
        public void onUidActive(int uid) {
            uidStarts(uid);
        }

        @Override
        public void onUidCachedChanged(int uid, boolean cached) {
            if (!cached) uidStarts(uid);
        }

        @Override
        public void onUidIdle(int uid, boolean disabled) {
            uidStarts(uid);
        }

        @Override
        public void onUidGone(int uid, boolean disabled) {
            uidSet.remove(uid);
        }
    };

    void registerObserver() {
        uidSet.clear();
        int flags = HiddenApiBridge.ActivityManager_UID_OBSERVER_ACTIVE()
                | HiddenApiBridge.ActivityManager_UID_OBSERVER_GONE()
                | HiddenApiBridge.ActivityManager_UID_OBSERVER_IDLE()
                | HiddenApiBridge.ActivityManager_UID_OBSERVER_CACHED();
        try {
            ActivityManagerService.registerUidObserver(uidObserver, flags, HiddenApiBridge.ActivityManager_PROCESS_STATE_UNKNOWN(), null);
            Log.i(TAG, "registered uid observer");
        } catch (RemoteException e) {
            Log.e(TAG, "failed to register uid observer", e);
        }
    }

    private void uidStarts(int uid) {
        if (!uidSet.contains(uid)) {
            uidSet.add(uid);
            var module = ConfigManager.getInstance().getModule(uid);
            if (module != null) sendBinder(uid, module);
        }
    }

    private void sendBinder(int uid, String name) {
        try {
            int userId = uid / PackageService.PER_USER_RANGE;
            var authority = name + AUTHORITY_SUFFIX;
            var provider = ActivityManagerService.getContentProvider(authority, userId);
            if (provider == null) {
                Log.d(TAG, "no service provider for " + name);
                return;
            }
            var extra = new Bundle();
            extra.putBinder("binder", asBinder());
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
        } catch (RemoteException | NoSuchMethodError e) {
            Log.w(TAG, "failed to send module binder for uid " + uid, e);
        }
    }
}
