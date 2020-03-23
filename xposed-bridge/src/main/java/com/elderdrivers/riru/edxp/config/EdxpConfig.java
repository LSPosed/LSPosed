package com.elderdrivers.riru.edxp.config;

public interface EdxpConfig {

    String getInstallerConfigPath(String suffix);

    String getDataPathPrefix();

    String getInstallerPackageName();

    boolean isDynamicModulesMode();

    boolean isNoModuleLogEnabled();

    boolean isResourcesHookEnabled();

    boolean isBlackWhiteListMode();
}
