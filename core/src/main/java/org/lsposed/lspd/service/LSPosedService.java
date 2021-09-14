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

import static android.content.pm.ShortcutManager.FLAG_MATCH_PINNED;
import static org.lsposed.lspd.service.PackageService.PER_USER_RANGE;
import static org.lsposed.lspd.service.ServiceManager.TAG;

import android.app.IApplicationThread;
import android.content.ComponentName;
import android.content.ContextWrapper;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IShortcutService;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.content.pm.ShortcutInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import org.lsposed.lspd.BuildConfig;
import org.lsposed.manager.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class LSPosedService extends ILSPosedService.Stub {
    private static final int AID_NOBODY = 9999;
    private static final int USER_NULL = -10000;
    private static final String SHORTCUT_ID = "org.lsposed.manager.shortcut";

    @Override
    public ILSPApplicationService requestApplicationService(int uid, int pid, String processName, IBinder heartBeat) {
        if (Binder.getCallingUid() != 1000) {
            Log.w(TAG, "Someone else got my binder!?");
            return null;
        }
        if (ServiceManager.getApplicationService().hasRegister(uid, pid)) {
            Log.d(TAG, "Skipped duplicated request for uid " + uid + " pid " + pid);
            return null;
        }
        if (!ServiceManager.getManagerService().shouldStartManager(pid, uid, processName) &&
                ConfigManager.getInstance().shouldSkipProcess(new ConfigManager.ProcessScope(processName, uid))) {
            Log.d(TAG, "Skipped " + processName + "/" + uid);
            return null;
        }
        Log.d(TAG, "returned service");
        return ServiceManager.requestApplicationService(uid, pid, heartBeat);
    }

    /**
     * This part is quite complex.
     * For modules, we never care about its user id, we only care about its apk path.
     * So we will only process module's removal when it's removed from all users.
     * And FULLY_REMOVE is exactly the one.
     * <p>
     * For applications, we care about its user id.
     * So we will process application's removal when it's removed from every single user.
     * However, PACKAGE_REMOVED will be triggered by `pm hide`, so we use UID_REMOVED instead.
     */

    synchronized public void dispatchPackageChanged(Intent intent) {
        if (intent == null) return;
        int uid = intent.getIntExtra(Intent.EXTRA_UID, AID_NOBODY);
        if (uid == AID_NOBODY || uid <= 0) return;
        int userId = intent.getIntExtra("android.intent.extra.user_handle", USER_NULL);
        if (userId == USER_NULL) userId = uid % PER_USER_RANGE;

        Uri uri = intent.getData();
        String moduleName = (uri != null) ? uri.getSchemeSpecificPart() : ConfigManager.getInstance().getModule(uid);

        ApplicationInfo applicationInfo = null;
        if (moduleName != null) {
            try {
                applicationInfo = PackageService.getApplicationInfo(moduleName, PackageManager.GET_META_DATA | PackageService.MATCH_ALL_FLAGS, 0);
            } catch (Throwable ignored) {
            }
        }

        boolean isXposedModule = applicationInfo != null &&
                applicationInfo.metaData != null &&
                applicationInfo.metaData.containsKey("xposedminversion");

        Log.d(TAG, "Package changed: uid=" + uid + " userId=" + userId + " action=" + intent.getAction() + " isXposedModule=" + isXposedModule);

        switch (intent.getAction()) {
            case Intent.ACTION_PACKAGE_FULLY_REMOVED: {
                // for module, remove module
                // because we only care about when the apk is gone
                if (moduleName != null)
                    if (ConfigManager.getInstance().removeModule(moduleName))
                        isXposedModule = true;
                break;
            }
            case Intent.ACTION_PACKAGE_ADDED:
            case Intent.ACTION_PACKAGE_CHANGED: {
                // make sure that the change is for the complete package, not only a
                // component
                String[] components = intent.getStringArrayExtra(Intent.EXTRA_CHANGED_COMPONENT_NAME_LIST);
                if (components != null && !Arrays.stream(components).reduce(false, (p, c) -> p || c.equals(moduleName), Boolean::logicalOr)) {
                    return;
                }
                // when package is changed, we may need to update cache (module cache or process cache)
                if (isXposedModule) {
                    ConfigManager.getInstance().updateCache();
                } else if (ConfigManager.getInstance().isUidHooked(uid)) {
                    // it will automatically remove obsolete app from database
                    ConfigManager.getInstance().updateAppCache();
                }
                break;
            }
            case Intent.ACTION_UID_REMOVED: {
                // when a package is removed (rather than hide) for a single user
                // (apk may still be there because of multi-user)
                if (isXposedModule) {
                    // it will automatically remove obsolete scope from database
                    ConfigManager.getInstance().updateCache();
                } else if (ConfigManager.getInstance().isUidHooked(uid)) {
                    // it will automatically remove obsolete app from database
                    ConfigManager.getInstance().updateAppCache();
                }
                break;
            }
        }
        if (isXposedModule) {
            Log.d(TAG, "module " + moduleName + " changed, dispatching to manager");
            var enabledModules = ConfigManager.getInstance().enabledModules();
            var scope = ConfigManager.getInstance().getModuleScope(moduleName);
            boolean systemModule = scope != null &&
                    scope.parallelStream().anyMatch(app -> app.packageName.equals("android"));
            boolean enabled = Arrays.asList(enabledModules).contains(moduleName);
            boolean removed = intent.getAction().equals(Intent.ACTION_PACKAGE_FULLY_REMOVED) ||
                    intent.getAction().equals(Intent.ACTION_UID_REMOVED);
            var action = enabled || removed ? "org.lsposed.action.MODULE_UPDATED" :
                    "org.lsposed.action.MODULE_NOT_ACTIVATAED";
            Intent broadcastIntent = new Intent(action);
            broadcastIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            broadcastIntent.addFlags(0x01000000);
            broadcastIntent.addFlags(0x00400000);
            broadcastIntent.setData(intent.getData());
            broadcastIntent.putExtras(intent.getExtras());
            broadcastIntent.putExtra(Intent.EXTRA_USER, userId);
            broadcastIntent.putExtra("systemModule", systemModule);
            var manager = ConfigManager.getInstance().getManagerPackageName();
            var component = ComponentName.createRelative(manager, ".receivers.ServiceReceiver");
            broadcastIntent.setComponent(component);

            try {
                ActivityManagerService.broadcastIntentWithFeature(null, broadcastIntent,
                        null, null, 0, null, null,
                        null, -1, null, true, false,
                        0);
            } catch (Throwable t) {
                Log.e(TAG, "Broadcast to manager failed: ", t);
            }
        }

        if (moduleName != null && ConfigManager.getInstance().isManager(moduleName) && userId == 0) {
            Log.d(TAG, "Manager updated");
            try {
                ConfigManager.getInstance().updateManager();
            } catch (Throwable e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }
    }


    synchronized public void dispatchBootCompleted(Intent intent) {
        ConfigManager.getInstance().ensureManager();
    }

    synchronized public void dispatchUserUnlocked(Intent intent) {
        try {
            var iss = IShortcutService.Stub.asInterface(android.os.ServiceManager.getService("shortcut"));
            while (!UserService.isUserUnlocked(0)) {
                Log.d(TAG, "user is not yet unlocked, waiting for 1s...");
                Thread.sleep(1000);
            }

            if (!iss.isRequestPinItemSupported(0, LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT)) {
                Log.d(TAG, "pinned shortcut not supported, skipping");
                return;
            }
            var shortcutIntent = PackageService.getLaunchIntentForPackage(BuildConfig.MANAGER_INJECTED_PKG_NAME);
            if (shortcutIntent == null) {
                var pkgInfo = PackageService.getPackageInfo(BuildConfig.MANAGER_INJECTED_PKG_NAME, PackageManager.GET_ACTIVITIES, 0);
                if (pkgInfo.activities != null && pkgInfo.activities.length > 0) {
                    for (var activityInfo : pkgInfo.activities) {
                        if (activityInfo.processName.equals(activityInfo.packageName)) {
                            shortcutIntent = new Intent();
                            shortcutIntent.setComponent(new ComponentName(activityInfo.packageName, activityInfo.name));
                            shortcutIntent.setAction(Intent.ACTION_MAIN);
                            break;
                        }
                    }
                }
            }
            shortcutIntent.addCategory("org.lsposed.manager.LAUNCH_MANAGER");
            var icon = ConfigFileManager.getResources().getDrawable(R.drawable.ic_launcher, ConfigFileManager.getResources().newTheme());
            var bitmap = Bitmap.createBitmap(icon.getIntrinsicWidth(), icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
            icon.draw(new Canvas(bitmap));
            var shortcut = new ShortcutInfo.Builder(new ContextWrapper(null) {
                @Override
                public String getPackageName() {
                    return "android";
                }

                public int getUserId() {
                    return 0;
                }
            }, SHORTCUT_ID)
                    .setShortLabel("LSPosed")
                    .setLongLabel("LSPosed")
                    .setIntent(shortcutIntent)
                    .setIcon(Icon.createWithBitmap(bitmap))
                    .build();

            for (var shortcutInfo : Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ? iss.getShortcuts("android", FLAG_MATCH_PINNED, 0).getList() : iss.getPinnedShortcuts("android", 0).getList()) {
                if (SHORTCUT_ID.equals(shortcutInfo.getId())) {
                    Log.d(TAG, "shortcut exists, updating");
                    iss.updateShortcuts("android", new ParceledListSlice<>(Collections.singletonList(shortcut)), 0);
                    return;
                }
            }

            iss.requestPinShortcut("android", shortcut, null, 0);

            Log.d(TAG, "done add shortcut");
        } catch (Throwable e) {
            Log.e(TAG, "add shortcut", e);
        }
    }

    private void registerPackageReceiver() {
        try {
            IntentFilter packageFilter = new IntentFilter();
            packageFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
            packageFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
            packageFilter.addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED);
            packageFilter.addDataScheme("package");

            IntentFilter uidFilter = new IntentFilter();
            uidFilter.addAction(Intent.ACTION_UID_REMOVED);

            var receiver = new IIntentReceiver.Stub() {
                @Override
                public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) {
                    new Thread(() -> dispatchPackageChanged(intent)).start();
                    try {
                        ActivityManagerService.finishReceiver(this, resultCode, data, extras, false, intent.getFlags());
                    } catch (Throwable e) {
                        Log.e(TAG, "finish receiver", e);
                    }
                }
            };

            ActivityManagerService.registerReceiver("android", null, receiver, packageFilter, null, -1, 0);
            ActivityManagerService.registerReceiver("android", null, receiver, uidFilter, null, -1, 0);
        } catch (Throwable e) {
            Log.e(TAG, "register package receiver", e);
        }
        Log.d(TAG, "registered package receiver");
    }

    private void registerBootReceiver() {
        try {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_LOCKED_BOOT_COMPLETED);

            ActivityManagerService.registerReceiver("android", null, new IIntentReceiver.Stub() {
                @Override
                public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) {
                    new Thread(() -> dispatchBootCompleted(intent)).start();
                    try {
                        ActivityManagerService.finishReceiver(this, resultCode, data, extras, false, intent.getFlags());
                    } catch (Throwable e) {
                        Log.e(TAG, "finish receiver", e);
                    }
                }
            }, intentFilter, null, 0, 0);
        } catch (Throwable e) {
            Log.e(TAG, "register boot receiver", e);
        }
        Log.d(TAG, "registered boot receiver");
    }

    private void registerUnlockReceiver() {
        try {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_USER_UNLOCKED);

            ActivityManagerService.registerReceiver("android", null, new IIntentReceiver.Stub() {
                @Override
                public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) {
                    new Thread(() -> dispatchUserUnlocked(intent)).start();
                    try {
                        ActivityManagerService.finishReceiver(this, resultCode, data, extras, false, intent.getFlags());
                    } catch (Throwable e) {
                        Log.e(TAG, "finish receiver", e);
                    }
                }
            }, intentFilter, null, 0, 0);
        } catch (Throwable e) {
            Log.e(TAG, "register unlock receiver", e);
        }
        Log.d(TAG, "registered unlock receiver");
    }

    @Override
    public void dispatchSystemServerContext(IBinder activityThread, IBinder activityToken) {
        Log.d(TAG, "received system context");
        ActivityManagerService.onSystemServerContext(IApplicationThread.Stub.asInterface(activityThread), activityToken);
        registerBootReceiver();
        registerPackageReceiver();
        registerUnlockReceiver();
    }

    @Override
    public boolean preStartManager(String pkgName, Intent intent) {
        Log.d(TAG, "checking manager intent");
        return ServiceManager.getManagerService().preStartManager(pkgName, intent);
    }
}
