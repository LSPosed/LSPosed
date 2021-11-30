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

import static org.lsposed.lspd.service.PackageService.PER_USER_RANGE;
import static org.lsposed.lspd.service.ServiceManager.TAG;
import static org.lsposed.lspd.service.ServiceManager.getExecutorService;

import android.app.IApplicationThread;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import org.lsposed.lspd.BuildConfig;

import java.util.Arrays;

public class LSPosedService extends ILSPosedService.Stub {
    private static final int AID_NOBODY = 9999;
    private static final int USER_NULL = -10000;
    private static final String ACTION_USER_ADDED = "android.intent.action.USER_ADDED";
    private static final String ACTION_USER_REMOVED = "android.intent.action.USER_REMOVED";
    private static final String ACTION_USER_INFO_CHANGED = "android.intent.action.USER_INFO_CHANGED";
    private static final String EXTRA_USER_HANDLE = "android.intent.extra.user_handle";
    private static final String EXTRA_REMOVED_FOR_ALL_USERS = "android.intent.extra.REMOVED_FOR_ALL_USERS";

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
        var intentAction = intent.getAction();
        var allUsers = intent.getBooleanExtra(EXTRA_REMOVED_FOR_ALL_USERS, false);
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

        switch (intentAction) {
            case Intent.ACTION_PACKAGE_FULLY_REMOVED: {
                // for module, remove module
                // because we only care about when the apk is gone
                if (moduleName != null && allUsers)
                    if (ConfigManager.getInstance().removeModule(moduleName)) {
                        broadcastOrShowNotification(moduleName, userId, intent);
                        isXposedModule = true;
                    }
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
                if (isXposedModule) {
                    broadcastOrShowNotification(moduleName, userId, intent);
                    // When installing a new Xposed module, we update the apk path to mark it as a
                    // module to send a broadcast when modules that have not been activated are
                    // uninstalled.
                    ConfigManager.getInstance().updateModuleApkPath(moduleName, ConfigManager.getInstance().getModuleApkPath(applicationInfo), true);
                    // when package is changed, we may need to update cache (module cache or process cache)
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
                    broadcastOrShowNotification(moduleName, userId, intent);
                    // it will automatically remove obsolete scope from database
                    ConfigManager.getInstance().updateCache();
                } else if (ConfigManager.getInstance().isUidHooked(uid)) {
                    // it will automatically remove obsolete app from database
                    ConfigManager.getInstance().updateAppCache();
                }
                break;
            }
        }
        boolean removed = Intent.ACTION_PACKAGE_FULLY_REMOVED.equals(intentAction) ||
                Intent.ACTION_UID_REMOVED.equals(intentAction);

        Log.d(TAG, "Package changed: uid=" + uid + " userId=" + userId + " action=" + intentAction + " isXposedModule=" + isXposedModule + " isAllUsers=" + allUsers);

        if (BuildConfig.DEFAULT_MANAGER_PACKAGE_NAME.equals(moduleName) && userId == 0) {
            Log.d(TAG, "Manager updated");
            try {
                ConfigManager.getInstance().updateManager(removed);
                LSPManagerService.createOrUpdateShortcut(false);
            } catch (Throwable e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }
    }

    private void broadcastOrShowNotification(String moduleName, int userId, Intent intent) {
        Log.d(TAG, "module " + moduleName + " changed, dispatching to manager");
        var internAction = intent.getAction();
        var allUsers = intent.getBooleanExtra(EXTRA_REMOVED_FOR_ALL_USERS, false);
        LSPManagerService.broadcastIntent(moduleName, userId, allUsers);
        var enabledModules = ConfigManager.getInstance().enabledModules();
        var scope = ConfigManager.getInstance().getModuleScope(moduleName);
        boolean systemModule = scope != null &&
                scope.parallelStream().anyMatch(app -> app.packageName.equals("android"));
        boolean enabled = Arrays.asList(enabledModules).contains(moduleName);
        if (!(Intent.ACTION_UID_REMOVED.equals(internAction) || Intent.ACTION_PACKAGE_FULLY_REMOVED.equals(internAction) || allUsers))
            LSPManagerService.showNotification(moduleName, userId, enabled, systemModule);
    }

    synchronized public void dispatchUserChanged(Intent intent) {
        if (intent == null) return;
        int uid = intent.getIntExtra(EXTRA_USER_HANDLE, AID_NOBODY);
        if (uid == AID_NOBODY || uid <= 0) return;
        var intentAction = intent.getAction();
        Log.d(TAG, "dispatchUserInfoChanged: userId=" + uid + " action=" + intentAction);
        try {
            //TODO
        } catch (Throwable e) {
            Log.e(TAG, "dispatch user info changed", e);
        }
    }

    synchronized public void dispatchUserUnlocked(Intent intent) {
        try {
            LSPManagerService.createOrUpdateShortcut(false);
        } catch (Throwable e) {
            Log.e(TAG, "dispatch user unlocked", e);
        }
    }

    synchronized public void dispatchConfigurationChanged(Intent intent) {
        try {
            ConfigFileManager.reloadConfiguration();
            LSPManagerService.createOrUpdateShortcut(false, false);
        } catch (Throwable e) {
            Log.e(TAG, "dispatch configuration changed", e);
        }
    }

    synchronized public void dispatchSecretCodeReceive() {
        Intent intent = LSPManagerService.getManagerIntent();
        try {
            var userInfo = ActivityManagerService.getCurrentUser();
            if (userInfo != null) {
                var userId = userInfo.id;
                if (userId == 0) {
                    ActivityManagerService.startActivityAsUserWithFeature("android", null,
                            intent, intent.getType(), null, null, 0, 0, null, null, userId);
                    LSPManagerService.createOrUpdateShortcut(false);
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, "dispatch secret code received", e);
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
                    getExecutorService().submit(() -> dispatchPackageChanged(intent));
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

    private void registerUnlockReceiver() {
        try {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_USER_UNLOCKED);

            ActivityManagerService.registerReceiver("android", null, new IIntentReceiver.Stub() {
                @Override
                public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) {
                    getExecutorService().submit(() -> dispatchUserUnlocked(intent));
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

    private void registerConfigurationReceiver() {
        try {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);

            ActivityManagerService.registerReceiver("android", null, new IIntentReceiver.Stub() {
                @Override
                public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) {
                    getExecutorService().submit(() -> dispatchConfigurationChanged(intent));
                    try {
                        ActivityManagerService.finishReceiver(this, resultCode, data, extras, false, intent.getFlags());
                    } catch (Throwable e) {
                        Log.e(TAG, "finish receiver", e);
                    }
                }
            }, intentFilter, null, 0, 0);
        } catch (Throwable e) {
            Log.e(TAG, "register configuration receiver", e);
        }
        Log.d(TAG, "registered configuration receiver");
    }

    private void registerSecretCodeReceiver() {
        try {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.provider.Telephony.SECRET_CODE");
            intentFilter.addDataAuthority("5776733", null);
            intentFilter.addDataScheme("android_secret_code");

            ActivityManagerService.registerReceiver("android", null, new IIntentReceiver.Stub() {
                @Override
                public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) {
                    getExecutorService().submit(() -> dispatchSecretCodeReceive());
                    try {
                        ActivityManagerService.finishReceiver(this, resultCode, data, extras, false, intent.getFlags());
                    } catch (Throwable e) {
                        Log.e(TAG, "finish receiver", e);
                    }
                }
            }, intentFilter, null, 0, 0);
        } catch (Throwable e) {
            Log.e(TAG, "register secret code receiver", e);
        }
        Log.d(TAG, "registered secret code receiver");
    }

    private void registerBootCompleteReceiver() {
        try {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_LOCKED_BOOT_COMPLETED);

            ActivityManagerService.registerReceiver("android", null, new IIntentReceiver.Stub() {
                @Override
                public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) {
                    getExecutorService().submit(() -> {
                        try {
                            var am = ActivityManagerService.getActivityManager();
                            if (am != null) am.setActivityController(null, false);
                        } catch (Throwable e) {
                            Log.e(TAG, "setActivityController", e);
                        }
                    });
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

    private void registerUserChangeReceiver() {
        try {
            IntentFilter userFilter = new IntentFilter();
            userFilter.addAction(ACTION_USER_ADDED);
            userFilter.addAction(ACTION_USER_REMOVED);
            userFilter.addAction(ACTION_USER_INFO_CHANGED);

            var receiver = new IIntentReceiver.Stub() {
                @Override
                public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) {
                    getExecutorService().submit(() -> dispatchUserChanged(intent));
                    try {
                        ActivityManagerService.finishReceiver(this, resultCode, data, extras, false, intent.getFlags());
                    } catch (Throwable e) {
                        Log.e(TAG, "finish receiver", e);
                    }
                }
            };

            ActivityManagerService.registerReceiver("android", null, receiver, userFilter, null, -1, 0);
        } catch (Throwable e) {
            Log.e(TAG, "register user info change receiver", e);
        }
        Log.d(TAG, "registered user info change receiver");
    }

    @Override
    public void dispatchSystemServerContext(IBinder activityThread, IBinder activityToken, String api) {
        Log.d(TAG, "received system context");
        ConfigManager.getInstance().setApi(api);
        ActivityManagerService.onSystemServerContext(IApplicationThread.Stub.asInterface(activityThread), activityToken);
        registerPackageReceiver();
        registerUnlockReceiver();
        registerConfigurationReceiver();
        registerSecretCodeReceiver();
        registerBootCompleteReceiver();
        registerUserChangeReceiver();
    }

    @Override
    public boolean preStartManager(String pkgName, Intent intent) {
        Log.d(TAG, "checking manager intent");
        return ServiceManager.getManagerService().preStartManager(pkgName, intent);
    }
}
