package com.elderdrivers.riru.edxp.whale.config;

import com.elderdrivers.riru.edxp.config.EdXpConfig;
import com.elderdrivers.riru.edxp.config.InstallerChooser;
import com.elderdrivers.riru.edxp.Main;
import com.elderdrivers.riru.edxp.whale.entry.hooker.XposedBlackListHooker;

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
        return Main.isDynamicModulesEnabled();
    }
}
