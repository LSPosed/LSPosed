package org.lsposed.lspd;

import org.lsposed.lspd.utils.ParceledListSlice;
import org.lsposed.lspd.Application;


interface ILSPManagerService {
    ParceledListSlice<PackageInfo> getInstalledPackagesFromAllUsers(int flags, boolean filterNoProcess) = 2;

    String[] enabledModules() = 3;

    boolean enableModule(String packageName) = 4;

    boolean disableModule(String packageName) = 5;

    boolean setModuleScope(String packageName, in ParceledListSlice<Application> scope) = 6;

    ParceledListSlice<Application> getModuleScope(String packageName) = 7;

    boolean isResourceHook() = 9;

    void setResourceHook(boolean enabled) = 10;

    boolean isVerboseLog() = 11;

    void setVerboseLog(boolean enabled) = 12;

    ParcelFileDescriptor getVerboseLog() = 16;

    ParcelFileDescriptor getModulesLog() = 17;

    int getXposedVersionCode() = 18;

    String getXposedVersionName() = 19;

    int getXposedApiVersion() = 20;

    boolean clearLogs(boolean verbose) = 21;

    PackageInfo getPackageInfo(String packageName, int flags, int uid) = 22;

    void forceStopPackage(String packageName, int userId) = 23;

    void reboot(boolean confirm, String reason, boolean wait) = 24;

    boolean uninstallPackage(String packageName) = 25;

    boolean isSepolicyLoaded() = 26;
}
