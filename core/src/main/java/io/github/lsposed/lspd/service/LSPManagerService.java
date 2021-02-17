package io.github.lsposed.lspd.service;

import android.content.pm.PackageInfo;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import java.util.List;

import de.robv.android.xposed.XposedBridge;
import io.github.lsposed.lspd.BuildConfig;
import io.github.lsposed.lspd.ILSPManagerService;
import io.github.lsposed.lspd.utils.ParceledListSlice;

public class LSPManagerService extends ILSPManagerService.Stub {

    LSPManagerService() {
    }

    @Override
    public IBinder asBinder() {
        return this;
    }

    @Override
    public int getXposedApiVersion() {
        return XposedBridge.getXposedVersion();
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
    public ParceledListSlice<PackageInfo> getInstalledPackagesFromAllUsers(int flags) throws RemoteException {
        return PackageService.getInstalledPackagesFromAllUsers(flags);
    }

    @Override
    public String[] enabledModules() {
        return ConfigManager.getInstance().enabledModules();
    }

    @Override
    public boolean enableModule(String packageName) throws RemoteException {
        PackageInfo pkgInfo = PackageService.getPackageInfo(packageName, 0, 0);
        if (pkgInfo == null) return false;
        return ConfigManager.getInstance().enableModule(packageName, pkgInfo.applicationInfo.sourceDir);
    }

    @Override
    public boolean setModuleScope(String packageName, int[] uid) {
        return ConfigManager.getInstance().setModuleScope(packageName, uid);
    }

    @Override
    public int[] getModuleScope(String packageName) {
        return ConfigManager.getInstance().getModuleScope(packageName);
    }

    @Override
    public boolean disableModule(String packageName) {
        return ConfigManager.getInstance().disableModule(packageName);
    }

    @Override
    public boolean isResourceHook() {
        return ConfigManager.getInstance().resourceHook();
    }

    @Override
    public void setResourceHook(boolean enabled) {
        ConfigManager.getInstance().setResourceHook(enabled);
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
    public int getVariant() {
        return ConfigManager.getInstance().variant();
    }

    @Override
    public void setVariant(int variant) {
        ConfigManager.getInstance().setVariant(variant);
    }

    @Override
    public boolean isPermissive() {
        return ConfigManager.getInstance().isPermissive();
    }

    @Override
    public ParcelFileDescriptor getVerboseLog() {
        return ConfigManager.getInstance().getVerboseLog();
    }

    @Override
    public ParcelFileDescriptor getModulesLog() {
        return ConfigManager.getInstance().getModulesLog();
    }
}
