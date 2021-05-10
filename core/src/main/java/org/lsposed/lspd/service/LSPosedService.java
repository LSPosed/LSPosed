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

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.Arrays;

import org.lsposed.lspd.Application;

import static org.lsposed.lspd.service.ServiceManager.TAG;

public class LSPosedService extends ILSPosedService.Stub {
    @Override
    public ILSPApplicationService requestApplicationService(int uid, int pid, String processName, IBinder heartBeat) {
        if (Binder.getCallingUid() != 1000) {
            Log.w(TAG, "Someone else got my binder!?");
            return null;
        }
        if (uid == 1000 && processName.equals("android")) {
            if (ConfigManager.getInstance().shouldSkipSystemServer())
                return null;
            else
                return ServiceManager.requestApplicationService(uid, pid, heartBeat);
        }
        if (ConfigManager.getInstance().shouldSkipProcess(new ConfigManager.ProcessScope(processName, uid))) {
            Log.d(TAG, "Skipped " + processName + "/" + uid);
            return null;
        }
        if (ServiceManager.getApplicationService().hasRegister(uid, pid)) {
            Log.d(TAG, "Skipped duplicated request for uid " + uid + " pid " + pid);
            return null;
        }
        Log.d(TAG, "returned service");
        return ServiceManager.requestApplicationService(uid, pid, heartBeat);
    }


    @Override
    public void dispatchPackageChanged(Intent intent) throws RemoteException {
        if (Binder.getCallingUid() != 1000 || intent == null) return;
        Uri uri = intent.getData();
        String packageName = (uri != null) ? uri.getSchemeSpecificPart() : null;
        if (packageName == null) {
            Log.e(TAG, "Package name is null");
            return;
        }
        Log.d(TAG, "Package changed: " + packageName);
        int uid = intent.getIntExtra(Intent.EXTRA_UID, -1);
        int userId = intent.getIntExtra(Intent.EXTRA_USER, -1);
        if (intent.getAction().equals(Intent.ACTION_PACKAGE_FULLY_REMOVED) && uid > 0) {
            if (userId == 0 || userId == -1) {
                ConfigManager.getInstance().removeModule(packageName);
            }
            Application app = new Application();
            app.packageName = packageName;
            app.userId = userId;
            ConfigManager.getInstance().removeApp(app);
            return;
        }

        if (intent.getAction().equals(Intent.ACTION_PACKAGE_CHANGED)) {
            // make sure that the change is for the complete package, not only a
            // component
            String[] components = intent.getStringArrayExtra(Intent.EXTRA_CHANGED_COMPONENT_NAME_LIST);
            if (components != null) {
                boolean isForPackage = false;
                for (String component : components) {
                    if (packageName.equals(component)) {
                        isForPackage = true;
                        break;
                    }
                }
                if (!isForPackage)
                    return;
            }
        }

        ApplicationInfo applicationInfo = PackageService.getApplicationInfo(packageName, PackageManager.GET_META_DATA, 0);
        boolean isXposedModule = (userId == 0 || userId == -1) &&
                applicationInfo != null &&
                applicationInfo.enabled &&
                applicationInfo.metaData != null &&
                applicationInfo.metaData.containsKey("xposedminversion");

        if (isXposedModule) {
            ConfigManager.getInstance().updateModuleApkPath(packageName, applicationInfo.sourceDir);
            Log.d(TAG, "Updated module apk path: " + packageName);

            boolean enabled = Arrays.asList(ConfigManager.getInstance().enabledModules()).contains(packageName);
            Intent broadcastIntent = new Intent(enabled ? "org.lsposed.action.MODULE_UPDATED" : "org.lsposed.action.MODULE_NOT_ACTIVATAED");
            broadcastIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            broadcastIntent.addFlags(0x01000000);
            broadcastIntent.addFlags(0x00400000);
            broadcastIntent.setData(intent.getData());
            broadcastIntent.setComponent(ComponentName.unflattenFromString(ConfigManager.getInstance().getManagerPackageName() + "/.receivers.ServiceReceiver"));

            try {
                ActivityManagerService.broadcastIntentWithFeature(null, null, broadcastIntent,
                        null, null, 0, null, null,
                        null, -1, null, true, false,
                        0);
            } catch (Throwable t) {
                Log.e(TAG, "Broadcast to manager failed: ", t);
            }
        }
        if (!intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED) && uid > 0 && ConfigManager.getInstance().isManager(packageName)) {
            Log.d(TAG, "Manager updated");
            try {
                ConfigManager.getInstance().updateManager();
                ConfigManager.grantManagerPermission();
            } catch (Throwable e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }
    }


    @Override
    public void dispatchBootCompleted(Intent intent) throws RemoteException {
        ConfigManager.getInstance().ensureManager();
    }
}
