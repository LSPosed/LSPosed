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

package hidden;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInstaller;
import android.content.res.AssetManager;
import android.content.res.CompatibilityInfo;
import android.content.res.Resources;
import android.content.res.ResourcesImpl;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.UserHandle;
import android.system.ErrnoException;
import android.system.Int32Ref;
import android.system.Os;
import android.util.MutableInt;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileDescriptor;

public class HiddenApiBridge {
    public static int AssetManager_addAssetPath(AssetManager am, String path) {
        return am.addAssetPath(path);
    }

    public static IBinder Binder_allowBlocking(IBinder binder) {
        return Binder.allowBlocking(binder);
    }

    public static void Resources_setImpl(Resources resources, ResourcesImpl impl) {
        resources.setImpl(impl);
    }

    public static int PackageInstaller_SessionParams_installFlags(PackageInstaller.SessionParams params) {
        return params.installFlags;
    }

    public static void PackageInstaller_SessionParams_installFlags(PackageInstaller.SessionParams params, int flags) {
        params.installFlags = flags;
    }

    public static IBinder Context_getActivityToken(Context ctx) {
        return ctx.getActivityToken();
    }

    public static File Environment_getDataProfilesDePackageDirectory(int userId, String packageName) {
        return Environment.getDataProfilesDePackageDirectory(userId, packageName);
    }

    public static Intent Context_registerReceiverAsUser(Context ctx, BroadcastReceiver receiver, UserHandle user,
                                                        IntentFilter filter, String broadcastPermission, Handler scheduler) {

        return ctx.registerReceiverAsUser(receiver, user, filter, broadcastPermission, scheduler);
    }

    public static UserHandle UserHandle_ALL() {
        return UserHandle.ALL;
    }

    public static UserHandle UserHandle(int h) {
        return new UserHandle(h);
    }

    public static String ApplicationInfo_credentialProtectedDataDir(ApplicationInfo applicationInfo) {
        return applicationInfo.credentialProtectedDataDir;
    }

    public static void ApplicationInfo_credentialProtectedDataDir(ApplicationInfo applicationInfo, String dir) {
        applicationInfo.credentialProtectedDataDir = dir;
    }

    public static String[] ApplicationInfo_resourceDirs(ApplicationInfo applicationInfo) {
        return applicationInfo.resourceDirs;
    }

    public static void ApplicationInfo_resourceDirs(ApplicationInfo applicationInfo, String[] resourceDirs) {
        applicationInfo.resourceDirs = resourceDirs;
    }

    @RequiresApi(31)
    public static String[] ApplicationInfo_overlayPaths(ApplicationInfo applicationInfo) {
        return applicationInfo.overlayPaths;
    }

    @RequiresApi(31)
    public static void ApplicationInfo_overlayPaths(ApplicationInfo applicationInfo, String[] overlayPaths) {
        applicationInfo.overlayPaths = overlayPaths;
    }

    public static CompatibilityInfo Resources_getCompatibilityInfo(Resources res) {
        return res.getCompatibilityInfo();
    }

    public static int Os_ioctlInt(FileDescriptor fd, int cmd, int arg) throws ErrnoException {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O_MR1) {
            return Os.ioctlInt(fd, cmd, new MutableInt(arg));
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return Os.ioctlInt(fd, cmd, new Int32Ref(arg));
        } else {
            return Os.ioctlInt(fd, cmd);
        }
    }

    public static int ActivityManager_UID_OBSERVER_GONE() {
        return ActivityManager.UID_OBSERVER_GONE;
    }

    public static int ActivityManager_UID_OBSERVER_ACTIVE() {
        return ActivityManager.UID_OBSERVER_ACTIVE;
    }

    public static int ActivityManager_UID_OBSERVER_IDLE() {
        return ActivityManager.UID_OBSERVER_IDLE;
    }

    public static int ActivityManager_UID_OBSERVER_CACHED() {
        return ActivityManager.UID_OBSERVER_CACHED;
    }

    public static int ActivityManager_PROCESS_STATE_UNKNOWN() {
        return ActivityManager.PROCESS_STATE_UNKNOWN;
    }
}
