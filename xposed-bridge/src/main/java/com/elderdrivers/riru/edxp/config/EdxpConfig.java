package com.elderdrivers.riru.edxp.config;

public interface EdxpConfig {

    String getInstallerConfigPath(String suffix);

    String getDataPathPrefix();

    String getInstallerPackageName();

    String getXposedPropPath();

    String getLibSandHookName();

    boolean isDynamicModulesMode();

    boolean isNoModuleLogEnabled();

    boolean isResourcesHookEnabled();

    boolean isBlackWhiteListMode();

    String getModulesList();
}
