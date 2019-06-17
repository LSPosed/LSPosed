package com.elderdrivers.riru.edxp.config;

public interface EdxpConfig {

    String getInstallerBaseDir();

    String getBlackListModulePackageName();

    boolean isDynamicModulesMode();

    boolean isResourcesHookEnabled();

    boolean isBlackWhiteListMode();
}
