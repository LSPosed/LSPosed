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

package io.github.lsposed.manager;

import android.content.pm.PackageInfo;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.List;

import io.github.lsposed.manager.receivers.LSPosedManagerServiceClient;

public class ConfigManager {

    public static int getXposedApiVersion() {
        try {
            return LSPosedManagerServiceClient.getXposedApiVersion();
        } catch (RemoteException | NullPointerException e) {
            // TODO:
            e.printStackTrace();
            return -1;
        }
    }

    public static String getXposedVersionName() {
        try {
            return LSPosedManagerServiceClient.getXposedVersionName();
        } catch (RemoteException | NullPointerException e) {
            // TODO:
            e.printStackTrace();
            return null;
        }
    }

    public static int getXposedVersionCode() {
        try {
            return LSPosedManagerServiceClient.getXposedVersionCode();
        } catch (RemoteException | NullPointerException e) {
            // TODO:
            e.printStackTrace();
            return -1;
        }
    }

    public static List<PackageInfo> getInstalledPackagesFromAllUsers(int flags) {
        List<PackageInfo> list = new ArrayList<>();
        try {
            list.addAll(LSPosedManagerServiceClient.getInstalledPackagesFromAllUsers(flags));
        } catch (RemoteException | NullPointerException e) {
            // TODO:
            e.printStackTrace();
        }
        return list;
    }

    public static String[] getEnabledModules() {
        try {
            return LSPosedManagerServiceClient.enabledModules();
        } catch (RemoteException | NullPointerException e) {
            // TODO:
            e.printStackTrace();
            return new String[0];
        }
    }

    public static boolean setModuleEnabled(String packageName, boolean enable) {
        try {
            return enable ? LSPosedManagerServiceClient.enableModule(packageName) : LSPosedManagerServiceClient.disableModule(packageName);
        } catch (RemoteException | NullPointerException e) {
            // TODO:
            e.printStackTrace();
            return false;
        }
    }

    public static boolean setModuleScope(String packageName, List<Integer> uidList) {
        try {
            int[] uids = new int[uidList.size()];
            for (int i = 0; i < uidList.size(); i++) {
                uids[i] = uidList.get(i);
            }
            return LSPosedManagerServiceClient.setModuleScope(packageName, uids);
        } catch (RemoteException | NullPointerException e) {
            // TODO:
            e.printStackTrace();
            return false;
        }
    }

    public static List<Integer> getModuleScope(String packageName) {
        List<Integer> list = new ArrayList<>();
        try {
            for (int uid : LSPosedManagerServiceClient.getModuleScope(packageName)) {
                list.add(uid);
            }
        } catch (RemoteException | NullPointerException e) {
            // TODO:
            e.printStackTrace();
        }
        return list;
    }

    public static boolean isResourceHookEnabled() {
        try {
            return LSPosedManagerServiceClient.isResourceHook();
        } catch (RemoteException | NullPointerException e) {
            // TODO:
            e.printStackTrace();
            return false;
        }
    }

    public static boolean setResourceHookEnabled(boolean enabled) {
        try {
            LSPosedManagerServiceClient.setResourceHook(enabled);
            return true;
        } catch (RemoteException | NullPointerException e) {
            // TODO:
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isVerboseLogEnabled() {
        try {
            return LSPosedManagerServiceClient.isVerboseLog();
        } catch (RemoteException | NullPointerException e) {
            // TODO:
            e.printStackTrace();
            return false;
        }
    }

    public static boolean setVerboseLogEnabled(boolean enabled) {
        try {
            LSPosedManagerServiceClient.setVerboseLog(enabled);
            return true;
        } catch (RemoteException | NullPointerException e) {
            // TODO:
            e.printStackTrace();
            return false;
        }
    }

    public static int getVariant() {
        try {
            return LSPosedManagerServiceClient.getVariant();
        } catch (RemoteException | NullPointerException e) {
            // TODO:
            e.printStackTrace();
            return 1;
        }
    }

    public static String getVariantString() {
        int variant = getVariant();
        switch (variant) {
            case 1:
                return "YAHFA";
            case 2:
                return "SandHook";
            default:
                return "Unknown";
        }
    }

    public static boolean setVariant(int variant) {
        try {
            LSPosedManagerServiceClient.setVariant(variant);
            return true;
        } catch (RemoteException | NullPointerException e) {
            // TODO:
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isPermissive() {
        try {
            return LSPosedManagerServiceClient.isPermissive();
        } catch (RemoteException | NullPointerException e) {
            // TODO:
            e.printStackTrace();
            return true;
        }
    }

    public static ParcelFileDescriptor getVerboseLog() {
        try {
            return LSPosedManagerServiceClient.getVerboseLog();
        } catch (RemoteException | NullPointerException e) {
            // TODO:
            e.printStackTrace();
            return null;
        }
    }

    public static ParcelFileDescriptor getModulesLog() {
        try {
            return LSPosedManagerServiceClient.getModulesLog();
        } catch (RemoteException | NullPointerException e) {
            // TODO:
            e.printStackTrace();
            return null;
        }
    }
}
