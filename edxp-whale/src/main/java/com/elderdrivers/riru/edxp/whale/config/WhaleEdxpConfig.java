package com.elderdrivers.riru.edxp.whale.config;

import com.elderdrivers.riru.edxp.config.ConfigManager;
import com.elderdrivers.riru.edxp.config.EdXpConfig;
import com.elderdrivers.riru.edxp.config.InstallerChooser;
import com.elderdrivers.riru.edxp.hooker.XposedBlackListHooker;

public class WhaleEdxpConfig implements EdXpConfig {
    @Override
    public String getInstallerBaseDir() {
        return InstallerChooser.INSTALLER_DATA_BASE_DIR;
    }

    @Override
    public String getBlackListModulePackageName() {
        return XposedBlackListHooker.BLACK_LIST_PACKAGE_NAME;
    }

    @Override
    public boolean isDynamicModulesMode() {
        return ConfigManager.isDynamicModulesEnabled();
    }

    @Override
    public boolean isResourcesHookEnabled() {
        return ConfigManager.isResourcesHookEnabled();
    }
}
