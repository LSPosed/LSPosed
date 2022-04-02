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

import static android.content.Context.BIND_AUTO_CREATE;
import static org.lsposed.lspd.service.ServiceManager.TAG;
import static org.lsposed.lspd.service.ServiceManager.getExecutorService;

import android.annotation.SuppressLint;
import android.app.INotificationManager;
import android.app.IServiceConnection;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AttributionSource;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.pm.VersionedPackage;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SELinux;
import android.os.SystemProperties;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;
import android.view.IWindowManager;

import androidx.annotation.NonNull;

import org.lsposed.daemon.BuildConfig;
import org.lsposed.daemon.R;
import org.lsposed.lspd.ILSPManagerService;
import org.lsposed.lspd.models.Application;
import org.lsposed.lspd.models.UserInfo;
import org.lsposed.lspd.util.FakeContext;
import org.lsposed.lspd.util.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import hidden.HiddenApiBridge;
import io.github.xposed.xposedservice.utils.ParceledListSlice;

public class LSPManagerService extends ILSPManagerService.Stub {
    private static final String PROP_NAME = "dalvik.vm.dex2oat-flags";
    private static final String PROP_VALUE = "--inline-max-code-units=0";
    // this maybe useful when obtaining the manager binder
    private static String RANDOM_UUID = null;
    private static final String SHORTCUT_ID = "org.lsposed.manager.shortcut";
    public static final int NOTIFICATION_ID = 114514;
    public static final String CHANNEL_ID = "lsposed";
    public static final String CHANNEL_NAME = "LSPosed Manager";
    public static final int CHANNEL_IMP = NotificationManager.IMPORTANCE_HIGH;

    private static final HandlerThread worker = new HandlerThread("manager worker");
    private static final Handler workerHandler;
    private static Intent managerIntent = null;

    static {
        worker.start();
        workerHandler = new Handler(worker.getLooper());
    }

    public class ManagerGuard implements IBinder.DeathRecipient {
        private final @NonNull
        IBinder binder;
        private final int pid;
        private final int uid;
        private final IServiceConnection connection = new IServiceConnection.Stub() {
            @Override
            public void connected(ComponentName name, IBinder service, boolean dead) {
            }
        };

        public ManagerGuard(@NonNull IBinder binder, int pid, int uid) {
            guard = this;
            this.pid = pid;
            this.uid = uid;
            this.binder = binder;
            try {
                this.binder.linkToDeath(this, 0);
                if (Utils.isMIUI) {
                    var intent = new Intent();
                    intent.setComponent(ComponentName.unflattenFromString("com.miui.securitycore/com.miui.xspace.service.XSpaceService"));
                    ActivityManagerService.bindService(intent, intent.getType(), connection, BIND_AUTO_CREATE, "android", 0);
                }
            } catch (Throwable e) {
                Log.e(TAG, "manager guard", e);
                guard = null;
            }
        }

        @Override
        public void binderDied() {
            try {
                binder.unlinkToDeath(this, 0);
                ActivityManagerService.unbindService(connection);
            } catch (Throwable e) {
                Log.e(TAG, "manager guard", e);
            }
            guard = null;
        }

        boolean isAlive() {
            return binder.isBinderAlive();
        }
    }

    public ManagerGuard guard = null;

    // guard to determine the manager or the injected app
    // that is to say, to make the parasitic success,
    // we should make sure no extra launch after parasitic
    // launch is queued and before the process is started
    private boolean pendingManager = false;
    private int managerPid = -1;

    LSPManagerService() {
    }

    private static Icon getIcon(int res) {
        var icon = ConfigFileManager.getResources().getDrawable(res, ConfigFileManager.getResources().newTheme());
        var bitmap = Bitmap.createBitmap(icon.getIntrinsicWidth(), icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
        icon.draw(new Canvas(bitmap));
        return Icon.createWithBitmap(bitmap);
    }

    private static Icon getManagerIcon() {
        try {
            return getIcon(R.mipmap.ic_launcher);
        } catch (Throwable e) {
            return getIcon(R.drawable.ic_launcher);
        }
    }

    private static Icon getNotificationIcon() {
        return getIcon(R.drawable.ic_outline_extension_24);
    }

    static Intent getManagerIntent() {
        try {
            if (managerIntent == null) {
                var intent = PackageService.getLaunchIntentForPackage(BuildConfig.MANAGER_INJECTED_PKG_NAME);
                if (intent == null) {
                    var pkgInfo = PackageService.getPackageInfo(BuildConfig.MANAGER_INJECTED_PKG_NAME, PackageManager.GET_ACTIVITIES, 0);
                    if (pkgInfo != null && pkgInfo.activities != null && pkgInfo.activities.length > 0) {
                        for (var activityInfo : pkgInfo.activities) {
                            if (activityInfo.processName.equals(activityInfo.packageName)) {
                                intent = new Intent();
                                intent.setComponent(new ComponentName(activityInfo.packageName, activityInfo.name));
                                intent.setAction(Intent.ACTION_MAIN);
                                break;
                            }
                        }
                    }
                }
                if (intent != null) {
                    if (intent.getCategories() != null) intent.getCategories().clear();
                    intent.addCategory("org.lsposed.manager.LAUNCH_MANAGER");
                    intent.setPackage(BuildConfig.MANAGER_INJECTED_PKG_NAME);
                    managerIntent = (Intent) intent.clone();
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, "get Intent", e);
        }
        return managerIntent;

    }

    public static PendingIntent getNotificationIntent(String modulePackageName, int moduleUserId) {
        try {
            var intent = (Intent) getManagerIntent().clone();
            intent.setData(new Uri.Builder().scheme("module").encodedAuthority(modulePackageName + ":" + moduleUserId).build());
            return PendingIntent.getActivity(new FakeContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } catch (Throwable e) {
            Log.e(TAG, "get notification intent", e);
            return null;
        }
    }

    public static void showNotification(String modulePackageName,
                                        int moduleUserId,
                                        boolean enabled,
                                        boolean systemModule) {
        try {
            var context = new FakeContext();
            String title = context.getString(enabled ? systemModule ?
                    R.string.xposed_module_updated_notification_title_system :
                    R.string.xposed_module_updated_notification_title :
                    R.string.module_is_not_activated_yet);
            String content = context.getString(enabled ? systemModule ?
                    R.string.xposed_module_updated_notification_content_system :
                    R.string.xposed_module_updated_notification_content :
                    R.string.module_is_not_activated_yet_detailed, modulePackageName);

            var style = new Notification.BigTextStyle();
            style.bigText(content);

            var notification = new Notification.Builder(context, CHANNEL_ID)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setSmallIcon(getNotificationIcon())
                    .setColor(Color.BLUE)
                    .setContentIntent(getNotificationIntent(modulePackageName, moduleUserId))
                    .setAutoCancel(true)
                    .setStyle(style)
                    .build();
            notification.extras.putString("android.substName", "LSPosed");
            var im = INotificationManager.Stub.asInterface(android.os.ServiceManager.getService("notification"));
            final NotificationChannel channel =
                    new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, CHANNEL_IMP);
            im.createNotificationChannels("android",
                    new android.content.pm.ParceledListSlice<>(Collections.singletonList(channel)));
            im.enqueueNotificationWithTag("android", "android", "114514", NOTIFICATION_ID, notification, 0);
        } catch (Throwable e) {
            Log.e(TAG, "post notification", e);
        }
    }

    @SuppressLint("WrongConstant")
    public static void broadcastIntent(Intent inIntent) {
        var intent = new Intent("org.lsposed.manager.NOTIFICATION");
        intent.putExtra(Intent.EXTRA_INTENT, inIntent);
        intent.addFlags(0x01000000); //Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND
        intent.addFlags(0x00400000); //Intent.FLAG_RECEIVER_FROM_SHELL
        intent.setPackage(BuildConfig.MANAGER_INJECTED_PKG_NAME);
        try {
            ActivityManagerService.broadcastIntentWithFeature(null, intent,
                    null, null, 0, null, null,
                    null, -1, null, true, false,
                    0);
            intent.setPackage(BuildConfig.DEFAULT_MANAGER_PACKAGE_NAME);
            ActivityManagerService.broadcastIntentWithFeature(null, intent,
                    null, null, 0, null, null,
                    null, -1, null, true, false,
                    0);
        } catch (Throwable t) {
            Log.e(TAG, "Broadcast to manager failed: ", t);
        }
    }

    public static void createOrUpdateShortcut(boolean force) {
        workerHandler.post(() -> createOrUpdateShortcutInternal(force, true));
    }

    public static void createOrUpdateShortcut(boolean force, boolean shouldCreate) {
        workerHandler.post(() -> createOrUpdateShortcutInternal(force, shouldCreate));
    }

    private synchronized static void createOrUpdateShortcutInternal(boolean force, boolean shouldCreate) {
        try {
            while (!UserService.isUserUnlocked(0)) {
                Log.d(TAG, "user is not yet unlocked, waiting for 1s...");
                Thread.sleep(1000);
            }
            var smCtor = ShortcutManager.class.getDeclaredConstructor(Context.class);
            smCtor.setAccessible(true);
            var context = new FakeContext("com.android.settings");
            var sm = smCtor.newInstance(context);
            if (!sm.isRequestPinShortcutSupported()) {
                Log.d(TAG, "pinned shortcut not supported, skipping");
                return;
            }
            var intent = getManagerIntent();
            var settingIntent = PackageService.getLaunchIntentForPackage("com.android.settings");
            var componentName = settingIntent != null ? settingIntent.getComponent() : new ComponentName("com.android.settings", "android.__dummy__");
            var shortcut = new ShortcutInfo.Builder(context, SHORTCUT_ID)
                    .setShortLabel("LSPosed")
                    .setLongLabel("LSPosed")
                    .setIntent(intent)
                    .setActivity(componentName)
                    .setCategories(intent.getCategories())
                    .setIcon(getManagerIcon())
                    .build();

            for (var shortcutInfo : sm.getPinnedShortcuts()) {
                if (SHORTCUT_ID.equals(shortcutInfo.getId()) && shortcutInfo.isPinned()) {
                    var shortcutIntent = sm.createShortcutResultIntent(shortcutInfo);
                    var request = (LauncherApps.PinItemRequest) shortcutIntent.getParcelableExtra(LauncherApps.EXTRA_PIN_ITEM_REQUEST);
                    var requestInfo = request.getShortcutInfo();
                    // https://cs.android.com/android/platform/superproject/+/android-8.1.0_r1:frameworks/base/services/core/java/com/android/server/pm/ShortcutRequestPinProcessor.java;drc=4ad6b57700bef4c484021f49e018117046562e6b;l=337
                    if (requestInfo.isPinned()) {
                        Log.d(TAG, "shortcut exists, updating");
                        sm.updateShortcuts(Collections.singletonList(shortcut));
                        return;
                    }
                }
            }
            var configManager = ConfigManager.getInstance();
            if (!force && configManager.isManagerInstalled()) {
                Log.d(TAG, "Manager has installed, skip adding shortcut");
                return;
            }
            // Only existing shortcuts are updated when system settings
            // are changed and no new shortcuts are requested
            if (!force && !shouldCreate) return;
            if (configManager.isAddShortcut()) {
                sm.requestPinShortcut(shortcut, null);
                Log.d(TAG, "done add shortcut");
            }
        } catch (Throwable e) {
            Log.e(TAG, "add shortcut", e);
        }
    }

    public ManagerGuard guardSnapshot() {
        var snapshot = guard;
        return snapshot != null && snapshot.isAlive() ? snapshot : null;
    }

    private void ensureWebViewPermission(File f) {
        if (!f.exists()) return;
        SELinux.setFileContext(f.getAbsolutePath(), "u:object_r:privapp_data_file:s0");
        try {
            Os.chown(f.getAbsolutePath(), BuildConfig.MANAGER_INJECTED_UID, BuildConfig.MANAGER_INJECTED_UID);
        } catch (ErrnoException e) {
            Log.e(TAG, "chown of webview", e);
        }
        if (f.isDirectory()) {
            for (var g : f.listFiles()) {
                ensureWebViewPermission(g);
            }
        }
    }

    private void ensureWebViewPermission() {
        try {
            var pkgInfo = PackageService.getPackageInfo(BuildConfig.MANAGER_INJECTED_PKG_NAME, 0, 0);
            File cacheDir = null;
            if (pkgInfo != null) {
                cacheDir = new File(HiddenApiBridge.ApplicationInfo_credentialProtectedDataDir(pkgInfo.applicationInfo) + "/cache");
            }
            var webviewDir = new File(cacheDir, "WebView");
            webviewDir.mkdirs();
            var httpCacheDir = new File(cacheDir, "http_cache");
            httpCacheDir.mkdirs();
            ensureWebViewPermission(webviewDir);
            ensureWebViewPermission(httpCacheDir);
        } catch (Throwable e) {
            Log.w(TAG, "cannot ensure webview dir", e);
        }
    }

    // To start injected manager, we should take care about conflict
    // with the target app since we won't inject into it
    // if we are not going to display manager.
    // Thus, when someone launching manager, we should no matter
    // stop any process of the target app
    // Ideally we should call force stop package here,
    // however it's not feasible because it will cause deadlock
    // Thus we will cancel the launch of the activity
    // and manually start activity with force stopping
    // However, the intent we got here is not complete since
    // there's no extras. We cannot do the same thing
    // where starting the target app while the manager is
    // still running.
    // We instead let the manager to restart the activity.
    synchronized boolean preStartManager(String pkgName, Intent intent) {
        // first, check if it's our target app, if not continue the start
        if (BuildConfig.MANAGER_INJECTED_PKG_NAME.equals(pkgName)) {
            Log.d(TAG, "starting target app of parasitic manager");
            // check if it's launching our manager
            if (intent.getCategories() != null &&
                    intent.getCategories().contains("org.lsposed.manager.LAUNCH_MANAGER")) {
                Log.d(TAG, "requesting launch of manager");
                // a new launch for the manager
                // check if there's one running
                // or it's run by ourselves after force stopping
                var snapshot = guardSnapshot();
                if ((RANDOM_UUID != null && intent.getCategories().contains(RANDOM_UUID)) ||
                        (snapshot != null && snapshot.isAlive() && snapshot.uid == BuildConfig.MANAGER_INJECTED_UID)) {
                    Log.d(TAG, "manager is still running or is on its way");
                    // there's one running parasitic manager
                    // or it's run by ourself after killing, resume it
                    return true;
                } else {
                    // new parasitic manager launch, set the flag and kill
                    // old processes
                    // we do it by cancelling the launch (return false)
                    // and start activity in a new thread
                    pendingManager = true;
                    getExecutorService().submit(() -> {
                        ensureWebViewPermission();
                        stopAndStartActivity(pkgName, intent, true);
                    });
                    Log.d(TAG, "requested to launch manager");
                    return false;
                }
            } else if (pendingManager) {
                // there's still parasitic manager, cancel a normal launch until
                // the parasitic manager is launch
                Log.d(TAG, "previous request is not yet done");
                return false;
            }
            // this is a normal launch of the target app
            // send it to the manager and let it to restart the package
            // if the manager is running
            // or normally restart without injecting
            Log.d(TAG, "launching the target app normally");
            return true;
        }
        return true;
    }

    synchronized void stopAndStartActivity(String packageName, Intent intent, boolean addUUID) {
        try {
            ActivityManagerService.forceStopPackage(packageName, 0);
            Log.d(TAG, "stopped old package");
            if (addUUID) {
                intent = (Intent) intent.clone();
                RANDOM_UUID = UUID.randomUUID().toString();
                intent.addCategory(RANDOM_UUID);
            }
            ActivityManagerService.startActivityAsUserWithFeature("android", null, intent, intent.getType(), null, null, 0, 0, null, null, 0);
            Log.d(TAG, "relaunching");
        } catch (RemoteException e) {
            Log.e(TAG, "stop and start activity", e);
        }
    }

    // return true to inject manager
    synchronized boolean shouldStartManager(int pid, int uid, String processName) {
        if (uid != BuildConfig.MANAGER_INJECTED_UID || !BuildConfig.MANAGER_INJECTED_PKG_NAME.equals(processName) || !pendingManager)
            return false;
        // pending parasitic manager launch it processes
        // now we have its pid so we allow it to be killed
        // and thus reset the pending flag and mark its pid
        pendingManager = false;
        managerPid = pid;
        Log.d(TAG, "starting injected manager: pid = " + pid + " uid = " + uid + " processName = " + processName);
        return true;
    }

    // return true to send manager binder
    synchronized boolean postStartManager(int pid, int uid) {
        if (pid == managerPid && uid == BuildConfig.MANAGER_INJECTED_UID) {
            RANDOM_UUID = null;
            return true;
        }
        return false;
    }

    public @NonNull
    IBinder obtainManagerBinder(@NonNull IBinder heartbeat, int pid, int uid) {
        new ManagerGuard(heartbeat, pid, uid);
        if (postStartManager(pid, uid)) {
            managerPid = 0;
        }
        return this;
    }

    public boolean isRunningManager(int pid, int uid) {
        var snapshotPid = managerPid;
        var snapshotGuard = guardSnapshot();
        return (pid == snapshotPid && uid == BuildConfig.MANAGER_INJECTED_UID) || (snapshotGuard != null && snapshotGuard.pid == pid && snapshotGuard.uid == uid);
    }

    void onSystemServerDied() {
        pendingManager = false;
        managerPid = 0;
        guard = null;
    }

    @Override
    public IBinder asBinder() {
        return this;
    }

    @Override
    public int getXposedApiVersion() {
        return BuildConfig.API_CODE;
    }

    @Override
    public int getXposedVersionCode() {
        return BuildConfig.VERSION_CODE;
    }

    @Override
    public String getXposedVersionName() {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public String getApi() {
        return ConfigManager.getInstance().getApi();
    }

    @Override
    public ParceledListSlice<PackageInfo> getInstalledPackagesFromAllUsers(int flags, boolean filterNoProcess) throws RemoteException {
        return PackageService.getInstalledPackagesFromAllUsers(flags, filterNoProcess);
    }

    @Override
    public String[] enabledModules() {
        return ConfigManager.getInstance().enabledModules();
    }

    @Override
    public boolean enableModule(String packageName) throws RemoteException {
        return ConfigManager.getInstance().enableModule(packageName);
    }

    @Override
    public boolean setModuleScope(String packageName, ParceledListSlice<Application> scope) throws RemoteException {
        return ConfigManager.getInstance().setModuleScope(packageName, scope.getList());
    }

    @Override
    public ParceledListSlice<Application> getModuleScope(String packageName) {
        List<Application> list = ConfigManager.getInstance().getModuleScope(packageName);
        if (list == null) return null;
        else return new ParceledListSlice<>(list);
    }

    @Override
    public boolean disableModule(String packageName) {
        return ConfigManager.getInstance().disableModule(packageName);
    }

    @Override
    public boolean isAddShortcut() {
        return ConfigManager.getInstance().isAddShortcut();
    }

    @Override
    public void setAddShortcut(boolean enabled) {
        ConfigManager.getInstance().setAddShortcut(enabled);
        if (enabled) createOrUpdateShortcut(true);
    }

    @Override
    public boolean isVerboseLog() {
        return ConfigManager.getInstance().verboseLog();
    }

    @Override
    public void setVerboseLog(boolean enabled) {
        ConfigManager.getInstance().setVerboseLog(enabled);
    }

    @Override
    public ParcelFileDescriptor getVerboseLog() {
        return ConfigManager.getInstance().getVerboseLog();
    }

    @Override
    public ParcelFileDescriptor getModulesLog() {
        workerHandler.post(() -> ServiceManager.getLogcatService().checkLogFile());
        return ConfigManager.getInstance().getModulesLog();
    }

    @Override
    public boolean clearLogs(boolean verbose) {
        return ConfigManager.getInstance().clearLogs(verbose);
    }

    @Override
    public PackageInfo getPackageInfo(String packageName, int flags, int uid) throws RemoteException {
        return PackageService.getPackageInfo(packageName, flags, uid);
    }

    @Override
    public void forceStopPackage(String packageName, int userId) throws RemoteException {
        ActivityManagerService.forceStopPackage(packageName, userId);
    }

    @Override
    public void reboot(boolean shutdown) {
        var value = shutdown ? "shutdown" : "reboot";
        SystemProperties.set("sys.powerctl", value);
    }

    @Override
    public boolean uninstallPackage(String packageName, int userId) throws RemoteException {
        try {
            if (ActivityManagerService.startUserInBackground(userId))
                return PackageService.uninstallPackage(new VersionedPackage(packageName, PackageManager.VERSION_CODE_HIGHEST), userId);
            else return false;
        } catch (InterruptedException | InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            Log.e(TAG, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean isSepolicyLoaded() {
        return ConfigManager.getInstance().isSepolicyLoaded();
    }

    @Override
    public List<UserInfo> getUsers() throws RemoteException {
        var users = new LinkedList<UserInfo>();
        for (var user : UserService.getUsers()) {
            var info = new UserInfo();
            info.id = user.id;
            info.name = user.name;
            users.add(info);
        }
        return users;
    }

    @Override
    public int installExistingPackageAsUser(String packageName, int userId) {
        try {
            if (ActivityManagerService.startUserInBackground(userId))
                return PackageService.installExistingPackageAsUser(packageName, userId);
            else return PackageService.INSTALL_FAILED_INTERNAL_ERROR;
        } catch (Throwable e) {
            Log.w(TAG, "install existing package as user: ", e);
            return PackageService.INSTALL_FAILED_INTERNAL_ERROR;
        }
    }

    @Override
    public boolean systemServerRequested() {
        return ServiceManager.systemServerRequested();
    }

    @Override
    public int startActivityAsUserWithFeature(Intent intent, int userId) throws RemoteException {
        if (!intent.getBooleanExtra("lsp_no_switch_to_user", false)) {
            intent.removeExtra("lsp_no_switch_to_user");
            var currentUser = ActivityManagerService.getCurrentUser();
            if (currentUser == null) return -1;
            var parent = UserService.getProfileParent(userId);
            if (parent < 0) return -1;
            if (currentUser.id != parent) {
                if (!ActivityManagerService.switchUser(parent)) return -1;
                var window = android.os.ServiceManager.getService("window");
                if (window != null) {
                    var wm = IWindowManager.Stub.asInterface(window);
                    wm.lockNow(null);
                }
            }
        }
        return ActivityManagerService.startActivityAsUserWithFeature("android", null, intent, intent.getType(), null, null, 0, 0, null, null, userId);
    }

    @Override
    public ParceledListSlice<ResolveInfo> queryIntentActivitiesAsUser(Intent intent, int flags, int userId) throws RemoteException {
        return PackageService.queryIntentActivities(intent, intent.getType(), flags, userId);
    }

    @Override
    public boolean dex2oatFlagsLoaded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ServiceManager.getDex2OatService().isAlive();
        } else {
            return SystemProperties.get(PROP_NAME).contains(PROP_VALUE);
        }
    }

    @Override
    public void setHiddenIcon(boolean hide) {
        Bundle args = new Bundle();
        args.putString("value", hide ? "0" : "1");
        args.putString("_user", "0");
        try {
            var contentProvider = ActivityManagerService.getContentProvider("settings", 0);
            if (contentProvider != null) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        contentProvider.call(new AttributionSource.Builder(1000).setPackageName("android").build(),
                                "settings", "PUT_global", "show_hidden_icon_apps_enabled", args);
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        contentProvider.call("android", null, "settings", "PUT_global", "show_hidden_icon_apps_enabled", args);
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentProvider.call("android", "settings", "PUT_global", "show_hidden_icon_apps_enabled", args);
                    }
                } catch (NoSuchMethodError e) {
                    Log.w(TAG, "setHiddenIcon: ", e);
                }
            }
        } catch (Throwable e) {
            Log.w(TAG, "setHiddenIcon: ", e);
        }
    }

    @Override
    public void getLogs(ParcelFileDescriptor zipFd) throws RemoteException {
        ConfigFileManager.getLogs(zipFd);
    }

    @Override
    public void restartFor(Intent intent) throws RemoteException {
        forceStopPackage(BuildConfig.MANAGER_INJECTED_PKG_NAME, 0);
        stopAndStartActivity(BuildConfig.MANAGER_INJECTED_PKG_NAME, intent, false);
    }

    @Override
    public void createShortcut() {
        createOrUpdateShortcut(true);
        setAddShortcut(true);
    }

    @Override
    public List<String> getDenyListPackages() {
        return ConfigManager.getInstance().getDenyListPackages();
    }

    @Override
    public void flashZip(String zipPath, ParcelFileDescriptor outputStream) {
        var processBuilder = new ProcessBuilder("magisk", "--install-module", zipPath);
        var fd = new File("/proc/self/fd/" + outputStream.getFd());
        processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(fd));
        try (outputStream; var fdw = new FileOutputStream(fd, true)) {
            var proc = processBuilder.start();
            if (proc.waitFor(10, TimeUnit.SECONDS)) {
                var exit = proc.exitValue();
                if (exit == 0) {
                    fdw.write("- Reboot after 5s\n".getBytes());
                    Thread.sleep(5000);
                    reboot(false);
                } else {
                    var s = "! Flash failed, exit with " + exit + "\n";
                    fdw.write(s.getBytes());
                }
            } else {
                proc.destroy();
                fdw.write("! Timeout, abort\n".getBytes());
            }
        } catch (IOException | InterruptedException e) {
            Log.e(TAG, "flashZip: ", e);
        }
    }

    @Override
    public boolean performDexOptMode(String packageName) throws RemoteException {
        return PackageService.performDexOptMode(packageName);
    }

    @Override
    public boolean getDexObfuscate() {
        return ConfigManager.getInstance().dexObfuscate();
    }

    @Override
    public void setDexObfuscate(boolean enabled) {
        ConfigManager.getInstance().setDexObfuscate(enabled);
    }
}
