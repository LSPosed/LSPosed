package com.elderdrivers.riru.edxp.config;

import android.text.TextUtils;

public class BaseEdxpConfig implements EdxpConfig {

    @Override
    public String getInstallerConfigPath(String suffix) {
        return ConfigManager.getInstallerConfigPath(suffix != null ? suffix : "");
    }

    @Override
    public String getDataPathPrefix() {
        return ConfigManager.getDataPathPrefix();
    }

    @Override
    public String getInstallerPackageName() {
        return ConfigManager.getInstallerPackageName();
    }

    @Override
    public boolean isDynamicModulesMode() {
        return ConfigManager.isDynamicModulesEnabled();
    }

    @Override
    public boolean isResourcesHookEnabled() {
        return ConfigManager.isResourcesHookEnabled();
    }

    @Override
    public boolean isNoModuleLogEnabled() {
        return ConfigManager.isNoModuleLogEnabled();
    }

    @Override
    public boolean isBlackWhiteListMode() {
        return ConfigManager.isBlackWhiteListEnabled();
    }
}
