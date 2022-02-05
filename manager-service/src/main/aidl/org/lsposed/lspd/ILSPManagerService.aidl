package org.lsposed.lspd;

import io.github.xposed.xposedservice.utils.ParceledListSlice;
import org.lsposed.lspd.models.UserInfo;
import org.lsposed.lspd.models.Application;


interface ILSPManagerService {
    String getApi() = 1;

    ParceledListSlice<PackageInfo> getInstalledPackagesFromAllUsers(int flags, boolean filterNoProcess) = 2;

    String[] enabledModules() = 3;

    boolean enableModule(String packageName) = 4;

    boolean disableModule(String packageName) = 5;

    boolean setModuleScope(String packageName, in ParceledListSlice<Application> scope) = 6;

    ParceledListSlice<Application> getModuleScope(String packageName) = 7;

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

    void reboot(boolean shutdown) = 24;

    boolean uninstallPackage(String packageName, int userId) = 25;

    boolean isSepolicyLoaded() = 26;

    List<UserInfo> getUsers() = 27;

    int installExistingPackageAsUser(String packageName, int userId) = 28;

    boolean systemServerRequested() = 29;

    int startActivityAsUserWithFeature(in Intent intent,  int userId) = 30;

    ParceledListSlice<ResolveInfo> queryIntentActivitiesAsUser(in Intent intent, int flags, int userId) = 31;

    boolean dex2oatFlagsLoaded() = 32;

    void setHiddenIcon(boolean hide) = 33;

    void getLogs(in ParcelFileDescriptor zipFd) = 34;

    void restartFor(in Intent intent) = 35;

    void createShortcut() = 36;

    boolean isAddShortcut() = 37;

    void setAddShortcut(boolean enabled) = 38;

    oneway void flashZip(String zipPath, in ParcelFileDescriptor outputStream) = 39;

    boolean performDexOptMode(String packageName) = 40;

    List<String> getDenyListPackages() = 41;

    boolean getDexObfuscate() = 42;

    void setDexObfuscate(boolean enable) = 43;
}
