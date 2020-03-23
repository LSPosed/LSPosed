package com.elderdrivers.riru.edxp.config;

public interface EdxpConfig {

    String getInstallerBaseDir();

    boolean isDynamicModulesMode();

    boolean isNoModuleLogEnabled();

    boolean isResourcesHookEnabled();

    boolean isBlackWhiteListMode();
}
