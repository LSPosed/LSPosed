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

import static android.content.Intent.EXTRA_UID;
import static org.lsposed.lspd.service.LSPNotificationManager.SCOPE_CHANNEL_ID;
import static org.lsposed.lspd.service.LSPNotificationManager.UPDATED_CHANNEL_ID;
import static org.lsposed.lspd.service.PackageService.PER_USER_RANGE;
import static org.lsposed.lspd.service.ServiceManager.TAG;
import static org.lsposed.lspd.service.ServiceManager.getExecutorService;

import android.app.IApplicationThread;
import android.app.IUidObserver;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Telephony;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.lsposed.daemon.BuildConfig;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.ZipFile;

import hidden.HiddenApiBridge;
import io.github.libxposed.service.IXposedScopeCallback;

public class LSPosedService extends ILSPosedService.Stub {
    private static final int AID_NOBODY = 9999;
    private static final int USER_NULL = -10000;
    private static final String ACTION_USER_ADDED = "android.intent.action.USER_ADDED";
    public static final String ACTION_USER_REMOVED = "android.intent.action.USER_REMOVED";
    private static final String EXTRA_USER_HANDLE = "android.intent.extra.user_handle";
    private static final String EXTRA_REMOVED_FOR_ALL_USERS = "android.intent.extra.REMOVED_FOR_ALL_USERS";

    private static boolean isModernModules(ApplicationInfo info) {
        String[] apks;
        if (info.splitSourceDirs != null) {
            apks = Arrays.copyOf(info.splitSourceDirs, info.splitSourceDirs.length + 1);
            apks[info.splitSourceDirs.length] = info.sourceDir;
        } else apks = new String[]{info.sourceDir};
        for (var apk : apks) {
            try (var zip = new ZipFile(apk)) {
                if (zip.getEntry("META-INF/xposed/java_init.list") != null) {
                    return true;
                }
            } catch (IOException ignored) {
            }
        }
        return false;
    }

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
        if (!ServiceManager.getManagerService().shouldStartManager(pid, uid, processName) && ConfigManager.getInstance().shouldSkipProcess(new ConfigManager.ProcessScope(processName, uid))) {
            Log.d(TAG, "Skipped " + processName + "/" + uid);
            return null;
        }
        Log.d(TAG, "returned service");
        return ServiceManager.requestApplicationService(uid, pid, processName, heartBeat);
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

    private void dispatchPackageChanged(Intent intent) {
        if (intent == null) return;
        int uid = intent.getIntExtra(EXTRA_UID, AID_NOBODY);
        if (uid == AID_NOBODY || uid <= 0) return;
        int userId = intent.getIntExtra("android.intent.extra.user_handle", USER_NULL);
        var intentAction = intent.getAction();
        var allUsers = intent.getBooleanExtra(EXTRA_REMOVED_FOR_ALL_USERS, false);
        if (userId == USER_NULL) userId = uid % PER_USER_RANGE;
        Uri uri = intent.getData();
        String moduleName = (uri != null) ? uri.getSchemeSpecificPart() : ConfigManager.getInstance().getModule(uid).packageName;

        ApplicationInfo applicationInfo = null;
        if (moduleName != null) {
            try {
                applicationInfo = PackageService.getApplicationInfo(moduleName, PackageManager.GET_META_DATA | PackageService.MATCH_ALL_FLAGS, 0);
            } catch (Throwable ignored) {
            }
        }

        boolean isXposedModule = applicationInfo != null && ((applicationInfo.metaData != null && applicationInfo.metaData.containsKey("xposedminversion")) || isModernModules(applicationInfo));

        switch (intentAction) {
            case Intent.ACTION_PACKAGE_FULLY_REMOVED: {
                // for module, remove module
                // because we only care about when the apk is gone
                if (moduleName != null && allUsers)
                    if (ConfigManager.getInstance().removeModule(moduleName)) {
                        isXposedModule = true;
                        broadcastAndShowNotification(moduleName, userId, intent, true);
                    }
                if (moduleName != null) {
                    LSPNotificationManager.cancelNotification(UPDATED_CHANNEL_ID, moduleName, userId);
                }
                break;
            }
            case Intent.ACTION_PACKAGE_REMOVED:
                if (moduleName != null) {
                    LSPNotificationManager.cancelNotification(UPDATED_CHANNEL_ID, moduleName, userId);
                }
                break;
            case Intent.ACTION_PACKAGE_ADDED:
            case Intent.ACTION_PACKAGE_CHANGED: {
                // make sure that the change is for the complete package, not only a
                // component
                String[] components = intent.getStringArrayExtra(Intent.EXTRA_CHANGED_COMPONENT_NAME_LIST);
                if (components != null && !Arrays.stream(components).reduce(false, (p, c) -> p || c.equals(moduleName), Boolean::logicalOr)) {
                    return;
                }
                if (isXposedModule) {
                    // When installing a new Xposed module, we update the apk path to mark it as a
                    // module to send a broadcast when modules that have not been activated are
                    // uninstalled.
                    // If cache not updated, assume it's not xposed module
                    isXposedModule = ConfigManager.getInstance().updateModuleApkPath(moduleName, ConfigManager.getInstance().getModuleApkPath(applicationInfo), false);
                } else if (ConfigManager.getInstance().isUidHooked(uid)) {
                    // it will automatically remove obsolete app from database
                    ConfigManager.getInstance().updateAppCache();
                }
                broadcastAndShowNotification(moduleName, userId, intent, isXposedModule);
                break;
            }
            case Intent.ACTION_UID_REMOVED: {
                // when a package is removed (rather than hide) for a single user
                // (apk may still be there because of multi-user)
                broadcastAndShowNotification(moduleName, userId, intent, isXposedModule);
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
        boolean removed = Intent.ACTION_PACKAGE_FULLY_REMOVED.equals(intentAction) || Intent.ACTION_UID_REMOVED.equals(intentAction);

        Log.d(TAG, "Package changed: uid=" + uid + " userId=" + userId + " action=" + intentAction + " isXposedModule=" + isXposedModule + " isAllUsers=" + allUsers);

        if (BuildConfig.DEFAULT_MANAGER_PACKAGE_NAME.equals(moduleName) && userId == 0) {
            Log.d(TAG, "Manager updated");
            ConfigManager.getInstance().updateManager(removed);
        }
    }

    private void broadcastAndShowNotification(String packageName, int userId, Intent intent, boolean isXposedModule) {
        Log.d(TAG, "package " + packageName + " changed, dispatching to manager");
        var action = intent.getAction();
        var allUsers = intent.getBooleanExtra(EXTRA_REMOVED_FOR_ALL_USERS, false);
        intent.putExtra("android.intent.extra.PACKAGES", packageName);
        intent.putExtra(Intent.EXTRA_USER, userId);
        intent.putExtra("isXposedModule", isXposedModule);
        LSPManagerService.broadcastIntent(intent);
        if (isXposedModule) {
            var enabledModules = ConfigManager.getInstance().enabledModules();
            var scope = ConfigManager.getInstance().getModuleScope(packageName);
            boolean systemModule = scope != null && scope.parallelStream().anyMatch(app -> app.packageName.equals("android"));
            boolean enabled = Arrays.asList(enabledModules).contains(packageName);
            if (!(Intent.ACTION_UID_REMOVED.equals(action) || Intent.ACTION_PACKAGE_FULLY_REMOVED.equals(action) || allUsers))
                LSPNotificationManager.notifyModuleUpdated(packageName, userId, enabled, systemModule);
        }
    }

    private void dispatchUserChanged(Intent intent) {
        if (intent == null) return;
        int uid = intent.getIntExtra(EXTRA_USER_HANDLE, AID_NOBODY);
        if (uid == AID_NOBODY || uid <= 0) return;
        LSPManagerService.broadcastIntent(intent);
    }

    private void dispatchBootCompleted(Intent intent) {
        try {
            var am = ActivityManagerService.getActivityManager();
            if (am != null) am.setActivityController(null, false);
        } catch (RemoteException e) {
            Log.e(TAG, "setActivityController", e);
        }
        var configManager = ConfigManager.getInstance();
        if (configManager.enableStatusNotification()) {
            LSPNotificationManager.notifyStatusNotification();
        }
    }

    private void dispatchConfigurationChanged(Intent intent) {
        ConfigFileManager.reloadConfiguration();
        var configManager = ConfigManager.getInstance();
        if (configManager.enableStatusNotification()) {
            LSPNotificationManager.notifyStatusNotification();
        }
    }

    private void dispatchSecretCodeReceive(Intent i) {
        LSPManagerService.openManager(null);
    }

    private void dispatchOpenManager(Intent intent) {
        LSPManagerService.openManager(intent.getData());
    }

    private void dispatchModuleScope(Intent intent) {
        Log.d(TAG, "dispatchModuleScope: " + intent);
        var data = intent.getData();
        var extras = intent.getExtras();
        if (extras == null || data == null) return;
        var callback = extras.getBinder("callback");
        var s = data.getEncodedAuthority().split(":", 2);
        if (s.length != 2) return;
        var packageName = s[0];
        int userId;
        try {
            userId = Integer.parseInt(s[1]);
        } catch (NumberFormatException e) {
            return;
        }
        var scopePackageName = data.getPath();
        if (scopePackageName == null) return;
        scopePackageName = scopePackageName.substring(1);
        var action = data.getQueryParameter("action");
        if (action == null) return;

        var iCallback = IXposedScopeCallback.Stub.asInterface(callback);
        try {
            ApplicationInfo applicationInfo = null;
            try {
                applicationInfo = PackageService.getApplicationInfo(scopePackageName, 0, userId);
            } catch (Throwable ignored) {
            }
            if (applicationInfo == null) {
                iCallback.onScopeRequestFailed(scopePackageName, "Package not found");
                return;
            }

            switch (action) {
                case "approve":
                    ConfigManager.getInstance().setModuleScope(packageName, scopePackageName, userId);
                    iCallback.onScopeRequestApproved(scopePackageName);
                    break;
                case "deny":
                    iCallback.onScopeRequestDenied(scopePackageName);
                    break;
                case "delete":
                    iCallback.onScopeRequestTimeout(scopePackageName);
                    break;
                case "block":
                    ConfigManager.getInstance().blockScopeRequest(packageName);
                    iCallback.onScopeRequestDenied(scopePackageName);
                    break;
            }
            Log.i(TAG, action + " scope " + scopePackageName + " for " + packageName + " in user " + userId);
        } catch (Throwable e) {
            try {
                iCallback.onScopeRequestFailed(scopePackageName, e.getMessage());
            } catch (Throwable ignored) {
                // callback died
            }
        }
        LSPNotificationManager.cancelNotification(SCOPE_CHANNEL_ID, packageName, userId);
    }

    private void registerReceiver(List<IntentFilter> filters, String requiredPermission, int userId, Consumer<Intent> task, int flag) {
        var receiver = new IIntentReceiver.Stub() {
            @Override
            public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) {
                getExecutorService().submit(() -> {
                    try {
                        task.accept(intent);
                    } catch (Throwable t) {
                        Log.e(TAG, "performReceive: ", t);
                    }
                });
                if (!ordered) return;
                try {
                    ActivityManagerService.finishReceiver(this, resultCode, data, extras, false, intent.getFlags());
                } catch (RemoteException e) {
                    Log.e(TAG, "finish receiver", e);
                }
            }
        };
        try {
            for (var filter : filters) {
                ActivityManagerService.registerReceiver("android", null, receiver, filter, requiredPermission, userId, flag);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "register receiver", e);
        }
    }

    private void registerReceiver(List<IntentFilter> filters, String requiredPermission, int userId, Consumer<Intent> task) {
        registerReceiver(filters, requiredPermission, userId, task, Context.RECEIVER_NOT_EXPORTED);
    }

    private void registerReceiver(List<IntentFilter> filters, int userId, Consumer<Intent> task) {
        registerReceiver(filters, null, userId, task, Context.RECEIVER_NOT_EXPORTED);
    }

    private void registerReceiver(List<IntentFilter> filters, int userId, Consumer<Intent> task, int flag) {
        registerReceiver(filters, null, userId, task, flag);
    }

    private void registerPackageReceiver() {
        var packageFilter = new IntentFilter();
        packageFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED);
        packageFilter.addDataScheme("package");

        var uidFilter = new IntentFilter(Intent.ACTION_UID_REMOVED);

        registerReceiver(List.of(packageFilter, uidFilter), -1, this::dispatchPackageChanged);
        Log.d(TAG, "registered package receiver");
    }

    private void registerConfigurationReceiver() {
        var intentFilter = new IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED);

        registerReceiver(List.of(intentFilter), 0, this::dispatchConfigurationChanged);
        Log.d(TAG, "registered configuration receiver");
    }

    private void registerSecretCodeReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            intentFilter.addAction(TelephonyManager.ACTION_SECRET_CODE);
        } else {
            // noinspection InlinedApi
            intentFilter.addAction(Telephony.Sms.Intents.SECRET_CODE_ACTION);
        }
        intentFilter.addDataAuthority("5776733", null);
        intentFilter.addDataScheme("android_secret_code");

        registerReceiver(List.of(intentFilter), 0, this::dispatchSecretCodeReceive, Context.RECEIVER_EXPORTED);
        Log.d(TAG, "registered secret code receiver");
    }

    private void registerBootCompleteReceiver() {
        var intentFilter = new IntentFilter(Intent.ACTION_LOCKED_BOOT_COMPLETED);

        registerReceiver(List.of(intentFilter), 0, this::dispatchBootCompleted);
        Log.d(TAG, "registered boot receiver");
    }

    private void registerUserChangeReceiver() {
        var userFilter = new IntentFilter();
        userFilter.addAction(ACTION_USER_ADDED);
        userFilter.addAction(ACTION_USER_REMOVED);

        registerReceiver(List.of(userFilter), -1, this::dispatchUserChanged);
        Log.d(TAG, "registered user info change receiver");
    }

    private void registerOpenManagerReceiver() {
        var intentFilter = new IntentFilter(LSPNotificationManager.openManagerAction);
        var moduleFilter = new IntentFilter(intentFilter);
        moduleFilter.addDataScheme("module");

        registerReceiver(List.of(intentFilter, moduleFilter), "android.permission.BRICK", 0, this::dispatchOpenManager);
        Log.d(TAG, "registered open manager receiver");
    }

    private void registerModuleScopeReceiver() {
        var intentFilter = new IntentFilter(LSPNotificationManager.moduleScope);
        intentFilter.addDataScheme("module");

        registerReceiver(List.of(intentFilter), "android.permission.BRICK", 0, this::dispatchModuleScope);
        Log.d(TAG, "registered module scope receiver");
    }

    private void registerUidObserver() {
        try {
            var which = HiddenApiBridge.ActivityManager_UID_OBSERVER_ACTIVE()
                    | HiddenApiBridge.ActivityManager_UID_OBSERVER_GONE()
                    | HiddenApiBridge.ActivityManager_UID_OBSERVER_IDLE()
                    | HiddenApiBridge.ActivityManager_UID_OBSERVER_CACHED();
            LSPModuleService.uidClear();
            ActivityManagerService.registerUidObserver(new IUidObserver.Stub() {
                @Override
                public void onUidActive(int uid) {
                    LSPModuleService.uidStarts(uid);
                }

                @Override
                public void onUidCachedChanged(int uid, boolean cached) {
                    if (!cached) LSPModuleService.uidStarts(uid);
                }

                @Override
                public void onUidIdle(int uid, boolean disabled) {
                    LSPModuleService.uidStarts(uid);
                }

                @Override
                public void onUidGone(int uid, boolean disabled) {
                    LSPModuleService.uidGone(uid);
                }
            }, which, HiddenApiBridge.ActivityManager_PROCESS_STATE_UNKNOWN(), null);
        } catch (RemoteException e) {
            Log.e(TAG, "registerUidObserver", e);
        }
    }

    @Override
    public void dispatchSystemServerContext(IBinder activityThread, IBinder activityToken, String api) {
        Log.d(TAG, "received system context");
        ConfigManager.getInstance().setApi(api);
        ActivityManagerService.onSystemServerContext(IApplicationThread.Stub.asInterface(activityThread), activityToken);
        registerPackageReceiver();
        registerConfigurationReceiver();
        registerSecretCodeReceiver();
        registerBootCompleteReceiver();
        registerUserChangeReceiver();
        registerOpenManagerReceiver();
        registerModuleScopeReceiver();
        registerUidObserver();
    }

    @Override
    public boolean preStartManager(String pkgName, Intent intent) {
        Log.d(TAG, "checking manager intent");
        return ServiceManager.getManagerService().preStartManager(pkgName, intent);
    }
}
