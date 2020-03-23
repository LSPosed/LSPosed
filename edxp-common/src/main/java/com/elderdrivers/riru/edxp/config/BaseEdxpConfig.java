package com.elderdrivers.riru.edxp.config;

public class BaseEdxpConfig implements EdxpConfig {

    @Override
    public String getInstallerBaseDir() {
        return InstallerChooser.INSTALLER_DATA_BASE_DIR;
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
